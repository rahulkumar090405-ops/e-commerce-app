package com.spring.userauth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.spring.userauth.entity.GoogleIdentity;

public interface GoogleIdentityRepository extends JpaRepository<GoogleIdentity, Long> {
    Optional<GoogleIdentity> findByIssuerAndSubject(String issuer, String subject);
}
