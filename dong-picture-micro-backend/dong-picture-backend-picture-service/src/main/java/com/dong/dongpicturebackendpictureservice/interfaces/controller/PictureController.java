package com.dong.dongpicturebackendpictureservice.interfaces.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.dongpicturebackendcommon.annotation.AuthCheck;
import com.dong.dongpicturebackendpictureservice.api.aliyunai.AliYunAiApi;
import com.dong.dongpicturebackendmodel.dto.ai.CreateOutPaintingTaskResponse;
import com.dong.dongpicturebackendmodel.dto.ai.GetOutPaintingTaskResponse;
import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendcommon.common.DeleteRequest;
import com.dong.dongpicturebackendcommon.common.ResultUtils;
import com.dong.dongpicturebackendcommon.constant.UserConstant;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendcommon.exception.ThrowUtils;
import com.dong.dongpicturebackendmodel.dto.picture.*;
import com.dong.dongpicturebackendmodel.entity.Picture;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.enums.PictureReviewStatusEnum;
import com.dong.dongpicturebackendmodel.vo.PictureTagCategory;
import com.dong.dongpicturebackendmodel.vo.PictureVO;
import com.dong.dongpicturebackendserviceclient.application.service.UserFeignClient;
import com.dong.dongpicturebackendpictureservice.domain.picture.service.PictureService;
import com.dong.dongpicturebackendpictureservice.infrastructure.algorithm.Item;
import com.dong.dongpicturebackendpictureservice.infrastructure.algorithm.TopK;
import com.dong.dongpicturebackendpictureservice.domain.picture.service.ThumbDomainService;
import com.dong.dongpicturebackendpictureservice.infrastructure.dco.CacheManager;
import com.dong.dongpicturebackendpictureservice.infrastructure.dco.CacheUtils;
import com.dong.dongpicturebackendpictureservice.infrastructure.dco.RankManager;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author by hongdou
 * @date 2025/6/21.
 * @DESC:
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private com.dong.dongpicturebackendserviceclient.application.service.SpaceFeignClient spaceFeignClient;

    @Resource
    private PictureService pictureService;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private TopK hotKeyDetector;

    @Resource
    private RankManager rankManager;

    @Resource
    private ThumbDomainService thumbDomainService;

    //    引入redis操作对象
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // TODO: 微服务迁移 - spaceService需要替换为SpaceFeignClient
    // @Resource
    // private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    // TODO: 微服务迁移 - spaceUserAuthManager需要迁移到space-service或使用Feign客户端
    // @Resource
    // private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();



    /**
     * @param multipartFile        传文件部分
     * @param pictureUploadRequest 自定义上传文件部分，例如图片的id
     * @param request              注意spring mvc为了防止攻击，默认限制上传文件大小是1M，需要进行配置
     * @return
     */
    // TODO: 微服务迁移 - 移除Sa-Token @SaSpaceCheckPermission，替换为统一的权限校验方案
    @PostMapping("/upload")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        // 获取登录用户
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        // 执行上传
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVO);
    }

    // TODO: 微服务迁移 - 移除Sa-Token @SaSpaceCheckPermission，替换为统一的权限校验方案
    @PostMapping("/upload/url")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request) {
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    // 完成增删改查

    /**
     * 图片删除接口
     *
     * @param request
     * @return
     */
    // TODO: 微服务迁移 - 移除Sa-Token @SaSpaceCheckPermission，替换为统一的权限校验方案
    @PostMapping("/delete")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 先判断请求是否为空以及前端传来的id是否有效
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 图片更新操作， 只有管理员可更新
     *
     * @param pictureUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        // 判断更新是否为空，以及id
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将请求转换为实体
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 将list转换为json
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        // 找出旧照片
        Picture oldPicture = pictureService.getById(id);

        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        // 插入数据库之前补充审核参数
        pictureService.fillReviewParams(oldPicture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据Id获取图片，仅管理员可用
     *
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片VO
     *
     * @param id
     * @param request
     * @return
     */
    // 注意这里不用鉴权，因为是用户查看图片，而不是管理员查看图片，不然会强制用户登录
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        // 先判断id是否存在
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 先查询数据库，再封装
        Picture picture = pictureService.getById(id);
        // 判断是否为空
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        Long spaceId = picture.getSpaceId();
        if (spaceId != null){
            // TODO: 微服务迁移 - 权限校验通过Feign调用space-service
        }
        // 记录访问（热点检测）
        String picKey = CacheUtils.getPictureCacheKey(String.valueOf(id));
        hotKeyDetector.add(picKey, 1);

        User loginUser = getLoginUser(request);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        // 权限列表
        List<String> permissionList = new java.util.ArrayList<>();
        if (loginUser != null && loginUser.getId().equals(picture.getUserId())) {
            // 图片所有者始终有编辑权限
            permissionList.add("picture:edit");
            permissionList.add("picture:delete");
            permissionList.add("picture:upload");
        } else if (loginUser != null && picture.getSpaceId() != null) {
            // 团队空间：检查用户是否为团队成员
            try {
                com.dong.dongpicturebackendcommon.common.BaseResponse<List<String>> resp =
                        spaceFeignClient.getPermissionList(picture.getSpaceId(), loginUser.getId());
                if (resp != null && resp.getData() != null) {
                    permissionList.addAll(resp.getData());
                }
            } catch (Exception e) {
                log.warn("SpaceFeignClient.getPermissionList failed, fallback to owner-only", e);
            }
        }
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/ranking/init")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> initRanking() {
        List<Picture> allPics = pictureService.list();
        int count = 0;
        for (Picture pic : allPics) {
            // 排行榜只加载公共图库中审核通过的图片
            if (pic.getSpaceId() != null || !(PictureReviewStatusEnum.PASS.getValue() == pic.getReviewStatus())) {
                continue;
            }
            if (pic.getCreateTime() != null) {
                rankManager.addToTimeRank(pic.getId(), pic.getCreateTime().getTime());
            }
            rankManager.updateThumbRank(pic.getId(), pic.getThumbCount() != null ? pic.getThumbCount() : 0);
            count++;
        }
        return ResultUtils.success("初始化完成，共 " + count + " 条");
    }

    @GetMapping("/hot")
    public BaseResponse<List<String>> getHotPictureIds() {
        List<Item> hotItems = hotKeyDetector.list();
        List<String> hotPictureIds = hotItems.stream()
                .map(item -> item.key().replace(CacheUtils.PICTURE_CACHE + ":", ""))
                .collect(java.util.stream.Collectors.toList());
        return ResultUtils.success(hotPictureIds);
    }

    /**
     * 分页获取图片列表，没有脱敏，因此是管理员权限
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 直接调用接口并返回
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表，由于是用户查看列表，需要返回封装类
     *
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        // 获取页面大小
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null){
            // 如果为空则说明是公共图库
            // 限制普通用户只能查看审核通过的图片，逻辑是设置查询条件的审核状态是审核通过的状态
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }else{
            // 私有空间
            // TODO: 微服务迁移 - Sa-Token已移除，需要替换为统一的权限校验方案
        }

        String sortField = pictureQueryRequest.getSortField();
        boolean useRank = "thumbCount".equals(sortField) || "createTime".equals(sortField);

        // 排行榜查询：ZSET → 批量查 MySQL
        if (useRank && spaceId == null) {
            List<Long> rankedIds;
            if ("thumbCount".equals(sortField)) {
                rankedIds = rankManager.getHotRankedIds(current, size);
            } else {
                rankedIds = rankManager.getLatestRankedIds(current, size);
            }
            if (!rankedIds.isEmpty()) {
                List<Picture> pictures = pictureService.listByIds(rankedIds);
                Map<Long, Picture> pictureMap = new HashMap<>();
                for (Picture p : pictures) {
                    // 过滤：公共图库只展示审核通过且spaceId为空的图片
                    if (p.getSpaceId() == null && PictureReviewStatusEnum.PASS.getValue() == p.getReviewStatus()) {
                        pictureMap.put(p.getId(), p);
                    }
                }
                List<PictureVO> voList = new ArrayList<>();
                for (Long id : rankedIds) {
                    Picture p = pictureMap.get(id);
                    if (p != null) {
                        voList.add(pictureService.getPictureVO(p, request));
                    }
                }
                long total = "thumbCount".equals(sortField) ?
                        rankManager.getThumbRankCount() : rankManager.getTimeRankCount();
                Page<PictureVO> result = new Page<>(current, size, total);
            result.setRecords(voList);
            setThumbStateForList(voList, request);
            return ResultUtils.success(result);
            }
            return ResultUtils.success(new Page<>(current, size, 0));
        }

        // 公共图库走多级缓存
        if (spaceId == null) {
            String cacheKey = CacheUtils.getPictureQueryCacheKey(pictureQueryRequest);
            Object cachedValue = cacheManager.getValueCache(cacheKey);
            if (cachedValue != null) {
                Page<PictureVO> cached = (Page<PictureVO>) cachedValue;
                setThumbStateForList(cached.getRecords(), request);
                return ResultUtils.success(cached);
            }
            Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                    pictureService.getQueryWrapper(pictureQueryRequest));
            Page<PictureVO> result = pictureService.getPictureVOPage(picturePage, request);
            setThumbStateForList(result.getRecords(), request);
            cacheManager.putValueToCache(cacheKey, result);
            return ResultUtils.success(result);
        }

        // 私有空间直接查库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> result = pictureService.getPictureVOPage(picturePage, request);
        setThumbStateForList(result.getRecords(), request);
        return ResultUtils.success(result);
    }

    /**
     * 获取图片列表的接口，有缓存的
     * 使用多级缓存
     *
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {
        // 下面缓存设置的逻辑实际上应该写在service中
        // 获取页面大小
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 限制普通用户只能查看审核通过的图片，逻辑是设置查询条件的审核状态是审核通过的状态
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 查询缓存，缓存中没有再查询数据库
        // 构建key,value,ttl
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest); // 将查询对象转为Json
        // 进一步压缩
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        // 构建redis key, 项目+方法名+key
        String cacheKey = String.format("dongpicture:listPictureVOByPageWithCache:%s", hashKey);
        // 先查本地缓存
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cacheValue != null){
            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachePage);
        }
        // 本地缓存没有命中，查询分布式缓存，获取操作对象
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        // 获取值
        cacheValue = opsForValue.get(cacheKey);
        if (cacheValue != null) {
            // 还要更新本地缓存
            LOCAL_CACHE.put(cacheKey, cacheValue);
            // 说明不为空，则反序列化
            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachePage);
        }


        // 查询数据库，查询数据库之前先查询缓存
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 查询完数据库，将其写入缓存当中
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 设置缓存过期时间,5-10分钟，避免缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        // 写入本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        // 写入分布式缓存
        opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        // 转换为封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    // TODO: 微服务迁移 - 移除Sa-Token @SaSpaceCheckPermission，替换为统一的权限校验方案
    @PostMapping("/edit")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        // 判断是否为空
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将前端DTO转换为实体类
        // 新建一个实体
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 将tags的list转换为json
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        pictureService.validPicture(picture);
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        // 插入数据库之前补充审核参数
        pictureService.fillReviewParams(picture, loginUser);
        Long id = pictureEditRequest.getId();
        // 获取旧照片
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库进行编辑
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        // 判断是否为空
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        // 校验
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    // TODO: 微服务迁移 - 移除Sa-Token @SaSpaceCheckPermission，替换为统一的权限校验方案
    @PostMapping("/search/color")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(
            @RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
            HttpServletRequest request
    ){
        // 判断是否为空
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        // 需要获取spaceId, picColor, loginUser
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();

        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        List<PictureVO> pictureVOS = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOS);
    }

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    // TODO: 微服务迁移 - 移除Sa-Token @SaSpaceCheckPermission，替换为统一的权限校验方案
    @PostMapping("/edit/batch")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request){
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建扩图任务
     */
    // TODO: 微服务迁移 - 移除Sa-Token @SaSpaceCheckPermission，替换为统一的权限校验方案
    @PostMapping("/out_painting/create_task")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request
    ) {
        // 校验
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // TODO: 微服务迁移 - 使用UserFeignClient替换userService
        User loginUser = getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 获取扩图任务
     * @param taskId 通过id查询该任务
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId){
        // 判断是否为空
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        log.info("taskId:{}", taskId);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

    private void setThumbStateForList(List<PictureVO> voList, HttpServletRequest request) {
        try {
            User user = getLoginUser(request);
            if (user != null && voList != null && !voList.isEmpty()) {
                thumbDomainService.getPictureThumbState(voList, user);
            }
        } catch (Exception e) {
            // 未登录用户无需设置点赞状态
            if (voList != null) {
                for (PictureVO vo : voList) {
                    vo.setHasThumb(false);
                }
            }
        }
    }

    private User getLoginUser(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (StrUtil.isBlank(userIdStr) || "null".equals(userIdStr)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        Long userId = Long.valueOf(userIdStr);
        User user = userFeignClient.getUserById(userId).getData();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
        return user;
    }
}
