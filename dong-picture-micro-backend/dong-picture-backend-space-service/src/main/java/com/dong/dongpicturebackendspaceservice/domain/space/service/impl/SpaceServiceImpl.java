package com.dong.dongpicturebackendspaceservice.domain.space.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.dongpicturebackendcommon.constant.UserConstant;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendcommon.exception.ThrowUtils;
import com.dong.dongpicturebackendmodel.dto.space.SpaceAddRequest;
import com.dong.dongpicturebackendmodel.dto.space.SpaceQueryRequest;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.entity.SpaceUser;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.enums.SpaceLevelEnum;
import com.dong.dongpicturebackendmodel.enums.SpaceRoleEnum;
import com.dong.dongpicturebackendmodel.enums.SpaceTypeEnum;
import com.dong.dongpicturebackendmodel.vo.SpaceVO;
import com.dong.dongpicturebackendmodel.vo.UserVO;
import com.dong.dongpicturebackendserviceclient.application.service.UserFeignClient;
import com.dong.dongpicturebackendspaceservice.domain.space.service.SpaceService;
import com.dong.dongpicturebackendspaceservice.domain.space.service.SpaceUserService;
import com.dong.dongpicturebackendspaceservice.infrastructure.mapper.SpaceMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 25141
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-08-19 16:21:01
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private TransactionTemplate transactionTemplate;


    @Resource
    private SpaceUserService spaceUserService;

    // 为了方便部署，可选
//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    public SpaceServiceImpl(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Long userId = spaceQueryRequest.getUserId();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();

        // 然后完善queryWrapper
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "name", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);


        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"),
                sortField);
        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        // 转换为视图封装类，由于关联了用户信息，因此需要查询用户信息
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            // TODO: 微服务中通过Feign调用user-service获取用户信息
            User user = userFeignClient.getUserById(userId).getData();
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        // 对分页中的space进行转换为VO类，
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 如果分页列表为空直接返回
        // 将对象列表转换为封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 关联查询用户信息
        // 先设置一个用户id和对应用户的映射，例如1-user1, 2-user2
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // TODO: 微服务中通过Feign调用user-service批量获取用户信息
        // 然后将用户信息写进去
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listUserByIds(new ArrayList<>(userIdSet)).getData()
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userFeignClient.getUserVOById(user.getId()).getData());
        });
        // 获取每一条数据并转换
        spaceVOPage.setRecords(spaceVOList);

        return spaceVOPage;
    }

    @Override
    public void validSpace(Space space, boolean add) {
        // 判断是否为空
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 获取对象值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        // 将级别转换为枚举对象，根据值获得对象
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 新增空间类别的校验
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        // 创建时校验
        if (add) {
            // 名称不能为空
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceType == null){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }

        }

        // 修改的校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能太长");
        }

        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }

    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 获取空间级别
        Integer spaceLevel = space.getSpaceLevel();
        // 转换为枚举类
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (spaceLevelEnum != null) {
            // 填充
            long maxSize = spaceLevelEnum.getMaxSize();
            // 如果管理员设置了最大值，则使用设置的最大值，反之则默认
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1、填充默认参数、
        // 将DTO转换为实体类
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 填充默认值
        if (StrUtil.isBlank(space.getSpaceName())) {
            // 如果为空，设置为默认空间
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null){
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充容量
        this.fillSpaceBySpaceLevel(space);
        // 2、校验参数、
        this.validSpace(space, true);
        // 3、校验权限（非管理员只能创建普通空间）、
        // 获取登录用户id
        Long userId = loginUser.getId();
        // 填充用户id
        space.setUserId(userId);
        // 如果当前创建的不是普通空间并且当前登录用户不是管理员
        // TODO: 微服务中通过检查用户userRole字段判断是否为管理员，替代原来的userService.isAdmin()
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        // 4、控制统一用户只能创建一个私有空间（使用 加锁+事务的方式实现）
        // 同时也保证只能创建一个团队空间，只需要补充一个spaceType即可
        // 不同用户可以同时执行，每个用户可以有自己的一把锁
        // intern，对字符串常量池进行加锁
        String lock = String.valueOf(userId).intern(); // 根据用户id生成锁
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 被锁住的代码，统一用户假设点了两次，也会一次次执行
                // 判断是否已有空间，当前空间的用户id是否等于当前登录的用户id
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();

                // 已有空间，不能创建
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建一个");
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间失败");
                // 如果是团队空间，则需要将创建者加入到空间成员中，作为管理员
                if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()){
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");

                }
                // 创建分表（仅对团队空间生效）
//                dynamicShardingManager.createSpacePictureTable(space);
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }

    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        // 仅本人或者登录用户是管理员才有该空间的权限
        // TODO: 微服务中通过检查用户userRole字段判断是否为管理员，替代原来的userService.isAdmin()
        if (!space.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }
    }
}
