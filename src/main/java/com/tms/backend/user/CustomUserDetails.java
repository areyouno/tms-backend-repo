package com.tms.backend.user;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {
    
    private final String uid;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    
    public CustomUserDetails(String uid, String email, String password, 
                           Collection<? extends GrantedAuthority> authorities) {
        this.uid = uid;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }
    
    public String getUid() {
        return uid;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
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
        return true;
    }
}