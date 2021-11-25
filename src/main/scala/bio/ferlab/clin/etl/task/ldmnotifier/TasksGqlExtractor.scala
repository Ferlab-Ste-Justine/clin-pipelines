package bio.ferlab.clin.etl.task.ldmnotifier

import bio.ferlab.clin.etl.task.ldmnotifier.model.{GqlResponse, Task}
import play.api.libs.json._

object TasksGqlExtractor {
  def buildGqlTasksQueryHttpPostBody(runName: String): String = {
    val query = s"""
                 |{
                 |	taskList: TaskList(run_name: "$runName") {
                 |		id
                 |	    owner @flatten {
                 |		  owner: resource(type: Organization) {
                 |				id
                 |				alias @first @singleton
                 |			}
                 |		}
                 |		 output @flatten {
                 |			valueReference @flatten {
                 |				attachments: resource(type: DocumentReference) {
                 |                     content @flatten {
                 |						 urls: attachment {
                 |							url
                 |						}
                 |					}
                 |				}
                 |			}
                 |		}
                 |	}
                 |}
                 |
                 |""".stripMargin

    s"""{ "query": ${JsString(query)} }"""
  }

  def checkIfGqlResponseHasData(json: JsValue): Either[String, Boolean] = {
    (json).validate[GqlResponse].fold(
      invalid => {
        println("invalid", invalid)
        Left(s"Errors: ${JsError.toJson(invalid)}")
      },
      valid => {
        val data = valid.data
        Right(data.isDefined && data.get.taskList.isDefined)
      },
    )
  }

  def extractTasksWhenHasData(json: JsValue): Either[String, Seq[Task]] = {
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
