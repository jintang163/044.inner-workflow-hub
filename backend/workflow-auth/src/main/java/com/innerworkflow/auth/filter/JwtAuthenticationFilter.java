package com.innerworkflow.auth.filter;

import com.innerworkflow.auth.service.SysUserService;
import com.innerworkflow.common.context.TenantContext;
import com.innerworkflow.common.dto.LoginUserDTO;
import com.innerworkflow.common.util.JwtUtils;
import com.innerworkflow.common.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SysUserService sysUserService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);

            if (token != null && JwtUtils.validateToken(token)) {
                Long userId = JwtUtils.getUserId(token);
                String username = JwtUtils.getUsername(token);

                if (userId != null && username != null && SecurityUtils.getCurrentUserOpt().isEmpty()) {
                    LoginUserDTO loginUser = sysUserService.getLoginUserByUsername(username);
                    if (loginUser != null) {
                        loginUser.setToken(token);

                        String tenantIdStr = request.getHeader(TENANT_ID_HEADER);
                        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                            Long headerTenantId = Long.valueOf(tenantIdStr);
                            if (loginUser.isSuperAdmin() || loginUser.belongsToTenant(headerTenantId)) {
                                loginUser.setTenantId(headerTenantId);
                                TenantContext.setTenantId(headerTenantId);
                            }
                        } else if (loginUser.getTenantId() != null) {
                            TenantContext.setTenantId(loginUser.getTenantId());
                        }

                        SecurityUtils.setCurrentUser(loginUser);

                        Set<SimpleGrantedAuthority> authorities = Collections.emptySet();
                        if (loginUser.getPermissions() != null) {
                            authorities = loginUser.getPermissions().stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toSet());
                        }

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("JWT认证过滤器处理异常: {}", e.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityUtils.clearCurrentUser();
            SecurityContextHolder.clearContext();
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        return JwtUtils.removeBearerPrefix(bearerToken);
    }
}
