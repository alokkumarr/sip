import org.json4s._
import org.scalatest.CancelAfterFailure

/* Test analysis service operations */
class AnalysisTest extends MaprTest with CancelAfterFailure {
  "Analysis service" should {
    requireMapr
    var id: String = null

    "create analysis" in {
      /* Write analysis */
      val body = actionKeyMessage("create", "semantic-123")
      val response = sendRequest(body)
      val JString(analysisId) = analyze(response) \ "id"
      id = analysisId
    }

    "update analysis" in {
      /* Update previously created analysis */
      val body = actionKeyAnalysisMessage("update", id,
        analysisJson(id, "customer-2"))
      val response = sendRequest(body)
      val JString(analysisId) = analyze(response) \ "id"
      analysisId must be (id)
    }

    "read analysis" in {
      /* Read back previously created analysis */
      val body = actionKeyMessage("read", id)
      val response = sendRequest(body)
      val analysis = analyze(response)
      val JString(name) = analysis \ "name"
      name must be (s"test-$id")
      val JString(customerCode) = analysis \ "customerCode"
      customerCode must be ("customer-2")
    }

    "execute analysis" in {
      /* Execute previously created analysis */
      val body = actionKeyMessage("read", id)
      val response = sendRequest(body)
      /* TODO */
    }

    "delete analysis" in {
      /* Delete previously created analysis */
      val body = actionKeyMessage("delete", id)
      val response = sendRequest(body)
      val JString(action) = response \ "contents" \ "action"
      action must be ("delete")
    }
  }

  def analyze(response: JValue): JValue = {
    (response \ "contents" \ "analyze")(0)
  }
}
