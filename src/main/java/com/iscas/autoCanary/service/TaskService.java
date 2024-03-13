package com.iscas.autoCanary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iscas.autoCanary.model.dto.TaskDto;
import com.iscas.autoCanary.pojo.Task;
import com.baomidou.mybatisplus.extension.service.IService;
import com.iscas.autoCanary.pojo.output.LogInfoDTO;

/**
* @author 86158
* @description 针对表【task(任务表)】的数据库操作Service
* @createDate 2024-02-26 14:59:24
*/
public interface TaskService extends IService<Task> {

    /**
     * 根据TaskId更新任务失败原因
     */
    void recordTaskFail(Long taskId,String reason);

    /**
     * 获取Task任务日志
     */
    LogInfoDTO getLog(Long taskId);

    IPage getTaskAndUsername(int pageNum, int pageSize);
}
