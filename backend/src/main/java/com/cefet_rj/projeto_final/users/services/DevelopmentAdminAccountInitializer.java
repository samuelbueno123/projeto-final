package com.cefet_rj.projeto_final.users.services;

import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevelopmentAdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.account.email}")
    private String email;

    @Value("${admin.account.password}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) {
        User admin = userRepository.findByEmail(email).orElseGet(User::new);
        admin.setFirstName("Ancora");
        admin.setLastName("Admin");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));

        Role adminRole = roleRepository.findByAuthority("ROLE_ADMIN");
        if (adminRole == null) {
            throw new IllegalStateException("ROLE_ADMIN must be seeded before the development administrator");
        }
        admin.getRoles().clear();
        admin.addRole(adminRole);
        userRepository.save(admin);
    }
}
