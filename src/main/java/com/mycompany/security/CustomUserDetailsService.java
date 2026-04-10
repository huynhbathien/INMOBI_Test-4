package com.mycompany.security;

import com.mycompany.entity.UserEntity;
import com.mycompany.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        UserEntity user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        return new CustomUserDetails(user);
    }

    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        log.debug("Loading user details for email: {}", email);

        UserEntity user = userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        return new CustomUserDetails(user);
    }

    /**
     * Inner class to represent user details with additional information
     */
    public static class CustomUserDetails implements UserDetails {
        private static final long serialVersionUID = 1L;

        private final UserEntity user;
        private final Collection<GrantedAuthority> authorities;

        public CustomUserDetails(UserEntity user) {
            this.user = user;
            this.authorities = buildAuthorities(user);
        }

        private static Collection<GrantedAuthority> buildAuthorities(UserEntity user) {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Add role authority
            if (user.getRole() != null && !user.getRole().isEmpty()) {
                String role = user.getRole();
                // Ensure role starts with ROLE_ for Spring Security convention
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }
                authorities.add(new SimpleGrantedAuthority(role));
            } else {
                // Default role if none specified
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return authorities;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            return user.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return user.isActive();
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return user.isActive();
        }

        // Getters for additional user information
        public UserEntity getUser() {
            return user;
        }

        public Long getUserId() {
            return user.getId();
        }

        public String getFullName() {
            return user.getFullName();
        }

        public String getEmail() {
            return user.getEmail();
        }

        public String getRole() {
            return user.getRole();
        }

        public String getAvatarUrl() {
            return user.getAvatarUrl();
        }
    }
}
