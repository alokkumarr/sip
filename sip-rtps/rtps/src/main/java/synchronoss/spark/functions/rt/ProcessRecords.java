package synchronoss.spark.functions.rt;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction2;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.kafka010.CanCommitOffsets;
import org.apache.spark.streaming.kafka010.HasOffsetRanges;
import org.apache.spark.streaming.kafka010.OffsetRange;
import org.elasticsearch.spark.sql.api.java.JavaEsSparkSQL;
import synchronoss.data.countly.model.CountlyModel;
import synchronoss.data.generic.model.GenericJsonModel;
import synchronoss.data.generic.model.transformation.Transform;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static synchronoss.spark.drivers.rt.EventProcessingApplicationDriver.DM_COUNTLY;
import static synchronoss.spark.drivers.rt.EventProcessingApplicationDriver.DM_GENERIC;
import static synchronoss.spark.drivers.rt.EventProcessingApplicationDriver.DM_SIMPLE;
import static synchronoss.spark.drivers.rt.EventProcessingApplicationDriver.DM_SIMPLE_JSON;

/**
 * Created by asor0002 on 5/19/2017.
 *
 */
public class ProcessRecords implements VoidFunction2<JavaRDD<ConsumerRecord<String, String>>, Time> {

    private static final Logger logger = Logger.getLogger(ProcessRecords.class);
    private static final long serialVersionUID = -3110740581467891647L;

    private JavaInputDStream<ConsumerRecord<String, String>> inStream;

    private String dataModel;
    private String definitions;

    // Generic model artifacts
    private static Map<String, List<Transform>> transformations;
    private static Set<String> types;
    private StructType schema = null;

    // ES artifacts
    private String esIndex;
    private String esType;
    private Map<String, String> esConfig;
    private static List<Column> esColumns = null;

    // Data Lake artifacts
    private String basePath;
    private String batchPrefix;
    private String outputType;
    private static final String fileNamePrefix = "part";

    public ProcessRecords(
        JavaInputDStream<ConsumerRecord<String, String>> inStream,
        String dataModel, String definitions,
        String esIndex, Map<String, String> esConfig,
        String basePath, String batchPrefix, String outputType){

        this.inStream = inStream;

        this.dataModel = dataModel;
        this.definitions = definitions;
        if(esIndex != null && !esIndex.isEmpty()) {
            if (esIndex.indexOf("/") > 0) {
                // This means we have Index/Type
                this.esIndex = esIndex.split("/")[0];
                this.esType = "/" + esIndex.split("/")[1];
            } else {
                this.esIndex = esIndex;
                this.esType = "";
            }
        } else {
            this.esIndex = null;
        }

        this.esConfig = esConfig;
        this.basePath = basePath;
        this.batchPrefix = batchPrefix;
        this.outputType = outputType;
    }
    public void  call(
        JavaRDD<ConsumerRecord<String, String>> in, Time tm) throws Exception {
        if (!in.isEmpty()) {
            logger.info("Starting new batch processing. Batch time: " + tm);
            switch(dataModel.toLowerCase()){
                case DM_GENERIC : {
                    if (transformations == null) {
                        transformations = GenericJsonModel.createTransformationsList(definitions);
                    }
                    if(types == null){
                        types = GenericJsonModel.getObjectTypeList(definitions);
                    }
                    if(schema == null){
                        schema = GenericJsonModel.createGlobalSchema(definitions);
                    }
                    ProcessGenericRecords(in, tm);
                    break;
                }
                case DM_COUNTLY : {
                    if(schema == null){
                        schema = CountlyModel.createGlobalSchema();
                    }
                    ProcessCountlyRecords(in, tm);
                    break;
                }
                case DM_SIMPLE : {
                    // Should be changed
                    ProcessSimpleRecords(in, tm);
                    break;
                }
                case DM_SIMPLE_JSON : {
                    ProcessSimpleJsonRecords(in, tm);
                    break;
                }
                default:
                    logger.error("Invalid data model configured : " + dataModel.toLowerCase());
                    break;
            }
        } //<-- if(!in.isEmpty())...

        CommitOffsets(in, tm);
    }

    // We have to support time based indexes
    // Configuration can specify index name with multiple date/time formats in curly brackets
    // In this case we will create index name based on current timestamp
    private static String parseIndexName(String template, Date now) {

        String indexName = template;
        Pattern patternIndexName = Pattern.compile("\\{(.*?)\\}");

        Matcher matchPattern = patternIndexName.matcher(template);
        boolean result = matchPattern.find();
        if (result) {
            StringBuffer sb = new StringBuffer();
            do {
                String format = matchPattern.group(1);
                String replacement;
                DateFormat df = new SimpleDateFormat(format);
                replacement = df.format(now);

                matchPattern.appendReplacement(sb, replacement);
                result = matchPattern.find();
            } while (result);
            matchPattern.appendTail(sb);
            indexName = sb.toString();
        }
        return indexName.toLowerCase();
    }

    private void ProcessSimpleRecords(JavaRDD<ConsumerRecord<String, String>> in, Time tm){
        JavaRDD<String> stringRdd = in.mapPartitions(new TransformSimpleRecord());
        SaveSimpleBatch(stringRdd, tm);
    }

    private void ProcessSimpleJsonRecords(JavaRDD<ConsumerRecord<String, String>> in, Time tm){
        JavaRDD<String> stringRdd = in.mapPartitions(new TransformSimpleJsonRecord());
        SaveSimpleBatch(stringRdd, tm);
    }

