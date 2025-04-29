-- Create database
create database if not exists music;
use music;

-- Drop tables
DROP TABLE IF EXISTS user, music_file;


-- User table
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment 'Account',
    userPassword varchar(512)                           not null comment 'Password',
    email        varchar(256)                           null comment 'Email',
    userName     varchar(256)                           null comment 'Username',
    userAvatar   varchar(1024)                          null comment 'Avatar',
    userProfile  varchar(512)                           null comment 'Profile',
    userRole     varchar(256) default 'user'            not null comment 'Role: user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment 'Edit time',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment 'Create time',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Update time',
    isDelete     tinyint      default 0                 not null comment 'Is deleted',
    UNIQUE KEY uk_userAccount (userAccount),
    UNIQUE KEY uk_email (email),
    INDEX idx_userName (userName)
) comment 'User' collate = utf8mb4_unicode_ci;



-- Music file table
create table if not exists music_file
(
    id              bigint auto_increment comment 'id' primary key,
    url             varchar(512)                       not null comment 'Music file url',
    name            varchar(128)                       not null comment 'Music name',
    artist          varchar(128)                       null comment 'Artist',
    album           varchar(128)                       null comment 'Album',
    introduction    varchar(512)                       null comment 'Introduction',
    category        varchar(64)                        null comment 'Category',
    tags            varchar(512)                       null comment 'Tags (JSON array)',
    fileSize        bigint                             null comment 'File size',
    duration        int                                null comment 'Duration (seconds)',
    bitRate         int                                null comment 'Bit rate',
    fileFormat      varchar(32)                        null comment 'File format',
    coverUrl        varchar(512)                             null comment 'Cover image',
    userId          bigint                             not null comment 'Creator user id',
    createTime      datetime default CURRENT_TIMESTAMP not null comment 'Create time',
    editTime        datetime default CURRENT_TIMESTAMP not null comment 'Edit time',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Update time',
    isDelete        tinyint  default 0                 not null comment 'Is deleted',
    INDEX idx_name (name),                 -- Improve query performance based on music name
    INDEX idx_artist (artist),             -- Improve query performance based on artist
    INDEX idx_album (album),               -- Improve query performance based on album
    INDEX idx_introduction (introduction), -- For fuzzy search of music introduction
    INDEX idx_category (category),         -- Improve query performance based on category
    INDEX idx_tags (tags),                 -- Improve query performance based on tags
    INDEX idx_coverId (coverUrl),           -- Improve query performance based on cover ID
    INDEX idx_userId (userId)            -- Improve query performance based on user ID
) comment 'Music file' collate = utf8mb4_unicode_ci;

ALTER TABLE music_file
    -- Add new columns
    ADD COLUMN reviewStatus INT DEFAULT 0 NOT NULL COMMENT 'Review status: 0-Pending; 1-Approved; 2-Rejected',
    ADD COLUMN reviewMessage VARCHAR(512) NULL COMMENT 'Review message',
    ADD COLUMN reviewerId BIGINT NULL COMMENT 'Reviewer ID',
    ADD COLUMN reviewTime DATETIME NULL COMMENT 'Review time';

-- Create index based on reviewStatus column
CREATE INDEX idx_reviewStatus ON music_file (reviewStatus);

ALTER TABLE `user` ADD COLUMN `user_status` INT DEFAULT 0 COMMENT 'User status: 0-Normal, 1-Banned';
ALTER TABLE `user` ADD COLUMN `banReason` varchar(258)  COMMENT 'Ban reason';
ALTER TABLE user ADD COLUMN banNumber INT DEFAULT 0 COMMENT 'banNumber';

