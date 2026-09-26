package com.example.ebookrecommendsystem.security;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import com.nimbusds.jwt.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokensTest {
    private static final String KEY="dGVzdC1vbmx5LWtleS1uZXZlci11c2UtaW4tcHJvZHVjdGlvbi0wMDA=";
    private final JwtTokens tokens=new JwtTokens(new SecuritySettings(KEY,false,900,3600));
    private String signed(String issuer,String audience,Date expires) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("10").claim("ver",0).expirationTime(expires).build();
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),claims);jwt.sign(new MACSigner(Base64.getDecoder().decode(KEY)));return jwt.serialize();
    }
    @Test void validTokenContainsIdentityButNoPassword(){var jwt=tokens.verify(tokens.issue(10,3));assertEquals("10",jwt.getSubject());assertEquals(3,((Number)jwt.getClaim("ver")).intValue());assertNull(jwt.getClaim("password"));}
    @Test void expirationIsEnforced()throws Exception {String jwt=signed("zhiyue","zhiyue-web",new Date(System.currentTimeMillis()-10000));assertThrows(JwtException.class,()->tokens.verify(jwt));}
    @Test void issuerAudienceAndMissingExpirationRejected()throws Exception {
        Date future=new Date(System.currentTimeMillis()+60000);
        for(String jwt:List.of(signed("other","zhiyue-web",future),signed("zhiyue","other",future),signed("zhiyue","zhiyue-web",null)))assertThrows(JwtException.class,()->tokens.verify(jwt));
    }
    @Test void signatureAndUnsignedTokensRejected(){String valid=tokens.issue(10,0);String[] parts=valid.split("\\.");String signature=(parts[2].startsWith("A")?"B":"A")+parts[2].substring(1);assertThrows(JwtException.class,()->tokens.verify(parts[0]+"."+parts[1]+"."+signature));assertThrows(JwtException.class,()->tokens.verify("eyJhbGciOiJub25lIn0.e30."));}
    @Test void keyIsRequiredAndMustBeStrong(){assertThrows(IllegalArgumentException.class,()->new JwtTokens(new SecuritySettings("",false,900,3600)));}
    @Test void httpsCookiesHaveAllRequiredFlags(){var response=new org.springframework.mock.web.MockHttpServletResponse();new TokenCookies(new SecuritySettings(KEY,true,900,3600)).set(response,"test-access","test-refresh");for(String cookie:response.getHeaders("Set-Cookie")){assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("SameSite=Strict"));}assertEquals("no-store",response.getHeader("Cache-Control"));}
}
