package com.iscas.autoCanary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iscas.autoCanary.common.PageRequest;
import com.iscas.autoCanary.model.dto.TaskDto;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.pojo.Task;
import com.iscas.autoCanary.pojo.User;
import com.iscas.autoCanary.pojo.output.LogInfoDTO;
import com.iscas.autoCanary.service.TaskService;
import com.iscas.autoCanary.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
* @author windpo
* @description 针对表【task(任务表)】的数据库操作Service实现
* @createDate 2024-02-26 14:59:24
*/
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task>
    implements TaskService{

    @Resource
    private TaskMapper taskMapper;

    /**
     * 根据TaskId更新任务失败原因
     * @param taskId
     * @param reason
     */
    public void recordTaskFail(Long taskId,String reason){
        Task task = new Task();
        task.setId(taskId);
        task.setReason(reason);
        if (!updateById(task)) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
    }

    @Override
    public LogInfoDTO getLog(Long taskId) {
        //获取任务
        Task task = getById(taskId);
        Optional.ofNullable(task).orElseThrow(()-> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        LogInfoDTO logInfoDTO = new LogInfoDTO();
        logInfoDTO.setIsSuccess(task.getIsSuccess()==0);
        List<String> logList = Arrays.asList(Optional
                .ofNullable(task.getLogInformation())
                .orElseThrow(()->new BusinessException(ErrorCode.TASK_LOG_NULL))
                .split("\\s+"));
        logInfoDTO.setLog(logList);
        return logInfoDTO;
    }

    @Override
    public IPage<TaskDto> getTaskAndUsername(int pageNum, int pageSize) {
        Page<Task> page = new Page<>(pageNum,pageSize);
        IPage<TaskDto> iPage = taskMapper.pageTaskAndUserNameAll(page);
        return iPage;
    }
}




