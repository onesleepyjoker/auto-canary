# 数据库初始化
# @author <a href=#">一只小小丑</a>
# @from 中科院软件所

-- 创建库
create database if not exists auto_canary;

-- 切换库
use auto_canary;

# 用户表
create table user
(
    username     varchar(256)                       null comment '用户昵称',
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                       null comment '账号',
    avatarUrl    varchar(1024)                      null comment '用户头像',
    gender       tinyint                            null comment '性别',
    userPassword varchar(512)                       not null comment '密码',
    phone        varchar(128)                       null comment '电话',
    email        varchar(512)                       null comment '邮箱',
    userStatus   int      default 0                 not null comment '状态 0 - 正常',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete     tinyint  default 0                 not null comment '是否删除',
    userRole     int      default 0                 not null comment '用户角色 0 - 普通用户 1 - 管理员',
    planetCode   varchar(512)                       null comment '员工编号'
)
    comment '用户';

# 导入示例用户
INSERT INTO auto_canary.user (username, userAccount, avatarUrl, gender, userPassword, phone, email, userStatus, createTime, updateTime, isDelete, userRole, planetCode) VALUES ('user1', 'auto_canary', 'https://himg.bdimg.com/sys/portraitn/item/public.1.e137c1ac.yS1WqOXfSWEasOYJ2-0pvQ', null, 'b0dd3697a192885d7c055db46155b26a', null, null, 0, '2023-08-06 14:14:22', '2023-08-06 14:39:37', 0, 1, '1');

create table image
(
    imageName varchar(256) null comment '镜像名称',
    id bigint auto_increment comment 'id'
        primary key,
    version varchar(256) default 1 not null comment '版本',
    namespace varchar(256) null comment '版本',
    imageStatus int default 0 not null comment '是否可用',
    imageUrl varchar(256) null comment '镜像地址',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete tinyint default 0 not null comment '是否删除',
    userId int default 0 not null comment '创建镜像的用户id'
) comment '镜像表';

create table imageCapabilities
(
    id bigint auto_increment comment 'id'
        primary key,
    description varchar(255) null comment '描述',
    imageList varchar(1024) null comment '镜像版本数据',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete tinyint default 0 not null comment '是否删除',
    userId int default 0 not null comment '创建镜像关系的用户id'
) comment '镜像关系表';





create table task
(
    id bigint auto_increment comment 'id'
        primary key,
    description varchar(255) null comment '描述',
    isSuccess tinyint default 0 not null comment '是否成功',
    logInformation varchar(255) null comment '日志信息',
    imageList varchar(1024) null comment '镜像版本数据',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete tinyint default 0 not null comment '是否删除',
    userId int default 0 not null comment '创建镜像关系的用户id'
) comment '任务表';