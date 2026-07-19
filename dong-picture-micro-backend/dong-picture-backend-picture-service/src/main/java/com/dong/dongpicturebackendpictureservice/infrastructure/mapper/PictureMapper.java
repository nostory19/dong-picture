package com.dong.dongpicturebackendpictureservice.infrastructure.mapper;

import com.dong.dongpicturebackendmodel.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;
import java.util.Map;

public interface PictureMapper extends BaseMapper<Picture> {

    void batchUpdateThumbCount(@Param("countMap") Map<Long, Long> countMap);
}
