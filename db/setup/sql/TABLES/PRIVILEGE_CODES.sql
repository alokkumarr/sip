DROP TABLE IF EXISTS PRIVILEGE_CODES;
CREATE TABLE PRIVILEGE_CODES (
  PRIVILEGE_CODES_SYS_ID BIGINT NOT NULL ,
  PRIVILEGE_CODES_NAME VARCHAR(255) NOT NULL,
  PRIVILEGE_CODES_DESC VARCHAR(255) NOT NULL
) ENGINE=InnoDB;