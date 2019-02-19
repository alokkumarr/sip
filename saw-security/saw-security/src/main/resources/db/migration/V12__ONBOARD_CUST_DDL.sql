/*******************************************************************************
 Filename:  V12__ONBOARD_CUST_DDL.sql
 Purpose:   To on-board new customer.
 Date:      13-02-2019
********************************************************************************/

/*******************************************************************************
 Stored Procedure Scripts Starts
********************************************************************************/

DROP PROCEDURE IF EXISTS onboard_customer ;

DELIMITER //
CREATE PROCEDURE onboard_customer (IN l_customer_code varchar(50) , IN l_product_name varchar(50), IN l_product_code varchar(50), IN l_email varchar(50), IN l_first_name varchar(50), IN l_middle_name varchar(50), IN l_last_name varchar(50))

 BEGIN


   DECLARE l_customer_sys_id  INT ;
   DECLARE l_product_sys_id  INT ;
   DECLARE l_module_sys_id_analyze  INT ;
   DECLARE l_module_sys_id_observe  INT ;
   DECLARE l_module_sys_id_workbench  INT ;

   DECLARE l_cust_prod_sys_id INT ;
   DECLARE l_prod_mod_sys_id_analyze INT ;
   DECLARE l_prod_mod_sys_id_observe INT ;
   DECLARE l_prod_mod_sys_id_workbench INT ;

   DECLARE l_cust_prod_mod_sys_id  INT ;
   DECLARE l_cust_prod_mod_feature_sys_id INT;
   DECLARE l_role_sys_id INT;
   DECLARE l_user_sys_id INT;
   DECLARE l_privilege_sys_id INT;
   DECLARE l_incremental_product_sys_id INT;
   DECLARE l_incremental_prod_mod_sys_id INT ;

 DECLARE exit handler for sqlexception

  BEGIN
    -- ERROR
    ROLLBACK;
    SELECT 'Error occured';
  END;

  DECLARE exit handler for sqlwarning
  BEGIN
    -- WARNING
    ROLLBACK;
    SELECT 'Warning occured';
  END;

  START TRANSACTION;


SELECT max(PRODUCT_SYS_ID)+1 into l_incremental_product_sys_id from PRODUCTS;


