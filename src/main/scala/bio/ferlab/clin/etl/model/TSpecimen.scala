package bio.ferlab.clin.etl.model


import bio.ferlab.clin.etl.fhir.FhirUtils
import bio.ferlab.clin.etl.model.TSpecimen.SPECIMEN_TYPE_CODING_SYSTEM
import ca.uhn.fhir.rest.client.api.IGenericClient
import org.hl7.fhir.r4.model._

import java.util.Date

trait TSpecimen {
  def buildResource(patientId: Reference, serviceRequest: Reference, parent: Option[Reference] = None): Either[IdType, Specimen]
}

case class TExistingSpecimen(sp: Specimen) extends TSpecimen {
  val id: IdType = IdType.of(sp)

  def buildResource(patientId: Reference, serviceRequest: Reference, parent: Option[Reference] = None): Either[IdType, Specimen] = Left(id)
}

object TSpecimen {

  val SPECIMEN_TYPE_CODING_SYSTEM = "http://fhir.cqgc.ferlab.bio/CodeSystem/specimen-type"
}

case class TNewSpecimen(lab: String, submitterId: String, specimenType: String, bodySite: String) extends TSpecimen {

  def buildResource(patientId: Reference, serviceRequest: Reference, parent: Option[Reference] = None): Either[IdType, Specimen] = {
    val specimen: Specimen = buildBase()
    specimen.setId(IdType.newRandomUuid())
    specimen.setSubject(patientId)
    specimen.getRequest.add(serviceRequest)
    parent.foreach { r => specimen.getParent.add(r) }
    Right(specimen)

  }

  def validateBaseResource()(implicit fhirClient: IGenericClient): OperationOutcome = {
    val baseResource = buildBase()
    FhirUtils.validateResource(baseResource)
  }

  private def buildBase() = {
    val specimen = new Specimen()
    specimen.setReceivedTimeElement(new DateTimeType(new Date()))
    specimen.getAccessionIdentifier.setSystem(s"https://cqgc.qc.ca/labs/$lab").setValue(submitterId)
    specimen.getType.addCoding()
      .setSystem(SPECIMEN_TYPE_CODING_SYSTEM)
      .setCode(specimenType)
    specimen
  }
}
