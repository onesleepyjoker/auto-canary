package com.iscas.autoCanary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iscas.autoCanary.model.dto.TaskDto;
import com.iscas.autoCanary.pojo.Task;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 86158
* @description 针对表【task(任务表)】的数据库操作Service
* @createDate 2024-02-26 14:59:24
*/
public interface TaskService extends IService<Task> {


    IPage getTaskAndUsername(int pageNum, int pageSize);
}
