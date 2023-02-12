package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
        //4.保存验证码到redis 2分钟的过期时间
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
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

        String phone = loginForm.getPhone();
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.格式不符合，则返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3.校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
            //.不一致，报错
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
        //7.保存用户信息到redis中
        //7.1随机生成一个token,作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //7.2将user对象转换为hash存储
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        //将user对象转换为Map
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        //忽略空值
                        .setIgnoreNullValue(true)
                        //序列化器为String类型，所以将Long类型的id转为String
                        .setFieldValueEditor((fileName, fieldValue) -> fieldValue.toString()));
        //7.3存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        //7.4设置有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS);
        //返回token
        return Result.ok(token);
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
