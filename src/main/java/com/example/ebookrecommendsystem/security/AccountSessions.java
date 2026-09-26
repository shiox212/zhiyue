package com.example.ebookrecommendsystem.security;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.mapper.*;
import com.example.ebookrecommendsystem.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import java.security.*;
import java.util.*;

@Service
public class AccountSessions {
    public record Tokens(User user,String access,String refresh) {}
    private final UserService users;
    private final UserMapper userMapper;
    private final SecurityMapper security;
    private final JwtTokens jwt;
    private final SecuritySettings settings;
    private final SecureRandom random=new SecureRandom();
    public AccountSessions(UserService users,UserMapper userMapper,SecurityMapper security,JwtTokens jwt,SecuritySettings settings) {
        this.users=users;this.userMapper=userMapper;this.security=security;this.jwt=jwt;this.settings=settings;
    }
    @Transactional public Result<Object> register(User user) {
        var result=users.register(user);
        if(result.getCode()==200) { security.createAccount(user.getId());security.assignReader(user.getId()); }
        return result;
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Tokens login(User credentials) {
        var account=userMapper.findByUsername(credentials.getUsername());
        if(account==null) throw new BadCredentialsException("账号或密码不正确，或账号不可用");
        Integer version=security.lockVersion(account.getId());
        if(version==null) throw new BadCredentialsException("账号安全配置未初始化");
        // Check the password after acquiring the same lock used by password changes and revocation.
        var result=users.login(credentials);
        if(result.getCode()!=200) throw new BadCredentialsException("账号或密码不正确，或账号不可用");
        var user=result.getData();
        return issue(user,version);
    }
    @Transactional public Tokens refresh(String raw) {
        if(raw==null || raw.length()>128) throw new BadCredentialsException("请重新登录");
        String hash=hash(raw);
        Integer id=security.refreshUser(hash,System.currentTimeMillis());
        if(id==null) throw new BadCredentialsException("请重新登录");
        Integer version=security.lockVersion(id);
        User user=userMapper.findById(id);
        if(version==null || user==null || !Integer.valueOf(1).equals(user.getStatus())
                || security.consumeRefresh(hash,version,System.currentTimeMillis())!=1)
            throw new BadCredentialsException("请重新登录");
        return issue(user,version);
    }
    private Tokens issue(User user,int version) {
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String refresh=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        security.deleteExpired(System.currentTimeMillis());
        security.saveRefresh(hash(refresh),user.getId(),version,System.currentTimeMillis()+settings.refreshSeconds()*1000L);
        user.setPassword(null);
        user.setRole(security.roles(user.getId()).contains("ADMIN")?1:0);
        return new Tokens(user,jwt.issue(user.getId(),version),refresh);
    }
    @Transactional public void logout(Integer id,String refresh) {
        if(id==null && refresh!=null && refresh.length()<=128) id=security.refreshUser(hash(refresh),System.currentTimeMillis());
        if(id!=null) revoke(id);
    }
    private void revoke(int id) {
        security.lockVersion(id);security.revokeAccess(id);security.revokeRefresh(id);
    }
    @Transactional public Result<Object> changePassword(int id,String oldPassword,String newPassword) {
        security.lockVersion(id);
        var result=users.changePassword(id,oldPassword,newPassword);
        if(result.getCode()==200) revoke(id);
        return result;
    }
    @Transactional public Result<Object> updateStatus(int id,Integer status) {
        security.lockVersion(id);
        var result=users.updateUserStatus(id,status);
        if(result.getCode()==200)revoke(id);
        return result;
    }
    public User current() {
        User user=userMapper.findById(Identity.currentId());
        if(user==null) throw new BadCredentialsException("请重新登录");
        user.setPassword(null);user.setRole(security.roles(user.getId()).contains("ADMIN")?1:0);
        return user;
    }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
