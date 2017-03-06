package sncr.metadata.ui_components

import java.util

import org.json4s.JsonAST.{JInt, JString, _}
import org.json4s.ParserUtil.ParseException
import org.json4s.native.JsonMethods._
import org.json4s.{JField => _, JNothing => _, JObject => _, JValue => _, _}
import org.slf4j.{Logger, LoggerFactory}
import sncr.metadata.engine.ProcessingResult._
import sncr.metadata.engine.{MDObjectStruct, Response}



/**
  * Created by srya0001 on 2/17/2017.
  */
import MDObjectStruct.formats
class UIMDRequestHandler(val docAsJson : JValue, val printPretty: Boolean = true) extends Response {


  val m_log: Logger = LoggerFactory.getLogger(classOf[UIMDRequestHandler].getName)


  def handleRequestIncorrect: List[JValue] =
  {
    List(new JObject(List(JField("result", new JInt(Rejected.id)), JField("reason", new JString("Request structure is not correct")))))
  }

  def validateAndExecute: String = {

    validate match
    {
      case (0, _) => execute
      case (res_id: Int, r: String) => {
        val msg = render(JObject(List(JField("result", new JInt(Rejected.id)),JField("reason", new JString(r)))))
        val response = if (!printPretty) compact(msg) else pretty(msg)
        m_log debug "Request is not valid: " + response
        response
      }
    }
  }


  def execute :String =
  {
    var elements : JValue = docAsJson \ "contents"

    val ticket: JValue = docAsJson \ "ticket"
    val action = (docAsJson \ "contents" \ "action").extract[String].toLowerCase
    elements = elements.removeField( pair => pair._1.equalsIgnoreCase("action") )

    var responses : List[JValue] = Nil

    m_log debug s"action = $action"
    m_log debug s"All Content Elements => " + compact(render(elements))

    action match {
      case "create" | "update" =>
        elements match {
          case JObject(all_content_elements) => {
            responses = JObject(all_content_elements)
              .obj
              .filter(an_element => !an_element._1.equalsIgnoreCase("keys"))
              .flatMap(content_element => {
                m_log debug (s"Content element ${content_element._1} => " + compact(render(content_element._2)))
                val mn = content_element._1
                content_element._2 match {
                  case ce: JObject => m_log debug "UI Item from object: " + compact(render(ce)); List(actionHandler(ticket, action, ce, mn))
                  case ce: JArray => ce.arr.map(ce_ae => {
                            m_log debug "UI Item, array element: " + compact(render(ce_ae))
                            actionHandler(ticket, action, ce_ae, mn)
                  })
                  case _ => handleRequestIncorrect
                }
              }
              )
          }
          case _ => return "Create/Update/Delete request is not correct, contents must be JSON object"
        }
/*
      case "update" =>
        elements match {
          case JObject(all_content_elements) => {
            responses = JObject(all_content_elements)
              .obj
              .filter(an_element => !an_element._1.equalsIgnoreCase("keys") )
              .flatMap(content_element => {
                m_log debug (s"Content element ${content_element._1} => " + compact(render(content_element._2)))
                val mn = content_element._1
                content_element._2 match {
                  case ce: JObject => m_log debug "UI Item from object: " + compact(render(ce)); List(actionHandler(ticket, action, ce, mn))
                  case ce: JArray => ce.arr.map(ce_ae => {
                    m_log debug "UI Item, array element: " + compact(render(ce_ae))
                    actionHandler(ticket, action, ce_ae, mn)
                  })
                  case _ => handleRequestIncorrect
                }
              }
              )
          }
          case _ => return "Create/Update/Delete request is not correct, contents must be JSON object"
        }
*/
      case "retrieve" | "search" | "delete" =>
        elements match {
          case JObject(all_content_elements) => {
            responses = JObject(all_content_elements).obj
              .filter(an_element => an_element._1.equalsIgnoreCase("keys"))
              .map( an_element => {m_log debug s"Search/Retrieval keys ${an_element._1} => ${compact(render(an_element._2))}"
                  actionHandler(ticket, action, an_element._2, "keys")
              })
          }
          case _ => return "Search/Retrieval request is not correct, contents must be JSON object"
        }

    }
    val cnt = JField("contents", new JArray(responses))
    val r_ticket = JField("ticket", ticket)
    val res = JField("performed_action", new JString(action))
    val requestResult = new JObject(List(r_ticket, res, cnt))
    if (!printPretty) compact(render(requestResult)) else pretty(render(requestResult)) + "\n"
  }


  private def actionHandler(ticket : JValue, action: String, content_element : JValue, module_name: String) : JValue =
  {
    val sNode = new UINode(ticket, content_element, module_name)
    val response = action match {
      case "create" => build(sNode.storeNode)
      case "retrieve" => build(sNode.retrieveNode(extractKeys))
      case "search" => build(sNode.searchNodes(extractKeys))
      case "delete" => build(sNode.removeNode(extractKeys))
      case "update" => build(sNode.modifyNode(extractKeys))
    }
    m_log debug s"Response: ${pretty(render(response))}\n"
    response
  }


