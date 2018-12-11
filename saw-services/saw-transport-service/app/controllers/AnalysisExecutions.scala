package controllers

import com.mapr.org.apache.hadoop.hbase.util.Bytes
import model.PaginateDataSet
import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods.parse
import play.mvc.Result
import sncr.datalake.engine.ExecutionType
import sncr.datalake.handlers.AnalysisNodeExecutionHelper
import sncr.datalake.{DLConfiguration, DLSession}
import sncr.metadata.analysis.AnalysisResult
import sncr.metadata.engine.MDObjectStruct
import sncr.saw.common.config.SAWServiceConfig

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.io.File

class AnalysisExecutions extends BaseController {
  val analysisController = new Analysis

  def list(analysisId: String): Result = {
    handle((json, ticket) => {
      val executions = listExecution(analysisId)
      /* Note: Keep "results" property for API backwards compatibility */
      ("executions", executions) ~ ("results", executions): JValue
    })
  }

  def listExecution(analysisId: String): List[JObject] =
  {
    val analysis = new sncr.datalake.engine.Analysis(analysisId)
    val sortedExecutions = analysis.listExecutions.map(result => {
      val content = result.getCachedData("content") match {
        case obj: JObject => obj
        case obj: JValue => unexpectedElement("object", obj)
      }
      val id = Bytes.toString(result.getRowKey)
      (id, (content \ "execution_finish_ts").extractOpt[Long],(content \ "exec-msg").extractOpt[String],
        (content \ "executionType").extractOpt[String] ,(content \ "type").extractOpt[String] )
    }).sortBy(result =>result._2).reverse
    var count = 0
    var esReportHistoryCount =0;
    val execHistory = SAWServiceConfig.executionHistory
    val junkExecution = scala.collection.mutable.Buffer[String]()
    var analysisType :String = null
    val executions = sortedExecutions.filter(result =>{
      count = count+1
      if (analysisType==null && result._5!=None)
        analysisType =result._5.get
      val currentTime = System.currentTimeMillis
      if(count<=execHistory && !(result._5!=None && result._5.get.equalsIgnoreCase("esReport"))) true
      else {
        if (!(result._5 != None && result._5.get.equalsIgnoreCase("esReport"))) {
          junkExecution += result._1
          false
        }
        else {
          if (esReportHistoryCount<execHistory)
          {
            if(!excludeOneTimeExecution(result._4.get,result._5.get)) esReportHistoryCount+=1
            else {
              // Set the execution result TTL 60 minute for oneTime execution since execution completed.
              // Applicable only for es-report since we are storing all the execution type to support
              // pagination.
              if ((currentTime- (60*60*1000)) >= result._2.get)
                junkExecution += result._1
            }
            !excludeOneTimeExecution(result._4.get,result._5.get)
          }
          else {
            // Set the execution result TTL 60 minute for oneTime execution since execution completed.
            if ((currentTime- (60*60*1000)) >= result._2.get)
              junkExecution += result._1
            false
          }
        }
      }
    }).map(result => {
      var executionType = ""
      if(result._4!= None && result._4.get.equalsIgnoreCase(
        ExecutionType.scheduled.toString))
        executionType ="Scheduled"
      else
        executionType = "Published"
      ("id", result._1) ~
        ("finished", result._2) ~
        ("status", result._3)  ~
        ("executionType",executionType)
    })

    // Run the execution result cleanup as async execution,
    // So, actual response will not be blocked by cleanup activity
    implicit val ec = ExecutionContext.global
    Future {
      analysis.deleteByID(junkExecution)
      if (analysisType.equalsIgnoreCase("report"))
        analysis.dataLakeCleanUp(junkExecution)
    }
    executions
  }

  // input type is string because of string stream
  def isJObject(abc: Any): Boolean = {
    return parse(abc + "").getClass.getName().equals("org.json4s.JObject")
  }

