package com.dong.dongpicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.dongpicturebackend.annotation.AuthCheck;
import com.dong.dongpicturebackend.api.aliyunai.AliYunAiApi;
import com.dong.dongpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.dong.dongpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.dong.dongpicturebackend.common.BaseResponse;
import com.dong.dongpicturebackend.common.DeleteRequest;
import com.dong.dongpicturebackend.common.ResultUtils;
import com.dong.dongpicturebackend.constant.UserConstant;
import com.dong.dongpicturebackend.exception.BusinessException;
import com.dong.dongpicturebackend.exception.ErrorCode;
import com.dong.dongpicturebackend.exception.ThrowUtils;
import com.dong.dongpicturebackend.manager.auth.SpaceUserAuthContext;
import com.dong.dongpicturebackend.manager.auth.SpaceUserAuthManager;
import com.dong.dongpicturebackend.manager.auth.StpKit;
import com.dong.dongpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.dong.dongpicturebackend.manager.auth.model.SpaceUserPermission;
import com.dong.dongpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.dong.dongpicturebackend.model.dto.picture.*;
import com.dong.dongpicturebackend.model.entity.Picture;
import com.dong.dongpicturebackend.model.entity.Space;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.model.enums.PictureReviewStatusEnum;
import com.dong.dongpicturebackend.model.vo.PictureTagCategory;
import com.dong.dongpicturebackend.model.vo.PictureVO;
import com.dong.dongpicturebackend.service.PictureService;
import com.dong.dongpicturebackend.service.SpaceService;
import com.dong.dongpicturebackend.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
    private UserService userService;

    @Resource
    private PictureService pictureService;

    //    引入redis操作对象
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

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
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 执行上传
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
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
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 先判断请求是否为空以及前端传来的id是否有效
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
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
        User loginUser = userService.getLoginUser(request);
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
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        // 先判断id是否存在
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 先查询数据库，再封装
        Picture picture = pictureService.getById(id);
        // 判断是否为空
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        Long spaceId = picture.getSpaceId();
        // 校验权限
        Space space = null;
        if (spaceId != null){
            // 编程式注解鉴权
            // TODO
//            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
//            ThrowUtils.throwIf(!hasPermission, ErrorCode.NOT_AUTH_ERROR);
//            User loginUser =  userService.getLoginUser(request);
            // 已经改为使用注解鉴权
//            pictureService.checkPictureAuth(loginUser, picture);
//            space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        }
        User loginUser = userService.getLoginUser(request);
        List<String> permissionsByRole = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionsByRole);
//        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(pictureVO);
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
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
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
            // TODO
//            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
//            ThrowUtils.throwIf(!hasPermission, ErrorCode.NOT_AUTH_ERROR);
//            User loginUser = userService.getLoginUser(request);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            if (!loginUser.getId().equals(space.getUserId())){
//                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "没有空间权限");
//            }
        }

        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 转换为封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

//    /**
//     * 获取图片列表的接口，有缓存的
//     *
//     * @param pictureQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/list/page/vo/cache")
//    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
//                                                                      HttpServletRequest request) {
//        // 获取页面大小
//        long current = pictureQueryRequest.getCurrent();
//        long size = pictureQueryRequest.getPageSize();
//
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 限制普通用户只能查看审核通过的图片，逻辑是设置查询条件的审核状态是审核通过的状态
//        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//        // 查询缓存，缓存中没有再查询数据库
//        // 构建key,value,ttl
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest); // 将查询对象转为Json
//        // 进一步压缩
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        // 构建redis key, 项目+方法名+key
//        String redisKey = String.format("dongpicture:listPictureVOByPageWithCache:%s", hashKey);
//        // 获取操作对象
//        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
//        // 获取值
//        String cacheValue = opsForValue.get(redisKey);
//        if (cacheValue != null) {
//            // 说明不为空，则反序列化
//            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
//            return ResultUtils.success(cachePage);
//        }
//
//
//        // 查询数据库，查询数据库之前先查询缓存
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
//                pictureService.getQueryWrapper(pictureQueryRequest));
//        // 查询完数据库，将其写入缓存当中
//        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//        cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        // 设置缓存过期时间,5-10分钟，避免缓存雪崩
//        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
//        opsForValue.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
//
//        // 转换为封装类
//        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
//    }

//    /**
//     * 获取图片列表的接口，有缓存的
//     * 使用本地缓存
//     *
//     * @param pictureQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/list/page/vo/cache")
//    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
//                                                                      HttpServletRequest request) {
//        // 获取页面大小
//        long current = pictureQueryRequest.getCurrent();
//        long size = pictureQueryRequest.getPageSize();
//
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 限制普通用户只能查看审核通过的图片，逻辑是设置查询条件的审核状态是审核通过的状态
//        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//        // 查询缓存，缓存中没有再查询数据库
//        // 构建key,value,ttl
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest); // 将查询对象转为Json
//        // 进一步压缩
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        // 构建redis key, 项目+方法名+key
//        String cacheKey = String.format("listPictureVOByPageWithCache:%s", hashKey);
//        // 然后从本地缓存中查找
//        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
//
//        // 获取操作对象
////        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
//        // 获取值
////        String cacheValue = opsForValue.get(cacheKey);
//        if (cacheValue != null) {
//            // 说明不为空，则反序列化
//            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
//            return ResultUtils.success(cachePage);
//        }
//
//
//        // 查询数据库，查询数据库之前先查询缓存
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
//                pictureService.getQueryWrapper(pictureQueryRequest));
//        // 查询完数据库，将其写入缓存当中
//        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//        cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        // 设置缓存过期时间,5-10分钟，避免缓存雪崩
////        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
//        // 写入本地缓存
//        LOCAL_CACHE.put(cacheKey, cacheValue);
////        opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
//
//        // 转换为封装类
//        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
//    }

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

    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
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
        // 判断用户编辑权限，仅限本人或者管理员权限
        // 因为要判断编辑权限，所以方法名有request
        User loginUser = userService.getLoginUser(request);
        // 插入数据库之前补充审核参数
        pictureService.fillReviewParams(picture, loginUser);
        Long id = pictureEditRequest.getId();
        // 获取旧照片
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅限本人或管理员编辑
//        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
//        }
        // 校验权限，改为使用主内鉴权
//        pictureService.checkPictureAuth(loginUser, oldPicture);
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
        // 获取用户并调用service接口
        User loginUser = userService.getLoginUser(request);
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
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(
            @RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
            HttpServletRequest request
    ){
        // 判断是否为空
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        // 需要获取spaceId, picColor, loginUser
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();

        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOS = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOS);
    }

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request){
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request
    ) {
        // 校验
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户，调用接口
        User loginUser = userService.getLoginUser(request);
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
}
