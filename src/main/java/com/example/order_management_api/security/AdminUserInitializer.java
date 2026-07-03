package com.example.order_management_api.security;

import com.example.order_management_api.model.Role;
import com.example.order_management_api.model.User;
import com.example.order_management_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the initial ADMIN account. Registration always creates USER accounts,
 * so without this there would be no way to obtain admin access.
 */
@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin.email:}") String adminEmail,
            @Value("${app.security.admin.password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("Admin account not configured (app.security.admin.email/password) - skipping admin seeding");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        userRepository.save(new User(adminEmail, passwordEncoder.encode(adminPassword), Role.ADMIN));
        log.info("Seeded admin account: {}", adminEmail);
    }
}