  def getExecutionResult(analysisId: String, executionId: String, page: Int, pageSize: Int, analysisType: String, executionType: String): JObject = {
    // changed from Int to Long because stream returns long.
    var totalRows: Long = 0
    var pagingData: JValue = null
    var queryBuilder : JValue=null
    var executedBy :String=null
    val analysis = new sncr.datalake.engine.Analysis(analysisId)
    val execution = analysis.getExecution(executionId)
    var dataStream: java.util.stream.Stream[String] = null
    m_log.trace("analysisType {}", analysisType)
    if (analysisType == "report") {

      if (executionType != null && executionType.equalsIgnoreCase("onetime"))
      {
        val outputLocation = AnalysisNodeExecutionHelper.getUserSpecificPath(DLConfiguration.commonLocation) +
          File.separator + "preview-" + executionId
        m_log.info("Location : "+outputLocation)
        dataStream = execution.loadOneTimeExecution(outputLocation, DLConfiguration.rowLimit)
        totalRows = execution.getRowCount(executionId,outputLocation)
      }
      else {
        val resultNode = AnalysisResult(analysisId, executionId)
        val desc = resultNode.getCachedData(MDObjectStruct.key_Definition.toString)
        queryBuilder = (desc.asInstanceOf[JValue] \ "queryBuilder")
        executedBy =   (desc.asInstanceOf[JValue] \ "executedBy").extractOrElse("Anonymous")
        // since we are using streams, we don't have to use cache as it's exactly the same i.e. both are streams
        dataStream = execution.loadExecution(executionId)
        // stream can not be reused hence calling it again. Won't be any memory impact
        totalRows = execution.getRowCount(executionId)
        /* To maintain the backward compatibility check the row count with
         execution result */
        if (totalRows == 0) {
          totalRows = execution.loadExecution(executionId).count()
          if (totalRows > 0) {
            log.info("recordCount" + totalRows)
            // if count not available in node and fetched from execution result, add count for next time reuse.
            resultNode.getObject("dataLocation") match {
              case Some(dir: String) => {
                // Get list of all files in the execution result directory
                DLSession.createRecordCount(dir, totalRows)
              }
              case obj => {
                log.debug("Data location not found for results: {}", executionId)
              }
            }
          }
        }
      }
      // result holder
      val data = new java.util.ArrayList[java.util.Map[String, (String, Object)]]
      // process only the required rows and not entire data set
      val skipVal = if ((page - 1) * pageSize > 0) {
        (page - 1) * pageSize
      } else {
        0
      }
      dataStream.skip(skipVal)
        .limit(pageSize)
        .iterator().asScala
        .foreach(line => {
          val resultsRow = new java.util.HashMap[String, (String, Object)]
          parse(line) match {
            case obj: JObject => {
              /* Convert the parsed JSON to the data type expected by the
               * loadExecution method signature */
              val rowMap = obj.extract[Map[String, Any]]
              rowMap.keys.foreach(key => {
                rowMap.get(key).foreach(value => resultsRow.put(key, ("unknown", value.asInstanceOf[AnyRef])))
              })
            }
            case obj => throw new RuntimeException("Unknown result row type from JSON: " + obj.getClass.getName)
          }
          data.add(resultsRow)
        })
      pagingData = analysisController.processReportResult(data)
      ("data", pagingData) ~ ("totalRows", totalRows) ~ ("queryBuilder", queryBuilder) ~ ("executedBy", executedBy)
    }
    else {
      val anares = AnalysisResult(analysisId, executionId)
      val results = new java.util.ArrayList[java.util.Map[String, (String, Object)]]
      val desc = anares.getCachedData(MDObjectStruct.key_Definition.toString)
      val d_type = (desc.asInstanceOf[JValue] \ "type").extractOpt[String];
      queryBuilder = (desc.asInstanceOf[JValue] \ "queryBuilder")
      executedBy =   (desc.asInstanceOf[JValue] \ "executedBy").extractOrElse("Anonymous")
      if (d_type.isDefined) {

        if (d_type.get == "chart" || d_type.get == "pivot") {
          ("data", execution.loadESExecutionData(anares)) ~ ("queryBuilder", queryBuilder)~ ("executedBy", executedBy)
        }
        else if (d_type.get == "esReport") {
          val data = execution.loadESExecutionData(anares)
            .extract[scala.List[Map[String, Any]]]
            .foreach(row => {
              val resultsRow = new java.util.HashMap[String, (String, Object)]
              row.keys.foreach(key => {
                row.get(key).foreach(value => resultsRow.put(key, ("unknown", value.asInstanceOf[AnyRef])))
              })
              results.add(resultsRow)
            })
          pagingData = analysisController.processReportResult(results)
          PaginateDataSet.INSTANCE.putCache(executionId, results)
          pagingData = analysisController.processReportResult(PaginateDataSet.INSTANCE.paginate(pageSize, page, executionId))
          totalRows = PaginateDataSet.INSTANCE.sizeOfData()
          m_log.trace("totalRows {}", totalRows)
          ("data", pagingData) ~ ("totalRows", totalRows) ~ ("queryBuilder", queryBuilder)~ ("executedBy", executedBy)
        }
        else throw new Exception("Unsupported data format")
      }
      else null
    } // end of chart & PIVOT
  }

  def getExecutionData(analysisId: String, executionId: String, page: Int, pageSize: Int, analysisType: String, executionType: String): Result = {
    handle(process = (json, ticket) => {
        getExecutionResult(analysisId,executionId,page,pageSize,analysisType,executionType)
    })
  }

  def getLatestExecutionData(analysisId: String, page: Int, pageSize: Int, analysisType: String, executionType: String): Result = {
    handle(process = (json, ticket) => {
      val executions = listExecution(analysisId)
      if(executions.isEmpty){
        ("data",List[JValue]() ) ~ ("totalRows", 0) ~ ("queryBuilder", null) ~ ("executedBy", null): JValue
      }
      else{
        val executionId = ((executions.head) \ "id").extract[String]
        getExecutionResult(analysisId, executionId, page, pageSize, analysisType, executionType)
      }

    })
  }

  def execute(analysisId: String): Result = {
    handle((json, ticket) => {
      analysisController.executeAnalysis(analysisId, "scheduled", null, null, null)._1
    })
  }

  /**
    * For ES-report history are getting saved for all execution types to achieve the
    * pagination for one time execution, method will exclude the all the one time execution
    * for es-report.
    * @param result
    * @return
    */
  private  def excludeOneTimeExecution(result : (String, String)) : Boolean = {
      if (result._1 !=None && result._2!=None && result._2.equalsIgnoreCase("esReport"))
        {
           val valid =  result._1 match {
            case "onetime" => true
            case "preview" => true
            case "scheduled" => false
            case "regularExecution" => true
            case "publish" => false
            case obj => throw new RuntimeException("Unknown execution type: " + obj)
          }
          return valid
        }
        false
      }
}
