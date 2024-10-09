package io.github.denrzv.audioreview.security;

import io.github.denrzv.audioreview.model.User;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final User user;

    /**
     * Convert User entity to UserDetails.
     *
     * @param user the User entity
     * @return UserDetailsImpl instance
     */
    public static UserDetailsImpl build(User user) {
        return new UserDetailsImpl(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // Additional methods can be overridden as needed

    @Override
    public boolean isAccountNonExpired() {
        return true; // Customize as per your requirements
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Customize as per your requirements
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}