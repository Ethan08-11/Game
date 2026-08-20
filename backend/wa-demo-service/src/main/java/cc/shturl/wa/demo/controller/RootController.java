package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台探活和浏览器打开服务根地址时走这里，避免被静态资源处理器当成「No static resource .」。
 */
@RestController
public class RootController {

    @GetMapping("/")
    public Result<String> root() {
        return Result.ok("wa-demo-service");
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
