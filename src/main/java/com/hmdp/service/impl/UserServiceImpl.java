package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 验证码功能
     *
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.格式不符合，则返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3.生成验证码到session
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到session
        session.setAttribute("code", code);
        session.setAttribute("phone", phone);
        //5.模拟验证吗的发送
        log.info("验证码发送成功！验证码为：{}", code);
        //返回
        return Result.ok();
    }

    /**
     * 登录注册功能
     *
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            //2.格式不符合，则返回错误信息
            return Result.fail("手机号格式错误");
        }
        String phone = (String) session.getAttribute("phone");
        if (phone != null && !phone.equals(loginForm.getPhone())) {
            //2.号码不一致，则返回错误信息
            return Result.fail("验证码不为该手机号的");
        }
        //2.校验验证码
        String code = (String) session.getAttribute("code");
        if (code == null || code.equals(loginForm.getPhone())) {
            //3.不一致，报错
            return Result.fail("验证码错误！");
        }
        //4.一致，则根据手机号查询用户

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(loginForm.getPhone() != null, User::getPhone, loginForm.getPhone()));
        //5.判断用户是否存在
        if (user == null) {
            //6.不存在，则创建新用户并保存
            user = createUserWithPhone(loginForm.getPhone());
        }
        //7.保存用户信息到session中
        session.setAttribute("user", user);
        return Result.ok();
    }

    /**
     * 创建新用户用户
     *
     * @param phone
     * @return
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        int insert = userMapper.insert(user);
        System.out.println(insert > 0 ? "创建成功" : "创建失败");
        return user;
    }
}