IF NOT exists(Select PRODUCT_NAME from Products where PRODUCT_NAME=l_product_name)
THEN
INSERT INTO PRODUCTS (PRODUCT_SYS_ID,PRODUCT_NAME,PRODUCT_CODE,PRODUCT_DESC,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
SELECT l_incremental_product_sys_id PRODUCT_SYS_ID,l_product_name PRODUCT_NAME,l_product_code PRODUCT_CODE,l_product_name PRODUCT_DESC,1 ACTIVE_STATUS_IND,Now() CREATED_DATE,'admin' CREATED_BY,NULL INACTIVATED_DATE,NULL INACTIVATED_BY,
NULL MODIFIED_DATE,NULL MODIFIED_BY;
END IF;

SELECT max(PROD_MOD_SYS_ID)+1 into l_incremental_prod_mod_sys_id from PRODUCT_MODULES;

select PRODUCT_SYS_ID into l_product_sys_id
from PRODUCTS
where PRODUCT_NAME = l_product_name;

select MODULE_SYS_ID into l_module_sys_id_analyze from MODULES where MODULE_NAME = 'ANALYZE' ;

select  MODULE_SYS_ID into l_module_sys_id_observe from MODULES where MODULE_NAME = 'OBSERVE' LIMIT 1 ;

select  MODULE_SYS_ID into l_module_sys_id_workbench from MODULES where MODULE_NAME = 'WORKBENCH' ;

INSERT INTO PRODUCT_MODULES (PROD_MOD_SYS_ID,PRODUCT_SYS_ID,MODULE_SYS_ID,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
SELECT l_incremental_prod_mod_sys_id PROD_MOD_SYS_ID,l_product_sys_id PRODUCT_SYS_ID,l_module_sys_id_analyze MODULE_SYS_ID,1 ACTIVE_STATUS_IND,Now() CREATED_DATE,'admin' CREATED_BY,
NULL INACTIVATED_DATE,NULL INACTIVATED_BY,NULL MODIFIED_DATE,NULL MODIFIED_BY;

INSERT INTO PRODUCT_MODULES (PROD_MOD_SYS_ID,PRODUCT_SYS_ID,MODULE_SYS_ID,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
SELECT l_incremental_prod_mod_sys_id+1 PROD_MOD_SYS_ID,l_product_sys_id PRODUCT_SYS_ID,l_module_sys_id_observe MODULE_SYS_ID,1 ACTIVE_STATUS_IND,Now() CREATED_DATE,'admin' CREATED_BY,
NULL INACTIVATED_DATE,NULL INACTIVATED_BY,NULL MODIFIED_DATE,NULL MODIFIED_BY;

INSERT INTO PRODUCT_MODULES (PROD_MOD_SYS_ID,PRODUCT_SYS_ID,MODULE_SYS_ID,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
SELECT l_incremental_prod_mod_sys_id+2 PROD_MOD_SYS_ID,l_product_sys_id PRODUCT_SYS_ID,l_module_sys_id_workbench MODULE_SYS_ID,1 ACTIVE_STATUS_IND,Now() CREATED_DATE,'admin' CREATED_BY,
NULL INACTIVATED_DATE,NULL INACTIVATED_BY,NULL MODIFIED_DATE,NULL MODIFIED_BY;

select max(customer_sys_id)+1 into l_customer_sys_id  from customers;
select l_customer_sys_id;




INSERT INTO CUSTOMERS (CUSTOMER_SYS_ID,CUSTOMER_CODE,COMPANY_NAME,COMPANY_BUSINESS,LANDING_PROD_SYS_ID,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY,PASSWORD_EXPIRY_DAYS,DOMAIN_NAME)
SELECT l_customer_sys_id CUSTOMER_SYS_ID,l_customer_code CUSTOMER_CODE,l_customer_code COMPANY_NAME,'Telecommunication' COMPANY_BUSINESS,
l_product_sys_id LANDING_PROD_SYS_ID,1 ACTIVE_STATUS_IND,Now() CREATED_DATE,'onboard' CREATED_BY,NULL INACTIVATED_DATE,NULL INACTIVATED_BY,
NULL MODIFIED_DATE,NULL MODIFIED_BY,
360 PASSWORD_EXPIRY_DAYS,concat(l_customer_code,'.COM') DOMAIN_NAME;

select max(cust_prod_sys_id)+1 into l_cust_prod_sys_id from customer_products;

INSERT INTO CUSTOMER_PRODUCTS (CUST_PROD_SYS_ID,CUSTOMER_SYS_ID,PRODUCT_SYS_ID,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
Select l_cust_prod_sys_id CUST_PROD_SYS_ID,l_customer_sys_id CUSTOMER_SYS_ID,l_product_sys_id PRODUCT_SYS_ID,1 ACTIVE_STATUS_IND,
Now() CREATED_DATE,'onboard' CREATED_BY,NULL INACTIVATED_DATE,NULL INACTIVATED_BY,NULL MODIFIED_DATE,NULL MODIFIED_BY;



select PROD_MOD_SYS_ID    into l_prod_mod_sys_id_analyze  from PRODUCT_MODULES  where PRODUCT_SYS_ID = l_product_sys_id  and MODULE_SYS_ID = l_module_sys_id_analyze ;


 select max(cust_prod_mod_sys_id)+1 into l_cust_prod_mod_sys_id from customer_product_modules ;

 INSERT INTO CUSTOMER_PRODUCT_MODULES (CUST_PROD_MOD_SYS_ID,CUST_PROD_SYS_ID,PROD_MOD_SYS_ID,CUSTOMER_SYS_ID,ACTIVE_STATUS_IND,MODULE_URL,`DEFAULT`,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
 SELECT  l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID, l_cust_prod_sys_id CUST_PROD_SYS_ID, l_prod_mod_sys_id_analyze PROD_MOD_SYS_ID,
 l_customer_sys_id CUSTOMER_SYS_ID, 1 ACTIVE_STATUS_IND, '/' MODULE_URL, 1  'DEFAULT', now() CREATED_DATE, 'onboard' CREATED_BY,
 NULL INACTIVATED_DATE, NULL INACTIVATED_BY, NULL MODIFIED_DATE, NULL MODIFIED_BY;

 select PROD_MOD_SYS_ID    into l_prod_mod_sys_id_observe from PRODUCT_MODULES  where PRODUCT_SYS_ID = l_product_sys_id and MODULE_SYS_ID = l_module_sys_id_observe ;


 INSERT INTO CUSTOMER_PRODUCT_MODULES (CUST_PROD_MOD_SYS_ID,CUST_PROD_SYS_ID,PROD_MOD_SYS_ID,CUSTOMER_SYS_ID,ACTIVE_STATUS_IND,MODULE_URL,`DEFAULT`,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
 SELECT  l_cust_prod_mod_sys_id+1 CUST_PROD_MOD_SYS_ID, l_cust_prod_sys_id CUST_PROD_SYS_ID, l_prod_mod_sys_id_observe PROD_MOD_SYS_ID,
 l_customer_sys_id CUSTOMER_SYS_ID, 1 ACTIVE_STATUS_IND, '/' MODULE_URL, 0  'DEFAULT', now() CREATED_DATE,
 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY, NULL MODIFIED_DATE, NULL MODIFIED_BY;

 select PROD_MOD_SYS_ID    into l_prod_mod_sys_id_workbench from PRODUCT_MODULES  where PRODUCT_SYS_ID = l_product_sys_id and MODULE_SYS_ID = l_module_sys_id_workbench ;

 INSERT INTO CUSTOMER_PRODUCT_MODULES (CUST_PROD_MOD_SYS_ID,CUST_PROD_SYS_ID,PROD_MOD_SYS_ID,CUSTOMER_SYS_ID,ACTIVE_STATUS_IND,MODULE_URL,`DEFAULT`,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
 SELECT  l_cust_prod_mod_sys_id+2 CUST_PROD_MOD_SYS_ID, l_cust_prod_sys_id CUST_PROD_SYS_ID, l_prod_mod_sys_id_workbench PROD_MOD_SYS_ID,
 l_customer_sys_id CUSTOMER_SYS_ID, 1 ACTIVE_STATUS_IND, '/' MODULE_URL, 0  'DEFAULT', now() CREATED_DATE,
 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY, NULL MODIFIED_DATE, NULL MODIFIED_BY;


 select max(cust_prod_mod_feature_sys_id)+1 into l_cust_prod_mod_feature_sys_id from customer_product_module_features;



INSERT INTO CUSTOMER_PRODUCT_MODULE_FEATURES (CUST_PROD_MOD_FEATURE_SYS_ID,CUST_PROD_MOD_SYS_ID,DEFAULT_URL,FEATURE_NAME,FEATURE_DESC,FEATURE_CODE,FEATURE_TYPE,`DEFAULT`,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
  select  l_cust_prod_mod_feature_sys_id CUST_PROD_MOD_FEATURE_SYS_ID, l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 '/' DEFAULT_URL, 'Canned Analysis' FEATURE_NAME, 'Standard Category' FEATURE_DESC, concat(l_customer_code, 'CANNEDANALYSIS1') FEATURE_CODE,
 concat ('PARENT_',concat(l_customer_code, 'CANNEDANALYSIS1')) FEATURE_TYPE, 1 'DEFAULT', 1 ACTIVE_STATUS_IND, now() CREATED_DATE,
 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY, NULL MODIFIED_DATE, NULL MODIFIED_BY;

 INSERT INTO CUSTOMER_PRODUCT_MODULE_FEATURES (CUST_PROD_MOD_FEATURE_SYS_ID,CUST_PROD_MOD_SYS_ID,DEFAULT_URL,FEATURE_NAME,FEATURE_DESC,FEATURE_CODE,FEATURE_TYPE,`DEFAULT`,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
  select  l_cust_prod_mod_feature_sys_id+1 CUST_PROD_MOD_FEATURE_SYS_ID, l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 '/' DEFAULT_URL, 'Optimization' FEATURE_NAME, 'Optimization sub-category' FEATURE_DESC, concat(l_customer_code, 'OPTIMIZATION1') FEATURE_CODE,
 concat ('CHILD_',concat(l_customer_code, 'CANNEDANALYSIS1')) FEATURE_TYPE, 0 'DEFAULT', 1 ACTIVE_STATUS_IND, now() CREATED_DATE, 'onboard' CREATED_BY,
 NULL INACTIVATED_DATE, NULL INACTIVATED_BY, NULL MODIFIED_DATE, NULL MODIFIED_BY;

 INSERT INTO CUSTOMER_PRODUCT_MODULE_FEATURES (CUST_PROD_MOD_FEATURE_SYS_ID,CUST_PROD_MOD_SYS_ID,DEFAULT_URL,FEATURE_NAME,FEATURE_DESC,FEATURE_CODE,FEATURE_TYPE,`DEFAULT`,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
  select  l_cust_prod_mod_feature_sys_id+2 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 '/' DEFAULT_URL, 'My Analysis' FEATURE_NAME, 'Default Category' FEATURE_DESC,
 concat(l_customer_code, 'MYANALYSIS1') FEATURE_CODE, concat('PARENT_',concat(l_customer_code, 'MYANALYSIS1')) FEATURE_TYPE,
 1 'DEFAULT', 1 ACTIVE_STATUS_IND, now() CREATED_DATE, 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY,
 NULL MODIFIED_DATE, NULL MODIFIED_BY;

 INSERT INTO CUSTOMER_PRODUCT_MODULE_FEATURES (CUST_PROD_MOD_FEATURE_SYS_ID,CUST_PROD_MOD_SYS_ID,DEFAULT_URL,FEATURE_NAME,FEATURE_DESC,FEATURE_CODE,FEATURE_TYPE,`DEFAULT`,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
  select  l_cust_prod_mod_feature_sys_id+3 CUST_PROD_MOD_FEATURE_SYS_ID, l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 '/' DEFAULT_URL, 'Drafts' FEATURE_NAME, 'Drafts' FEATURE_DESC, concat(l_customer_code, 'DRAFTS1') FEATURE_CODE, concat('CHILD_',concat(l_customer_code, 'MYANALYSIS1')) FEATURE_TYPE,
 0 'DEFAULT', 1 ACTIVE_STATUS_IND, now() CREATED_DATE, 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY, NULL MODIFIED_DATE, NULL MODIFIED_BY;

INSERT INTO CUSTOMER_PRODUCT_MODULE_FEATURES (CUST_PROD_MOD_FEATURE_SYS_ID,CUST_PROD_MOD_SYS_ID,DEFAULT_URL,FEATURE_NAME,FEATURE_DESC,FEATURE_CODE,FEATURE_TYPE,`DEFAULT`,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
  select  l_cust_prod_mod_feature_sys_id+4 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_cust_prod_mod_sys_id+1 CUST_PROD_MOD_SYS_ID,
 '/' DEFAULT_URL, 'My DASHBOARD' FEATURE_NAME, 'Default Category' FEATURE_DESC,
 concat(l_customer_code, 'MYDASHBOARD1') FEATURE_CODE, concat('PARENT_',concat(l_customer_code, 'MYDASHBOARD1')) FEATURE_TYPE,
 1 'DEFAULT', 1 ACTIVE_STATUS_IND, now() CREATED_DATE, 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY,
 NULL MODIFIED_DATE, NULL MODIFIED_BY;

 INSERT INTO CUSTOMER_PRODUCT_MODULE_FEATURES (CUST_PROD_MOD_FEATURE_SYS_ID,CUST_PROD_MOD_SYS_ID,DEFAULT_URL,FEATURE_NAME,FEATURE_DESC,FEATURE_CODE,FEATURE_TYPE,`DEFAULT`,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
  select  l_cust_prod_mod_feature_sys_id+5 CUST_PROD_MOD_FEATURE_SYS_ID, l_cust_prod_mod_sys_id+1 CUST_PROD_MOD_SYS_ID,
 '/' DEFAULT_URL, 'Drafts' FEATURE_NAME, 'Drafts' FEATURE_DESC, concat(l_customer_code, 'DRAFTS2') FEATURE_CODE, concat('CHILD_',concat(l_customer_code, 'MYDASHBOARD1')) FEATURE_TYPE,
 0 'DEFAULT', 1 ACTIVE_STATUS_IND, now() CREATED_DATE, 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY, NULL MODIFIED_DATE, NULL MODIFIED_BY;

INSERT INTO CUSTOMER_PRODUCT_MODULE_FEATURES (CUST_PROD_MOD_FEATURE_SYS_ID,CUST_PROD_MOD_SYS_ID,DEFAULT_URL,FEATURE_NAME,FEATURE_DESC,FEATURE_CODE,FEATURE_TYPE,`DEFAULT`,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
  select  l_cust_prod_mod_feature_sys_id+6 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_cust_prod_mod_sys_id+2 CUST_PROD_MOD_SYS_ID,
 '/' DEFAULT_URL, 'Data Ingestion Service' FEATURE_NAME, 'Data Ingestion Service' FEATURE_DESC,
 concat(l_customer_code, 'DIS0000001') FEATURE_CODE, concat('PARENT_',concat(l_customer_code, 'DIS0000001')) FEATURE_TYPE,
 1 'DEFAULT', 1 ACTIVE_STATUS_IND, now() CREATED_DATE, 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY,
 NULL MODIFIED_DATE, NULL MODIFIED_BY;

 INSERT INTO CUSTOMER_PRODUCT_MODULE_FEATURES (CUST_PROD_MOD_FEATURE_SYS_ID,CUST_PROD_MOD_SYS_ID,DEFAULT_URL,FEATURE_NAME,FEATURE_DESC,FEATURE_CODE,FEATURE_TYPE,`DEFAULT`,ACTIVE_STATUS_IND,CREATED_DATE,CREATED_BY,INACTIVATED_DATE,INACTIVATED_BY,MODIFIED_DATE,MODIFIED_BY)
  select  l_cust_prod_mod_feature_sys_id+7 CUST_PROD_MOD_FEATURE_SYS_ID, l_cust_prod_mod_sys_id+2 CUST_PROD_MOD_SYS_ID,
 'datasource/create' DEFAULT_URL, 'Channel Management' FEATURE_NAME, 'Channel Management' FEATURE_DESC, concat(l_customer_code, 'CHANNELMANAGE001') FEATURE_CODE, concat('CHILD_',concat(l_customer_code, 'DIS0000001')) FEATURE_TYPE,
 0 'DEFAULT', 1 ACTIVE_STATUS_IND, now() CREATED_DATE, 'onboard' CREATED_BY, NULL INACTIVATED_DATE, NULL INACTIVATED_BY, NULL MODIFIED_DATE, NULL MODIFIED_BY;


 select max(role_sys_id)+1 into l_role_sys_id from roles;
 select l_role_sys_id;

 INSERT INTO ROLES (ROLE_SYS_ID, CUSTOMER_SYS_ID, ROLE_NAME, ROLE_CODE, ROLE_DESC, ROLE_TYPE, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
 select  l_role_sys_id ROLE_SYS_ID,
 l_customer_sys_id CUSTOMER_SYS_ID,
 'ADMIN' ROLE_NAME,
 concat(l_customer_code,'_ADMIN_USER') ROLE_CODE,
 'Admin User' ROLE_DESC,
 'ADMIN' ROLE_TYPE,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 select max(user_sys_id)+1 into l_user_sys_id from users;

 INSERT INTO users (USER_SYS_ID, USER_ID, EMAIL, ROLE_SYS_ID, CUSTOMER_SYS_ID, ENCRYPTED_PASSWORD, FIRST_NAME, MIDDLE_NAME, LAST_NAME, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
 select  l_user_sys_id USER_SYS_ID,
 concat(concat('sawadmin@',lower(l_customer_code),'.com')) USER_ID,
 l_email EMAIL,
 l_role_sys_id ROLE_SYS_ID,
 l_customer_sys_id CUSTOMER_SYS_ID,
 'Y1lw/IcaP7G++mCJ/TlIGXnAM9Ud+b58niTjkxtdc4I=' ENCRYPTED_PASSWORD,
 l_first_name FIRST_NAME,
 l_middle_name MIDDLE_NAME,
 l_last_name LAST_NAME,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;


 Select max(privilege_sys_id)+1 into l_privilege_sys_id from privileges;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
 SELECT l_privilege_sys_id PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 l_cust_prod_mod_feature_sys_id CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
 SELECT l_privilege_sys_id+1 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 l_cust_prod_mod_feature_sys_id+1 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
  SELECT l_privilege_sys_id+2 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 l_cust_prod_mod_feature_sys_id+2 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
   SELECT l_privilege_sys_id+3 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 l_cust_prod_mod_feature_sys_id+3 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
    SELECT l_privilege_sys_id+4 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id CUST_PROD_MOD_SYS_ID,
 0 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
     SELECT l_privilege_sys_id+5 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 0 CUST_PROD_MOD_SYS_ID,
 0 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
  SELECT l_privilege_sys_id+6 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id+1 CUST_PROD_MOD_SYS_ID,
 l_cust_prod_mod_feature_sys_id+4 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
   SELECT l_privilege_sys_id+7 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id+1 CUST_PROD_MOD_SYS_ID,
 l_cust_prod_mod_feature_sys_id+5 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
  SELECT l_privilege_sys_id+8 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id+2 CUST_PROD_MOD_SYS_ID,
 l_cust_prod_mod_feature_sys_id+6 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 INSERT INTO PRIVILEGES (PRIVILEGE_SYS_ID, CUST_PROD_SYS_ID,CUST_PROD_MOD_SYS_ID, CUST_PROD_MOD_FEATURE_SYS_ID, ROLE_SYS_ID, ANALYSIS_SYS_ID, PRIVILEGE_CODE, PRIVILEGE_DESC, ACTIVE_STATUS_IND, CREATED_DATE, CREATED_BY)
   SELECT l_privilege_sys_id+9 PRIVILEGE_SYS_ID,
 l_cust_prod_sys_id CUST_PROD_SYS_ID,
 l_cust_prod_mod_sys_id+2 CUST_PROD_MOD_SYS_ID,
 l_cust_prod_mod_feature_sys_id+7 CUST_PROD_MOD_FEATURE_SYS_ID,
 l_role_sys_id ROLE_SYS_ID,
 '0' ANALYSIS_SYS_ID,
 '128' PRIVILEGE_CODE,
 'All' PRIVILEGE_DESC,
 1 ACTIVE_STATUS_IND,
 now() CREATED_DATE,
 'onboard' CREATED_BY;

 COMMIT;
 END;
//
DELIMITER ;

/*******************************************************************************
 Stored Procedure Scripts Ends
********************************************************************************/
