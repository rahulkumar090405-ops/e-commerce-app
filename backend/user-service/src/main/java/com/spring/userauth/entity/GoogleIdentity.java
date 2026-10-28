package com.spring.userauth.entity;

import jakarta.persistence.*;

/** A deliberate account link, never created by an email match or a login. */
@Entity
@Table(name = "google_identities", uniqueConstraints =
        @UniqueConstraint(name = "uk_google_issuer_subject", columnNames = {"issuer", "subject"}))
public class GoogleIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String issuer;

    @Column(nullable = false, length = 255)
    private String subject;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected GoogleIdentity() {}

    public GoogleIdentity(String issuer, String subject, User user) {
        this.issuer = issuer;
        this.subject = subject;
        this.user = user;
    }

    public User getUser() { return user; }
}
