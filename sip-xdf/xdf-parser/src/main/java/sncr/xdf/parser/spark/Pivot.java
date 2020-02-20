package sncr.xdf.parser.spark;

import org.apache.log4j.Logger;
import sncr.xdf.exceptions.XDFException;
import sncr.xdf.context.XDFReturnCode;
import org.apache.spark.sql.RelationalGroupedDataset;
import java.util.stream.IntStream;
import java.util.Arrays;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.StringType;
import org.apache.spark.sql.types.StructField;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.explode;
import sncr.bda.conf.PivotFields;

public class Pivot {
    private static final Logger logger = Logger.getLogger(Pivot.class);
    private String pivotFieldName = null;
    private String aggFieldName = null;
    private static final String SPARK_COLUMN_NAME_DELIMITER = ".";
    private static final String NEW_COLUMN_NAME_DELIMITER = "_";

    public Dataset<Row> applyPivot(Dataset<Row> inputDS, PivotFields pivotFields){
        logger.debug("==> applyPivot()");
        logger.info("inputDS count : " + inputDS.count());
        if(pivotFields == null) {
            return inputDS;
        }else{
            String[] groupByColumns = pivotFields.getGroupByColumns();
            String pivotColumn = pivotFields.getPivotColumn();
            String aggregateColumn = pivotFields.getAggregateColumn();
            validatePivotFields(groupByColumns, pivotColumn, aggregateColumn);

            Dataset<Row> pivotExplodeDS = parsePivotField(inputDS, pivotColumn);
            logger.info("pivotExplodeDS count : "+ pivotExplodeDS.count());

            Dataset<Row> aggExplodeDS = parseAggregateField(pivotExplodeDS, pivotColumn, aggregateColumn);
            logger.info("aggExplodeDS count : "+ aggExplodeDS.count());

            RelationalGroupedDataset groupByDS = null;
            logger.info("Number of GroupBy Columns are : "+ groupByColumns.length);
            if(groupByColumns.length == 1){
                groupByDS = aggExplodeDS.groupBy(groupByColumns[0]);
            }else{
                groupByDS = aggExplodeDS.groupBy(groupByColumns[0].trim(), IntStream.range(1, groupByColumns.length)
                    .mapToObj(index -> groupByColumns[index].trim())
                    .toArray(String[]::new));
            }
            logger.info("groupByDS count : "+ groupByDS.count());

            logger.info("pivotFieldName : " + pivotFieldName);
            logger.info("aggFieldName : " + aggFieldName);
            Dataset<Row> pivotDS = groupByDS.pivot(pivotFieldName).agg(max(aggFieldName));
            logger.info("pivotDS count : "+ pivotDS.count());

            return pivotDS;
        }
    }

    private Dataset<Row> parsePivotField(Dataset<Row> inputDS, String pivotColumn){
        logger.debug("==> parsePivotField()");
        StructType schema = inputDS.schema();
        logger.debug("inputDS Schema : "+ schema);
        StructField[] fields = schema.fields();
        logger.debug("DS Fields : "+ Arrays.toString(fields));
        String[] pivotColumnSplit = pivotColumn.trim().split("\\"+SPARK_COLUMN_NAME_DELIMITER);
        Dataset<Row> explodeDS = inputDS;
        StructField pivotField = null;
        int index = 0;
        int length = pivotColumnSplit.length;
        logger.debug("pivotColumnSplit length : "+ length);
        for(String column : pivotColumnSplit){
            logger.debug("pivot index : "+ index);
            if(pivotFieldName == null){
                pivotFieldName = column;
            }else{
                pivotFieldName = pivotFieldName + SPARK_COLUMN_NAME_DELIMITER + column;
            }
            logger.debug("Partial pivotFieldName : "+ pivotFieldName);
            pivotField = getDSFiled(fields, pivotFieldName);
            logger.debug("pivotField name: "+ pivotField.name());
            logger.debug("pivotField type: "+ pivotField.dataType());
            if(pivotField.dataType() instanceof ArrayType){
                String newFiledName = pivotFieldName.replace(SPARK_COLUMN_NAME_DELIMITER, NEW_COLUMN_NAME_DELIMITER);
                logger.debug("newFiledName: "+ newFiledName);
                do{
                    explodeDS = explodeDS.withColumn(newFiledName,explode(explodeDS.col(pivotFieldName)));
                    logger.info("explodeDS count : "+ explodeDS.count());
                    schema = explodeDS.schema();
                    logger.debug("explodeDS Schema : "+ schema);
                    fields = schema.fields();
                    logger.debug("explodeDS Fields : "+ Arrays.toString(fields));
                    pivotField = getDSFiled(fields, newFiledName);
                    pivotFieldName = newFiledName;
                    logger.debug("inside ArrType : pivotField name: "+ pivotField.name());
                    logger.debug("inside ArrType : pivotField type: "+ pivotField.dataType());
                }while(pivotField.dataType() instanceof ArrayType);

                if(index == length-1 && !(pivotField.dataType() instanceof StringType)){
                    throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Pivot column - " + pivotFieldName + " - should be String Type.");
                }else if(index != length-1 && !(pivotField.dataType() instanceof StructType)){
                    throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Pivot Parent column - " + pivotFieldName + " - should be Struct or Array Type.");
                }
            }else{
                if(index == length-1 && !(pivotField.dataType() instanceof StringType)){
                    throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Pivot column - " + pivotFieldName + " - should be String Type.");
                }else if(index != length-1 && !(pivotField.dataType() instanceof StructType)){
                    throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Pivot Parent column - " + pivotFieldName + " - should be Struct or Array Type.");
                }
            }
            index++;
        }
        return explodeDS;
    }

