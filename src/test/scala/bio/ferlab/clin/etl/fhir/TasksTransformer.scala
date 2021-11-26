package bio.ferlab.clin.etl.fhir

import bio.ferlab.clin.etl.task.ldmnotifier.TasksTransformer
import bio.ferlab.clin.etl.task.ldmnotifier.model._
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{FlatSpec, GivenWhenThen}

class TasksTransformer extends FlatSpec with GivenWhenThen {
  def makeFakeTask(): Seq[Task] = {
    val Task1Ldm1 = Task(
      id = "task1",
      owner = Owner(id = "LDM1", alias = "LDM1"),
      attachments = Attachments(urls = Seq(Url(url = "url1-LDM1")))
    )
    val Task2Ldm1 = Task(
      id = "task2",
      owner = Owner(id = "LDM1", alias = "LDM1"),
      attachments = Attachments(urls = Seq(Url(url = "url1-LDM1"), Url(url = "url2-LDM1")))
    )
    val Task3Ldm2 = Task(
      id = "task3",
      owner = Owner(id = "LDM2", alias = "LDM2"),
      attachments = Attachments(urls = Seq(Url(url = "url1-LDM2")))
    )
    val Task4Ldm3 = Task(
      id = "task4",
      owner = Owner(id = "LDM3", alias = "LDM3"),
      attachments = Attachments(urls = Seq(Url(url = "url1-LDM3"), Url(url = "url2-LDM3")))
    )
    Seq(Task1Ldm1, Task2Ldm1, Task3Ldm2, Task4Ldm3)
  }

  "Attachments urls in tasks" should "be grouped by each of their aliases" in {
    Given("4 tasks having aliases and attachments (possibly with duplicates)")
    val tasks = makeFakeTask()

    Then("all attachments (more precisely, url values) should be grouped by each of their alias")
    val aliasToUrls = TasksTransformer.groupAttachmentUrlsByEachOfOwnerAliases(tasks)
    aliasToUrls.isEmpty shouldBe false

    And("this very group should have the correct size")
    aliasToUrls.size shouldBe 3

    And("each alias should have the corresponding urls with no duplicates")
    aliasToUrls.get("LDM3") shouldBe Some(List("url1-LDM3", "url2-LDM3"))
    aliasToUrls.get("LDM2") shouldBe Some(List("url1-LDM2"))
    aliasToUrls.get("LDM1") shouldBe Some(List("url1-LDM1", "url2-LDM1"))
  }
}