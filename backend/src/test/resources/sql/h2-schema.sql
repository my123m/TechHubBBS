-- H2-compatible schema for integration tests
-- Must stay in sync with production schema.sql
-- DROP tables first (reverse dependency order) to ensure clean state between test classes

DROP TABLE IF EXISTS "post_similarity" CASCADE;
DROP TABLE IF EXISTS "post_keyword" CASCADE;
DROP TABLE IF EXISTS "ai_qa_history" CASCADE;
DROP TABLE IF EXISTS "ai_summary" CASCADE;
DROP TABLE IF EXISTS "notification" CASCADE;
DROP TABLE IF EXISTS "comment_recommend" CASCADE;
DROP TABLE IF EXISTS "user_like" CASCADE;
DROP TABLE IF EXISTS "favorite" CASCADE;
DROP TABLE IF EXISTS "follow" CASCADE;
DROP TABLE IF EXISTS "post_draft" CASCADE;
DROP TABLE IF EXISTS "comment" CASCADE;
DROP TABLE IF EXISTS "user_profile" CASCADE;
DROP TABLE IF EXISTS "post" CASCADE;
DROP TABLE IF EXISTS "category" CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;

CREATE TABLE IF NOT EXISTS "user" (
    "id"          BIGINT       NOT NULL,
    "username"    VARCHAR(50)  NOT NULL,
    "password"    VARCHAR(255) NOT NULL,
    "email"       VARCHAR(100) DEFAULT NULL,
    "avatar_url"  VARCHAR(500) DEFAULT NULL,
    "bio"         VARCHAR(500) DEFAULT NULL,
    "role"        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    "status"      TINYINT      NOT NULL DEFAULT 1,
    "create_time" TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE ("username"),
    UNIQUE ("email")
);

CREATE TABLE IF NOT EXISTS "category" (
    "id"          BIGINT       NOT NULL,
    "name"        VARCHAR(50)  NOT NULL,
    "description" VARCHAR(255) DEFAULT NULL,
    "sort_order"  INT          NOT NULL DEFAULT 0,
    "status"      TINYINT      NOT NULL DEFAULT 1,
    "create_time" TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE ("name")
);

CREATE TABLE IF NOT EXISTS "post" (
    "id"                   BIGINT       NOT NULL,
    "title"                VARCHAR(200) NOT NULL,
    "content"              CLOB         NOT NULL,
    "category_id"          BIGINT       NOT NULL,
    "author_id"            BIGINT       NOT NULL,
    "type"                 TINYINT      NOT NULL DEFAULT 0,
    "status"               TINYINT      NOT NULL DEFAULT 1,
    "visibility"           TINYINT      NOT NULL DEFAULT 0,
    "view_count"           INT          NOT NULL DEFAULT 0,
    "like_count"           INT          NOT NULL DEFAULT 0,
    "comment_count"        INT          NOT NULL DEFAULT 0,
    "divine_comment_count" INT          NOT NULL DEFAULT 0,
    "eligible_for_divine"  TINYINT      NOT NULL DEFAULT 0,
    "create_time"          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time"          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"              TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id"),
    FOREIGN KEY ("category_id") REFERENCES "category" ("id"),
    FOREIGN KEY ("author_id") REFERENCES "user" ("id")
);

