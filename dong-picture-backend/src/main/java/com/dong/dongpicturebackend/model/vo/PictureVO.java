package com.dong.dongpicturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.dong.dongpicturebackend.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import springfox.documentation.spring.web.json.Json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/6/20.
 * @DESC: 用作返回给前端封装的图片信息
 */
@Data
public class PictureVO implements Serializable {
    /**
     * id
     * 生成更长数量的id，防止别人爬虫
     */
    private Long id;

    /**
     * 图片url
     */
    private String url;

    /**
     * 缩略图Url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（Json数组）
     * 数据库存储的是json类型
     * 为了返回给前端方便，将tags转换为List
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 额外返回是哪个用户上传的，用户的具体信息
     */
    private UserVO userVO;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 增加返回权限列表
     */
    private List<String> permissionList = new ArrayList<>();


    private static final long serialVersionID = 1L;


    /**
     * vo转实体类
     * @param pictureVO
     * @return
     */
    public static Picture voToObj(PictureVO pictureVO){
        // 如何转，直接复制一遍属性就可以了
        if (pictureVO == null){
            return null;
        }
        // 复制属性
        // 创建实体类
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);
        // 处理tags,将列表转换为json
        picture.setTags(JSONUtil.toJsonStr(picture.getTags()));
        return picture;
    }


    /**
     * 将对象转换为封装类
     * @param picture
     * @return
     */
    public static PictureVO objToVO(Picture picture){
        if (picture == null){
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);
        // 处理tags，将json转换为List
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }
}