    private Dataset<Row> parseAggregateField(Dataset<Row> inputDS, String pivotColumn, String aggregateColumn){
        logger.debug("==> parseAggregateField()");
        StructType schema = inputDS.schema();
        logger.debug("inputDS Schema : "+ schema);
        StructField[] fields = schema.fields();
        logger.debug("DS Fields : "+ Arrays.toString(fields));
        String[] pivotColSplit = pivotColumn.trim().split("\\"+SPARK_COLUMN_NAME_DELIMITER);
        int pivotColLength = pivotColSplit.length;
        logger.debug("pivotColumnSplit length : "+ pivotColLength);
        String[] aggColSplit = aggregateColumn.trim().split("\\"+SPARK_COLUMN_NAME_DELIMITER);
        int length = aggColSplit.length;
        logger.debug("aggColSplit length : "+ length);
        Dataset<Row> explodeDS = inputDS;
        StructField aggField = null;
        int index = 0;
        for(String column : aggColSplit){
            logger.debug("agg index : "+ index);
            if(aggFieldName == null){
                aggFieldName = column;
            }else{
                aggFieldName = aggFieldName + SPARK_COLUMN_NAME_DELIMITER + column;
            }
            logger.debug("Partial aggFieldName : "+ aggFieldName);
            boolean isAlreadyExploded = isAlreadyExploded(pivotColumn, pivotColLength, aggFieldName, index);
            logger.debug("isAlreadyExploded : "+ isAlreadyExploded);
            if(!isAlreadyExploded){
                aggField = getDSFiled(fields, aggFieldName);
                logger.debug("aggField name: "+ aggField.name());
                logger.debug("aggField type: "+ aggField.dataType());
                if(index != length-1){
                    if(aggField.dataType() instanceof ArrayType){
                        String newFiledName = aggFieldName.replace(SPARK_COLUMN_NAME_DELIMITER, NEW_COLUMN_NAME_DELIMITER);
                        logger.debug("newFiledName: "+ newFiledName);
                        do{
                            explodeDS = explodeDS.withColumn(newFiledName,explode(explodeDS.col(aggFieldName)));
                            logger.info("explodeDS count : "+ explodeDS.count());
                            schema = explodeDS.schema();
                            logger.debug("explodeDS Schema : "+ schema);
                            fields = schema.fields();
                            logger.debug("explodeDS Fields : "+ Arrays.toString(fields));
                            aggField = getDSFiled(fields, newFiledName);
                            aggFieldName = newFiledName;
                            logger.debug("inside ArrType : aggField name: "+ aggField.name());
                            logger.debug("inside ArrType : aggField type: "+ aggField.dataType());
                        }while(aggField.dataType() instanceof ArrayType);

                        if(!(aggField.dataType() instanceof StructType)){
                            throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Aggregate Parent column - " + aggFieldName + " - should be Struct or Array Type.");
                        }
                    }
                }
            }
            index++;
        }
        return explodeDS;
    }

    private void validatePivotFields(String[] groupByColumns, String pivotColumn, String aggregateColumn){
        logger.debug("==> validatePivotFields()");
        logger.debug("pivotColumn : " + pivotColumn);
        if(pivotColumn == null || pivotColumn.trim().isEmpty()){
            throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Pivot column is null or empty.");
        }

        logger.debug("aggregateColumn : " + aggregateColumn);
        if(aggregateColumn == null || aggregateColumn.trim().isEmpty()){
            throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Aggregate column is null or empty.");
        }
        if(pivotColumn.trim().equalsIgnoreCase(aggregateColumn.trim())){
            throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Pivot & Aggregate columns should not be same.");
        }

        logger.debug("groupByColumns : " + Arrays.toString(groupByColumns));
        if(groupByColumns == null || groupByColumns.length == 0
            || Arrays.stream(groupByColumns).anyMatch(col -> col == null || col.trim().isEmpty())){
            throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Group By columns array is null or empty or contains null or empty values.");
        }

        if(Arrays.stream(groupByColumns).anyMatch(col -> col.trim().equalsIgnoreCase(pivotColumn) || col.trim().equalsIgnoreCase(aggregateColumn))){
            throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Group By columns array should not contains Pivot & Aggregate columns.");
        }
    }

    private StructField getDSFiled(StructField[] fields, String fieldName){
        logger.trace("==> getDSFiled()");
        String[] fieldNameSplit = fieldName.trim().split("\\"+SPARK_COLUMN_NAME_DELIMITER);
        int length = fieldNameSplit.length;
        logger.debug("fieldNameSplit length : "+ length);
        int index = 0;
        StructField structField = null;
        for(String fieldParentName : fieldNameSplit){
            boolean fieldNotFound = true;
            for(StructField field : fields){
                if(field.name().equalsIgnoreCase(fieldParentName)){
                    fieldNotFound = false;
                    if(index == length-1){
                        structField = field;
                        break;
                    }else{
                        if(field.dataType() instanceof StructType){
                            fields = ((StructType)field.dataType()).fields();
                            break;
                        }else{
                            throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. Parent column - "+fieldParentName+" - should be StructType.");
                        }
                    }
                }
            }
            if(fieldNotFound){
                throw new XDFException(XDFReturnCode.CONFIG_ERROR, "Pivot Config is not correct. column - "+fieldName+" - is not exist in Dataset schema.");
            }
            index++;
        }
        return structField;
    }

    private boolean isAlreadyExploded(String pivotColumn, int pivotColLength, String aggFieldName, int aggIndex){
        if(aggIndex < pivotColLength-1){
            return pivotColumn.toUpperCase().startsWith(aggFieldName.toUpperCase()+SPARK_COLUMN_NAME_DELIMITER);
        }else if(aggIndex == pivotColLength-1){
            return pivotColumn.equalsIgnoreCase(aggFieldName);
        }else{
            return false;
        }
    }

}
