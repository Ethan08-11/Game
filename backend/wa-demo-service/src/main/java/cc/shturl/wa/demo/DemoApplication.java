package cc.shturl.wa.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Demo 微服务启动类
 * <p>
 * 复制本模块并重命名 demo 为实际服务名即可快速创建新微服务
 * </p>
 */
@SpringBootApplication(scanBasePackages = "cc.shturl.wa")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cc.shturl.wa.api.feign")
@EnableScheduling
@MapperScan("cc.shturl.wa.demo.mapper")
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
