package com.dong.dongpicturebackend.mapper;

import com.dong.dongpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mapstruct.Mapper;

/**
* @author 25141
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2025-06-20 21:20:41
* @Entity com.dong.dongpicturebackend.model.entity.Picture
*/
@Mapper
public interface PictureMapper extends BaseMapper<Picture> {

}




