CREATE DATABASE IF NOT EXISTS sight;

-- 레거시 시스템의 테이블에 PK가 존재하지 않는 형태로 정의되어 있어 Entity로 선언할 수 없습니다.
-- 따라서, 테이블을 init query에서 정의하도록 합니다.
CREATE TABLE `khlug_group_member` (
  `group` bigint NOT NULL,
  `member` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
