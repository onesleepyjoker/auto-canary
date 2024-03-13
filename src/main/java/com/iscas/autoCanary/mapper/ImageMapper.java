package com.iscas.autoCanary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iscas.autoCanary.pojo.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
* @author 一只小小丑
* @description 针对表【image(镜像表)】的数据库操作Mapper
* @createDate 2024-02-02 09:48:54
* @Entity com.iscas.autoCanary.pojo.Image
*/
@Mapper
public interface ImageMapper extends BaseMapper<Image> {
    @Update("UPDATE image SET isDelete = 0 WHERE id=#{id}")
    void recoverById(Long id);
}




