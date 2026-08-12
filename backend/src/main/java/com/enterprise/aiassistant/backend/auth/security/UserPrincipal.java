package com.enterprise.aiassistant.backend.auth.security;

import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserPrincipal implements UserDetails {
    private Long id;

    private String username;

    private String password;

    private Role role;

    // Snapshot của Department tại thời điểm authenticate — dùng cho ABAC scope check.
    private Long departmentId;

    private Set<Permission> permissions;

    private boolean enabled;

    public static UserPrincipal from(User user) {
        return from(user, Set.of());
    }

    public static UserPrincipal from(User user, Set<Permission> permissions) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                permissions != null ? permissions : Set.of(),
                user.isEnabled()
        );
    }

    public boolean hasPermission(Permission permission) {
        return permissions != null && permissions.contains(permission);
    }

    // ROLE_* cho hasRole(), tên permission trần cho hasAuthority() ở @PreAuthorize.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        if (permissions != null) {
            permissions.forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.name()))
            );
        }

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
