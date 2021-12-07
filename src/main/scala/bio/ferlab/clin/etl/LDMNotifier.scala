package bio.ferlab.clin.etl

import bio.ferlab.clin.etl.mail.MailerService.{adjustBccType, makeSmtpMailer}
import bio.ferlab.clin.etl.mail.{EmailParams, MailerService}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksGqlExtractor.{checkIfGqlResponseHasData, fetchTasksFromFhir}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksMessageComposer.{createMetaDataAttachmentFile, createMsgBody}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksTransformer.{AliasToEmailAddress, AliasToUrlValues, groupAttachmentUrlsByEachOfOwnerAliases, mapLdmAliasToEmailAddress}
import bio.ferlab.clin.etl.task.ldmnotifier.model.Task
import cats.implicits._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

object LDMNotifier extends App {
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  withSystemExit({
    withLog {
      withConf { conf =>
        if (args.length == 0) {
          "first argument must be runName".invalidNel[Any]
        }
        val runName = args(0)
        val mailer = new MailerService(makeSmtpMailer(conf))

        /* ====>  FIXME
          1) add auth :val auth = new Auth(conf.keycloak)
          val tasksResponses = auth.withToken((_, rpt) => fetchTasksFromFhir(conf.fhir.url, rpt, runName))
          2) fail-fast given no data
        * */

        val tasksE: Either[String, Seq[Task]] = for {
          strResponseBody <- fetchTasksFromFhir("http://localhost:8080/fhir", "", runName).body
          rawParsedResponse = Json.parse(strResponseBody)
          taskList <- checkIfGqlResponseHasData(rawParsedResponse)
        } yield taskList

        val tasksV: ValidationResult[Seq[Task]] = tasksE.toValidatedNel

        def sendEmails(aliasToEmailAddress: AliasToEmailAddress, urlsByAlias: AliasToUrlValues): ValidationResult[List[Unit]] = {
          urlsByAlias.toList.traverse { case (ldmAlias, urls) =>
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
          }
        }

        tasksV.flatMap { t: Seq[Task] =>
          val a = mapLdmAliasToEmailAddress(t)
          val u = groupAttachmentUrlsByEachOfOwnerAliases(t)
          sendEmails(a, u)
        }

      }
    }
  })
}
