package com.dong.dongpicturebackend.model.vo;

import com.dong.dongpicturebackend.model.entity.SpaceUser;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author by hongdou
 * @date 2025/10/16.
 * @DESC: 空间成员视图包装类，
 * 还可以额外关联空间信息和创建空间的用户信息
 * 编写SpaceUser实体类和VO类转换方法
 */
@Data
public class SpaceUserVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 用户id
     */
    private Long userId;


    /**
     * 空间角色：viewer-浏览者，editor-编辑者，admin-管理员
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 空间信息
     */
    private SpaceVO space;

    public static final long serialVersionUID = 1L;

    // 封装转换类对象

    /**
     * 视图类转实体类
     *
     * @param spaceUserVO
     * @return
     */
    public static SpaceUser voToObj(SpaceUserVO spaceUserVO) {
        if (spaceUserVO == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVO, spaceUser);
        return spaceUser;
    }

    /**
     * 实体类转视图类
     *
     * @param spaceUser
     * @return
     */
    public static SpaceUserVO objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;

        }
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser, spaceUserVO);
        return spaceUserVO;
    }
}
