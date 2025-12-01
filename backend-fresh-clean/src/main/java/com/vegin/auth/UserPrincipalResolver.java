package com.vegin.auth;

import com.vegin.module.users.Entity.User;
import com.vegin.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPrincipalResolver {

    private final UserRepository userRepository; // email -> id 조회용

    public Long getCurrentUserId(Authentication auth) {
        Object p = (auth != null) ? auth.getPrincipal() : null;

        if (p instanceof com.vegin.auth.UserPrincipal up) {
            return up.getId();
        }
        if (p instanceof org.springframework.security.core.userdetails.User u) {
            // username == email 이라고 가정
            String email = u.getUsername();
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() -> new IllegalStateException("User not found by email: " + email));
        }
        throw new IllegalStateException("Unknown principal type: " + (p == null ? "null" : p.getClass().getName()));
    }
}
//}
//
//
//@Component
//public class UserPrincipalResolver {
//
//    public Long getCurrentUserId() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
//            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
//        }
//
//        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
//        return principal.getId(); // ✅ 여기서 UserPrincipal의 id를 꺼내는 거야
//    }
//
//    public UserPrincipal getCurrentUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
//            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
//        }
//
//        return (UserPrincipal) authentication.getPrincipal();
//    }
//}
