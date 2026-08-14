package cc.shturl.wa.api.feign;

import cc.shturl.wa.api.dto.RemoteUserResp;
import cc.shturl.wa.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 远程调用接口示例
 * <p>
 * name 对应 Nacos 注册的服务名，请替换为实际目标服务名
 * </p>
 */
@FeignClient(name = "wa-user-service", contextId = "userFeignClient", path = "/api/users")
public interface UserFeignClient {

    /**
     * 根据 ID 查询用户（示例接口）
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    Result<RemoteUserResp> getUserById(@PathVariable("id") Long id);
}
