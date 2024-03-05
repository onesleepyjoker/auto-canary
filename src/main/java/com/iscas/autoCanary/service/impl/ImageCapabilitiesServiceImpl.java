package com.iscas.autoCanary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.mapper.ImageMapper;
import com.iscas.autoCanary.pojo.Image;
import com.iscas.autoCanary.pojo.ImageCapabilities;
import com.iscas.autoCanary.pojo.output.ImageOutput;
import com.iscas.autoCanary.pojo.output.MarkLineOutput;
import com.iscas.autoCanary.service.ImageCapabilitiesService;
import com.iscas.autoCanary.mapper.ImagecapabilitiesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author windpo
* @description 实现版本标签操作相关接口
*/
@Service
public class ImageCapabilitiesServiceImpl extends ServiceImpl<ImagecapabilitiesMapper, ImageCapabilities>
    implements ImageCapabilitiesService {

    @Resource
    ImageMapper imageMapper;

    @Override
    public void addMark(Long userId,String description ,List<Long> imageIdList,Long imageMappingId) {
        List<Long> versionRoute = new ArrayList<>();
        //version列表去重
        imageIdList = imageIdList.stream().distinct().collect(Collectors.toList());
        //version列表小于2
        if (imageIdList.isEmpty()||imageIdList.size()<2) {
            throw new BusinessException(ErrorCode.VERSION_MAPPING_NUM_LACK,"已去除镜像：版本重复的标签");
        }

        //查询列表的镜像版本是否存在
        for (Long imageId : imageIdList) {
            Image imageRecord = imageMapper.selectOne(new QueryWrapper<Image>().eq("id", imageId));
            if (imageRecord==null) {
                throw new BusinessException(ErrorCode.NO_IMAGES,"镜像id："+imageId+"不存在");
            }
            versionRoute.add(imageId);
        }
        String imageList = versionRoute.toString();

        ImageCapabilities imageCapabilities = new ImageCapabilities();
        //若imageMappingId不存在则为插入，检查传入List是否存在
        if (imageMappingId==null) {
            if (getOne(new QueryWrapper<ImageCapabilities>().eq("imageList",imageList))!=null) {
                throw new BusinessException(ErrorCode.VERSION_MAPPING_EXIST);
            }
            imageCapabilities.setCreateTime(new Date());
        }else{
         //若imageMappingId存在则为更新，检查传入id是否正确
            if(getOne(new QueryWrapper<ImageCapabilities>().eq("id",imageMappingId))==null){
                throw new BusinessException(ErrorCode.NO_VERSION_MAPPING);
            }
            imageCapabilities.setId(imageMappingId);
            imageCapabilities.setUpdateTime(new Date());
        }

        //设置属性并更新
        imageCapabilities.setImageList(imageList);
        imageCapabilities.setDescription(description);
        imageCapabilities.setUserId(userId);
        if (!saveOrUpdate(imageCapabilities)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新该路径失败");
        }
    }

    @Override
    public void delMark(Long imageMappingId) {
        if (!removeById(imageMappingId)) {
            throw new BusinessException(ErrorCode.NO_VERSION_MAPPING,"删除指定映射失败");
        }

    }

    @Override
    public List<MarkLineOutput> getMarks() {
        //根据createTime查询最新8条记录
        List<ImageCapabilities> list = lambdaQuery().orderByDesc(ImageCapabilities::getCreateTime)
                .last("LIMIT 8")
                .list();

        //将ImageCapabilities转化成MarkLineOutput
        ArrayList<ImageOutput> imageList = new ArrayList<>();
        List<MarkLineOutput> res=list.stream().map((imageCapabilities)->{
            MarkLineOutput markLineOutput = new MarkLineOutput();
            markLineOutput.setId(imageCapabilities.getId());
            markLineOutput.setDescription(imageCapabilities.getDescription());
            markLineOutput.setUserId(imageCapabilities.getUserId());
            markLineOutput.setCreateTime(imageCapabilities.getCreateTime());
            markLineOutput.setUpdateTime(imageCapabilities.getUpdateTime());

            //镜像兼容列表[1,2,3]的对应镜像信息[{imageName,version},{imageName:version}]
            Gson gson = new Gson();
            List<Long> versionList= gson.fromJson(imageCapabilities.getImageList(), new TypeToken<List<Long>>() {
            }.getType());
            //select imageName,version by id
            for (Long imageId : versionList) {
                Image imageRecord = imageMapper.selectOne(new QueryWrapper<Image>().eq("id", imageId));
                if (imageRecord!=null) {
                    imageList.add(new ImageOutput(imageRecord.getImageName(),imageRecord.getVersion()));
                }
            }
            markLineOutput.setMarkLine(imageList);
            return markLineOutput;
        }).collect(Collectors.toList());
        return res;
    }

    @Override
    public List<MarkLineOutput> getMark(Long imageId) {
        //判断image是否存在
        Image image = imageMapper.selectOne(new QueryWrapper<Image>().eq("id", imageId));
        if (image==null) {
            throw new BusinessException(ErrorCode.NO_IMAGES,"镜像id："+imageId+"不存在");
        }

        List<ImageCapabilities> list = lambdaQuery().orderByDesc(ImageCapabilities::getCreateTime).list();

        //过滤掉不包含指定imageId的路径，将ImageCapabilities转化成MarkLineOutput
        ArrayList<ImageOutput> imageList = new ArrayList<>();
        List<MarkLineOutput> res = new ArrayList<>();
        for (ImageCapabilities imageCapabilities : list) {
            MarkLineOutput markLineOutput = new MarkLineOutput();
            //镜像兼容列表[1,2,3]的对应镜像信息[{imageName,version},{imageName:version}]
            Gson gson = new Gson();
            List<Long> versionList= gson.fromJson(imageCapabilities.getImageList(), new TypeToken<List<Long>>() {
            }.getType());
            //过滤不包含imageId的路径
            if(!versionList.contains(imageId)){
                continue;
            }

            //select imageName,version by id
            for (Long version : versionList) {
                Image imageRecord = imageMapper.selectOne(new QueryWrapper<Image>().eq("id", version));
                if (imageRecord!=null) {
                    imageList.add(new ImageOutput(imageRecord.getImageName(),imageRecord.getVersion()));
                }
            }
            markLineOutput.setMarkLine(imageList);

            markLineOutput.setId(imageCapabilities.getId());
            markLineOutput.setDescription(imageCapabilities.getDescription());
            markLineOutput.setUserId(imageCapabilities.getUserId());
            markLineOutput.setCreateTime(imageCapabilities.getCreateTime());
            markLineOutput.setUpdateTime(imageCapabilities.getUpdateTime());
            res.add(markLineOutput);
        }
        return res;
    }
}




