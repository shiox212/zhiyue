USE ebook_recommend_db;

SELECT VERSION() AS mysql_version;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'ebook_recommend_db'
ORDER BY table_name;

SELECT column_name, column_type, is_nullable, column_key, column_default
FROM information_schema.columns
WHERE table_schema = 'ebook_recommend_db'
  AND table_name = 'user'
ORDER BY ordinal_position;

SELECT COUNT(*) AS user_count FROM user;
SELECT COUNT(*) AS book_count FROM book;
SELECT COUNT(*) AS bookshelf_count FROM bookshelf;
SELECT COUNT(*) AS rating_count FROM rating;
SELECT COUNT(*) AS review_count FROM review;
