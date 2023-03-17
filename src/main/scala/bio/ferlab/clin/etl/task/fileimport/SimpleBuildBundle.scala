package bio.ferlab.clin.etl.task.fileimport

import bio.ferlab.clin.etl.ValidationResult
import bio.ferlab.clin.etl.conf.FerloadConf
import bio.ferlab.clin.etl.fhir.FhirUtils._
import bio.ferlab.clin.etl.fhir.IClinFhirClient
import bio.ferlab.clin.etl.task.fileimport.model._
import bio.ferlab.clin.etl.task.fileimport.validation.DocumentReferencesValidation.validateFiles
import bio.ferlab.clin.etl.task.fileimport.validation.OrganizationValidation.{validateOrganization, validateOrganizationByAlias}
import bio.ferlab.clin.etl.task.fileimport.validation.SpecimenValidation.{validateSample, validateSpecimen}
import bio.ferlab.clin.etl.task.fileimport.validation.TaskExtensionValidation._
import bio.ferlab.clin.etl.task.fileimport.validation.simple.PatientValidation.validatePatient
import bio.ferlab.clin.etl.task.fileimport.validation.simple.ServiceRequestValidation.validateServiceRequest
import ca.uhn.fhir.rest.client.api.IGenericClient
import cats.data.ValidatedNel
import cats.implicits._
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.{IdType, Resource}
import org.slf4j.{Logger, LoggerFactory}

object SimpleBuildBundle {


  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  def validate(legacy: Boolean, metadata: SimpleMetadata, files: Seq[FileEntry])(implicit clinClient: IClinFhirClient, fhirClient: IGenericClient, ferloadConf: FerloadConf): ValidationResult[TBundle] = {
    LOGGER.info("################# Validate Resources ##################")
    val taskExtensions = validateTaskExtension(metadata)
    val mapFiles = files.map(f => (f.filename, f)).toMap
    val allResources: ValidatedNel[String, List[BundleEntryComponent]] = metadata.analyses.toList.map { a =>

      (
        validateOrganization(a),
        validatePatient(a.patient),
        validateServiceRequest(a),
        validateSpecimen(a),
        validateSample(a),
        validateFiles(legacy, mapFiles, a),
        taskExtensions.map(_.forAliquot(a.labAliquotId)),
        ).mapN(createResources)

    }.combineAll

    allResources.map(TBundle)
  }

  def createResources(organization: IdType, patient: IdType, serviceRequest: TServiceRequest, specimen: TSpecimen, sample: TSpecimen, files: TDocumentReferences, taskExtensions: TaskExtensions)(implicit ferloadConf: FerloadConf): List[BundleEntryComponent] = {
    val task = TTask(taskExtensions)
    val specimenResource = specimen.buildResource(patient.toReference(), serviceRequest.sr.toReference(), organization.toReference())
    val sampleResource = sample.buildResource(patient.toReference(), serviceRequest.sr.toReference(), organization.toReference(), Some(specimenResource.toReference()))
    val documentReferencesResources: DocumentReferencesResources = files.buildResources(patient.toReference(), organization.toReference(), sampleResource.toReference())
    val serviceRequestResource = serviceRequest.buildResource(specimenResource.toReference(), sampleResource.toReference())
    val taskResource: Resource = task.buildResource(serviceRequest.sr.toReference(), patient.toReference(), organization.toReference(), sampleResource.toReference(), documentReferencesResources)

    val resourcesToCreate = (documentReferencesResources.resources() :+ taskResource).toList

    val resourcesToUpdate = Seq(serviceRequestResource).flatten

    val bundleEntriesSpecimen = Seq(specimenResource.toOption, sampleResource.toOption).flatten.map { s =>
      val be = new BundleEntryComponent()
      be.setFullUrl(s.getIdElement.getValue)
        .setResource(s)
        .getRequest
        .setUrl("Specimen")
        .setIfNoneExist(s"accession=${s.getAccessionIdentifier.getSystem}|${s.getAccessionIdentifier.getValue}")
        .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST)
      be
    }

    val bundleEntriesToCreate: Seq[BundleEntryComponent] = bundleEntriesSpecimen ++ bundleCreate(resourcesToCreate)

    val bundleEntriesToUpdate = bundleUpdate(resourcesToUpdate)
    (bundleEntriesToCreate ++ bundleEntriesToUpdate).toList


  }

}
