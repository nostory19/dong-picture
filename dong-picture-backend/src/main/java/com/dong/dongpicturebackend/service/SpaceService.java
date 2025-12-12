package com.dong.dongpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.dongpicturebackend.model.dto.space.SpaceAddRequest;
import com.dong.dongpicturebackend.model.dto.space.SpaceQueryRequest;
import com.dong.dongpicturebackend.model.entity.Space;
import com.dong.dongpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

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
     * @param request
     * @return
     */
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 分页获取空间封装
     * @param spacePage
     * @param request
     * @return
     */
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage,
                                            HttpServletRequest request);

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

    SpaceVO getSpaceVO(Space space);
}
