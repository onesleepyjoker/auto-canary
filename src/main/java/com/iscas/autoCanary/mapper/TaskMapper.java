package com.iscas.autoCanary.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iscas.autoCanary.model.dto.TaskDto;
import com.iscas.autoCanary.pojo.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 86158
* @description 针对表【task(任务表)】的数据库操作Mapper
* @createDate 2024-02-26 14:59:24
* @Entity com.iscas.autoCanary.pojo.Task
*/
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("select task.id,user.username,task.isSuccess,task.createTime from task left outer join user on task.userId = user.id")
    IPage<TaskDto> pageTaskAndUserNameAll(Page<Task> page);

}




