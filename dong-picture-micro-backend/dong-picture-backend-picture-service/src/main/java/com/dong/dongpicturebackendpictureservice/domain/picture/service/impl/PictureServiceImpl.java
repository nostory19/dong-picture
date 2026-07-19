package com.dong.dongpicturebackendpictureservice.domain.picture.service.impl;

import java.awt.*;
import java.io.IOException;
import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.dongpicturebackendpictureservice.api.aliyunai.AliYunAiApi;
import com.dong.dongpicturebackendpictureservice.infrastructure.dco.RankManager;
import com.dong.dongpicturebackendmodel.dto.ai.CreateOutPaintingTaskRequest;
import com.dong.dongpicturebackendmodel.dto.ai.CreateOutPaintingTaskResponse;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendcommon.exception.ThrowUtils;
import com.dong.dongpicturebackendpictureservice.infrastructure.manager.CosManager;
import com.dong.dongpicturebackendpictureservice.infrastructure.manager.FileManager;
import com.dong.dongpicturebackendpictureservice.infrastructure.manager.upload.FilePictureUpload;
import com.dong.dongpicturebackendpictureservice.infrastructure.manager.upload.PictureUploadTemplate;
import com.dong.dongpicturebackendpictureservice.infrastructure.manager.upload.UrlPictureUpload;
import com.dong.dongpicturebackendmodel.dto.file.UploadPictureResult;
import com.dong.dongpicturebackendmodel.dto.picture.*;
import com.dong.dongpicturebackendmodel.entity.Picture;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.enums.PictureReviewStatusEnum;
import com.dong.dongpicturebackendmodel.vo.PictureVO;
import com.dong.dongpicturebackendmodel.vo.UserVO;
import com.dong.dongpicturebackendpictureservice.domain.picture.service.PictureService;
import com.dong.dongpicturebackendpictureservice.infrastructure.mapper.PictureMapper;
import com.dong.dongpicturebackendcommon.utils.ColorSimlarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 25141
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-06-20 21:20:41
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private FileManager fileManager;

    // TODO: 微服务迁移 - userService需要替换为UserFeignClient
    // TODO: 微服务迁移 - UserService is in user-service module, replace with UserFeignClient
    // @Resource
    // private UserService userService;

    // TODO: 微服务迁移 - spaceService需要替换为SpaceFeignClient
    // TODO: 微服务迁移 - SpaceService is in space-service module, replace with SpaceFeignClient
    // @Resource
    // private SpaceService spaceService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Autowired
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AliYunAiApi aliYunAiApi;

    @Resource
    private RankManager rankManager;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        // 先判断用户是否登录
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR, "用户未登录");
        // 校验空间是否存在，检验是否有空间权限
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            // 判断空间是否存在
            // TODO: 微服务迁移 - 使用SpaceFeignClient替换spaceService
            // Space space = spaceService.getById(spaceId);
            // ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 改为使用统一的权限校验，不能仅仅是空间的创建人能创建图片，当为团队空间，协作者也能编辑
            // 追加校验额度
            // if (space.getTotalCount() >= space.getMaxCount()) {
            //     throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不够");
            // }
            // if (space.getTotalSize() >= space.getMaxSize()) {
            //     throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            // }
        }
        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            // 添加完审核功能后，这里进行了优化，如果图片更新了，记得将审核状态进行更改
            Picture oldPicture = this.getById(pictureId); // 获取老图片
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 校验空间是否一致
            // 没传spaceId直接复用原来的spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() == null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id不一致");
                }
            }
        }
        // 上传图片
        // 这里的前缀给公共用户的前缀，再按用户进行划分
        // 将按照用户划分目录修改为按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 则是公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 私有空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据参数类型，判断使用哪种upload方法，实现url上传和文件上传的兼容
        PictureUploadTemplate pictureUploadTemple = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemple = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemple.uploadPicture(inputSource, uploadPathPrefix);
        // 构造入库的Picture对象
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        // 还要对返回的结果写入数据中
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 构造名称的时候，获取到手动设置的图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        // 补充图片主色调
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());

        // 插入数据库之前补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 保存数据库
        // 还是要区分是更新还是创建
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            // 如果是公共图库则不需要更新空间额度
            // TODO: 微服务迁移 - 使用SpaceFeignClient.increaseUsage替换spaceService
            // if (finalSpaceId != null) {
            //     boolean update = spaceService.lambdaUpdate()
            //             .eq(Space::getId, finalSpaceId)
            //             .setSql("totalSize = totalSize + " + picture.getPicSize())
            //             .setSql("totalCount = totalCount + 1")
            //             .update();
            //     ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            // }
            return picture;
        });
        // 新增公共图库图片加入排行榜
        if (pictureId == null && spaceId == null) {
            rankManager.addToTimeRank(picture.getId(), picture.getCreateTime().getTime());
            rankManager.updateThumbRank(picture.getId(), picture.getThumbCount() != null ? picture.getThumbCount() : 0);
        }
        return PictureVO.objToVO(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();

        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewerId = pictureQueryRequest.getReviewerId();
        Date reviewTime = pictureQueryRequest.getReviewTime();

        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 从多字段中进行搜索
        // SearchTest支持同时从name和introduction中检索，可以用queryWrapper的or语法构造查询条件
        if (StrUtil.isNotBlank(searchText)) {
            // 拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }

        // 然后完善queryWrapper
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        // json数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取单个图片的封装
     *
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVO(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();

        if (userId != null && userId > 0) {
            // TODO: 微服务迁移 - 使用UserFeignClient替换userService
            // User user = userService.getById(userId);
            // UserVO userVO = userService.getUserVO(user);
            // pictureVO.setUserVO(userVO);
        }

        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 对分页中的picture进行转换为VO类，
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 如果分页列表为空直接返回
        // 将对象列表转换为封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVO).collect(Collectors.toList());
        // 关联查询用户信息
        // 先设置一个用户id和对应用户的映射，例如1-user1, 2-user2
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        // Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
        //         .collect(Collectors.groupingBy(User::getId));
        // //填充信息
        // pictureVOList.forEach(pictureVO -> {
        //     Long userId = pictureVO.getUserId();
        //     User user = null;
        //     if (userIdUserListMap.containsKey(userId)) {
        //         user = userIdUserListMap.get(userId).get(0);
        //     }
        //     pictureVO.setUserVO(userService.getUserVO(user));
        // });

        // 获取每一条数据并转换
        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        // 先判断图片是否为空
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时,id不能为空，
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * @param pictureReviewRequest 前端发来的审核请求，按理来说申请修改审核状态，应该就是从待审核变成通过和拒绝，
     *                             所以不能再是待审核状态
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {

        // 校验参数
        // 获取用户ID
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断图片是否存在
        Picture oldPicture = this.getById(id); // 获取图片
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 如果已经是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 判断是否重复
        // 数据库操作
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

    }


    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 如果当前登录用户是管理员角色，则自动过审
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        // if (userService.isAdmin(loginUser)) {
        //     // 管理员自动过审
        //     // 更新review相关的四个字段
        //     picture.setReviewMessage("管理员自动过审");
        //     picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //     picture.setReviewerId(loginUser.getId());
        //     picture.setReviewTime(new Date());
        // } else {
        //     // 反之则设置为待审状态
        //     picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        // }
        // 暂时默认设置为待审状态
        picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 获取信息
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        // 校验参数
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条");
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");

        }

        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }

        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0; // 统计上传数量
        // 逐条处理
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过： {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题，例如特殊字符
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex); // ?之后的都不要
            }
            // 获取图片名称
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            if (StrUtil.isBlank(namePrefix)) {
                namePrefix = searchText; // 如果前缀为空，则设置为搜索词
            }

            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                // 名称+序号，便于区分
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功， id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break; // 超过限制的爬取数量->停止
            }
        }
        return uploadCount; // 返回成功上传的图片数量。
    }

    // 加上Async注解，异步执行
    // 在项目中开启异步支持
    @Async
    @Override
    public void cleanPictureFile(Picture oldPicture) {
        // 删除图片文件
        // 判断图片是否有多条记录
        // 获取图片地址
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 判断个数
        if (count > 1) {
            return;
        }
        // 多条记录则不删除

        // 删除原始图片
        cosManager.deleteObject(oldPicture.getUrl());
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long loginUserId = loginUser.getId();
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 则是公共图库
            // TODO: 微服务迁移 - 使用UserFeignClient替换userService
            // if (!picture.getUserId().equals(loginUserId) && userService.isAdmin(loginUser)) {
            //     throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            // }
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
        } else {
            // 则是空间权限
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);

        // 判断数据库中是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        transactionTemplate.execute(status -> {
            // 操作数据库，删除图片
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放额度
            // 注意上传图片和删除图片的空间额度更新一定要判断是否存在空间id，不然就是公共图库，不用更新额度
            // TODO: 微服务迁移 - 使用SpaceFeignClient替换spaceService
            // if (oldPicture.getSpaceId() != null) {
            //     boolean update = spaceService.lambdaUpdate()
            //             .eq(Space::getId, oldPicture.getSpaceId())
            //             .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
            //             .setSql("totalCount = totalCount - 1")
            //             .update();
            //     ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            // }
            return true;
        });
        // 清理对象存储的图片
        this.cleanPictureFile(oldPicture);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        // 校验空间权限
        // TODO: 微服务迁移 - 使用SpaceFeignClient替换spaceService
        // Space space = spaceService.getById(spaceId);
        // ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // if (!loginUser.getId().equals(space.getUserId())) {
        //     throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "没有空间权限");
        // }
        // 计算该空间下的图片主色调
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 必须要求主色调，没有图片则返回
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        // 将颜色字符串转换为主色调
        Color targetColor = Color.decode(picColor);
        // 计算相似度
        List<Picture> sortedPictureList = pictureList.stream(

                ).sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    // 十六进制，判断是否为空
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }

                    Color pictureColor = Color.decode(hexColor);
                    // 计算相似度
                    return -ColorSimlarUtils.calculateColorSimilarity(targetColor, pictureColor);


                })).limit(12)
                .collect(Collectors.toList());
        // 返回结果
        return sortedPictureList.stream().map(
                        PictureVO::objToVO)
                .collect(Collectors.toList());

    }

    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 获取数据
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        // 获取空间id
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        // 获取分类
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        // 判断是否为空
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        // 校验空间权限
        // 获取空间
        // TODO: 微服务迁移 - 使用SpaceFeignClient替换spaceService
        // Space space = spaceService.getById(spaceId);
        // ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // if (!loginUser.getId().equals(space.getUserId())){
        //     throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "没有空间权限");
        // }
        // 执行更新
        // 先查询，只选择需要的字段
        List<Picture> pictureList = this.lambdaQuery().select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.isEmpty()){
            return;
        }

        // 然后更新
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)){
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)){
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 还要更新命名，
        // 获取名称规则
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);
        // 批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        // 判断是否有空存在，有空则抛出异常
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId)).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验，改为使用注解鉴权
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }


    /**
     * nameRule格式： 图片{序号}
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        // 判断参数
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)){
            return;
        }
        // 然后重命名
        long count = 1;
        try{
            for (Picture picture : pictureList){
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        }catch (Exception e){
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }
}
