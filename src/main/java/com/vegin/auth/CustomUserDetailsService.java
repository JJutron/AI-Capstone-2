package com.vegin.auth;

import com.vegin.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Not found: " + email));

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    public UserDetails loadUserById(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Not found ID=" + id));

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}

//@Service
//@RequiredArgsConstructor
//public class CustomUserDetailsService implements UserDetailsService {
//    private final UserRepository userRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String email) throws  UsernameNotFoundException{
//        var user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("Not found" + email));
//
//        return new UserPrincipal(
//                user.getId(),
//                user.getEmail(),
//                user.getPassword(),
//                List.of(new SimpleGrantedAuthority("ROLE_USER")));
////        return org.springframework.security.core.userdetails.User
////                .withUsername(user.getEmail())
////                .password(user.getPassword())
////                .authorities("ROLE_USER")
////                .build();
//    }
//
//    public UserDetails loadUserById(Long id) {
//        var user = userRepository.findById(id)
//                .orElseThrow(() -> new UsernameNotFoundException("Not found"));
//        return org.springframework.security.core.userdetails.User
//                .withUsername(user.getEmail())
//                .password(user.getPassword())
//                .authorities("ROLE_USER")
//                .build();
//    }
//}
//
//
