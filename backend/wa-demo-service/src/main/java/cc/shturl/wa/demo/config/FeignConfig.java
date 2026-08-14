package cc.shturl.wa.demo.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign 配置
 */
@Configuration
public class FeignConfig {

    /**
     * Feign 日志级别（开发环境可设为 FULL，生产环境建议 BASIC 或 NONE）
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
