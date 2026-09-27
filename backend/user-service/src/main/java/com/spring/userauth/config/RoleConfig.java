package com.spring.userauth.config;

import java.util.Date;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.spring.userauth.entity.Role;
import com.spring.userauth.enumer.RoleName;
import com.spring.userauth.repository.RoleRepository;

@Configuration
public class RoleConfig {

	@Bean
	CommandLineRunner initializeRoles(RoleRepository roleRepository) {

        return args -> {

            for (RoleName roleName : RoleName.values()) {

                if (!roleRepository.existsByName(roleName)) {

                    Role role = new Role();
                    role.setName(roleName);
                    role.setCreatedDate(new Date());
                    roleRepository.save(role);

                    System.out.println("Created role: " + roleName);
                }
            }
        };
    }
	
}
