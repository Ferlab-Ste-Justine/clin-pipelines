package bio.ferlab.clin.etl.fhir

import bio.ferlab.clin.etl.fhir.testutils.FhirTestUtils
import bio.ferlab.clin.etl.task.ldmnotifier.TasksGqlExtractor
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{FlatSpec, GivenWhenThen}

class TasksGqlExtractorSpec extends FlatSpec with GivenWhenThen {
  "A well-formed graphql response with data" should "give task(s)" in {
    Given("a correctly parsed json containing 4 tasks")
    val parsed = FhirTestUtils.parseJsonFromResource("task/graphql_http_resp_run_name_1.json")

    Then("the tasks should be extracted with no errors")
    val either = TasksGqlExtractor.extractTasks(parsed.get)
    either.isLeft shouldBe false

    And("their size be the same as in the raw data")
    val tasks = either.right.get
    tasks.size shouldBe 4
    println(tasks)//TODO
  }
}

