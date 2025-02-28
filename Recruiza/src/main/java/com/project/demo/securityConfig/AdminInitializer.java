package com.project.demo.securityConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import com.project.demo.entity.User;
import com.project.demo.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@example.com";

        if (userService.findUserEmail(adminEmail) == null) {
            User admin = User.builder()
                    .userName("Admin User")
                    .userEmail(adminEmail)
                    .userMobile("1234567890")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .userStatus("ACTIVE")
                    .enabled(true)
                    .userCode("ADM001")
                    .build();

            userService.insertUser(admin);
            System.out.println("Администратор создан: " + adminEmail);
        } else {
            System.out.println("Администратор уже существует: " + adminEmail);
        }
    }
}
