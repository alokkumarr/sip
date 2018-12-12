package sncr.xdf.ngcomponent;

import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import sncr.bda.conf.Parameter;
import sncr.xdf.context.InternalContext;
import sncr.xdf.context.NGContext;
import sncr.xdf.core.spark.SparkOps;

import java.util.List;

public interface WithSpark {

    default void initSpark(SparkSession sparkSession, InternalContext ctx, NGContext ngctx) {
        WithSparkHelper.logger.trace("Configure spark context: " + ngctx.componentName);
        SparkConf sparkConf = new SparkConf().setAppName(ngctx.componentName + "::" + ngctx.applicationID + "::" + ngctx.batchID );

        /// Overwrite configuration with parameters from component
        List<Parameter> componentSysParams = ngctx.componentConfiguration.getParameters();

        //TODO: We must have default parameters
        if(componentSysParams != null && componentSysParams.size() > 0) {
            SparkOps.setSparkConfig(sparkConf, componentSysParams);
        }
        //ctx = new JavaSparkContext(sparkConf);
        if (sparkSession == null)
            ctx.sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
        else
            ctx.sparkSession = sparkSession;
    }

    class WithSparkHelper {
        private static final Logger logger = Logger.getLogger(WithSpark.class);
    }

}
