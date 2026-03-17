package com.cefet_rj.projeto_final.config.customgrant;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

@Getter
public final class CustomUserAuthorities {

    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserAuthorities(String username,
                                 Collection<? extends GrantedAuthority> authorities) {

        Assert.hasText(username, "username must not be null or empty");
        Assert.notNull(authorities, "authorities must not be null");

        this.username = username;

        this.authorities = Collections.unmodifiableCollection(authorities);
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