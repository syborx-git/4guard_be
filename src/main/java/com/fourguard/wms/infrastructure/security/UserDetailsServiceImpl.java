package com.fourguard.wms.infrastructure.security;

import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation.
 * Integrates Spring Security with our Hexagonal UserRepositoryPort.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Support login by username or email
        UserEntity entity = userRepositoryPort.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con identificador: " + identifier));

        if (!Boolean.TRUE.equals(entity.getIsEnabled()) || entity.getStatus() != UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("La cuenta está desactivada o suspendida");
        }

        // Map role and permissions to authorities
        List<SimpleGrantedAuthority> authorities = entity.getRole().getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.getName()))
                .collect(Collectors.toList());

        // Add the role itself (prefixed with ROLE_ for standard Spring Security setups)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + entity.getRole().getName()));

        return new User(
                entity.getUsername(),
                entity.getPassword(),
                true,
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}
