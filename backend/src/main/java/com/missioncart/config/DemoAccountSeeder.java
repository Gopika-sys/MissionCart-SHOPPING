package com.missioncart.config;

import com.missioncart.users.Address;
import com.missioncart.users.AddressRepository;
import com.missioncart.users.User;
import com.missioncart.users.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final AddressRepository addressRepo;
    private final PasswordEncoder passwordEncoder;

    public DemoAccountSeeder(UserRepository userRepo, AddressRepository addressRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.addressRepo = addressRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepo.existsByEmailIgnoreCase("ananya@missioncart.local")) {
            User customer = new User();
            customer.setEmail("ananya@missioncart.local");
            customer.setPasswordHash(passwordEncoder.encode("MissionCart@2026"));
            customer.setFullName("Ananya Sharma");
            customer.setPhone("+91 98765 43210");
            customer.setRole("CUSTOMER");
            customer = userRepo.save(customer);

            Address addr = new Address();
            addr.setUserId(customer.getId());
            addr.setLabel("Hostel / Home");
            addr.setRecipientName("Ananya Sharma");
            addr.setPhone("+91 98765 43210");
            addr.setLine1("Room 204, Indiranagar Residency");
            addr.setCity("Bengaluru");
            addr.setState("Karnataka");
            addr.setPostalCode("560038");
            addr.setDefaultAddress(true);
            addressRepo.save(addr);
        }

        if (!userRepo.existsByEmailIgnoreCase("seller@missioncart.local")) {
            User seller = new User();
            seller.setEmail("seller@missioncart.local");
            seller.setPasswordHash(passwordEncoder.encode("SellerCart@2026"));
            seller.setFullName("Kora Living Studio");
            seller.setPhone("+91 98111 22334");
            seller.setRole("SELLER");
            userRepo.save(seller);
        }
    }
}
