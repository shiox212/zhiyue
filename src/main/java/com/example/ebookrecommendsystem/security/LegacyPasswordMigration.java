package com.example.ebookrecommendsystem.security;
import com.example.ebookrecommendsystem.mapper.UserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit maintenance mode only. Never prints credentials or provides an online plaintext fallback. */
@Component
@ConditionalOnProperty(name="app.security.migrate-legacy-passwords",havingValue="true")
public class LegacyPasswordMigration implements ApplicationRunner {
    private final UserMapper users;private final PasswordEncoder encoder;
    public LegacyPasswordMigration(UserMapper users,PasswordEncoder encoder,org.springframework.core.env.Environment environment){
        if(!"none".equalsIgnoreCase(environment.getProperty("spring.main.web-application-type","servlet")))
            throw new IllegalStateException("旧密码迁移只允许离线运行：设置 spring.main.web-application-type=none");
        this.users=users;this.encoder=encoder;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        int count=0;
        for(var user:users.findAll()) {
            String old=user.getPassword();
            if(old!=null && !old.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")) {
                if(users.updatePassword(user.getId(),encoder.encode(old))!=1)throw new IllegalStateException("旧密码迁移失败，事务回滚");
                count++;
            }
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).info("已迁移旧密码记录数量：{}；请关闭维护开关后再运行服务",count);
    }
}
