package com.hmdp.interceptor;


import com.hmdp.utils.UserHolder;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.判断是否需要拦截（ThreadLocal中是否有用户）
        if (UserHolder.getUser() == null) {
            //设置状态码
            response.setStatus(401);
            //拦截
            return false;
        }
        //存在用户信息,放行
        return true;
    }
}
