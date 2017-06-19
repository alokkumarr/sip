package sncr.datalake.engine

import com.mapr.org.apache.hadoop.hbase.util.Bytes
import org.slf4j.{Logger, LoggerFactory}
import sncr.datalake.engine.ExecutionType.ExecutionType
import sncr.metadata.analysis.{AnalysisNode, AnalysisResult}

/**
  * Created by srya0001 on 6/8/2017.
  */
class Analysis(val analysisId : String) {

  protected val m_log: Logger = LoggerFactory.getLogger(classOf[Analysis].getName)
  val an = AnalysisNode(analysisId)
  /**
    * start execution of an analysis and return an execution object immediately; store the "type" parameter in
    * the execution object, but no need for the Spark SQL executor to do anything with it
    */

  def execute(execType: ExecutionType) : AnalysisExecution =
  {
    m_log debug s"Execute analysis as ${execType.toString}"
    val analysisExecution = new AnalysisExecution(an, execType)
    analysisExecution.startExecution( !(execType == ExecutionType.preview))
    analysisExecution
  }

  /**
    * Returns the list of executions for that analysis
    */
  def listExecutions : List[AnalysisResult] = {
    val keys = Map("analysisId" -> analysisId)
    val analysisResult = new AnalysisResult(analysisId)
    val rowIds : List[Array[Byte]] = analysisResult.simpleMetadataSearch(keys, "and")
    rowIds.map( rowId => { val rowIdStr = Bytes.toString( rowId ); AnalysisResult(analysisId, rowIdStr)} )
  }

  def delete : Unit = an.deleteAnalysisResults()

  def getExecution(id: String ) : AnalysisExecution =
  {
      val analysisExecution = new AnalysisExecution(an, ExecutionType.onetime)
      analysisExecution
  }

}

object ExecutionStatus extends Enumeration{

  type ExecutionStatus = Value
  val INIT = Value(10,"Init")
  val STARTED = Value(1,"Execution started")
  val IN_PROGRESS = Value(2,"Execution is in progress")
  val FAILED = Value(-1,"Execution failed")
  val COMPLETED = Value(0,"Execution successfully completed")

}