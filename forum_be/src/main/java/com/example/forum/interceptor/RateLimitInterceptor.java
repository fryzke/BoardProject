package com.example.forum.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.forum.annotation.RateLimit;
import com.example.forum.exception.RateLimitExceedException;
import com.example.forum.service.RateLimitService;
import com.example.forum.utils.ServletUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit != null) {
            String clientIp = ServletUtils.getClientIp(request);
            String endpointKey = handlerMethod.getBeanType().getSimpleName() + ":" + handlerMethod.getMethod().getName();
            String key = clientIp + ":" + endpointKey;

            boolean allowed = rateLimitService.isAllowed(
                key,
                rateLimit.capacity(),
                rateLimit.refillRate(),
                rateLimit.requested()
            );
            if (!allowed) {
                throw new RateLimitExceedException("요청 한도를 초과했습니다. 나중에 다시 시도해주세요.");
            }
        }

        return true;
    }
}
