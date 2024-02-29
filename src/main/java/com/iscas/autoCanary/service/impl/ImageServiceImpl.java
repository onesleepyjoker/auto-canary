package com.iscas.autoCanary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaweicloud.sdk.swr.v2.model.ShowReposResp;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.model.dto.ImageQuery;
import com.iscas.autoCanary.pojo.Image;
import com.iscas.autoCanary.pojo.User;
import com.iscas.autoCanary.service.ImageRepoService;
import com.iscas.autoCanary.service.ImageService;
import com.iscas.autoCanary.mapper.ImageMapper;
import com.iscas.autoCanary.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 一只小小丑
 * @description 针对表【image(镜像表)】的数据库操作Service实现
 * @createDate 2024-02-02 09:48:54
 */
@Service
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image>
        implements ImageService {

    @Autowired
    private ImageRepoService imageRepoService;

    @Autowired
    private UserService userService;


    @Override
    public Integer synchronizeImage(HttpServletRequest request,String namespace) {
//        获取华为云镜像列表
        List<ShowReposResp> cceImgList = imageRepoService.getCCEImgList(namespace);
        AtomicInteger num= new AtomicInteger();//保证下面执行的原子性
//        创建镜像将镜像列表循环放到数据库当中
        User loginUser = userService.getLoginUser(request);
        for (ShowReposResp showReposResp : cceImgList) {
            for (String tag : showReposResp.getTags()) {
//                自定义查询条件放到数据库当中进行查询
                ImageQuery imageQuery = new ImageQuery();
                imageQuery.setImageName(showReposResp.getName());
                imageQuery.setNamespace(showReposResp.getNamespace());
                imageQuery.setVersion(tag);
                imageQuery.setImageURL(showReposResp.getPath()+":"+tag);
                List<Image> images = this.listImages(imageQuery);
//                如果查不到对应的数据说明数据库中没有该镜像 加入镜像
                if (images.isEmpty()) {
                    num.getAndIncrement();//记录增加的条数
                    Image image = new Image();
                    image.setImageName(showReposResp.getName());
                    image.setNamespace(showReposResp.getNamespace());
                    image.setVersion(tag);
                    image.setUserId(loginUser.getId());
                    image.setImageUrl(showReposResp.getPath()+":"+tag);
                    this.save(image);
                    num.getAndIncrement();
                }
            }
        }
        return num.get();
    }

    public List<Image> listImages(ImageQuery imageQuery) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        if (imageQuery != null) {
            if (StringUtils.isNotBlank(imageQuery.getImageName())) {
                queryWrapper.eq("imageName", imageQuery.getImageName());
            }
            if (StringUtils.isNotBlank(imageQuery.getNamespace())) {
                queryWrapper.eq("namespace", imageQuery.getNamespace());
            }
            if (StringUtils.isNotBlank(imageQuery.getVersion())) {
                queryWrapper.eq("version", imageQuery.getVersion());
            }
            if (StringUtils.isNotBlank(imageQuery.getImageURL())){
                queryWrapper.eq("imageUrl", imageQuery.getImageURL());
            }
        }
        List<Image> list = this.list(queryWrapper);
        return list;
    }

//    添加镜像的业务层方法 （判断镜像是否符合业务逻辑  保存到数据库当中）
    @Override
    public long  createImage(Image image, User userLogin) {
        if (image == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userLogin == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId=userLogin.getId();

//        判断添加的镜像是否合法
        String imageName = image.getImageName();
        String version = image.getVersion();
        String namespace = image.getNamespace();
        String imageUrl = image.getImageUrl();
        if (StringUtils.isBlank(imageName)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"镜像名称不能为空");
        }
        if (StringUtils.isBlank(version)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"镜像版本不能为空");
        }
        if (StringUtils.isBlank(namespace)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"命名空间不能为空");
        }
        if (StringUtils.isBlank(imageUrl)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"镜像地址不能为空");
        }
        image.setUserId(userId);

//        判断镜像是否已经存在
        ImageQuery imageQuery = new ImageQuery();
        imageQuery.setImageName(image.getImageName());
        imageQuery.setVersion(image.getVersion());
        imageQuery.setNamespace(image.getNamespace());
        imageQuery.setImageURL(image.getImageUrl());
        QueryWrapper<Image> queryWrapper = this.getQueryWrapper(imageQuery);
        Image one = this.getOne(queryWrapper);
        if (one !=null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"镜像已存在");
        }

//        添加镜像
        boolean save = this.saveOrUpdate(image);
        if (!save){
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"创建镜像失败");
        }
        return image.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteImage(long id, User userLogin) {
        Image image = getById(id);
        if (image== null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该镜像不存在");
        }
        if (userLogin == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean b = removeById(id);
        if (!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除镜像失败");
        }
        return b;
    }

    @Override
    public long updateImage(Image image, User userLogin) {
        if (image == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userLogin == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId=userLogin.getId();
//        修改之前先查看是否存在
        Long id = image.getId();
        Image imageOld = this.getById(id);
        if (imageOld == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该镜像不存在");
        }
        String imageName = image.getImageName();
        String version = image.getVersion();
        String namespace = image.getNamespace();
        String imageUrl = image.getImageUrl();
        if (StringUtils.isBlank(imageName)){
            image.setImageName(imageOld.getImageName());
        }
        if (StringUtils.isBlank(version)){
            image.setVersion(imageOld.getVersion());
        }
        if (StringUtils.isBlank(namespace)){
            image.setNamespace(imageOld.getNamespace());
        }
        if (StringUtils.isBlank(imageUrl)){
            image.setImageUrl(imageOld.getImageUrl());
        }
        image.setUserId(userId);
        image.setUpdateTime(Timestamp.valueOf(LocalDateTime.now()));//转换成时间戳的形式保存当前时间
        boolean save = this.updateById(image);
        if (!save){
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"创建镜像失败");
        }
        return image.getId();
    }

//    拼接qw方便分页查询
    public QueryWrapper<Image> getQueryWrapper(ImageQuery imageQuery) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        if (imageQuery != null) {
            if (imageQuery.getId()!= null){
                queryWrapper.eq("id", imageQuery.getId());
            }
            if (StringUtils.isNotBlank(imageQuery.getImageName())) {
                queryWrapper.eq("imageName", imageQuery.getImageName());
            }
            if (StringUtils.isNotBlank(imageQuery.getNamespace())) {
                queryWrapper.eq("namespace", imageQuery.getNamespace());
            }
            if (StringUtils.isNotBlank(imageQuery.getVersion())) {
                queryWrapper.eq("version", imageQuery.getVersion());
            }
            if (StringUtils.isNotBlank(imageQuery.getImageURL())){
                queryWrapper.eq("imageUrl", imageQuery.getImageURL());
            }
        }
        return queryWrapper;
    }

    public String getImageType(long id){
        String stringYaml = this.getById(id).getYaml();
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        Map<String, Object> yamlData = (Map<String, Object>) yaml.load(stringYaml);

        String kind = (String) yamlData.get("kind");
        System.out.println(kind);
        return kind;
    }
}




