package com.dong.dongpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.dong.dongpicturebackend.config.CosClientConfig;

import com.dong.dongpicturebackend.exception.BusinessException;
import com.dong.dongpicturebackend.exception.ErrorCode;
import com.dong.dongpicturebackend.exception.ThrowUtils;
import com.dong.dongpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/6/19.
 * @DESC: manager包存储一些可复用的
 * 这里的CosManager，负责文件上传和下载
 * 更业务有一点关系
 */

@Slf4j
@Service
@Deprecated
public class FileManager {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     *
     * @param multipartFile 上传的文件
     * @param uploadPathPrefix 文件上传路径的前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix){
        // 校验操作可能涉及比较多，单独写一个方法进行校验
        validPicture(multipartFile);

        // 图片上传地址
            // 为了防止文件名重复，加上uuid
        String uuid = RandomUtil.randomString(16); // 随机生成16位
            // 获取原始文件名
        String originalFilename = multipartFile.getOriginalFilename();
            // 为了更好的管理图片，加上时间戳
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
            // 上面最终上传的文件名称为 日期_uuid_.格式， 并不是原始上传的文件名称（避免原始文件名有冲突）
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
            // 上面的操作仅仅是进行图片的命名变换，下面就是进行上传操作

        // 解析结果并返回
        File file = null;
        // putObject接收file对象， 将multipartFile转换为file对象
        try {
            file = File.createTempFile(uploadPath, null);
            // 传输到本地临时文件中
            multipartFile.transferTo(file);
            // 上传到对象存储，使用CosManager
                // 注意使用putPictureObject而不是putObject
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file); // 获取到上传结果对象
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

//            String format = imageInfo.getFormat();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            // 自己计算scale，为了防止精度丢失 *1.0
            double picScale = NumberUtil.round(width * 1.0/ height, 2).doubleValue();

            // 封装返回结果 UploadPictureResult
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            // url 应该就是访问地址，即域名+ 上传路径
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            // 获取文件名，即原始文件的名称（注意区分我们自己定义的图片路径和原始文件名）
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 临时文件清理
            deleteTempFile(file);
        }


    }



    /**
     * 对图片进行校验
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
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

    /**
     * 清理临时文件
     * @param file
     */
    public void deleteTempFile(File file) {
        // 将上传到本地的临时文件删除
        if (file != null) {
            // 删除临时文件
            boolean deleteResult = file.delete();
            // 如果没有删除成功
            if (!deleteResult) {
                log.error("file delete error, filepath = {}", file.getAbsolutePath());
            }
        }
    }

}
