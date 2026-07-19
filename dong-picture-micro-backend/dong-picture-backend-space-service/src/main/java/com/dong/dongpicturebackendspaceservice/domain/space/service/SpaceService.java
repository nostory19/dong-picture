package com.dong.dongpicturebackendspaceservice.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.dongpicturebackendmodel.dto.space.SpaceAddRequest;
import com.dong.dongpicturebackendmodel.dto.space.SpaceQueryRequest;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.vo.SpaceVO;

/**
* @author 25141
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-08-19 16:21:01
*/
public interface SpaceService extends IService<Space> {

    /**
     * 分页查询接口，需要根据用户传入的参数来构造SQL查询，
     * 转换为MyBatis plus框架，就不用自己拼接SQL了，而是通过QueryWrapper对象生成SQL查询。
     * @param spaceQueryRequest
     * @return
     */
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 空间对象转封装类
     * @param space
     * @return
     */
    // TODO: 在微服务架构中，request中的用户信息由网关通过header传递，此处不再依赖HttpServletRequest
    // 原签名: public SpaceVO getSpaceVO(Space space, HttpServletRequest request);
    public SpaceVO getSpaceVO(Space space);

    /**
     * 分页获取空间封装
     * @param spacePage
     * @return
     */
    // TODO: 在微服务架构中，request中的用户信息由网关通过header传递，此处不再依赖HttpServletRequest
    // 原签名: public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

    /**
     * 空间校验
     * @param space
     * @param add 是否为创建时校验,true则是创建时候的校验
     */
    public void validSpace(Space space, boolean add);


    /**
     * 根据空间对象的级别填充当前空间最大容量
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间，返回创建好的空间的id
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间权限，校验该用户是否有该空间的权限
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(Space space, User loginUser);
}
