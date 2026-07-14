package com.kitchen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminInterceptor())
            .addPathPatterns("/api/**");
    }

    private static class AdminInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // 小程序请求放行（带 X-App-Source: miniapp 头）
            if ("miniapp".equals(request.getHeader("X-App-Source"))) {
                return true;
            }
            // 登录接口放行
            if ("/api/admin/login".equals(request.getRequestURI())) {
                return true;
            }
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("adminId") == null) {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
                return false;
            }
            return true;
        }
    }
}
