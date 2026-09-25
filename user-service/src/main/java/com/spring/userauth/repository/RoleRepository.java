package com.spring.userauth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.userauth.entity.Role;
import com.spring.userauth.entity.User;
import com.spring.userauth.enumer.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByName(RoleName roleUser);

	boolean existsByName(RoleName roleName);

}
