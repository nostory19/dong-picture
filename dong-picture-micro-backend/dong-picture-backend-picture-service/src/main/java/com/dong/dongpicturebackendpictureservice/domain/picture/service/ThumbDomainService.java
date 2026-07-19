package com.dong.dongpicturebackendpictureservice.domain.picture.service;

import com.dong.dongpicturebackendmodel.entity.User;
import com.dong.dongpicturebackendmodel.vo.PictureVO;
import java.util.List;

public interface ThumbDomainService {
    boolean doThumb(Long pictureId, User loginUser);
    boolean undoThumb(Long pictureId, User loginUser);
    boolean hasThumb(Long userId, Long pictureId);
    void getPictureThumbState(List<PictureVO> pictureVOList, User loginUser);
}
