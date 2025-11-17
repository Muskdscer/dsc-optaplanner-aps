package com.upec.factoryscheduling.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upec.factoryscheduling.auth.service.UserDetailsServiceImpl;
import com.upec.factoryscheduling.auth.utils.JwtUtils;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE-1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            // 获取 JWT 令牌
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // 从 JWT 令牌中获取用户名
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                // 加载用户详情
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                // 设置认证详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
            handleException(e, request, response);
        }
    }

    /**
     * 从请求头中解析 JWT 令牌
     *
     * @param request HTTP 请求
     * @return JWT 令牌
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }


    private void handleException(Exception ex,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        try {
            // 根据异常类型设置相应的HTTP状态码和错误信息
            if (ex instanceof ServletException) {
                handleServletException((ServletException) ex, request, response);
            } else if (ex instanceof IOException) {
                handleIOException((IOException) ex, request, response);
            } else {
                handleGenericException(ex, request, response);
            }
        } catch (Exception e) {
            // 最后的保障，设置基本错误状态
            logger.error("在异常处理过程中发生错误", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleServletException(ServletException ex,
                                        HttpServletRequest request,
                                        HttpServletResponse response) throws IOException {
        HttpStatus status = determineHttpStatus(ex);
        sendErrorResponse(response, status, ex.getMessage());
    }

    private void handleIOException(IOException ex,
                                   HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                "I/O处理错误");
    }

    private void handleGenericException(Exception ex,
                                        HttpServletRequest request,
                                        HttpServletResponse response) throws IOException {
        HttpStatus status = determineHttpStatus(ex);
        String message = (ex instanceof RuntimeException) ?
                ex.getMessage() : "服务器内部错误";

        sendErrorResponse(response, status, message);
    }

    private HttpStatus determineHttpStatus(Exception ex) {
        if (ex instanceof AuthenticationException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        } else if (ex instanceof RuntimeException) {
            return HttpStatus.BAD_REQUEST;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   HttpStatus status,
                                   String message) throws IOException {
        if (response.isCommitted()) {
            logger.warn("响应已提交，无法发送错误响应");
            return;
        }
        response.resetBuffer(); // 重置缓冲区
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(status.value(), message));
        response.getWriter().flush();
    }
}
