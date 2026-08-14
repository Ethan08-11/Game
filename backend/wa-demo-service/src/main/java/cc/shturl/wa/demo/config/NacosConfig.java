package cc.shturl.wa.demo.config;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

/**
 * Nacos 服务注册与配置中心
 * <p>
 * 具体连接参数在 application.yml 中通过环境变量配置，
 * 服务发现由 {@link EnableDiscoveryClient} 启用（已在启动类声明）
 * </p>
 */
@Configuration
public class NacosConfig {
    // Nacos 相关 Bean 由 spring-cloud-starter-alibaba-nacos-* 自动配置
    // 如需自定义负载均衡、命名空间等，可在此扩展
}
