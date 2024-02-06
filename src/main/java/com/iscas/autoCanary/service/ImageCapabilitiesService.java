package com.iscas.autoCanary.service;

import com.iscas.autoCanary.pojo.Image;
import com.iscas.autoCanary.pojo.ImageCapabilities;
import com.baomidou.mybatisplus.extension.service.IService;
import com.iscas.autoCanary.pojo.output.MarkLineOutput;

import java.util.List;

/**
* @author windpo
* @description 对镜像标记的相关操作
*/
public interface ImageCapabilitiesService extends IService<ImageCapabilities> {
    /**
     * 新增一个标记
     * @param userId
     */
    void addMark(Long userId,String description,List<Long> imageIdList,Long imageMappingId);

    /**
     * 删除一个标记
     */
    void delMark(Long imageMappingId);

    /**
     * 获取最近的8个标记
     * @return
     */
    List<MarkLineOutput> getMarks();

    /**
     * 获取某个镜像兼容的标记
     * @param imgName
     * @param version
     * @return
     */
    List<MarkLineOutput> getMark(Long imageId);
}
