package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.demo.dto.resp.CustomerInfoResp;
import cc.shturl.wa.demo.entity.Bullies;
import cc.shturl.wa.demo.entity.CustomerTypes;
import cc.shturl.wa.demo.mapper.BulliesMapper;
import cc.shturl.wa.demo.mapper.CustomerTypesMapper;
import cc.shturl.wa.demo.service.CustomerService;
import cc.shturl.wa.demo.service.GameEdition;
import cc.shturl.wa.demo.service.GameEditionService;
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
    private final GameEditionService gameEditionService;

    @Override
    public CustomerInfoResp getCurrentCustomer(Long userId) {
        List<CustomerTypes> customers = visibleCustomers(userId, true);
        if (customers.isEmpty()) {
            return BullyCatalog.toCustomerResp(null, null);
        }
        CustomerTypes customer = pickWeightedCustomer(customers);
        return BullyCatalog.toCustomerResp(customer, findBullyForCustomer(customer.getCustomerCode()));
    }

    @Override
    public List<CustomerInfoResp> listCustomers(Long userId) {
        return visibleCustomers(userId, false).stream()
                .map(customer -> BullyCatalog.toCustomerResp(customer, findBullyForCustomer(customer.getCustomerCode())))
                .toList();
    }

    private List<CustomerTypes> visibleCustomers(Long userId, boolean enabledOnly) {
        boolean publicEdition = gameEditionService.isPublic(userId);
        List<CustomerTypes> customers = customerTypesMapper.selectList(Wrappers.<CustomerTypes>lambdaQuery()
                .orderByAsc(CustomerTypes::getSortNo, CustomerTypes::getId));
        if (customers == null) {
            return List.of();
        }
        return customers.stream()
                .filter(customer -> !enabledOnly || customer.getStatus() == null || customer.getStatus() == 1)
                .filter(customer -> publicEdition ? isPublicCustomer(customer) : isInternalCustomer(customer))
                .toList();
    }

    private boolean isPublicCustomer(CustomerTypes customer) {
        return customer != null
                && GameEdition.PUBLIC.equalsIgnoreCase(customer.getEdition())
                && GameEdition.isPublicCustomer(customer.getCustomerCode());
    }

    private boolean isInternalCustomer(CustomerTypes customer) {
        return customer != null && !isPublicCustomer(customer);
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
