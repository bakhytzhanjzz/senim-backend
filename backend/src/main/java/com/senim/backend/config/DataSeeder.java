package com.senim.backend.config;

import com.senim.backend.domain.Agency;
import com.senim.backend.domain.Role;
import com.senim.backend.domain.SubscriptionTier;
import com.senim.backend.domain.User;
import com.senim.backend.repository.AgencyRepository;
import com.senim.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            AgencyRepository agencyRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (agencyRepository.existsByName("Demo Agency")) {
            log.info("Seed data already present — skipping.");
            return;
        }

        Agency agency = agencyRepository.save(
                Agency.builder()
                        .name("Demo Agency")
                        .subscriptionTier(SubscriptionTier.PRO)
                        .build()
        );
        log.info("Seeded agency: {} ({})", agency.getName(), agency.getId());

        User owner = userRepository.save(
                User.builder()
                        .email("owner@demo.senim.kz")
                        .passwordHash(passwordEncoder.encode("Password1!"))
                        .fullName("Aibek Dzhaksybekov")
                        .role(Role.OWNER)
                        .agencyId(agency.getId())
                        .build()
        );
        log.info("Seeded owner: {} ({})", owner.getEmail(), owner.getId());

        User agent1 = userRepository.save(
                User.builder()
                        .email("agent1@demo.senim.kz")
                        .passwordHash(passwordEncoder.encode("Password1!"))
                        .fullName("Daniyar Seitkali")
                        .role(Role.AGENT)
                        .agencyId(agency.getId())
                        .build()
        );
        log.info("Seeded agent: {} ({})", agent1.getEmail(), agent1.getId());

        User agent2 = userRepository.save(
                User.builder()
                        .email("agent2@demo.senim.kz")
                        .passwordHash(passwordEncoder.encode("Password1!"))
                        .fullName("Zarina Bekova")
                        .role(Role.AGENT)
                        .agencyId(agency.getId())
                        .build()
        );
        log.info("Seeded agent: {} ({})", agent2.getEmail(), agent2.getId());
    }
}
