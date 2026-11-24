package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.CustomerInfoResp;

import java.util.List;

public interface CustomerService {
    CustomerInfoResp getCurrentCustomer(Long userId);
    List<CustomerInfoResp> listCustomers(Long userId);
}
