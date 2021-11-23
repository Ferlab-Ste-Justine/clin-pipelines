package bio.ferlab.clin.etl.fhir

import bio.ferlab.clin.etl.task.ldmnotifier.TasksTransformer
import bio.ferlab.clin.etl.task.ldmnotifier.model._
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{FlatSpec, GivenWhenThen}

class TasksTransformer extends FlatSpec with GivenWhenThen {
  def makeFakeTask(): Seq[Task] = {
    val Task1Ldm1 = new Task(
      id = "task1",
      owner = new Owner(resource = new OwnerResource(id = "LDM1", alias = Seq("LDM1"))),
      output = Seq(new Output(valueReference = new ValueReference(
        reference = "DocumentReference/1",
        resource = new OutputResource(
          id = "DocumentReference/1/_history/1",
          content = Seq(
            new Content(new Attachment(url = "url1-LDM1")),
          )
        )))))

    val Task2Ldm1 = new Task(
      id = "task2",
      owner = new Owner(resource = new OwnerResource(id = "LDM1", alias = Seq("LDM1"))),
      output = Seq(new Output(valueReference = new ValueReference(
        reference = "DocumentReference/2",
        resource = new OutputResource(
          id = "DocumentReference/2/_history/1",
          content = Seq(
            new Content(new Attachment(url = "url2-LDM1"))
          )
        )))))

    val Task3Ldm2 = new Task(
      id = "task3",
      owner = new Owner(resource = new OwnerResource(id = "LDM2", alias = Seq("LDM2"))),
      output = Seq(new Output(valueReference = new ValueReference(
        reference = "DocumentReference/3",
        resource = new OutputResource(
          id = "DocumentReference/3/_history/1",
          content = Seq(
            new Content(new Attachment(url = "url1-LDM2")),
            new Content(new Attachment(url = "url2-LDM2"))
          )
        )))))

    val Task4Ldm3 = new Task(
      id = "task4",
      owner = new Owner(resource = new OwnerResource(id = "LDM3", alias = Seq("LDM3"))),
      output = Seq(new Output(valueReference = new ValueReference(
        reference = "DocumentReference/4",
        resource = new OutputResource(
          id = "DocumentReference/4/_history/1",
          content = Seq(
            new Content(new Attachment(url = "url1-LDM3"))
          )
        )))))

    Seq(Task1Ldm1, Task2Ldm1, Task3Ldm2, Task4Ldm3)
  }

  "Attachments urls in tasks" should "be grouped by each of their aliases" in {
    Given("4 tasks having aliases and attachments")
    val tasks = makeFakeTask()

    Then("all attachments should be grouped by each of their aliases")
    val aliasToUrls = TasksTransformer.groupAttachmentUrlsByEachOfOwnerAliases(tasks)
    aliasToUrls.isEmpty shouldBe false

    And("this very group should have the correct size")
    aliasToUrls.size shouldBe 3

    And("each alias should have the corresponding urls with no duplicates")
    aliasToUrls.get("LDM3") shouldBe Some(Vector("url1-LDM3"))
    aliasToUrls.get("LDM2") shouldBe Some(Vector("url1-LDM2", "url2-LDM2"))
    aliasToUrls.get("LDM1") shouldBe Some(Vector("url1-LDM1", "url2-LDM1"))
  }
}