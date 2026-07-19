package com.dong.dongpicturebackendpictureservice.interfaces.controller;

import com.dong.dongpicturebackendcommon.annotation.AuthCheck;
import com.dong.dongpicturebackendcommon.common.BaseResponse;
import com.dong.dongpicturebackendcommon.common.ResultUtils;
import com.dong.dongpicturebackendcommon.constant.UserConstant;
import com.dong.dongpicturebackendcommon.exception.BusinessException;
import com.dong.dongpicturebackendcommon.exception.ErrorCode;
import com.dong.dongpicturebackendpictureservice.infrastructure.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author by hongdou
 * @date 2025/6/19.
 * @DESC:
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private CosManager cosManager;


    /**
     * 测试文件上传接口
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 后端接收前端传来的文件，一般情况是通过form表单接收的，

        // 获取文件名，文件路径
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);

        File file = null;
        // putObject接收file对象， 将multipartFile转换为file对象
        try {
            file = File.createTempFile(filepath, null);
            // 传输到本地临时文件中
            multipartFile.transferTo(file);
            // 上传到对象存储，使用CosManager
            cosManager.putObject(filepath, file);
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 将上传到本地的临时文件删除
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                // 如果没有删除成功
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);

                }
            }
        }
    }


    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        // 涉及到流，注意需要关闭
        COSObjectInputStream objectContent = null;
        // 将流转换成数组
        try {
            COSObject getobject = cosManager.getobject(filepath);
            objectContent = getobject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(objectContent);
            // 设置响应头
            // 设置响应头让浏览器知道是下载还是看文件
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 将流写入响应中
            response.getOutputStream().write(bytes);
            // 刷新
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }finally {
            // 关闭流
            if (objectContent != null){
                objectContent.close();
            }
        }

    }
}
