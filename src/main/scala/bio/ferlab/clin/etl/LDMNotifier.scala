package bio.ferlab.clin.etl

import bio.ferlab.clin.etl.keycloak.Auth
import bio.ferlab.clin.etl.task.ldmnotifier.TasksGqlExtractor
import cats.data.Validated.{Invalid, Valid}
import org.slf4j.{Logger, LoggerFactory}
import sttp.client3.{HttpURLConnectionBackend, Identity, Response, UriContext, basicRequest}
import sttp.model.MediaType

object LDMNotifier extends App {
  private type TasksResponse = Identity[Response[Either[String, String]]]

  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  def fetchTasksFromFhir(baseUrl: String, token: String, runName: String): TasksResponse = {
    val backend = HttpURLConnectionBackend()
    val response = basicRequest
      .headers(Map("Authorization" -> s"Bearer $token"))
      .contentType(MediaType.ApplicationJson)
      .body(TasksGqlExtractor.buildGqlTasksQueryHttpPostBody(runName))
      .post(uri"$baseUrl/${"$graphql"}?_count=1000")
      .send(backend)
    backend.close
    response
  }

  withSystemExit {
    withLog {
      withConf(conf => {
        if (args.length == 0) {
          LOGGER.error("first argument must be runName")
          Invalid()
        }
        val runName = args(0)

        val auth = new Auth(conf.keycloak)
        val tasksResponses = auth.withToken((_, rpt) => fetchTasksFromFhir(conf.fhir.url, rpt, runName))
        if (tasksResponses.body.isLeft) {
          LOGGER.error("An error occurred while fetching tasks from fhir server")
          Invalid()
        }
        Valid()
      })
    }
  }
}
