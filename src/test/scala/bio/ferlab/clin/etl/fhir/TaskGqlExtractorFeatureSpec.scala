package bio.ferlab.clin.etl.fhir

import bio.ferlab.clin.etl.fhir.testutils.{FhirServerSuite, FhirTestUtils}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksGqlExtractor
import bio.ferlab.clin.etl.task.ldmnotifier.model.GqlResponse
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{BeforeAndAfterAll, FlatSpec, GivenWhenThen, Ignore}
import play.api.libs.json.Json
import sttp.client3.{HttpURLConnectionBackend, UriContext, basicRequest}
import sttp.model.{MediaType, StatusCode}

@Ignore //ignore this tests as long as fhir server is not correctly configured for it.
class TaskGqlExtractorFeatureSpec extends FlatSpec with GivenWhenThen with FhirServerSuite with BeforeAndAfterAll {
  def synchronouslyLoadFakeBundleForTasks(): Unit = {
    //  Assumes it magically never fails.
    val rawJsonForBundle = FhirTestUtils.getStringJsonFromResource("task/mock_bundle_for_tasks_1.json")
    val backend = HttpURLConnectionBackend()
   basicRequest
      .contentType(MediaType.ApplicationJson)
      .body(rawJsonForBundle.get)
      .post(uri"$fhirBaseUrl")
      .send(backend)
    backend.close
  }

  override def beforeAll(): Unit = {
    synchronouslyLoadFakeBundleForTasks()
  }

  "fetchTasksFromFhir" should "get an expected 'empty' graphql response" in {
    Given("a live fhir server and a runName that leads to no tasks")
    val resp = TasksGqlExtractor.fetchTasksFromFhir(fhirBaseUrl, "", "runNameThatDoesNotExist")
    Then("a valid status code is observed")
    resp.code.code shouldBe StatusCode.Ok.code
    And("no error message is present")
    resp.body.isLeft shouldBe false
    And("the response body is the one expected")
    val strResponseBody = resp.body.right.get
    val parsedBodyResponse = Json.parse(strResponseBody).validate[GqlResponse]
    parsedBodyResponse.isSuccess shouldBe true
  }

  "fetchTasksFromFhir" should "get an expected graphql response" in {
    Given("a live fhir server and a runName that leads to at least one task")
    val runNameInMockLoadedBundle = "201106_A00516_0169_AHFM3HDSXY"
    val resp = TasksGqlExtractor.fetchTasksFromFhir(fhirBaseUrl, "", runNameInMockLoadedBundle)
    Then("a valid status code is observed")
    resp.code.code shouldBe StatusCode.Ok.code
    And("no error message is present")
    resp.body.isLeft shouldBe false
    And("the response body is the one expected")
    val strResponseBody = resp.body.right.get
    val parsedBodyResponse = Json.parse(strResponseBody).validate[GqlResponse]
    parsedBodyResponse.isSuccess shouldBe true
    And("it contains data")
    val dataOption = parsedBodyResponse.get.data
    dataOption.isDefined shouldBe true
    And("data contains task(s)")
    val tasksOption = dataOption.get.taskList
    tasksOption.isDefined shouldBe true
    And("the number of expected task(s) is right")
    tasksOption.get.size shouldBe 1
  }
}
