package bio.ferlab.clin.etl.task.ldmnotifier

import bio.ferlab.clin.etl.task.ldmnotifier.model.{GqlResponse, Task}
import play.api.libs.json._

object TasksGqlExtractor {
  def extractTasks(json: JsValue): Either[String, Seq[Task]] = {
    json.validate[GqlResponse].fold(
      invalid => {
        Left(s"Errors: ${JsError.toJson(invalid)}")
      },
      valid => {
        Right(valid.data.taskList)
      },
    )
  }
}
