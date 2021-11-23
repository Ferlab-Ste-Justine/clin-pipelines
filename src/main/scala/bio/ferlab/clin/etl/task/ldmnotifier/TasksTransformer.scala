package bio.ferlab.clin.etl.task.ldmnotifier

import bio.ferlab.clin.etl.task.ldmnotifier.model.Task

object TasksTransformer {
  private def flatMapTaskOutputToAttachmentUrls(task: Task): Seq[String] = {
    task.output.flatMap(output => output.valueReference.resource.content.map(_.attachment.url))
  }

  private def mapEachOfTaskAliasesToAttachmentUrls(task: Task) = {
    task.owner.resource.alias.map(alias => (alias, flatMapTaskOutputToAttachmentUrls(task)))
  }

  private def retainOnlyDistinctUrlsForEachGroup(aliasToUrls: Seq[(String, Seq[String])]) = {
    aliasToUrls.flatMap(_._2).distinct
  }

  def groupAttachmentUrlsByEachOfOwnerAliases(tasks: Seq[Task]): Map[String, Seq[String]] = {
    val aliasToAttachmentUrls = tasks.flatMap(mapEachOfTaskAliasesToAttachmentUrls)
    aliasToAttachmentUrls.groupBy(_._1).mapValues(retainOnlyDistinctUrlsForEachGroup)
  }
}
