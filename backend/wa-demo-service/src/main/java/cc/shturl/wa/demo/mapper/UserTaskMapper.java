package cc.shturl.wa.demo.mapper;

import cc.shturl.wa.demo.entity.UserTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserTaskMapper extends BaseMapper<UserTask> {
}