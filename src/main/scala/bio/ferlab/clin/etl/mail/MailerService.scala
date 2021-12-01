package bio.ferlab.clin.etl.mail

import play.api.libs.mailer._
import javax.inject.Inject

class MailerService @Inject() (mailerClient: MailerClient) {
  private type MessageId = String

  def sendEmail(to: String, bodyText: String, attachments: Seq[AttachmentFile]): MessageId = {
    val email = Email(
      "subject",
      "Mister FROM <from@email.com>",
      Seq(to),
      attachments = attachments,
      bodyText = Some(bodyText),
    )
    mailerClient.send(email)
  }

}