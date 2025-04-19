-- 创建库
create database if not exists Music_Repo;
use Music_Repo;

-- 删除表
DROP TABLE IF EXISTS user, music_file;


-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    email        varchar(256)                           null comment '邮箱',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    UNIQUE KEY uk_email (email),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;



-- 音乐文件表
create table if not exists music_file
(
    id              bigint auto_increment comment 'id' primary key,
    url             varchar(512)                       not null comment '音乐文件 url',
    name            varchar(128)                       not null comment '音乐名称',
    artist          varchar(128)                       null comment '艺术家',
    album           varchar(128)                       null comment '专辑',
    introduction    varchar(512)                       null comment '简介',
    category        varchar(64)                        null comment '分类',
    tags            varchar(512)                       null comment '标签（JSON 数组）',
    fileSize        bigint                             null comment '文件大小',
    duration        int                                null comment '时长（秒）',
    bitRate         int                                null comment '比特率',
    fileFormat      varchar(32)                        null comment '文件格式',
    coverUrl        varchar(512)                             null comment '封面图片',
    userId          bigint                             not null comment '创建用户 id',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime        datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于音乐名称的查询性能
    INDEX idx_artist (artist),             -- 提升基于艺术家的查询性能
    INDEX idx_album (album),               -- 提升基于专辑的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索音乐简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_coverId (coverUrl),           -- 提升基于封面ID的查询性能
    INDEX idx_userId (userId)            -- 提升基于用户 ID 的查询性能
) comment '音乐文件' collate = utf8mb4_unicode_ci;

ALTER TABLE music_file
    -- 添加新列
    ADD COLUMN reviewStatus INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512) NULL COMMENT '审核信息',
    ADD COLUMN reviewerId BIGINT NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime DATETIME NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON music_file (reviewStatus);


ALTER TABLE `user` ADD COLUMN `user_status` INT DEFAULT 0 COMMENT '用户状态：0-正常，1-被封禁';
ALTER TABLE `user` ADD COLUMN `banReason` varchar(258)  COMMENT '用户状态：0-正常，1-被封禁';

