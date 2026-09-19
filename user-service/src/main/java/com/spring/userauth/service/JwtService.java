package com.spring.userauth.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	
	private final SecretKey secretKey;
	private final long expiration;
	
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes()
        );

        this.expiration = expiration;
    }

    public String generateToken(UserDetails userDetails)
    {
    	Date now = new Date();
    	
    	Date expiryDate = new Date(now.getTime() +expiration);
		
    	return Jwts.builder()
    			.setSubject(userDetails.getUsername())
    			.issuedAt(now)
    			.expiration(expiryDate)
    			.signWith(secretKey)
    			.compact();
    }
    
    public String extractEmail(String token)
    {
    	return extractAllClaims(token)
                .getSubject();

    }
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        String email = extractEmail(token);

        return email.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {

        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }


}
