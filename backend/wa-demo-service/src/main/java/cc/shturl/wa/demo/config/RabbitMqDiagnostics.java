package cc.shturl.wa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时打印 RabbitMQ 连接诊断（不含密码明文），方便核对 Zeabur 环境变量是否生效。
 */
@Component
public class RabbitMqDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqDiagnostics.class);

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password:}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host:/}")
    private String vhost;

    @Value("${spring.rabbitmq.listener.simple.auto-startup:false}")
    private boolean listenerAutoStartup;

    @Override
    public void run(ApplicationArguments args) {
        int pwdLen = password == null ? 0 : password.length();
        log.info("RabbitMQ config: host={}, port={}, username={}, passwordLength={}, vhost={}, listenerAutoStartup={}",
                host, port, username, pwdLen, vhost, listenerAutoStartup);
        if (pwdLen == 0) {
            log.warn("RabbitMQ password is empty. Set RABBITMQ_PASSWORD on backend as a literal string (do not reference ${PASSWORD}, it conflicts with MySQL).");
        }
    }
}
