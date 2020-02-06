/*******************************************************************************
 Filename:  V28__DDL_CUSTOMER_BRAND_DETAILS.SQL
 Purpose:   Alter customer details table to store customer logo and customer color for branding.
 Date:      14-01-2020
********************************************************************************/

	/*******************************************************************************
	 ALTER Table Scripts Starts
	********************************************************************************/

  ALTER TABLE CONFIG_VAL
    	 ADD CONSTRAINT UNIQUE_INDEX UNIQUE (`CONFIG_VAL_CODE`, `CONFIG_VAL_OBJ_TYPE`, `CONFIG_VAL_OBJ_GROUP`);

	/*******************************************************************************
  	 ALTER Table Scripts Ends
  	********************************************************************************/

  /*******************************************************************************
   TABLE Scripts Ends
  ********************************************************************************/