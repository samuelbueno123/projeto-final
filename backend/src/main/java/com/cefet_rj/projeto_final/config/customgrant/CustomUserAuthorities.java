package com.cefet_rj.projeto_final.config.customgrant;

import java.util.Collection;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

public final class CustomUserAuthorities {

    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserAuthorities(String username,
                                 Collection<? extends GrantedAuthority> authorities) {

        Assert.hasText(username, "username must not be null or empty");
        Assert.notNull(authorities, "authorities must not be null");

        this.username = username;
        this.authorities = authorities;
    }

    public String getUsername() {
        return username;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomUserAuthorities that)) return false;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}