CREATE TABLE IF NOT EXISTS "comment" (
    "id"               BIGINT    NOT NULL,
    "content"          CLOB      NOT NULL,
    "post_id"          BIGINT    NOT NULL,
    "user_id"          BIGINT    NOT NULL,
    "parent_id"        BIGINT    DEFAULT NULL,
    "reply_to_user_id" BIGINT    DEFAULT NULL,
    "like_count"       INT       NOT NULL DEFAULT 0,
    "recommend_count"  INT       NOT NULL DEFAULT 0,
    "is_divine"        TINYINT   NOT NULL DEFAULT 0,
    "divine_time"      TIMESTAMP DEFAULT NULL,
    "create_time"      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    FOREIGN KEY ("post_id") REFERENCES "post" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "comment_recommend" (
    "id"          BIGINT    NOT NULL,
    "comment_id"  BIGINT    NOT NULL,
    "user_id"     BIGINT    NOT NULL,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE ("comment_id", "user_id"),
    FOREIGN KEY ("comment_id") REFERENCES "comment" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "user_like" (
    "id"          BIGINT      NOT NULL,
    "user_id"     BIGINT      NOT NULL,
    "target_type" VARCHAR(10) NOT NULL,
    "target_id"   BIGINT      NOT NULL,
    "create_time" TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE ("user_id", "target_type", "target_id"),
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "favorite" (
    "id"          BIGINT    NOT NULL,
    "user_id"     BIGINT    NOT NULL,
    "post_id"     BIGINT    NOT NULL,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE ("user_id", "post_id"),
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("post_id") REFERENCES "post" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "follow" (
    "id"          BIGINT    NOT NULL,
    "follower_id" BIGINT    NOT NULL,
    "followee_id" BIGINT    NOT NULL,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE ("follower_id", "followee_id"),
    FOREIGN KEY ("follower_id") REFERENCES "user" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("followee_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "post_draft" (
    "id"            BIGINT    NOT NULL,
    "user_id"       BIGINT    NOT NULL,
    "post_id"       BIGINT    DEFAULT NULL,
    "title"         VARCHAR(200) DEFAULT NULL,
    "content"       CLOB      DEFAULT NULL,
    "category_id"   BIGINT    DEFAULT NULL,
    "visibility"    TINYINT   DEFAULT 0,
    "last_saved_at" TIMESTAMP DEFAULT NULL,
    "create_time"   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time"   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE ("user_id", "post_id"),
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "ai_summary" (
    "id"            BIGINT    NOT NULL,
    "user_id"       BIGINT    NOT NULL,
    "post_id"       BIGINT    NOT NULL,
    "content"       CLOB      DEFAULT NULL,
    "status"        TINYINT   NOT NULL DEFAULT 0,
    "error_message" VARCHAR(500) DEFAULT NULL,
    "create_time"   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time"   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE ("user_id", "post_id"),
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("post_id") REFERENCES "post" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "ai_qa_history" (
    "id"          BIGINT    NOT NULL,
    "user_id"     BIGINT    NOT NULL,
    "post_id"     BIGINT    NOT NULL,
    "question"    CLOB      NOT NULL,
    "answer"      CLOB      NOT NULL,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("post_id") REFERENCES "post" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "notification" (
    "id"          BIGINT       NOT NULL,
    "user_id"     BIGINT       NOT NULL,
    "type"        VARCHAR(20)  NOT NULL,
    "source_id"   BIGINT       DEFAULT NULL,
    "content"     VARCHAR(500) NOT NULL,
    "is_read"     TINYINT      NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "user_profile" (
    "user_id"          BIGINT NOT NULL,
    "keyword_weights"  CLOB   DEFAULT NULL,
    "last_update_time" TIMESTAMP DEFAULT NULL,
    PRIMARY KEY ("user_id"),
    FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "post_keyword" (
    "post_id"      BIGINT       NOT NULL,
    "keyword"      VARCHAR(100) NOT NULL,
    "tfidf_weight" DOUBLE       NOT NULL DEFAULT 0,
    PRIMARY KEY ("post_id", "keyword"),
    FOREIGN KEY ("post_id") REFERENCES "post" ("id") ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "post_similarity" (
    "post_id_a"        BIGINT NOT NULL,
    "post_id_b"        BIGINT NOT NULL,
    "similarity_score" DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY ("post_id_a", "post_id_b"),
    FOREIGN KEY ("post_id_a") REFERENCES "post" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("post_id_b") REFERENCES "post" ("id") ON DELETE CASCADE
);
