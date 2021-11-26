package bio.ferlab.clin.etl.fhir

import bio.ferlab.clin.etl.fhir.testutils.FhirTestUtils
import bio.ferlab.clin.etl.task.ldmnotifier.TasksGqlExtractor
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{FlatSpec, GivenWhenThen}

class TasksGqlExtractorSpec extends FlatSpec with GivenWhenThen {
  "buildGqlTasksQueryHttpPostBody" should "give a correct http post body" in {
    Given("a runName (for example, 'abc') ")
    val runName = "abc"
    Then("a correct graphql query should be produced")
    val httpBody = TasksGqlExtractor.buildGqlTasksQueryHttpPostBody(runName)
    httpBody shouldBe "{ \"query\": \"\\n{\\n\\ttaskList: TaskList(run_name: \\\"abc\\\") {\\n\\t\\tid\\n\\t    owner @flatten {\\n\\t\\t  owner: resource(type: Organization) {\\n\\t\\t\\t\\tid\\n\\t\\t\\t\\talias @first @singleton\\n\\t\\t\\t}\\n\\t\\t}\\n\\t\\t output @flatten {\\n\\t\\t\\tvalueReference @flatten {\\n\\t\\t\\t\\tattachments: resource(type: DocumentReference) {\\n                     content @flatten {\\n\\t\\t\\t\\t\\t\\t urls: attachment {\\n\\t\\t\\t\\t\\t\\t\\turl\\n\\t\\t\\t\\t\\t\\t}\\n\\t\\t\\t\\t\\t}\\n\\t\\t\\t\\t}\\n\\t\\t\\t}\\n\\t\\t}\\n\\t}\\n}\\n\\n\" }"
  }

  "A well-formed graphql response with no data" should "be handled correctly" in {
    Given("a correctly parsed json containing no data ({ 'data': {} })")
    val parsed = FhirTestUtils.parseJsonFromResource("task/graphql_http_resp_empty_data_no_errors.json")
    val eitherErrorOrData = TasksGqlExtractor.checkIfGqlResponseHasData(parsed.get)
    Then("no error is reported")
    val hasError = eitherErrorOrData.isLeft
    hasError shouldBe false
    And("reports that there is no data, as well")
    val hasData = eitherErrorOrData.right.get
    hasData shouldBe false
  }

  "A well-formed graphql response with data" should "give task(s)" in {
    Given("a correctly parsed json containing 4 tasks")
    val parsed = FhirTestUtils.parseJsonFromResource("task/graphql_http_resp_run_name_1.json")

    Then("the tasks should be extracted with no errors")
    val eitherErrorOrData = TasksGqlExtractor.checkIfGqlResponseHasData(parsed.get)
    eitherErrorOrData.isLeft shouldBe false
    eitherErrorOrData.isRight shouldBe true

    val eitherErrorOrTasks = TasksGqlExtractor.extractTasksWhenHasData(parsed.get)
    eitherErrorOrTasks.isLeft shouldBe false

    And("their size be the same as in the raw data")
    val tasks = eitherErrorOrTasks.right.get
    tasks.size shouldBe 4
  }
}

