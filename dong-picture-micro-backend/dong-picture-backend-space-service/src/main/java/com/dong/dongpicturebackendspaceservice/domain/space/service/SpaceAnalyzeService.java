package com.dong.dongpicturebackendspaceservice.domain.space.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.dongpicturebackendmodel.dto.space.analyze.*;
import com.dong.dongpicturebackendmodel.entity.Space;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.vo.space.analyze.*;

import java.util.List;

/**
 * @author by hongdou
 * @date 2025/10/12.
 * @DESC: 空间分析服务
 */
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 空间使用分析
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 空间图片分类分析
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 空间图片标签分析
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);


    /**
     * 空间图片大小分析
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);


    /**
     * 用户上传行为分析，统计用户在某个时间段内的上传图片数量
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 空间排行分析，返回前N名的空间
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
