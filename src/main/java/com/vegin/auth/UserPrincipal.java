package com.vegin.auth;

import com.vegin.module.users.Entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final List<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String password,
                         List<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                List.of(() -> "ROLE_USER")
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}


//@Getter
//@AllArgsConstructor
//public class UserPrincipal implements UserDetails {
//    private Long id;
//    private String email;
//    private String password;
//
//    public static UserPrincipal from(User user) {
//        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword());
//    }
//
//    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
//    @Override public String getUsername() { return email; }
//    @Override public boolean isAccountNonExpired() { return true; }
//    @Override public boolean isAccountNonLocked() { return true; }
//    @Override public boolean isCredentialsNonExpired() { return true; }
//    @Override public boolean isEnabled() { return true; }
//}