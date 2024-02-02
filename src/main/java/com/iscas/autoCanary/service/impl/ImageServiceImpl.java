package com.iscas.autoCanary.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iscas.autoCanary.pojo.Image;
import com.iscas.autoCanary.service.impl.ImageService;
import com.iscas.autoCanary.mapper.ImageMapper;
import org.springframework.stereotype.Service;

/**
* @author 一只小小丑
* @description 针对表【image(镜像表)】的数据库操作Service实现
* @createDate 2024-02-02 09:48:54
*/
@Service
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image>
    implements ImageService{

}




