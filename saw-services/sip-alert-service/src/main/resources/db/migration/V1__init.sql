

CREATE TABLE `ALERT_CUSTOMER_DETAILS` (
    `ALERT_CUSTOMER_SYS_ID` INT(11) NOT NULL AUTO_INCREMENT COMMENT 'Auto Increment system ID .',
    `CUSTOMER_CODE` VARCHAR(100) NOT NULL COMMENT 'Customer Code ',
    `PRODUCT_CODE` VARCHAR(45) NOT NULL COMMENT 'Product Code ',
    `ACTIVE_IND` TINYINT(4) NOT NULL COMMENT 'Customer active indicator',
    `CREATED_BY` VARCHAR(45) DEFAULT NULL COMMENT 'Customer details created by user',
    `CREATED_TIME` DATETIME DEFAULT NULL COMMENT 'Customer details created time',
    `MODIFIED_BY` VARCHAR(45) DEFAULT NULL COMMENT 'Customer details modified by user',
    `MODIFIED_TIME` DATETIME DEFAULT NULL COMMENT 'Customer details modified time',
    PRIMARY KEY (`ALERT_CUSTOMER_SYS_ID`)
)  ENGINE=INNODB;

CREATE TABLE `DATAPOD_DETAILS` (
    `DATAPOD_ID` VARCHAR(100) NOT NULL COMMENT 'Datapod ID ',
    `DATAPOD_NAME` VARCHAR(45) NOT NULL COMMENT 'Datapod name',
    `ALERT_CUSTOMER_SYS_ID` INT(11) NOT NULL,
    `CREATED_TIME` DATETIME NOT NULL COMMENT 'Datapod created time',
    `CREATED_BY` VARCHAR(45) NOT NULL COMMENT 'Datapod created by user',
    PRIMARY KEY (`DATAPOD_ID`),
    KEY `ALERT_CUSTOMER_SYS_ID_FK_idx` (`ALERT_CUSTOMER_SYS_ID`),
    CONSTRAINT `ALERT_CUSTOMER_SYS_ID_FK` FOREIGN KEY (`ALERT_CUSTOMER_SYS_ID`)
        REFERENCES `ALERT_CUSTOMER_DETAILS` (`ALERT_CUSTOMER_SYS_ID`)
        ON DELETE CASCADE ON UPDATE CASCADE
)  ENGINE=INNODB;

CREATE TABLE `ALERT_RULES_DETAILS` (
    `ALERT_RULES_SYS_ID` INT(11) NOT NULL AUTO_INCREMENT COMMENT 'Auto generated',
    `DATAPOD_ID` VARCHAR(45) NOT NULL COMMENT 'Datapod ID to be used for evaluating the alert. ',
    `ALERT_NAME` VARCHAR(100) NOT NULL COMMENT 'Alert Rule Name',
    `ALERT_DESCRIPTION` VARCHAR(250) DEFAULT NULL COMMENT 'Alert Rule Description',
    `CATEGORY` VARCHAR(100) NOT NULL COMMENT 'Alert Rule Name',
    `SEVERITY` ENUM('CRITICAL', 'MEDIUM', 'LOW') NOT NULL COMMENT 'Severity of Alert , Values can be Critical , Medium and Low',
    `MONITORING_ENTITY` VARCHAR(45) NOT NULL COMMENT 'Column Name from Datapod to use to Monitor the Alert metrics. ',
    `AGGREGATION` ENUM('MAX', 'MIN', 'AVG', 'SUM', 'COUNT', 'PERCENTAGE') NOT NULL COMMENT 'Type Of Aggregation for metrics calculation.',
    `OPERATOR` ENUM('EQ', 'NEQ', 'GT', 'LT', 'GTE', 'LTE', 'SW', 'EW', 'CONTAINS', 'ISIN', 'ISNOTIN', 'BTW') NOT NULL COMMENT 'List of Operators to check the threshold values.',
    `THRESHOLD_VALUE` DECIMAL(5 , 0 ) NOT NULL COMMENT 'Threshold Values',
    `ACTIVE_IND` TINYINT(4) NOT NULL COMMENT 'Rule Active Indicator',
    `CREATED_BY` VARCHAR(45) NOT NULL COMMENT 'Alert rule created by user',
    `CREATED_TIME` DATETIME NOT NULL COMMENT 'Alert rule created Time',
    `MODIFIED_TIME` DATETIME DEFAULT NULL COMMENT 'Alert rule Modified time',
    `MODIFIED_BY` VARCHAR(45) DEFAULT NULL COMMENT 'Alert rule modified by user',
    PRIMARY KEY (`ALERT_RULES_SYS_ID`),
    KEY `DatapodID_fk_idx` (`DATAPOD_ID`),
    CONSTRAINT `DatapodID_fk` FOREIGN KEY (`DATAPOD_ID`)
        REFERENCES `DATAPOD_DETAILS` (`DATAPOD_ID`)
        ON DELETE NO ACTION ON UPDATE NO ACTION
)  ENGINE=INNODB;

CREATE TABLE `ALERT_TRIGGER_DETAILS_LOG` (
    `ALERT_TRIGGER_SYS_ID` INT(11) NOT NULL,
    `ALERT_RULES_SYS_ID` INT(11) NOT NULL,
    `ALERT_STATE` ENUM('ALARM', 'OK') NOT NULL,
    `START_TIME` DATETIME DEFAULT NULL,
    PRIMARY KEY (`ALERT_TRIGGER_SYS_ID`),
    KEY `AlERT_RULES_SYS_ID_idx` (`ALERT_RULES_SYS_ID`),
    CONSTRAINT `AlERT_RULES_SYS_ID` FOREIGN KEY (`ALERT_RULES_SYS_ID`)
        REFERENCES `ALERT_RULES_DETAILS` (`ALERT_RULES_SYS_ID`)
        ON DELETE CASCADE ON UPDATE CASCADE
)  ENGINE=INNODB ;

