package com.dong.dongpicturebackendspaceservice.infrastructure.mapper;

import com.dong.dongpicturebackendmodel.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * Picture mapper for cross-table read queries from space-service.
 * Used by SpaceAnalyzeServiceImpl for analytics queries on the picture table.
 */
public interface PictureMapper extends BaseMapper<Picture> {

}
