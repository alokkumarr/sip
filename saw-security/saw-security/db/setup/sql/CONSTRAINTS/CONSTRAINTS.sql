ALTER TABLE PRODUCTS ADD CONSTRAINT PRODUCTS_PK PRIMARY KEY (PRODUCT_SYS_ID); 
ALTER TABLE PRODUCTS CHANGE PRODUCT_SYS_ID PRODUCT_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE PRODUCTS AUTO_INCREMENT = 1;

ALTER TABLE MODULES ADD CONSTRAINT MODULES_PK PRIMARY KEY (MODULE_SYS_ID); 
ALTER TABLE MODULES CHANGE MODULE_SYS_ID MODULE_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE MODULES AUTO_INCREMENT = 1;

ALTER TABLE PRODUCT_MODULES ADD CONSTRAINT PRODUCT_MODULES_PK PRIMARY KEY (PROD_MOD_SYS_ID); 
ALTER TABLE PRODUCT_MODULES CHANGE PROD_MOD_SYS_ID PROD_MOD_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE PRODUCT_MODULES AUTO_INCREMENT = 1;
ALTER TABLE PRODUCT_MODULES ADD CONSTRAINT PRODUCT_MODULE_FK_1 FOREIGN KEY (PRODUCT_SYS_ID) REFERENCES PRODUCTS(PRODUCT_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE PRODUCT_MODULES ADD CONSTRAINT PRODUCT_MODULE_FK_2 FOREIGN KEY (MODULE_SYS_ID) REFERENCES MODULES(MODULE_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE CUSTOMERS ADD CONSTRAINT CUSTOMERS_PK PRIMARY KEY (CUSTOMER_SYS_ID); 
ALTER TABLE CUSTOMERS CHANGE CUSTOMER_SYS_ID CUSTOMER_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE CUSTOMERS AUTO_INCREMENT = 1;

ALTER TABLE CUSTOMER_PRODUCTS ADD CONSTRAINT CUSTOMER_PRODUCTS_PK PRIMARY KEY (CUST_PROD_SYS_ID); 
ALTER TABLE CUSTOMER_PRODUCTS CHANGE CUST_PROD_SYS_ID CUST_PROD_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE CUSTOMER_PRODUCTS AUTO_INCREMENT = 1;
ALTER TABLE CUSTOMER_PRODUCTS ADD CONSTRAINT CUSTOMER_PRODUCTS_FK_1 FOREIGN KEY (CUSTOMER_SYS_ID) REFERENCES CUSTOMERS(CUSTOMER_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE CUSTOMER_PRODUCTS ADD CONSTRAINT CUSTOMER_PRODUCTS_FK_2 FOREIGN KEY (PRODUCT_SYS_ID) REFERENCES PRODUCTS(PRODUCT_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE CUSTOMER_PRODUCT_MODULES ADD CONSTRAINT CUSTOMER_PRODUCT_MODULES_PK PRIMARY KEY (CUST_PROD_MOD_SYS_ID); 
ALTER TABLE CUSTOMER_PRODUCT_MODULES CHANGE CUST_PROD_MOD_SYS_ID CUST_PROD_MOD_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE CUSTOMER_PRODUCT_MODULES AUTO_INCREMENT = 1;
ALTER TABLE CUSTOMER_PRODUCT_MODULES ADD CONSTRAINT CUSTOMER_PRODUCT_MODULES_FK_2 FOREIGN KEY (PROD_MOD_SYS_ID) REFERENCES PRODUCT_MODULES(PROD_MOD_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE CUSTOMER_PRODUCT_MODULES ADD CONSTRAINT CUSTOMER_PRODUCT_MODULES_FK_3 FOREIGN KEY (CUSTOMER_SYS_ID) REFERENCES CUSTOMERS(CUSTOMER_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE CUSTOMER_PRODUCT_MODULE_FEATURES ADD CONSTRAINT CUST_PROD_MOD_FEATURE_SYS_ID PRIMARY KEY (CUST_PROD_MOD_FEATURE_SYS_ID); 
ALTER TABLE CUSTOMER_PRODUCT_MODULE_FEATURES CHANGE CUST_PROD_MOD_FEATURE_SYS_ID CUST_PROD_MOD_FEATURE_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE CUSTOMER_PRODUCT_MODULE_FEATURES AUTO_INCREMENT = 1;
ALTER TABLE CUSTOMER_PRODUCT_MODULE_FEATURES ADD CONSTRAINT CUSTOMER_PRODUCT_MODULE_FEATURES_FK_1 FOREIGN KEY (CUST_PROD_MOD_SYS_ID) REFERENCES CUSTOMER_PRODUCT_MODULES(CUST_PROD_MOD_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE CUSTOMER_PRODUCT_MODULE_FEATURES ADD CONSTRAINT CUSTOMER_PRODUCT_MODULE_FEATURES_UK  UNIQUE (FEATURE_CODE);

ALTER TABLE ROLES ADD CONSTRAINT ROLE_SYS_ID_PK PRIMARY KEY (ROLE_SYS_ID); 
ALTER TABLE ROLES CHANGE ROLE_SYS_ID ROLE_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE ROLES AUTO_INCREMENT = 1;
ALTER TABLE ROLES ADD CONSTRAINT ROLES_FK_1 FOREIGN KEY (CUSTOMER_SYS_ID) REFERENCES CUSTOMERS(CUSTOMER_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE ROLES_TYPE ADD CONSTRAINT ROLES_TYPE_SYS_ID_PK PRIMARY KEY (ROLES_TYPE_SYS_ID); 
ALTER TABLE ROLES_TYPE CHANGE ROLES_TYPE_SYS_ID ROLES_TYPE_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE ROLES_TYPE AUTO_INCREMENT = 1;
ALTER TABLE ROLES_TYPE ADD CONSTRAINT ROLES_TYPE_UK  UNIQUE (ROLES_TYPE_NAME);

ALTER TABLE ANALYSIS ADD CONSTRAINT ANALYSIS_PK PRIMARY KEY (ANALYSIS_SYS_ID);
ALTER TABLE ANALYSIS CHANGE ANALYSIS_SYS_ID ANALYSIS_SYS_ID BIGINT AUTO_INCREMENT;
ALTER TABLE ANALYSIS AUTO_INCREMENT=1;
ALTER TABLE ANALYSIS ADD CONSTRAINT ANALYSIS_UK  UNIQUE (ANALYSIS_ID);

ALTER TABLE `PRIVILEGE_CODES` ADD CONSTRAINT PRIVILEGE_CODES_PK PRIMARY KEY (PRIVILEGE_CODES_SYS_ID); 
ALTER TABLE `PRIVILEGE_CODES` CHANGE PRIVILEGE_CODES_SYS_ID PRIVILEGE_CODES_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE `PRIVILEGE_CODES` AUTO_INCREMENT = 1;
ALTER TABLE PRIVILEGE_CODES ADD CONSTRAINT PRIVILEGE_CODES_UK  UNIQUE (PRIVILEGE_CODES_NAME);

ALTER TABLE `PRIVILEGE_GROUPS` ADD CONSTRAINT PRIVILEGE_GROUPS_PK PRIMARY KEY (PRIVILEGE_GRP_SYS_ID); 
ALTER TABLE `PRIVILEGE_GROUPS` CHANGE PRIVILEGE_GRP_SYS_ID PRIVILEGE_GRP_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE `PRIVILEGE_GROUPS` AUTO_INCREMENT = 1;


ALTER TABLE `PRIVILEGES` ADD CONSTRAINT PRIVILEGES_PK PRIMARY KEY (PRIVILEGE_SYS_ID); 
ALTER TABLE `PRIVILEGES` CHANGE PRIVILEGE_SYS_ID PRIVILEGE_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE `PRIVILEGES` AUTO_INCREMENT = 1;
ALTER TABLE `PRIVILEGES` ADD CONSTRAINT PRIVILEGES_FK_3 FOREIGN KEY (CUST_PROD_SYS_ID) REFERENCES CUSTOMER_PRODUCTS(CUST_PROD_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `PRIVILEGES` ADD CONSTRAINT PRIVILEGES_FK_4 FOREIGN KEY (ROLE_SYS_ID) REFERENCES ROLES(ROLE_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `PRIVILEGE_GROUP_CODES` ADD CONSTRAINT PRIVILEGE_GROUP_CODES_PK PRIMARY KEY (PRIVILEGE_GROUP_CODES_SYS_ID); 
ALTER TABLE `PRIVILEGE_GROUP_CODES` CHANGE PRIVILEGE_GROUP_CODES_SYS_ID PRIVILEGE_GROUP_CODES_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE `PRIVILEGE_GROUP_CODES` AUTO_INCREMENT = 1;
ALTER TABLE PRIVILEGE_GROUP_CODES ADD CONSTRAINT PRIVILEGE_GROUP_CODES_FK_1 FOREIGN KEY (PRIVILEGE_CODES_SYS_ID) REFERENCES PRIVILEGE_CODES(PRIVILEGE_CODES_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE PRIVILEGE_GROUP_CODES ADD CONSTRAINT PRIVILEGE_GROUP_CODES_FK_2 FOREIGN KEY (PRIVILEGE_GRP_SYS_ID) REFERENCES `PRIVILEGE_GROUPS`(PRIVILEGE_GRP_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;


ALTER TABLE USERS ADD CONSTRAINT USERS_PK PRIMARY KEY (USER_SYS_ID);
ALTER TABLE USERS ADD CONSTRAINT USERS_UK  UNIQUE (USER_ID);
ALTER TABLE USERS CHANGE USER_SYS_ID USER_SYS_ID BIGINT AUTO_INCREMENT;
ALTER TABLE USERS AUTO_INCREMENT = 1;
ALTER TABLE USERS ADD CONSTRAINT USERS_FK_1 FOREIGN KEY (CUSTOMER_SYS_ID) REFERENCES CUSTOMERS(CUSTOMER_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE USERS ADD CONSTRAINT USERS_FK_2 FOREIGN KEY (ROLE_SYS_ID) REFERENCES ROLES(ROLE_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE PASSWORD_HISTORY ADD CONSTRAINT PASSWORD_HISTORY_PK  PRIMARY KEY (PASSWORD_HISTORY_SYS_ID);
ALTER TABLE PASSWORD_HISTORY CHANGE PASSWORD_HISTORY_SYS_ID PASSWORD_HISTORY_SYS_ID BIGINT AUTO_INCREMENT;
ALTER TABLE PASSWORD_HISTORY AUTO_INCREMENT = 1;
ALTER TABLE PASSWORD_HISTORY ADD CONSTRAINT PASSWORD_HISTORY_FK_1 FOREIGN KEY (USER_SYS_ID) REFERENCES USERS(USER_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE RESET_PWD_DTLS ADD CONSTRAINT RESET_PWD_DTLS_PK PRIMARY KEY (RESET_PWD_DTLS_SYS_ID); 
ALTER TABLE RESET_PWD_DTLS CHANGE RESET_PWD_DTLS_SYS_ID RESET_PWD_DTLS_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE RESET_PWD_DTLS AUTO_INCREMENT = 1;

ALTER TABLE SEC_GROUP ADD CONSTRAINT SEC_GROUP_SYS_ID_PK PRIMARY KEY (SEC_GROUP_SYS_ID);
ALTER TABLE SEC_GROUP CHANGE COLUMN SEC_GROUP_SYS_ID SEC_GROUP_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE SEC_GROUP AUTO_INCREMENT = 1;

ALTER TABLE SEC_GROUP_DSK_ATTRIBUTE ADD CONSTRAINT SEC_GROUP_DSK_ATTRIBUTE_SYS_ID_PK PRIMARY KEY (SEC_GROUP_DSK_ATTRIBUTE_SYS_ID);
ALTER TABLE SEC_GROUP_DSK_ATTRIBUTE CHANGE COLUMN SEC_GROUP_DSK_ATTRIBUTE_SYS_ID SEC_GROUP_DSK_ATTRIBUTE_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE SEC_GROUP_DSK_ATTRIBUTE AUTO_INCREMENT = 1;
ALTER TABLE SEC_GROUP_DSK_ATTRIBUTE ADD CONSTRAINT FK_SEC_GROUP_ID FOREIGN KEY (SEC_GROUP_SYS_ID) REFERENCES SEC_GROUP (SEC_GROUP_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE SEC_GROUP_DSK_VALUE ADD CONSTRAINT SEC_GROUP_DSK_VALUE_SYS_ID_PK PRIMARY KEY (SEC_GROUP_DSK_VALUE_SYS_ID);
ALTER TABLE SEC_GROUP_DSK_VALUE CHANGE COLUMN SEC_GROUP_DSK_VALUE_SYS_ID SEC_GROUP_DSK_VALUE_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE SEC_GROUP_DSK_VALUE AUTO_INCREMENT = 1;
ALTER TABLE SEC_GROUP_DSK_VALUE ADD CONSTRAINT FK_SEC_GROUP_DSK_ATTRIBUTE_SYS_ID FOREIGN KEY (SEC_GROUP_DSK_ATTRIBUTE_SYS_ID) REFERENCES SEC_GROUP_DSK_ATTRIBUTE (SEC_GROUP_DSK_ATTRIBUTE_SYS_ID) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE CONFIG_VAL ADD CONSTRAINT CONFIG_VAL_SYS_ID_PK PRIMARY KEY (CONFIG_VAL_SYS_ID);
ALTER TABLE CONFIG_VAL CHANGE COLUMN CONFIG_VAL_SYS_ID CONFIG_VAL_SYS_ID BIGINT AUTO_INCREMENT NOT NULL;
ALTER TABLE CONFIG_VAL AUTO_INCREMENT = 1;
ALTER TABLE CONFIG_VAL ADD UNIQUE INDEX `CONFIG_VAL_CODE_UNIQUE` (`CONFIG_VAL_CODE` ASC);

