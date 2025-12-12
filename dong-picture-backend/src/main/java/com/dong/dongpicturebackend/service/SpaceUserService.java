package com.dong.dongpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dong.dongpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.dong.dongpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.dong.dongpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
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
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserById(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员封装类列表
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);
    /**
     * 验证空间成员对象
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);
}
