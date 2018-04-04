package sncr.xdf.ngcomponent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import sncr.bda.conf.ComponentConfiguration;
import sncr.bda.datasets.conf.DataSetProperties;
import sncr.bda.services.AuditLogService;
import sncr.bda.services.DLDataSetService;
import sncr.bda.services.TransformationService;
import sncr.xdf.context.ComponentServices;
import sncr.xdf.context.NGContext;

import java.util.HashMap;
import java.util.Map;


/**
 *  The AbstractComponent class is base class for all XDF components.
 *  All component should be implemented as follow:
 *   - Component specific class inherits from AbstractComponent
 *   - Component specific class should implement interfaces with given functionality
 *   or using base classes:
 *      - Read data from a source (???)
 *      - Write data (DLBatchWriter)
 *      - Move data from temp location to permanent location: WithMovableResult
 *      - Read and write result from/to metadata
 *      - Support Spark context
 *      and so on.
 *      All mentioned above are design/development time solution
 *
 *      However, component also can be run:
 *      - with/without support of metadata
 *      - with/without Writing/moving result to permanent location
 *      - with writing full result vs creating sample
 *      - with internal Spark context vs External Spark context.
 *     These are runtime options.
 *
 */
public class NGContextServices implements WithDataSet, WithProjectScope{

    private static final Logger logger = Logger.getLogger(NGContextServices.class);

    protected NGContext ngctx;
    protected final Services services = new Services();

    public NGContextServices( ComponentServices[] cs, String xdfRoot,  ComponentConfiguration componentConfiguration, String applicationID, String componentName, String batchID){
        ngctx = new NGContext(xdfRoot, componentConfiguration, applicationID, componentName, batchID);
        for (int i = 0; i < cs.length; i++) {
            this.ngctx.serviceStatus.put(cs[i], false);
        }
    }

    public NGContextServices(String xdfRoot,  ComponentConfiguration componentConfiguration, String applicationID, String componentName, String batchID) {
        ngctx = new NGContext(xdfRoot, componentConfiguration, applicationID, componentName, batchID);
        this.ngctx.serviceStatus.put(ComponentServices.InputDSMetadata, false);
        this.ngctx.serviceStatus.put(ComponentServices.OutputDSMetadata, false);
        this.ngctx.serviceStatus.put(ComponentServices.Project, false);
        this.ngctx.serviceStatus.put(ComponentServices.TransformationMetadata, false);
        this.ngctx.serviceStatus.put(ComponentServices.Sample, true);
        this.ngctx.serviceStatus.put(ComponentServices.Spark, false);

    }

    public NGContext getNgctx(){
        return ngctx;
    }


    public int initContext(){

        try {


            if (ngctx.serviceStatus.containsKey(ComponentServices.InputDSMetadata) ||
                ngctx.serviceStatus.containsKey(ComponentServices.OutputDSMetadata) ||
                ngctx.serviceStatus.containsKey(ComponentServices.TransformationMetadata))
            {
                services.md = new DLDataSetService(ngctx.xdfDataRootSys);
                services.als = new AuditLogService(services.md.getRoot());
            }

            if (ngctx.serviceStatus.containsKey(ComponentServices.Project)){
                services.prj =  this;
                if (initProject() != 0){
                    logger.error("Could not init project data");
                    return  -1;
                }
            }

            if (ngctx.serviceStatus.containsKey(ComponentServices.TransformationMetadata)) {
                services.transformationMD = new TransformationService(ngctx.xdfDataRootSys);
                if (initTransformation() != 0){
                    logger.error("Could not init transformation data");
                    return  -1;
                }
            }

            if (ngctx.serviceStatus.containsKey(ComponentServices.OutputDSMetadata))  services.mddl =  this;

        }
        catch(Exception e){
            String error = "Services initialization has failed: " + ExceptionUtils.getFullStackTrace(e);
            logger.error(error);
            return -1;
        }

        return 0;
    }


    private int initProject() {
        try {
            services.prj.getProjectData(ngctx);
            ngctx.serviceStatus.put(ComponentServices.Project, true);
        } catch (Exception e) {
            String error = "component initialization (input-discovery/output-preparation) exception: " + ExceptionUtils.getFullStackTrace(e);
            logger.error(error);
            return -1;

        }
        return 0;
    }

