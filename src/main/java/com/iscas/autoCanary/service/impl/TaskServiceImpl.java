package com.iscas.autoCanary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iscas.autoCanary.common.PageRequest;
import com.iscas.autoCanary.model.dto.TaskDto;
import com.iscas.autoCanary.pojo.Task;
import com.iscas.autoCanary.pojo.User;
import com.iscas.autoCanary.service.TaskService;
import com.iscas.autoCanary.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
* @author 86158
* @description 针对表【task(任务表)】的数据库操作Service实现
* @createDate 2024-02-26 14:59:24
*/
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task>
    implements TaskService{

    @Resource
    private TaskMapper taskMapper;

    @Override
    public IPage<TaskDto> getTaskAndUsername(int pageNum, int pageSize) {
        Page<Task> page = new Page<>(pageNum,pageSize);
        IPage<TaskDto> iPage = taskMapper.pageTaskAndUserNameAll(page);
        return iPage;
    }
}




