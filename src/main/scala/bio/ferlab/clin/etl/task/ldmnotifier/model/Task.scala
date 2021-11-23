package bio.ferlab.clin.etl.task.ldmnotifier.model

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class GqlResponse(data: Data)

object GqlResponse {
  implicit val gqlResponseReads: Reads[GqlResponse] = (JsPath \ "data").read[Data].map(GqlResponse.apply)
}

case class Data(taskList: Seq[Task])

object Data {
  implicit val dataReads: Reads[Data] = (JsPath \ "TaskList").read[Seq[Task]].map(Data.apply)
}

case class Task(id: String, owner: Owner, output: Seq[Output])

object Task {
  implicit val taskReads: Reads[Task] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "owner").read[Owner] and
      (JsPath \ "output").read[Seq[Output]]
    ) (Task.apply _)
}

case class Owner(resource: OwnerResource)

object Owner {
  implicit val ownerReads: Reads[Owner] = (JsPath \ "resource").read[OwnerResource].map(Owner.apply)
}

case class OwnerResource(id: String, alias: Seq[String])

object OwnerResource {
  implicit val ownerResourceReads: Reads[OwnerResource] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "alias").read[Seq[String]]
    ) (OwnerResource.apply _)
}

case class Output(valueReference: ValueReference)

object Output {
  implicit val outputReads: Reads[Output] = (JsPath \ "valueReference").read[ValueReference].map(Output.apply)
}

case class ValueReference(reference: String, resource: OutputResource)

object ValueReference {
  implicit val valueReferenceReads: Reads[ValueReference] = (
    (JsPath \ "reference").read[String] and
      (JsPath \ "resource").read[OutputResource]
    ) (ValueReference.apply _)
}

case class OutputResource(id: String, content: Seq[Content])

object OutputResource {
  implicit val outputResourceReads: Reads[OutputResource] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "content").read[Seq[Content]]
    ) (OutputResource.apply _)
}

case class Content(attachment: Attachment)

object Content {
  implicit val contentReads: Reads[Content] = (JsPath \ "attachment").read[Attachment].map(Content.apply)
}

case class Attachment(url: String)

object Attachment {
  implicit val attachmentReads: Reads[Attachment] = (JsPath \ "url").read[String].map(Attachment.apply)
}