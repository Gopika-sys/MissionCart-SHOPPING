package com.missioncart.admin;

import com.missioncart.config.AppProperties;
import com.missioncart.users.User;
import com.missioncart.users.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public AdminBootstrapRunner(UserRepository userRepo, PasswordEncoder passwordEncoder, AppProperties props) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        String email = props.admin().bootstrapEmail();
        String pass = props.admin().bootstrapPassword();
        if (email == null || email.isBlank() || pass == null || pass.isBlank()) {
            return;
        }
        if (userRepo.countByRole("ADMIN") == 0 && !userRepo.existsByEmailIgnoreCase(email)) {
            User admin = new User();
            admin.setEmail(email.trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(pass));
            admin.setFullName("MissionCart Administrator");
            admin.setRole("ADMIN");
            admin.setEnabled(true);
            userRepo.save(admin);
        }
    }
}
