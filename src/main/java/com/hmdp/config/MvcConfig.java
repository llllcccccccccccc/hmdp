package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //order值越小的拦截器，先执行，
        // 初始值是0，都是0的情况下，先添加的执行
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/",
                        "/shop-type/**",
                        "/blog/hot",
                        "/upload/**",
                        "/user/code",
                        "/user/login"
                ).order(1);

        registry.addInterceptor(new RefreshTokenInterceptor())
                .addPathPatterns("/**").order(0);
    }
}
