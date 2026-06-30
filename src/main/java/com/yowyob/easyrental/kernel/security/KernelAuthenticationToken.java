package com.yowyob.easyrental.kernel.security;

import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Authentication token carrying kernel JWT claims.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Getter
public class KernelAuthenticationToken extends AbstractAuthenticationToken {

    private final KernelAuthClaims claims;
    private final String rawToken;

    public KernelAuthenticationToken(KernelAuthClaims claims, String rawToken) {
        super(buildAuthorities(claims));
        this.claims = claims;
        this.rawToken = rawToken;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return rawToken;
    }

    @Override
    public Object getPrincipal() {
        return claims.principal();
    }

    private static Collection<GrantedAuthority> buildAuthorities(KernelAuthClaims claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String permission : claims.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        for (String role : claims.roles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        if (KernelPermissionMapper.isOrganizationOwner(claims)
                || KernelPermissionMapper.isOrganizationOwnerCandidate(claims)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ORGANIZATION"));
        }
        if (claims.permissions().stream().anyMatch(p -> p.contains("tenant:admin") || p.contains("system:admin"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (KernelPermissionMapper.isAgencyStaff(claims)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_STAFF"));
        }
        if (claims.permissions().isEmpty() && claims.roles().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT"));
        }
        return authorities;
    }
}
