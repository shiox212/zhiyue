-- Structure only; no user records or credentials. Run once on an empty test database.
CREATE DATABASE IF NOT EXISTS ebook_recommend_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ebook_recommend_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user (
    id          INT             AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username    VARCHAR(50)     NOT NULL UNIQUE COMMENT '登录账号，全局唯一',
    password    VARCHAR(100)    NOT NULL COMMENT '登录密码（BCrypt哈希存储）',
    nickname    VARCHAR(50)     DEFAULT '热心读者' COMMENT '用户昵称',
    role        TINYINT         DEFAULT 0 COMMENT '角色: 0-普通用户, 1-管理员',
    status      TINYINT         DEFAULT 1 COMMENT '账号状态: 0-已禁用, 1-正常',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE INDEX idx_user_username ON user(username);

CREATE TABLE IF NOT EXISTS book (
    id          INT             AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title       VARCHAR(200)    NOT NULL COMMENT '书名',
    author      VARCHAR(100)    DEFAULT NULL COMMENT '作者',
    category    VARCHAR(50)     DEFAULT NULL COMMENT '分类（如: 科幻小说、历史传记）',
    cover_url   TEXT            DEFAULT NULL COMMENT '封面图片URL（支持内联 SVG data URI）',
    description TEXT            DEFAULT NULL COMMENT '图书简介',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书表';

CREATE INDEX idx_book_category ON book(category);

CREATE INDEX idx_book_title ON book(title(50));

CREATE INDEX idx_book_author ON book(author(30));

CREATE TABLE IF NOT EXISTS rating (
    id          INT             AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id     INT             NOT NULL COMMENT '用户ID（外键）',
    book_id     INT             NOT NULL COMMENT '图书ID（外键）',
    score       DECIMAL(2,1)    NOT NULL COMMENT '评分: 0.0 ~ 5.0，保留1位小数',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',

    -- 唯一约束：防止同一用户对同一本书重复评分
    CONSTRAINT uk_rating_user_book UNIQUE (user_id, book_id),

    -- 外键约束：级联删除，用户或图书被删除时自动清理关联评分
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE

) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户评分表';

CREATE INDEX idx_rating_book_id ON rating(book_id);

CREATE TABLE IF NOT EXISTS review (
    id          INT             AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id     INT             NOT NULL COMMENT '用户ID（外键）',
    book_id     INT             NOT NULL COMMENT '图书ID（外键）',
    content     TEXT            NOT NULL COMMENT '评论正文',
    status      TINYINT         DEFAULT 1 COMMENT '审核状态: 0-待审核, 1-已通过',
    like_count  INT             DEFAULT 0 COMMENT '点赞数',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',

    -- 外键约束
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE

) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户书评表';

CREATE INDEX idx_review_book_status ON review(book_id, status);

CREATE TABLE IF NOT EXISTS review_like (
    id          INT             AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    review_id   INT             NOT NULL COMMENT '书评ID（外键）',
    user_id     INT             NOT NULL COMMENT '用户ID（外键）',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

    CONSTRAINT uk_review_like UNIQUE (review_id, user_id),
    CONSTRAINT fk_review_like_review FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_like_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE

) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '书评点赞记录表';

CREATE TABLE IF NOT EXISTS bookshelf (
    id          INT             AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id     INT             NOT NULL COMMENT '用户ID（外键）',
    book_id     INT             NOT NULL COMMENT '图书ID（外键）',
    read_status TINYINT         DEFAULT 0 COMMENT '阅读状态: 0-想读, 1-在读, 2-已读',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '加入书架时间',

    -- 唯一约束：防止同一用户重复添加同一本书
    CONSTRAINT uk_bookshelf_user_book UNIQUE (user_id, book_id),

    -- 外键约束
    CONSTRAINT fk_bookshelf_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookshelf_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE

) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户书架表';

CREATE INDEX idx_bookshelf_user_id ON bookshelf(user_id);
