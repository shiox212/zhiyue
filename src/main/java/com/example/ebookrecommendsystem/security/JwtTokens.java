package com.example.ebookrecommendsystem.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.core.*;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Component
public class JwtTokens {
    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final SecuritySettings settings;
    public JwtTokens(SecuritySettings settings) {
        this.settings = settings;
        byte[] key;
        try { key = Base64.getDecoder().decode(settings.jwtSecretBase64()); }
        catch (Exception e) { throw new IllegalArgumentException("必须配置本机 JWT_SECRET_BASE64（至少32字节随机值的Base64）"); }
        if (key.length < 32) throw new IllegalArgumentException("JWT 密钥至少需要32字节");
        var secret = new SecretKeySpec(key, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret));
        decoder = NimbusJwtDecoder.withSecretKey(secret).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator("zhiyue"), jwt -> jwt.getAudience().contains("zhiyue-web") && jwt.getExpiresAt()!=null
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
    }
    public String issue(int userId, int version) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("zhiyue").subject(Integer.toString(userId))
                .audience(java.util.List.of("zhiyue-web")).issuedAt(now).expiresAt(now.plusSeconds(settings.accessSeconds()))
                .id(UUID.randomUUID().toString()).claim("ver", version).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    public Jwt verify(String token) { return decoder.decode(token); }
}
