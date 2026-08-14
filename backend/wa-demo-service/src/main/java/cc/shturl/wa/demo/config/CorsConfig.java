package cc.shturl.wa.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许携带cookie
        config.setAllowCredentials(true);
        // 允许方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 预检请求有效期
        config.setMaxAge(3600L);

        // ========== 核心：自定义Origin校验规则 ==========
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.1.25:*",
                "http://192.168.2.1:*",
                "http://192.168.13.1:*"
        ));

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}