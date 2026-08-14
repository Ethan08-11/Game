package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.CustomerInfoResp;
import cc.shturl.wa.demo.entity.CustomerTypes;
import cc.shturl.wa.demo.mapper.CustomerTypesMapper;
import cc.shturl.wa.demo.service.CustomerService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerTypesMapper customerTypesMapper;

    @Override
    public CustomerInfoResp getCurrentCustomer() {
        List<CustomerTypes> customers = customerTypesMapper.selectList(Wrappers.<CustomerTypes>lambdaQuery()
                .eq(CustomerTypes::getStatus, 1)
                .orderByAsc(CustomerTypes::getSelectionWeight, CustomerTypes::getId));
        if (customers == null || customers.isEmpty()) {
            return new CustomerInfoResp(null, null, null, null, null, null, null, null, null, 0);
        }
        CustomerTypes customer = pickWeightedCustomer(customers);
        return new CustomerInfoResp(customer.getId(), customer.getCustomerCode(), customer.getCustomerName(),
                customer.getDescription(), customer.getImageUrl(), customer.getEffectType(), customer.getEffectValue(),
                customer.getTriggerChance(), customer.getSelectionWeight(), customer.getStatus());
    }

    @Override
    public List<CustomerInfoResp> listCustomers() {
        List<CustomerTypes> customers = customerTypesMapper.selectList(Wrappers.<CustomerTypes>lambdaQuery()
                .orderByAsc(CustomerTypes::getSelectionWeight, CustomerTypes::getId));
        return customers.stream().map(customer -> new CustomerInfoResp(customer.getId(), customer.getCustomerCode(),
                customer.getCustomerName(), customer.getDescription(), customer.getImageUrl(), customer.getEffectType(),
                customer.getEffectValue(), customer.getTriggerChance(), customer.getSelectionWeight(), customer.getStatus())).toList();
    }

    private CustomerTypes pickWeightedCustomer(List<CustomerTypes> customers) {
        int totalWeight = customers.stream()
                .mapToInt(customer -> Math.max(customer.getTriggerChance() == null ? 0 : customer.getTriggerChance(), 0))
                .sum();
        if (totalWeight <= 0) {
            return customers.get(0);
        }
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (CustomerTypes customer : customers) {
            cumulative += Math.max(customer.getTriggerChance() == null ? 0 : customer.getTriggerChance(), 0);
            if (random < cumulative) {
                return customer;
            }
        }
        return customers.get(0);
    }
}
