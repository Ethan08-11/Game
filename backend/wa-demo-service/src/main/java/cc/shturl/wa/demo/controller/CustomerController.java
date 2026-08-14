package cc.shturl.wa.demo.controller;

import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.resp.CustomerInfoResp;
import cc.shturl.wa.demo.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/current")
    public Result<CustomerInfoResp> getCurrentCustomer() {
        return Result.ok(customerService.getCurrentCustomer());
    }

    @GetMapping
    public Result<List<CustomerInfoResp>> listCustomers() {
        return Result.ok(customerService.listCustomers());
    }
}
