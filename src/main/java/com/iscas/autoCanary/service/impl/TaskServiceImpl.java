package com.iscas.autoCanary.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iscas.autoCanary.pojo.Task;
import com.iscas.autoCanary.service.TaskService;
import com.iscas.autoCanary.mapper.TaskMapper;
import org.springframework.stereotype.Service;

/**
* @author 86158
* @description 针对表【task(任务表)】的数据库操作Service实现
* @createDate 2024-02-26 14:59:24
*/
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task>
    implements TaskService{

}




