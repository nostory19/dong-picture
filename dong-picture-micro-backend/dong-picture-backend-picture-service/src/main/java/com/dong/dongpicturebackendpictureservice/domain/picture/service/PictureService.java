package com.dong.dongpicturebackendpictureservice.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.dongpicturebackendmodel.dto.ai.CreateOutPaintingTaskRequest;
import com.dong.dongpicturebackendmodel.dto.ai.CreateOutPaintingTaskResponse;
import com.dong.dongpicturebackendmodel.dto.picture.*;
import com.dong.dongpicturebackendmodel.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 25141
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-06-20 21:20:41
*/
public interface PictureService extends IService<Picture> {

    /**
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 分页查询接口，需要根据用户传入的参数来构造SQL查询，
     * 转换为MyBatis plus框架，就不用自己拼接SQL了，而是通过QueryWrapper对象生成SQL查询。
     * @param pictureQueryRequest
     * @return
     */
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片对象转封装类
     * @param picture
     * @param request
     * @return
     */
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     * @param picturePage
     * @param request
     * @return
     */
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage,
                                            HttpServletRequest request);

    /**
     * 图片校验
     * @param picture
     */
    public void validPicture(Picture picture);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充Review信息的公共方法
     * @param picture
     * @param loginUser
     */
    public void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     * @param pictureUploadByBatchRequest
     * @param loginUser 只有管理员才能用这个功能
     * @return
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 清理图片文件
     * @param oldPicture
     */
    void cleanPictureFile(Picture oldPicture);

    /**
     * 通用方法，校验当前用户能不能看到这个图片
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    void deletePicture(long pictureId, User loginUser);

    /**
     * 根据颜色搜索图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建扩图任务
     * @param createOutPaintingTaskRequest
     * @param loginUser
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createOutPaintingTaskRequest, User loginUser);
}
