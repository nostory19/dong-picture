package com.dong.dongpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;

import com.dong.dongpicturebackend.config.CosClientConfig;
import com.dong.dongpicturebackend.exception.BusinessException;
import com.dong.dongpicturebackend.exception.ErrorCode;

import com.dong.dongpicturebackend.manager.CosManager;
import com.dong.dongpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;


import javax.annotation.Resource;
import java.io.File;

import java.util.Date;
import java.util.List;


/**
 * @author by hongdou
 * @date 2025/6/19.
 * @DESC: manager包存储一些可复用的
 * 这里的CosManager，负责文件上传和下载
 * 更业务有一点关系
 */

/**
 * 模板方法
 */

@Slf4j
public abstract class PictureUploadTemplate {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * @param inputSource      上传的文件
     * @param uploadPathPrefix 文件上传路径的前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 校验操作可能涉及比较多，单独写一个方法进行校验
        validPicture(inputSource);

        // 图片上传地址
        // 为了防止文件名重复，加上uuid
        String uuid = RandomUtil.randomString(16); // 随机生成16位
        // 获取原始文件名
        String originalFilename = getOriginalFilename(inputSource);
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
            processFile(inputSource, file);
            // 上传到对象存储，使用CosManager
            // 注意使用putPictureObject而不是putObject
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file); // 获取到上传结果对象
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 获取上传后图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            // 得到处理转换后的所有图片
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                // 现在是上传一个图片，获取压缩之后的文件信息
                CIObject compressedCiObject = objectList.get(0);
                CIObject thumbnailCiObject = compressedCiObject;
                // 判断是否进行缩略图
                if (objectList.size() > 1) {
                    // 由于还进行了缩略图，因此还有缩略图的结果
                    thumbnailCiObject = objectList.get(1);
                }

                // 然后封装返回结果
                return buildResult(originalFilename, compressedCiObject, thumbnailCiObject, imageInfo);
            }
            // 封装返回结果
            return buildResult(originalFilename, file, uploadPath, imageInfo);

        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 临时文件清理
            deleteTempFile(file);
        }


    }



    /**
     * 处理输入源并生成本地临时文件
     *
     * @param inputSource
     */

    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验输入源 （本地文件或Url）
     *
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 清理临时文件
     *
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


    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve()); // 拿到图片主色调
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    /**
     *
     * @param originalFilename
     * @param compressedCiObject 压缩对象
     * @param thumbnailCiObject 缩略图对象
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        // 设置压缩后的原图地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return uploadPictureResult;
    }
}