    private void SaveSimpleBatch(JavaRDD<String> stringRdd, Time tm){
        if(basePath != null && !basePath.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
            Date batchDt = new Date(tm.milliseconds());
            
            String batchName = batchPrefix + sdf.format(batchDt);
            String path = basePath + File.separator + batchName;
            String strTmpPath = basePath + File.separator + "__TMP-" + batchName;
            // Save data as TEMP
            stringRdd.saveAsTextFile(strTmpPath);
            // Rename and cleanup
            finalizeBatch(strTmpPath, path, true);
        }
    }

    private void ProcessGenericRecords(JavaRDD<ConsumerRecord<String, String>> in, Time tm){
        JavaRDD<String> jsonRdd = in.mapPartitions(new TransformJsonRecord(definitions));
        SparkConf cnf = in.context().getConf();
        processJsonRecords(jsonRdd, tm, cnf);
    }

    private void ProcessCountlyRecords(JavaRDD<ConsumerRecord<String, String>> in, Time tm){
        JavaRDD<String> jsonRdd = in.mapPartitions(new TransformCountlyRecord());
        SparkConf cnf = in.context().getConf();
        processJsonRecords(jsonRdd, tm, cnf);

    }

    private int processJsonRecords(JavaRDD<String> jsonRdd, Time tm, SparkConf cnf){
        SparkSession sess = SparkSession.builder().config(cnf).getOrCreate();
        JavaRDD<Row> rowRdd = sess.read().schema(schema).json(jsonRdd).toJavaRDD();
        Dataset<Row> df = sess.createDataFrame(rowRdd, schema);

        // Elastic Search
        if (esIndex != null && !esIndex.isEmpty()) {
            // Store batch in ES
            Dataset<Row> df2;
            if (esColumns != null && esColumns.size() > 0)
                df2 = df.select(scala.collection.JavaConversions.asScalaBuffer(esColumns));
            else
                df2 = df;

            // See configuration files for detailed explanations
            // on how data is written to ES
            // We use ES.Hadoop settings to control this flow
            String currentEsIndex = parseIndexName(esIndex, new Date()) + esType;
            JavaEsSparkSQL.saveToEs(df2, currentEsIndex, esConfig);
        }
        //======================================================================
        // Data Lake
        // Store it in file system as separate directory
        if(basePath != null && !basePath.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
            Date batchDt = new Date(tm.milliseconds());
            
            String batchName = batchPrefix + sdf.format(batchDt);
            String path = basePath + File.separator + batchName;
            String strTmpPath = basePath + File.separator + "__TMP-" + batchName;

            if (outputType.equals("parquet")) {
                df.write().mode(SaveMode.Append).parquet(strTmpPath);
            } else if (outputType.equals("json")) {
                df.write().mode(SaveMode.Append).json(strTmpPath);
            } else {
                logger.error("Invalid output type :" + outputType);
            }
            // Done with writing - safe to rename batch directory
            finalizeBatch(strTmpPath, path, false);
        } //<-- if(basePath != null && !basePath.isEmpty())

        /* Alert metrics collection */
        if (logger.isTraceEnabled()) {
          logger.trace("Alert metrics to be be collected: " + df.count());
        }

        return 0;
    }

    private int finalizeBatch(String strTmpPath, String finalPath, boolean isSimple){
        // Done with writing - safe to rename batch directory
        try {
            String defaultFS = "maprfs:///";
            Configuration config = new Configuration();
            config.set("fs.defaultFS", defaultFS);
            FileSystem fs = FileSystem.get(config);

            Path tmpPath = new Path(strTmpPath);

            RemoteIterator<LocatedFileStatus> it = fs.listFiles(tmpPath, false);
            while (it.hasNext()) {
                Path pathToDelete = it.next().getPath();
                // We have to delete all non-parquet files/directories
                // Little bit shaky - how to define parquet files
                if(pathToDelete.getName().startsWith("_S")) {
                    fs.delete(pathToDelete, true);
                }
            }
            // Results are ready - make it final
            // !!!!!!!!!!!Should use another technique for rename
            fs.rename(tmpPath, new Path(finalPath));
            
            /**
             * Simple scenario's no suffix such as UUID
             * is not added. Hence renaming file to maintian
             * uniqueness with timestamp as suffix
             */
            if(isSimple) {
            	FileStatus[] files = fs.globStatus(new Path(finalPath+ 
            			Path.SEPARATOR + fileNamePrefix+ "*"));
           	
            	
            	for(FileStatus fileStatus: files) {
            		 logger.debug("******** part file name renaming for simple.... ********"+ fileStatus.getPath().getName());
            		
                	 
            		fs.rename(new Path(finalPath+ Path.SEPARATOR + fileStatus.getPath().getName()), new Path(finalPath+ 
                    		Path.SEPARATOR + fileNamePrefix + "-" +  UUID.randomUUID().toString()));
            		
            		logger.info("******** rename completed ********");
            	}
            	
            }
            
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return 0;
    }

    private void CommitOffsets(JavaRDD<ConsumerRecord<String, String>> rdd, Time tm) {
        if(rdd.rdd() instanceof HasOffsetRanges) {
            OffsetRange[] offsetRanges = ((HasOffsetRanges)rdd.rdd()).offsetRanges();
            boolean commit = false;
            for(OffsetRange o : offsetRanges) {
                if(o.fromOffset() < o.untilOffset()) {
                    logger.info("Batch Time: " + tm + ". Updated offsets : " + o.topic() + ", part. : " + o.partition()
                                       + " [ from: " + o.fromOffset() + "; until: " + o.untilOffset() + "]");
                    commit = true;
                }
            }
            if(commit) {
                logger.info("Committing new offsets.");
                ((CanCommitOffsets)inStream.inputDStream()).commitAsync(offsetRanges);
            }
        }
    }
}
