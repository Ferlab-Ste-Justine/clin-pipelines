package bio.ferlab.clin.etl.task.ldmnotifier

import bio.ferlab.clin.etl.task.ldmnotifier.model.{Attachments, Task}

object TasksTransformer {
  private type AliasUrlValues = (String, Seq[String])

  private type AliasToUrlValues = Map[String, Seq[String]]

  private def mapAttachmentsToUrlValues(attachments: Attachments): Seq[String] = {
    attachments.urls.map(url => url.url)
  }

  private def retainOnlyDistinctUrlValuesForEachGroup(aliasToUrlsForAllTasks: Seq[AliasUrlValues] )= {
    val flattenedUrlValues = aliasToUrlsForAllTasks.flatMap(tupleAliasUrlValues => tupleAliasUrlValues._2)
    flattenedUrlValues.distinct
  }

  def groupAttachmentUrlsByEachOfOwnerAliases(tasks: Seq[Task]): AliasToUrlValues = {
    val aliasesToUrlsFromEachTask = tasks.map(task => (task.owner.alias, mapAttachmentsToUrlValues(task.attachments)))
    aliasesToUrlsFromEachTask
      .groupBy(tupleAliasUrlValue => tupleAliasUrlValue._1)
      .mapValues(retainOnlyDistinctUrlValuesForEachGroup)
  }
}
