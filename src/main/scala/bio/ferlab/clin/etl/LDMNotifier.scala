package bio.ferlab.clin.etl

import bio.ferlab.clin.etl.mail.MailerService.{adjustBccType, makeSmtpMailer}
import bio.ferlab.clin.etl.mail.{EmailParams, MailerService}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksMessageComposer.{createMetaDataAttachmentFile, createMsgBody}
import bio.ferlab.clin.etl.task.ldmnotifier.TasksTransformer.{groupAttachmentUrlsByEachOfOwnerAliases, mapLdmAliasToEmailAddress}
import bio.ferlab.clin.etl.task.ldmnotifier.model.{Attachments, Owner, Task, Url}
import cats.data.Validated.Invalid
import cats.data.ValidatedNel
import cats.implicits._
import org.slf4j.{Logger, LoggerFactory}

object LDMNotifier extends App {
  def makeFakeTask(): Seq[Task] = { //FIXME: just for simple testing...will be removed
    val Task1Ldm1 = Task(
      id = "task1",
      owner = Owner(id = "LDM1", alias = "LDM1", email = "LDM1@mail.com"),
      attachments = Attachments(urls = Seq(Url(url = "https://ferload.qa.clin.ferlab.bio/2b52adba.tbi")))
    )
    val Task2Ldm1 = Task(
      id = "task2",
      owner = Owner(id = "LDM1", alias = "LDM1", email = "LDM1@mail.com"),
      attachments = Attachments(urls = Seq(Url(url = "https://ferload.qa.clin.ferlab.bio/2b52adba.tbi"), Url(url = "https://ferload.qa.clin.ferlab.bio/f04c.tbi")))
    )
    val Task3Ldm2 = Task(
      id = "task3",
      owner = Owner(id = "LDM2", alias = "LDM2", email = "LDM2@mail.com"),
      attachments = Attachments(urls = Seq(Url(url = "https://ferload.qa.clin.ferlab.bio/8c71.tbi")))
    )
    val Task4Ldm3 = Task(
      id = "task4",
      owner = Owner(id = "LDM3", alias = "LDM3", email = "LDM3@mail.com"),
      attachments = Attachments(urls = Seq(Url(url = "https://ferload.qa.clin.ferlab.bio/8c71-ed041f1aad03.tbi"), Url(url = "https://ferload.qa.clin.ferlab.bio/2b52adba-f04c-ed041f1aad03.tbi")))
    )
    Seq(Task1Ldm1, Task2Ldm1, Task3Ldm2, Task4Ldm3)
  }

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

        val tasks = makeFakeTask()
        /* FIXME
        * val auth = new Auth(conf.keycloak)
          val tasksResponses = auth.withToken((_, rpt) => fetchTasksFromFhir(conf.fhir.url, rpt, runName))
           if (tasksResponses.body.isLeft) {
                LOGGER.error(s"fail to fetch tasks from fhir server: $e")
                Invalid()
             }
        * */


        val aliasToEmailAddress = mapLdmAliasToEmailAddress(tasks)
        val urlsByAlias = groupAttachmentUrlsByEachOfOwnerAliases(tasks)
        val results: List[ValidationResult[Unit]] = urlsByAlias.map{ case(ldm, urls) =>
          val toLDM = aliasToEmailAddress(ldm)
          val blindCC = adjustBccType(conf)
          withExceptions {
            mailer.sendEmail(EmailParams(
              toLDM,
              conf.mailer.from,
              adjustBccType(conf),
              "subjectTODO", //FIXME find common subject
              createMsgBody(urls),
              Seq(createMetaDataAttachmentFile(runName, urls))
            ))
            val extraInfoIfAvailable = if (blindCC.isEmpty) "" else s"and ${blindCC.mkString(",")}"
            LOGGER.info(s"email sent to $toLDM $extraInfoIfAvailable")
          }
        }.toList
        val sequence: ValidationResult[List[Unit]] = results.sequence
        sequence

      }
    }
  })
}
