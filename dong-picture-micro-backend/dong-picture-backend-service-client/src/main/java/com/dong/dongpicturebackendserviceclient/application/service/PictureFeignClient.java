package com.dong.dongpicturebackendserviceclient.application.service;

import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendmodel.entity.Picture;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "dong-picture-backend-picture-service", path = "/inner")
public interface PictureFeignClient {

    @GetMapping("/getById")
    BaseResponse<Picture> getPictureById(@RequestParam("pictureId") Long pictureId);

    @PostMapping("/batchQuery")
    BaseResponse<List<Picture>> batchQueryPictures(@RequestBody List<Long> pictureIds);
}
