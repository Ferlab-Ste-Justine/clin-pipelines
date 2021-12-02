package bio.ferlab.clin.etl.task.ldmnotifier

import bio.ferlab.clin.etl.task.ldmnotifier.model.{GqlResponse, Task}
import play.api.libs.json._
import sttp.client3.{HttpURLConnectionBackend, Identity, Response, UriContext, basicRequest}
import sttp.model.MediaType

object TasksGqlExtractor {
  private type TasksResponse = Identity[Response[Either[String, String]]]

  val GQL_COUNT_UPPER_BOUND = 1000 //needed right now to make graphql queries work.

  def buildGqlTasksQueryHttpPostBody(runName: String): String = {
    val query = s"""
                 |{
                 |	taskList: TaskList(run_name: "$runName") {
                 |		id
                 |	    owner @flatten {
                 |		  owner: resource(type: Organization) {
                 |				id
                 |				alias @first @singleton
                 |        email: telecom @first @singleton {
                 |                    value
                 |            }
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

  def fetchTasksFromFhir(baseUrl: String, token: String, runName: String): TasksResponse = {
    val backend = HttpURLConnectionBackend()
    val response = basicRequest
      .headers(Map("Authorization" -> s"Bearer $token"))
      .contentType(MediaType.ApplicationJson)
      .body(buildGqlTasksQueryHttpPostBody(runName))
      .post(uri"$baseUrl/${"$graphql"}?_count=$GQL_COUNT_UPPER_BOUND")
      .send(backend)
    backend.close
    response
  }

  def checkIfGqlResponseHasData(json: JsValue): Either[String, Boolean] = {
    (json).validate[GqlResponse].fold(
      invalid => {
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