  def extractKeys : Map[String, Any] =
  {
    val filter_values : Map[String, JValue]  = (docAsJson \ "contents" \ "keys"
     match{
        case JObject(keyList) => keyList.filter( kf => SearchDictionary.searchFields.contains( kf._1) )
        case _ => Map.empty
     }).toList.toMap

    m_log trace s"Extracted keys: ${filter_values.mkString("{", ", ", "}")}"

    filter_values.map(key_values =>
      SearchDictionary.searchFields(key_values._1) match {
        case "String"  => key_values._1 -> key_values._2.extract[String]
        case "Int"     => key_values._1 -> key_values._2.extract[Int]
        case "Long"    => key_values._1 -> key_values._2.extract[Long]
        case "Boolean" => key_values._1 -> key_values._2.extract[Boolean]
      })
  }

  def validate : (Int, String ) = {

    if (docAsJson == null || docAsJson.toSome.isEmpty)
      (Error.id, "Validation fails: document is empty")

    val action : String = (docAsJson \ "contents" \ "action").extractOpt[String].getOrElse("invalid").toLowerCase()
    if (!List("create", "update", "delete", "retrieve", "search", "scan").exists(_.equalsIgnoreCase(action))){
      val msg = s"Action is incorrect: $action"
      m_log debug Rejected.id + " ==> " + msg
      return (Rejected.id, msg)
    }

    m_log debug "Validate token, action and content section"
    UIMDRequestHandler.requiredFields.keySet.foreach {
      case k@"ticket" => UIMDRequestHandler.requiredFields(k).foreach(rf => {
        val fieldValue = docAsJson \ k \ rf
        if (fieldValue == null || fieldValue.extractOpt[String].isEmpty) {
          val msg = s"Required token field $k.$rf is missing or empty"
          m_log debug Rejected.id + " ==> " + msg
          return (Rejected.id, msg)
        }
      })
      case k@"root" => {
        UIMDRequestHandler.requiredFields(k).foreach {
          case rf@"contents" =>
            val fieldValue = docAsJson \ rf
            if (fieldValue == null || !fieldValue.isInstanceOf[JObject] || fieldValue.toSome.isEmpty) {
              val msg = s"Required object $k.$rf is missing or empty"
              m_log debug Rejected.id + " ==> " + msg
              return (Rejected.id, msg)
            }
        }
      }
    }
    action match {
      case "retrieve" | "search" | "delete" =>
      {
        val keySection = docAsJson \ "contents" \ "keys"
        if ( keySection == null || keySection.toSome.isEmpty) {
          val msg = s"Keys list (filter) is missing or empty"; m_log debug Rejected.id + " ==> " + msg; return (Rejected.id, msg)
        }

        val all_keys = docAsJson \ "contents" \ "keys"
        var filteringKeys: List[JValue] = Nil
        all_keys match {
          case JObject(keys) => {
            filteringKeys = JObject(keys).obj.filter(k => SearchDictionary.searchFields.contains(k._1)).map(a_key => {
              m_log debug (s"Module ${a_key._1} => " + compact(render(a_key._2)))
              a_key._2
            })
          }
          case _ => {
            val msg = "Incorrect request structure"; return (Rejected.id, msg)
          }
        }
        if (filteringKeys.isEmpty) {
          val msg = s"Keys list does not have any pre-defined keys"; m_log debug Rejected.id + " ==> " + msg; return (Rejected.id, msg)
        }
        (Success.id, "Success")
      }
      case "update" | "create" =>
        { val elements = docAsJson \ "contents"
          elements match {
            case JObject(all_content_elements) => {
              if(JObject(all_content_elements).obj.filter(an_element => !an_element._1.equalsIgnoreCase("keys")).nonEmpty)
                return (Success.id, "Success")
              else{
                  val msg = s"Create and update require module description"
                  m_log debug Rejected.id + " ==> " + msg
                  return (Rejected.id, msg)
              }
            }
            case _ => {
              val msg = "Incorrect request structure"; return (Rejected.id, msg)
            }
        }
      }
    }
    (Success.id, "Success")
  }

}


object UIMDRequestHandler{

  val m_log: Logger = LoggerFactory.getLogger("SemanticMDRequestHandler")

  val requiredFields = Map(
    "ticket" -> List("ticketId", "masterLoginId","customer_code","dataSecurityKey","userName"),
    "root" -> List("contents")
  )

  def getHandlerForRequest(document: String, printPretty: Boolean) : List[UIMDRequestHandler] = {
    try {
      val docAsJson = parse(document, false, false)
      docAsJson.children.flatMap(reqSegment => {
        m_log.debug("Process JSON: " + compact(render(reqSegment)))
        reqSegment match {
          case rs: JObject => List(new UIMDRequestHandler(rs, printPretty))
          case rs: JArray => rs.arr.map(jv => new UIMDRequestHandler(jv, printPretty))
          case _ => List.empty
        }
      })
    }
    catch{
      case pe:ParseException => m_log error( s"Could not parse the document, error: ", pe ); List.empty
      case x : Throwable => m_log error( s"Exception while parsing the document, error: ", x ); List.empty
    }
  }

  import scala.collection.JavaConversions._
  def getHandlerForRequest4Java(document: String, printPretty: Boolean) : util.List[UIMDRequestHandler] = {
    getHandlerForRequest(document, printPretty)
  }

  def scanSemanticTable(printPretty: Boolean) : String = {
    val srh = new UIMDRequestHandler( null )
    val sNode = new UINode(null, null, null)
    val result = if (!printPretty) compact(render(srh.build(sNode.scanNodes)))
                 else pretty(render(srh.build(sNode.scanNodes)))
    m_log debug result
    result
  }

}
