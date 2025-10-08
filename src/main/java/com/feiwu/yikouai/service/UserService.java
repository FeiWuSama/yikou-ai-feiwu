package com.feiwu.yikouai.service;

import com.feiwu.yikouai.model.dto.user.UserQueryDto;
import com.feiwu.yikouai.model.vo.user.LoginUserVO;
import com.feiwu.yikouai.model.vo.user.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.feiwu.yikouai.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author <a href="https://github.com/feiwusama">绯雾sama</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密后的密码字符串
     * 该方法接收一个原始密码字符串，对其进行加密处理，并返回加密后的结果
     *
     * @param userPassword 用户输入的原始密码字符串
     * @return 加密处理后的密码字符串
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return 脱敏的已登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取用户脱敏信息
     *
     * @param user 用户实体对象，包含完整的用户信息
     * @return UserVO 视图对象，可能只包含需要展示的部分字段
     */
    UserVO getUserVO(User user);


    /**
     * 根据用户列表获取用户脱敏信息列表
     *
     * @param userList 用户实体对象列表
     * @return 用户视图对象列表，包含展示所需的信息
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 根据用户查询条件数据传输对象(UserQueryDto)创建查询包装器(QueryWrapper)
     * 该方法用于构建数据库查询条件，支持动态查询
     *
     * @param userQueryDto 用户查询条件数据传输对象，包含查询所需的参数
     * @return QueryWrapper 返回一个包含查询条件的QueryWrapper对象
     */
    QueryWrapper getQueryWrapper(UserQueryDto userQueryDto);
}
