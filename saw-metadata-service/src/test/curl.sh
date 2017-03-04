curl -XPOST -H "Content-Type:application/json" localhost:9101/md --data '
{
  "ticket":{
    "ticketId":"28_1481019911763_2105901292",
    "windowId":"28_1481019911763_1066874973",
    "masterLoginId":"admin@att.com",
    "customer_code":"ATT",
    "userName":"Adminstrator",
    "password":null,
    "prodCode":"1",
    "roleType":"Admin",
    "createdTime":1481019911763,
    "dataSecurityKey":"ATT_ADMIN",
    "validUpto":1481027111763,
    "valid":true,
    "validityReason":"User Authenticated Successfully",
    "validMins":20,
    "iat":1481019912
  },
	"contents" : {
		"action" : "search",
		"keys" : {
				"type" : "alert"
		}
	}
}'


------------------------

curl -XPOST -H "Content-Type:application/json" localhost:9101/md --data-binary @/var/saw/service/data/r.json

curl -XPOST -H "Content-Type:application/json" localhost:9101/md --data '
{
  "ticket":{
    "ticketId":"28_1481019911763_2105901292",
    "windowId":"28_1481019911763_1066874973",
    "masterLoginId":"admin@att.com",
    "customer_code":"ATT",
    "userName":"Adminstrator",
    "password":null,
    "prodCode":"1",
    "roleType":"Admin",
    "createdTime":1481019911763,
    "dataSecurityKey":"ATT_ADMIN",
    "validUpto":1481027111763,
    "valid":true,
    "validityReason":"User Authenticated Successfully",
    "validMins":20,
    "iat":1481019912
  },
	"contents" : {
		"action" : "create",
		"menu": [  
		 {  
				"_id":"main_cat_123456785421",
				"category_name":"My Analyses",
				"module":"ANALYZE",
				"customer_code":"ATT",
				"data_security_key":"20170612ATT",
				"role_type":"ADMIN",
				"children":[  
					 {  
							"sub_category_id":"sub_cat_123456785421",
							"sub_category_name":"Optimization",
							"list":[  
								 {  
										"_id":"sub_cat_123456785421_Optimization",
										"customer_product_feature_id":"feature_menu_item_1",
										"active":"true",
										"artifact_name":"",
										"type":"report",
										"description":"This report is sample 1",
										"scheduled":"Every Friday at 9 AM",
										"metric_name":"Incident_Analysis",
										"created_by":"saurav.paul@synchronoss.com",
										"created_date":"02-11-2017",
										"type_id":"analyses_report_345678"
								 },
								 {  
										"_id":"sub_cat_123456785431_Optimization",
										"customer_product_feature_id":"feature_menu_item_2",
										"active":"true",
										"artifact_name":"",
										"type":"report",
										"description":"This report is sample 2",
										"scheduled":"Every Wednesday at 10 AM",
										"metric_name":"Incident_Analysis",
										"created_by":"saurav.paul@synchronoss.com",
										"created_date":"02-14-2017",
										"type_id":""
								 }
							]
					 }
				]
		 },
		 {  
				"_id":"main_cat_123456785422",
				"category_name":"Canned Analyses",
				"module":"ANALYZE",
				"customer_code":"ATT",
				"data_security_key":"20170612ATT",
				"role_type":"ADMIN",
				"children":[  
					 {  
							"sub_category_id":"sub_cat_123456785421",
							"sub_category_name":"Public Optimization",
							"list":[  
								 {  
										"_id":"sub_cat_123456785421_Public_Optimization",
										"customer_product_feature_id":"feature_menu_item_1",
										"active":"true",
										"artifact_name":"",
										"type":"report",
										"description":"This report is sample 1",
										"scheduled":"Every Friday at 9 AM",
										"metric_name":"Campaigne",
										"created_by":"saurav.paul@synchronoss.com",
										"created_date":"02-11-2017",
										"type_id":""
								 },
								 {  
										"_id":"sub_cat_123456785431_Public_Optimization",
										"customer_product_feature_id":"feature_menu_item_2",
										"active":"true",
										"artifact_name":"",
										"type":"report",
										"description":"This report is sample 2",
										"scheduled":"Every Wednesday at 10 AM",
										"metric_name":"Campaigne",
										"created_by":"saurav.paul@synchronoss.com",
										"created_date":"02-14-2017",
										"type_id":""
								 }
							]
					 }
				]
		 },
		 {  
				"_id":"main_cat_123456785422",
				"category_name":"Indirect Analyses",
				"module":"OBSERVE",
				"customer_code":"TMOBILE",
				"data_security_key":"20170413TMOBILE",
				"role_type":"ADMIN",
				"children":[  
					 {  
							"sub_category_id":"sub_cat_123456785421",
							"sub_category_name":"Order Analysis",
							"list":[  
								 {  
										"_id":"1",
										"customer_product_feature_id":"dashboard_feature_id_1",
										"active":"true",
										"artifact_name":"Order Analysis",
										"type":"dashboard",
										"description":"This dashboard is sample dashboard",
										"scheduled":"",
										"metric_name":"",
										"created_by":"saurav.paul@synchronoss.com",
										"created_date":"03-03-2017",
										"type_id":"dashboard_artifact_id"
								 },
								 {  
										"_id":"2",
										"customer_product_feature_id":"dashboard_feature_id_2",
										"active":"true",
										"artifact_name":"Operation Analyses",
										"type":"dashboard",
										"description":"This dashboard is sample dashboard",
										"scheduled":"",
										"metric_name":"",
										"created_by":"saurav.paul@synchronoss.com",
										"created_date":"03-03-2017",
										"type_id":"dashboard_artifact_id"
								 }
							]
					 }
				]
		 }
	]
	}
}
'