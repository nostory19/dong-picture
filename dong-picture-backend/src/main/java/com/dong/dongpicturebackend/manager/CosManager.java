package com.dong.dongpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.dong.dongpicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author by hongdou
 * @date 2025/6/19.
 * @DESC: manager包存储一些可复用的
 * 这里的CosManager，负责文件上传和下载
 * 与业务逻辑无关，是通用的
 */

@Component
public class CosManager {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

//    使用文档提供的，简单接口，上传本地文件到COS

    /**
     * 上传文件 （方法名没有明确是文件还是图片，只是Object）
     *
     * @param key  保存的位置，唯一
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key
     * @return
     */
    public COSObject getobject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);

        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传并解析图片的方法，方法名明确说明是上传图片
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 添加图片处理规则，
        // 这里的规则添加上后，会和图片一起发送给对象存储，然后就会一句规则进行调整
        // 获取图片的基本信息也被是作为一种图片的处理
        PicOperations picOperations = new PicOperations();
        // 1表示返回原图信息
        picOperations.setIsPicInfo(1);

        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 1. 增加新的功能：对图片进行压缩，转成webp格式
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        // 2. 在原图基础上，添加缩略图规则，并且仅对>20kb的图进行缩略图
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String suffix = FileUtil.getSuffix(key);
            if (StrUtil.isBlank(suffix)) {
                suffix = "png"; // 默认后缀
            }
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + suffix;
//        String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);

            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            // 128 x 128 或者256 x 256
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);

        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 删除操作
     * @param key
     */
    public void deleteObject(String key){
        // 执行删除操作
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
