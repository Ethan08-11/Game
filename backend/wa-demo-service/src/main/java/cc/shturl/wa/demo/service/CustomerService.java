package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.CustomerInfoResp;

import java.util.List;

public interface CustomerService {
    CustomerInfoResp getCurrentCustomer();
    List<CustomerInfoResp> listCustomers();
}
