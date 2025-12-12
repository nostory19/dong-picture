package com.dong.dongpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.dongpicturebackend.constant.UserConstant;
import com.dong.dongpicturebackend.exception.BusinessException;
import com.dong.dongpicturebackend.exception.ErrorCode;
import com.dong.dongpicturebackend.manager.auth.StpKit;
import com.dong.dongpicturebackend.model.dto.user.UserQueryRequest;
import com.dong.dongpicturebackend.model.entity.User;
import com.dong.dongpicturebackend.model.enums.UserRoleEnum;
import com.dong.dongpicturebackend.model.vo.LoginUserVO;
import com.dong.dongpicturebackend.model.vo.UserVO;
import com.dong.dongpicturebackend.service.UserService;
import com.dong.dongpicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 25141
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-02-24 15:56:47
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    /**
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户帐号过段");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 3. 账号唯一性检查
        // 3.1 先查询数据库中是否存在相同的账号
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);// 查询账号
        long count = this.baseMapper.selectCount(queryWrapper); // 查询数据有多少条
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 密码加密
        String EncryptPassword = getEncryptPassword(userPassword);
        // 4.插入数据到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(EncryptPassword);
        // 设置用户的默认名称和默认权限
        user.setUserName("默认名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResutl = this.save(user);
        if (!saveResutl){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 加密方法
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 直接使用工具类,单向加密
        // 加盐，混淆密码
        final String SALT = "hongdou";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验
        if (StrUtil.hasBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户帐号过段");
        }
        if (userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 2. 对密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 3.查询数据库中是否存在
        // 这里没有加事务，可能会冲突？？？搞清楚这个问题。
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        // 使用selectone，只有一条数据
        User user = this.baseMapper.selectOne(queryWrapper);
        //4. 不存在抛异常
        if (user == null){
            log.info("user login failed, userAccount can not matfh userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 5. 保存用户登录态
        // 不同用户从前端请求登录，传来的session是不同的，
        // 理解session就是一个map
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 团队空间的存储
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        // 上述用户登录态的key是固定的，但是为了避免拼错，使用一个常量进行代替。

        return this.getLoginUserVO(user); // 将user转换成loginUser
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {

        if (user == null){
            return null;
        }
        // 直接将一个对象的值设置给另一个对象
        LoginUserVO loginUserVO = new LoginUserVO();

        BeanUtils.copyProperties(user, loginUserVO);

        // 这样user中的数据就经过了脱敏，返回的就是loginUserVO
        return loginUserVO;
    }

    /**
     * 获取脱敏后的用户数据
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        return userVO;

    }

    /**
     * 获取脱敏后的用户列表
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        // 也是先判断用户列表是否为空
        if (CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        // 由于是一个集合，使用stream
//        userList.stream()
//                .map(user -> getUserVO(user))
//                .collect(Collectors.toList());

        // 将上述更新，将lambda表达式变为方法引用，是因为map中遍历的数据跟方法的接收参数是一样的，都是user
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 内部业务逻辑使用的
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断用户是否登录
        // 在userLogin方法中实现了登录，并把登录用户保存到了session中
        // 因此也是从session中取到
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        // 转成user
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 上面取到的currentUser就有一个问题，这是从session中保存的拿到的，
        // 那么假设当前用户修改了一些信息，从session中取到的就不是最新的
        // 还是查询数据库，获取最新的
        Long currentUserId = currentUser.getId();
        currentUser = this.getById(currentUserId);
        if (currentUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        return currentUser;
    }

    @Override
    public boolean userLogOut(HttpServletRequest request) {
        // 先判断是否登录
        // 从session中拿取
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        // 转换成user
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> gerQueryMapper(UserQueryRequest userQueryRequest) {
        // 先判断是是否为空
        if (userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 取出参数并拼接sql
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();


        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        userQueryWrapper.eq(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        userQueryWrapper.eq(StrUtil.isNotBlank(userName), "userName", userName);
        userQueryWrapper.eq(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        userQueryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        userQueryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        return userQueryWrapper;

    }

    @Override
    public boolean isAdmin(User user) {
        // 获取用户角色即可
        return user!= null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());

    }


}