    public int registerOutputDataSet() {

        final int[] rc2 = {0};
        if (services.mddl != null) {

            if (ngctx.componentConfiguration.getOutputs() != null && ngctx.componentConfiguration.getOutputs().size() > 0) {

                WithDataSet.DataSetHelper dsaux = new WithDataSet.DataSetHelper(ngctx, services.md);
                ngctx.outputDataSets = services.mddl.ngBuildPathForOutputDataSets(dsaux);
                ngctx.outputs = services.mddl.ngBuildPathForOutputs(dsaux);

                final int[] rc = {0};

                try
                {
                  JsonObject ale = services.als.generateDSAuditLogEntry(ngctx, "INIT", ngctx.inputDataSets, ngctx.outputDataSets);
                  String aleId = services.als.createAuditLog(ngctx, ale);

                  ngctx.componentConfiguration.getOutputs().forEach(o ->
                  {
                        logger.debug("Add output object to data object repository: " + o.getDataSet());

                        if (ngctx.serviceStatus.containsKey(ComponentServices.OutputDSMetadata)) {
                            JsonElement ds = services.md.readOrCreateDataSet(ngctx, ngctx.outputDataSets.get(o.getDataSet()));
                            if (ds == null) {
                                String error = "Could not create metadata for output dataset [" + o.getDataSet() + "]: " ;
                                logger.error(error);
                                rc[0] = -1;
                                return;
                            }

                            JsonObject dsObj = ds.getAsJsonObject();
                            String id = dsObj.getAsJsonPrimitive(DataSetProperties.Id.toString()).getAsString();
                            //TODO:: Add to list of IDs
                            ngctx.registeredOutputDSIds.add(id);

                            String step = "Could not create activity log entry for DataSet [" + o.getDataSet() + "]: " ;
                            try {
                                step = "Could not update metadata of DataSet [" + o.getDataSet() + "]: " ;
                                services.md.getDSStore().updateStatus(id,"INIT", ngctx.startTs, null, aleId, ngctx.batchID);
                            } catch (Exception e) {
                                String error = step + ExceptionUtils.getFullStackTrace(e);
                                logger.error(error);
                                rc[0] = -1;
                                return;
                            }
                        }
                    });
                    ngctx.serviceStatus.put(ComponentServices.OutputDSMetadata, true);
                } catch (Exception e) {
                  logger.error("Could not create Audit log entry" +
                      ExceptionUtils.getFullStackTrace(e));
                  return -1;
                }

            }
            return rc2[0];
        }
        else{
            String  error = "Incorrect initialization sequence or dataset service is not available";
            logger.error(error);
            return -1;
        }
    }

    private int initTransformation(){
        if (services.transformationMD == null ||
            !ngctx.serviceStatus.containsKey(ComponentServices.TransformationMetadata)){
            logger.error("Incorrect initialization sequence or service is not available");
            return -1;
        }
        try {
            ngctx.transformationID =
                    services.transformationMD.readOrCreateTransformation(ngctx, ngctx.componentConfiguration);
            ngctx.serviceStatus.put(ComponentServices.TransformationMetadata, true);
        } catch (Exception e) {
            String error = "Exception at transformation init: " + ExceptionUtils.getFullStackTrace(e);
            logger.error(error);
            return -1;
        }
        return 0;
    }


    protected ComponentConfiguration validateConfig(String config) throws Exception {
        return NGContextServices.analyzeAndValidate(config);
    }

    public static ComponentConfiguration analyzeAndValidate(String cfg) throws Exception
    {
        ComponentConfiguration config = new Gson().fromJson(cfg, ComponentConfiguration.class);
        return config;
    }

    @Override
    public String toString(){
        String strCtx = "Execution context: " + ngctx.toString();
        return strCtx;
    }

    /**
     * The class is container for internal services
     */
    public class Services {
        public WithProjectScope prj;
        public WithDataSet mddl;

        public DLDataSetService md;
        public AuditLogService als;

        public TransformationService transformationMD;
    }


}
