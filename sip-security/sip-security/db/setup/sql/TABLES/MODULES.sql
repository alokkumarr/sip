DROP TABLE IF EXISTS MODULES;
CREATE TABLE MODULES
(
  MODULE_SYS_ID      BIGINT  NOT NULL,
  MODULE_NAME        VARCHAR(100) NOT NULL,
  MODULE_CODE        VARCHAR(50) NOT NULL,
  MODULE_DESC        VARCHAR(500) NOT NULL,
  ACTIVE_STATUS_IND    TINYINT NOT NULL,
  CREATED_DATE     DATETIME NOT NULL,
  CREATED_BY       VARCHAR(255) NOT NULL,
  INACTIVATED_DATE   DATETIME,
  INACTIVATED_BY     VARCHAR(255),
  MODIFIED_DATE     DATETIME,
  MODIFIED_BY        VARCHAR(255)
)ENGINE=InnoDB;
