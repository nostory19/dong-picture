package com.dong.dongpicturebackendspaceservice.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.dongpicturebackendmodel.dto.spaceuser.SpaceUserAddRequest;
import com.dong.dongpicturebackendmodel.dto.spaceuser.SpaceUserQueryRequest;
import com.dong.dongpicturebackendmodel.entity.SpaceUser;
import com.dong.dongpicturebackendmodel.vo.SpaceUserVO;

import java.util.List;

/**
* @author 25141
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-10-16 19:51:47
*/
public interface SpaceUserService extends IService<SpaceUser> {


    /**
     * 添加空间成员
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 获取查询包装类
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 通过id获取空间成员封装类
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserById(SpaceUser spaceUser);

    /**
     * 获取空间成员封装类列表
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取单个空间成员封装
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 验证空间成员对象
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);
}
