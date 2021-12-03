package bio.ferlab.clin.etl

import bio.ferlab.clin.etl.mail.MailerService.{adjustBccType, makeSmtpMailer}
import bio.ferlab.clin.etl.mail.{EmailParams, MailerService}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksGqlExtractor.{checkIfGqlResponseHasData, extractTasksWhenHasData, fetchTasksFromFhir}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksMessageComposer.{createMetaDataAttachmentFile, createMsgBody}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksTransformer.{groupAttachmentUrlsByEachOfOwnerAliases, mapLdmAliasToEmailAddress}
import cats.data.Validated.Invalid
import cats.implicits._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

object LDMNotifier extends App {
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  withSystemExit({
    withLog {
      withConf { conf =>
        if (args.length == 0) {
          LOGGER.error("first argument must be runName")
          Invalid()
        }
        val runName = args(0)
        val mailer = new MailerService(makeSmtpMailer(conf))

        /* ====>  FIXME
          1) add auth :val auth = new Auth(conf.keycloak)
          val tasksResponses = auth.withToken((_, rpt) => fetchTasksFromFhir(conf.fhir.url, rpt, runName))
          2) fail-fast given no data
        * */
        val tasksResponses = fetchTasksFromFhir("http://localhost:8080/fhir", "", runName)
        if (tasksResponses.body.isLeft) {
          LOGGER.error(s"fail to fetch tasks from fhir server")
        }
        val strResponseBody = tasksResponses.body.right.get
        val rawParsedResponse = Json.parse(strResponseBody);

        val eitherErrorMsgOrData = checkIfGqlResponseHasData(rawParsedResponse)
        if (eitherErrorMsgOrData.isLeft) {
          //FIXME exit program
          LOGGER.error(s"Error while inspecting response: ${eitherErrorMsgOrData.left.get}")
        }

        val hasNoData = !eitherErrorMsgOrData.right.get
        if (hasNoData) {
          //FIXME exit program
          LOGGER.warn(s"No found task for runName $runName")
        }

        val eitherErrorMsgOrTasks = extractTasksWhenHasData(rawParsedResponse)
        if (eitherErrorMsgOrTasks.isLeft) {
          //FIXME exit program
          LOGGER.error(s"error while extracting tasks: ${eitherErrorMsgOrTasks.left.get}")
        }

        val tasks = eitherErrorMsgOrTasks.right.get

        val aliasToEmailAddress = mapLdmAliasToEmailAddress(tasks)
        val urlsByAlias = groupAttachmentUrlsByEachOfOwnerAliases(tasks)

        val validations: List[ValidationResult[Unit]] = urlsByAlias.map { case (ldmAlias, urls) =>
          val toLDM = aliasToEmailAddress(ldmAlias)
          val blindCC = adjustBccType(conf)

          withExceptions {
            mailer.sendEmail(EmailParams(
              toLDM,
              conf.mailer.from,
              adjustBccType(conf),
              "Nouvelles données du CQGC",
              createMsgBody(urls),
              Seq(createMetaDataAttachmentFile(runName, urls))
            ))
            val extraInfoIfAvailable = if (blindCC.isEmpty) "" else s"and ${blindCC.mkString(",")}"
            LOGGER.info(s"email sent to $toLDM $extraInfoIfAvailable")
          }
        }.toList

        val validationResults: ValidationResult[List[Unit]] = validations.sequence
        if (validationResults.isInvalid) {
          LOGGER.error("Error(s) occurred while sending email(s)")
        }
        validationResults
      }
    }
  })
}
