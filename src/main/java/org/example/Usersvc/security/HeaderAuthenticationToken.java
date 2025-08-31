package org.example.Usersvc.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * API Gateway 헤더 기반 인증을 위한 커스텀 Authentication Token
 * JWT 대신 API Gateway에서 전달받은 헤더 정보를 기반으로 인증 상태를 나타냄
 * 
 * 주요 기능:
 * - 사용자 ID를 Principal로 사용
 * - 사용자 이메일을 Credential로 사용  
 * - 헤더 기반 인증은 항상 authenticated=true
 */
@Getter
public class HeaderAuthenticationToken extends AbstractAuthenticationToken {
    
    private final String userId;
    private final String userEmail;
    
    public HeaderAuthenticationToken(String userId, String userEmail, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.userEmail = userEmail;
        super.setAuthenticated(true); // 헤더 기반 인증은 API Gateway에서 이미 검증됨
    }
    
    @Override
    public Object getCredentials() {
        return userEmail;
    }
    
    @Override
    public Object getPrincipal() {
        return userId;
    }
    
    @Override
    public String getName() {
        return userId;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (!isAuthenticated) {
            super.setAuthenticated(false);
        } else {
            throw new IllegalArgumentException(
                "Cannot set this token to trusted - use constructor which takes authorities");
        }
    }
}