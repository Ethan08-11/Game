package cc.shturl.wa.demo.service.impl;

import cc.shturl.wa.api.dto.RemoteUserResp;
import cc.shturl.wa.api.feign.UserFeignClient;
import cc.shturl.wa.common.constant.CommonConstants;
import cc.shturl.wa.common.exception.BusinessException;
import cc.shturl.wa.common.result.Result;
import cc.shturl.wa.demo.dto.req.ExampleCreateReq;
import cc.shturl.wa.demo.dto.resp.ExampleResp;
import cc.shturl.wa.demo.entity.ExampleEntity;
import cc.shturl.wa.demo.mapper.ExampleMapper;
import cc.shturl.wa.demo.mq.producer.ExampleEventProducer;
import cc.shturl.wa.demo.service.ExampleCacheService;
import cc.shturl.wa.demo.service.ExampleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 示例业务实现类（骨架代码，不含具体业务逻辑）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleServiceImpl implements ExampleService {

    private final ExampleMapper exampleMapper;
    private final ExampleEventProducer exampleEventProducer;
    private final ExampleCacheService exampleCacheService;
    private final UserFeignClient userFeignClient;

    @Override
    public ExampleResp getById(Long id) {
        // 优先读 Redis 缓存（由 MQ 消费者异步预热）
        ExampleResp cached = exampleCacheService.getFromCache(id);
        if (cached != null) {
            return cached;
        }

        ExampleEntity entity = exampleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("数据不存在");
        }
        return convertToResp(entity);
    }

    @Override
    public Page<ExampleResp> pageList(int pageNum, int pageSize) {
        Page<ExampleEntity> page = new Page<>(pageNum, pageSize);
        exampleMapper.selectPage(page, new LambdaQueryWrapper<ExampleEntity>()
                .orderByDesc(ExampleEntity::getCreatedAt));

        Page<ExampleResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        respPage.setRecords(page.getRecords().stream().map(this::convertToResp).toList());
        return respPage;
    }

    @Override
    public Long create(ExampleCreateReq req) {
        // TODO: 在此编写具体业务逻辑

        ExampleEntity entity = new ExampleEntity();
        BeanUtils.copyProperties(req, entity);
        exampleMapper.insert(entity);

        // 发布 MQ 事件：异步缓存预热 + 审计记录（不阻塞当前请求）
        exampleEventProducer.publishExampleCreated(entity);

        return entity.getId();
    }

    @Override
    public void deleteById(Long id) {
        int rows = exampleMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("数据不存在");
        }
    }

    /**
     * Feign 远程调用示例（供 Controller 或其他 Service 参考）
     */
    public RemoteUserResp remoteGetUser(Long userId) {
        Result<RemoteUserResp> result = userFeignClient.getUserById(userId);
        if (!result.isSuccess()) {
            throw new BusinessException("远程调用失败: " + result.getMessage());
        }
        return result.getData();
    }

    private ExampleResp convertToResp(ExampleEntity entity) {
        ExampleResp resp = new ExampleResp();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }
}
