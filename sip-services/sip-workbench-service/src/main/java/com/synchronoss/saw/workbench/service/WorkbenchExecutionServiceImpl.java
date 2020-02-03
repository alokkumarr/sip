package com.synchronoss.saw.workbench.service;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.apache.hadoop.fs.Path;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.mapr.db.Admin;
import com.mapr.db.FamilyDescriptor;
import com.mapr.db.MapRDB;
import com.mapr.db.TableDescriptor;
import com.synchronoss.saw.workbench.executor.service.WorkbenchExecutor;

import sncr.bda.base.MetadataBase;
import sncr.bda.conf.ComponentConfiguration;
import sncr.bda.core.file.HFileOperations;


@Service
public class WorkbenchExecutionServiceImpl implements WorkbenchExecutionService {
  private final Logger log = LoggerFactory.getLogger(getClass().getName());
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${workbench.project-key}")
  @NotNull
  private String project;

  @Value("${workbench.project-root}")
  @NotNull
  private String root;

  @Value("${workbench.livy-uri}")
  @NotNull
  private String livyUri;

  @Value("${workbench.preview-limit}")
  @NotNull
  private Integer previewLimit;

  @Value("${workbench.project-root}/services/metadata/previews")
  @NotNull
  private String previewsTablePath;
  


  @Autowired
  WorkbenchExecutor executor;
  
  
  
  @PostConstruct
  private void init() throws Exception {
	  log.info("#### Inside Post Construct ####");
      /* Workaround: If the "/apps/spark" directory does not exist in
       * the data lake, Apache Livy will fail with a file not found
       * error.  So create the "/apps/spark" directory here.  */
      String appsSparkPath = "/apps/spark";
      if (!HFileOperations.exists(appsSparkPath)) {
          HFileOperations.createDir(appsSparkPath);
      }

    /* Initialize the previews MapR-DB table */
    try (Admin admin = MapRDB.newAdmin()) {
      if (!admin.tableExists(previewsTablePath)) {
        log.info("Creating previews table: {}", previewsTablePath);
        TableDescriptor table = MapRDB.newTableDescriptor(previewsTablePath);
        FamilyDescriptor family = MapRDB.newDefaultFamilyDescriptor().setTTL(3600);
        table.addFamily(family);
        admin.createTable(table).close();
      }
    }
    /*
     * Cache a Workbench Livy client to reduce startup time for first operation
     */
    try {
//      cacheWorkbenchClient();
    } catch (Exception e) {
      /* If Apache Livy is not installed in the environment, fail
       * gracefully by letting the Workbench Service still start up.
       * If Apache Livy is later installed, the Workbench Service will
       * be able to recover by reattempting to create the client.  */
      log.error("Unable to create Workbench client upon startup", e);
    }
  }

  

  
  /**
   * Execute a transformation component on a dataset to create a new dataset.
   */
  @Override
  public ObjectNode execute(
    String project, String name, String component, String cfg) throws Exception {
	  
	  
	System.out.print("Checking logger level...");
	org.apache.log4j.Logger logger4j = org.apache.log4j.Logger.getRootLogger();
	logger4j.setLevel(org.apache.log4j.Level.toLevel("DEBUG"));
	System.out.print("Logger level set to debug");
	
	//org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(this.getClass());
	//logger.setLevel(org.apache.log4j.Level.toLevel("DEBUG"));
	
	
    log.debug("Executing dataset transformation starts here ");
    log.debug("XDF Configuration = " + cfg);
    
    
    //createDatasetDirectory(project, MetadataBase.DEFAULT_CATALOG, name);
    
    
    log.info("execute name = " + name);
    log.info("execute root = " + root);
    log.info("execute component = " + component);

    ComponentConfiguration config = new Gson().fromJson(cfg, ComponentConfiguration.class);

    log.info("Component Config = " + config);

    String batchID = new DateTime().toString("yyyyMMdd_HHmmssSSS");

   
    executor.executeJob(project, name, component, cfg);
    
    //String project, String name, String component, String cfg
    //client.submit(new WorkbenchExecuteJob(workBenchcontext));
    /*ObjectNode root = mapper.createObjectNode();
    ArrayNode ids = root.putArray("outputDatasetIds");
    for (String id: workBenchcontext.registeredOutputDSIds) {
      ids.add(id);
    }*/
    log.info("Executing dataset transformation ends here ");
    return null;
  }

  /**
   * Execute a transformation component on a dataset to create a new dataset.
   */
  @Override
  public String createDatasetDirectory(String project, String catalog, String name) throws Exception {
    log.trace("generate data system path for starts here :" + project + " : " + name); 
    String path = root + Path.SEPARATOR + project + Path.SEPARATOR + MetadataBase.PREDEF_DL_DIR
        + Path.SEPARATOR + MetadataBase.PREDEF_DATA_SOURCE + Path.SEPARATOR
        + catalog + Path.SEPARATOR + name + Path.SEPARATOR
        + MetadataBase.PREDEF_DATA_DIR;
    log.info("createDatasetDirectory path = " + path);
    if (!HFileOperations.exists(path)) {
      HFileOperations.createDir(path);
    }
    log.trace("generate data system path for starts here " + path);
    return path;
  }

  @Value("${metastore.base}")
  @NotNull
  private String metastoreBase;

  /**
   * Preview the output of a executing a transformation component on a dataset.
   * Also used for simply viewing the contents of an existing dataset.
   */
  @Override
  public ObjectNode preview(String project, String name) throws Exception {
    return null;
  }

  private void handlePreviewFailure(String previewId) {
   
  }

  @Override
  public ObjectNode getPreview(String previewId) throws Exception {
	  return null;
  }
}
