package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.CustomerInfoResp;
import cc.shturl.wa.demo.entity.Bullies;
import cc.shturl.wa.demo.entity.CustomerTypes;
import cc.shturl.wa.demo.mapper.BulliesMapper;
import cc.shturl.wa.demo.mapper.CustomerTypesMapper;
import cc.shturl.wa.demo.service.CustomerService;
import cc.shturl.wa.demo.service.support.BullyCatalog;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerTypesMapper customerTypesMapper;
    private final BulliesMapper bulliesMapper;

    @Override
    public CustomerInfoResp getCurrentCustomer() {
        List<CustomerTypes> customers = customerTypesMapper.selectList(Wrappers.<CustomerTypes>lambdaQuery()
                .eq(CustomerTypes::getStatus, 1)
                .orderByAsc(CustomerTypes::getSortNo, CustomerTypes::getId));
        if (customers == null || customers.isEmpty()) {
            return BullyCatalog.toCustomerResp(null, null);
        }
        CustomerTypes customer = pickWeightedCustomer(customers);
        return BullyCatalog.toCustomerResp(customer, findBullyForCustomer(customer.getCustomerCode()));
    }

    @Override
    public List<CustomerInfoResp> listCustomers() {
        List<CustomerTypes> customers = customerTypesMapper.selectList(Wrappers.<CustomerTypes>lambdaQuery()
                .orderByAsc(CustomerTypes::getSortNo, CustomerTypes::getId));
        return customers.stream()
                .map(customer -> BullyCatalog.toCustomerResp(customer, findBullyForCustomer(customer.getCustomerCode())))
                .toList();
    }

    private Bullies findBullyForCustomer(String customerCode) {
        String bullyCode = BullyCatalog.bullyCodeForCustomer(customerCode);
        if (bullyCode == null) {
            return null;
        }
        return bulliesMapper.selectOne(Wrappers.<Bullies>lambdaQuery()
                .eq(Bullies::getBullyCode, bullyCode)
                .last("LIMIT 1"));
    }

    private CustomerTypes pickWeightedCustomer(List<CustomerTypes> customers) {
        int totalWeight = customers.stream()
                .mapToInt(customer -> Math.max(customer.getSelectionWeight() == null ? 0 : customer.getSelectionWeight(), 0))
                .sum();
        if (totalWeight <= 0) {
            return customers.get(0);
        }
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (CustomerTypes customer : customers) {
            cumulative += Math.max(customer.getSelectionWeight() == null ? 0 : customer.getSelectionWeight(), 0);
            if (random < cumulative) {
                return customer;
            }
        }
        return customers.get(0);
    }
}
