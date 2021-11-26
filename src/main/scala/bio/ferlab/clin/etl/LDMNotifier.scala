package bio.ferlab.clin.etl

import bio.ferlab.clin.etl.keycloak.Auth
import bio.ferlab.clin.etl.task.ldmnotifier.TasksGqlExtractor.fetchTasksFromFhir
import cats.data.Validated.{Invalid, Valid}
import org.slf4j.{Logger, LoggerFactory}

object LDMNotifier extends App {
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)
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
