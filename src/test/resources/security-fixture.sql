DROP ALL OBJECTS;
CREATE TABLE user(id INT AUTO_INCREMENT PRIMARY KEY,username VARCHAR(50) UNIQUE NOT NULL,password VARCHAR(100) NOT NULL,nickname VARCHAR(50),role INT DEFAULT 0,status INT DEFAULT 1,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE book(id INT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(200),author VARCHAR(100),category VARCHAR(50),cover_url TEXT,description TEXT,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE bookshelf(id INT AUTO_INCREMENT PRIMARY KEY,user_id INT,book_id INT,read_status INT,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(user_id,book_id));
CREATE TABLE rating(id INT AUTO_INCREMENT PRIMARY KEY,user_id INT,book_id INT,score DOUBLE,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE review(id INT AUTO_INCREMENT PRIMARY KEY,user_id INT,book_id INT,content TEXT,status INT DEFAULT 1,like_count INT DEFAULT 0,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE review_like(id INT AUTO_INCREMENT PRIMARY KEY,review_id INT,user_id INT,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
INSERT INTO user(id,username,password,nickname,role,status) VALUES(10,'reader','will-be-hashed','读者',0,1),(20,'other','will-be-hashed','其他人',0,1),(30,'admin','will-be-hashed','管理员',1,1),(40,'legacy','legacy-pass','旧账号',0,1);
INSERT INTO book(id,title,category) VALUES(1,'测试图书','科幻小说');
