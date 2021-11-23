package bio.ferlab.clin.etl.task.ldmnotifier

import bio.ferlab.clin.etl.task.ldmnotifier.model.Task
import play.api.libs.json._

object TasksGqlExtractor {
  def extractTasks(json: JsValue): Either[String, Seq[Task]] = {
    (json \ "data" \ "taskList").validate[Seq[Task]].fold(
      invalid => {
        Left(s"Errors: ${JsError.toJson(invalid)}")
      },
      valid => {
        Right(valid)
      },
    )
  }
}
