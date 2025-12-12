package com.dong.dongpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.dong.dongpicturebackend.exception.ErrorCode;
import com.dong.dongpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/7/6.
 * @DESC: 文件上传
 */
// 打上Service生成Bean注解
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;

        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 校验文件大小
        long fileSize = multipartFile.getSize(); // 以字节为单位
        final long ONE_M = 1024 * 1024; // 定义1M
        ThrowUtils.throwIf(fileSize > 8 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过8MB");
        // 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());// 根据文件原始名获取后缀
// 设置允许上传的文件列表
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }
}
