package com.dong.dongpicturebackenduserservice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.*;

/**
 * @author by hongdou
 * @date 2025/11/11.
 * @DESC: 全局自定义请求包装类
 * 使得InputStream 可以重复读取
 * 样板代码，直接复制粘贴
 */
@Slf4j
public class RequestWrapper extends HttpServletRequestWrapper {

    private final String body;

    // 构造函数，将请求体中的数据读取出来
    public RequestWrapper(HttpServletRequest request) {
        // 从请求中获取请求体中的数据
        super(request);
        //
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = request.getInputStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            char[] charBuffer = new char[128];
            int bytesRead;
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                stringBuilder.append(charBuffer, 0, bytesRead);
            }
        } catch (IOException ignored) {

        }
        body = stringBuilder.toString();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }


            @Override
            public void setReadListener(ReadListener readListener) {}
            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public String getBody() {
        return this.body;
    }
}
