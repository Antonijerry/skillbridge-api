package com.skillbridge.auth.service;

import java.time.Instant;

import com.skillbridge.auth.dto.TokenResponse;
import com.skillbridge.employee.domain.Employee;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long expirySeconds;

    public TokenService(JwtEncoder encoder, @Value("${skillbridge.jwt.expiry-minutes:60}") long expiryMinutes) {
        this.encoder = encoder;
        this.expirySeconds = expiryMinutes * 60;
    }

    public TokenResponse issue(Employee employee) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(employee.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                .claim("role", employee.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();   // without this, Nimbus assumes RS256
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expirySeconds);
    }
}