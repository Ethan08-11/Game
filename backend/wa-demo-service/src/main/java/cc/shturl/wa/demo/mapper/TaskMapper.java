package cc.shturl.wa.demo.mapper;

import cc.shturl.wa.demo.entity.Tasks;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Tasks> {
}
