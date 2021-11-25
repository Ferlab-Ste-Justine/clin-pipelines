package bio.ferlab.clin.etl.fhir

import bio.ferlab.clin.etl.LDMNotifier
import bio.ferlab.clin.etl.fhir.testutils.FhirServerSuite
import bio.ferlab.clin.etl.task.ldmnotifier.model.GqlResponse
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{FlatSpec, GivenWhenThen}
import play.api.libs.json.Json
import sttp.model.StatusCode

class LDMNotifierSpec extends FlatSpec with FhirServerSuite with GivenWhenThen {
  "fetchTasksFromFhir" should "get an expected graphql response" in {
    Given("a live fhir server and a runName that leads to no tasks")
    val resp = LDMNotifier.fetchTasksFromFhir(fhirBaseUrl, "", "runNameThatDoesNotExist")
    Then("a valid status code is observed")
    resp.code.code shouldBe StatusCode.Ok.code
    And("no error message is present")
    resp.body.isLeft shouldBe false
    And("the response body is the one expected")
    val strResponseBody = resp.body.right.get
    val parsedBodyResponse = Json.parse(strResponseBody).validate[GqlResponse]
    parsedBodyResponse.isSuccess shouldBe true
  }

}
