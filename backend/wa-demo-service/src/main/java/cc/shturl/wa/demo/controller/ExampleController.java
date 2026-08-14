package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.constant.CommonConstants;
import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.req.ExampleCreateReq;
import cc.shturl.wa.demo.dto.resp.ExampleResp;
import cc.shturl.wa.demo.service.ExampleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例控制器
 */
@RestController
@RequestMapping("/api/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    /**
     * 根据 ID 查询
     */
    @GetMapping("/{id}")
    public Result<ExampleResp> getById(@PathVariable Long id) {
        return Result.ok(exampleService.getById(id));
    }

    /**
     * 分页查询
     */
    @GetMapping
    public Result<Page<ExampleResp>> pageList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        pageNum = Math.max(pageNum, CommonConstants.DEFAULT_PAGE_NUM);
        pageSize = Math.min(Math.max(pageSize, 1), CommonConstants.MAX_PAGE_SIZE);
        return Result.ok(exampleService.pageList(pageNum, pageSize));
    }

    /**
     * 创建
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ExampleCreateReq req) {
        return Result.ok(exampleService.create(req));
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        exampleService.deleteById(id);
        return Result.ok();
    }
}
