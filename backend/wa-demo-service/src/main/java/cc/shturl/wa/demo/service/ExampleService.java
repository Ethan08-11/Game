package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.req.ExampleCreateReq;
import cc.shturl.wa.demo.dto.resp.ExampleResp;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 示例业务接口
 */
public interface ExampleService {

    /**
     * 根据 ID 查询
     */
    ExampleResp getById(Long id);

    /**
     * 分页查询
     */
    Page<ExampleResp> pageList(int pageNum, int pageSize);

    /**
     * 创建
     */
    Long create(ExampleCreateReq req);

    /**
     * 删除
     */
    void deleteById(Long id);
}
