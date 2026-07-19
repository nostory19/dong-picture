package com.dong.dongpicturebackendspaceservice.domain.space.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendcommon.exception.ThrowUtils;
import com.dong.dongpicturebackendmodel.dto.spaceuser.SpaceUserAddRequest;
import com.dong.dongpicturebackendmodel.dto.spaceuser.SpaceUserQueryRequest;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.entity.SpaceUser;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.enums.SpaceRoleEnum;
import com.dong.dongpicturebackendmodel.vo.SpaceUserVO;
import com.dong.dongpicturebackendmodel.vo.SpaceVO;
import com.dong.dongpicturebackendmodel.vo.UserVO;
import com.dong.dongpicturebackendserviceclient.application.service.UserFeignClient;
import com.dong.dongpicturebackendspaceservice.domain.space.service.SpaceService;
import com.dong.dongpicturebackendspaceservice.domain.space.service.SpaceUserService;
import com.dong.dongpicturebackendspaceservice.infrastructure.mapper.SpaceUserMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 25141
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-10-16 19:51:47
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{
    @Resource
    private UserFeignClient userFeignClient;

    // 添加Lazy，避免循环依赖，让注解延迟加载
    @Resource
    @Lazy
    private SpaceService spaceService;
    // 需要加@Lazy，不然会造成循环依赖

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        // dto转换为entity
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);
        // 数据库添加
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId(); // 返回成功后的id

    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null){
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();

        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);

        return queryWrapper;
    }

    @Override
    public SpaceUserVO getSpaceUserById(SpaceUser spaceUser) {
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);// 对象转封装类
        // 关联查询用户信息和空间信息
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        if (userId != null && userId > 0){
            // TODO: 微服务中通过Feign调用user-service获取用户信息
            User user = userFeignClient.getUserById(userId).getData();
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            spaceUserVO.setUser(userVO);
        }
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space);
            spaceUserVO.setSpace(spaceVO);
        }

        return spaceUserVO;

    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 多个spaceUser进行查询，并返回封装类
        if (CollUtil.isEmpty(spaceUserList)){
            return Collections.emptyList();
        }
        // 对象列表 =》 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo)
                .collect(Collectors.toList());
        // 收集关联查询的用户ID和空间ID
        Set<Long> userIdSet = spaceUserList.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toSet());
        // 批量查询用户和空间
        // TODO: 微服务中通过Feign调用user-service批量获取用户信息
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listUserByIds(
                new java.util.ArrayList<>(userIdSet)).getData().stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 填充用户和空间信息
        // 遍历spaceUserList
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 通过userIdSet和spaceIdSet获取对应的用户和空间
            User user = null;
            if (userIdUserListMap.containsKey(userId)){
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userFeignClient.getUserVOById(user.getId()).getData());
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)){
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;

    }



    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            // TODO: 微服务中通过Feign调用user-service获取用户信息
            User user = userFeignClient.getUserById(userId).getData();
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }


    /**
     * 验证空间成员对象
     * @param spaceUser
     * @param add add参数用来区分是创建数据时校验还是编辑时校验
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add){
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，spaceId和userId不能为空
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add){
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            // 然后分别校验space和user是否存在
            // TODO: 微服务中通过Feign调用user-service验证用户是否存在
            User user = userFeignClient.getUserById(userId).getData();
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 空间角色不为空且不合法
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null){
            // 说明不合法
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }
}
