package com.dong.dongpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.dong.dongpicturebackend.manager.auth.SpaceUserAuthManager;
import com.dong.dongpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.dong.dongpicturebackend.model.entity.Picture;
import com.dong.dongpicturebackend.model.entity.Space;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.model.enums.SpaceTypeEnum;
import com.dong.dongpicturebackend.service.PictureService;
import com.dong.dongpicturebackend.service.SpaceService;
import com.dong.dongpicturebackend.service.SpaceUserService;
import com.dong.dongpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author by hongdou
 * @date 2025/12/10.
 * @DESC: websocket拦截器，连接前先校验
 */

@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 握手前校验，校验用户是否有编辑图片的权限
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 给websocket会话添加属性
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 获取当前用户
            // 获取HttpServletrequest
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 从请求中获取参数，例如?pictureId
            String pictureId = httpServletRequest.getParameter("pictureId");
            // 如果不存在
            if(StrUtil.isBlank(pictureId)){
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            // 获取当前登录用户
            User loginUser = userService.getLoginUser(httpServletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            // 校验当前用户是否有编辑图片的权限
            // 利用pictureId获取图片
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            // 从图片获取空间id
            Long spaceId = picture.getSpaceId();
            Space space = null;
//            if (spaceId == null) {
//                log.error("图片不存在空间id，拒绝握手");
//                return false;
//            }else {
            if (spaceId != null){

                space = spaceService.getById(spaceId);
                if (ObjUtil.isEmpty(space)) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                // 如果是团队空间，并且有编辑权限才能建立连接
                if(space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("非团队空间，拒绝握手");
                    return false;
                }
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("用户无编辑图片权限，拒绝握手");
                return false;
            }
//            }
            // 设置用户登录等信息到websocket会话中
            // 给attributes中添加用户信息
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            // 记得转换为long类型
            attributes.put("pictureId", Long.valueOf(pictureId));
        }


        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
