package bio.ferlab.clin.etl.fhir

import bio.ferlab.clin.etl.FileImport
import bio.ferlab.clin.etl.fhir.FhirUtils.Constants.CodingSystems
import bio.ferlab.clin.etl.fhir.testutils.{FhirTestUtils, WholeStackSuite}
import bio.ferlab.clin.etl.task.fileimport.model.TTasks
import ca.uhn.fhir.rest.api.SummaryEnum
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model._
import org.scalatest.{FlatSpec, Matchers}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import scala.collection.JavaConverters._
import scala.io.Source

class FeatureSpec extends FlatSpec with WholeStackSuite with Matchers {

  "run" should "return no errors" in {
    withS3Objects { (inputPrefix, outputPrefix) =>
      transferFromResources(inputPrefix, "good")

      val patientId = FhirTestUtils.loadPatients().getIdPart
      val fhirPatientId = s"Patient/$patientId"
      val serviceRequestId = FhirTestUtils.loadServiceRequest(patientId)
      val fhirServiceRequestId = s"ServiceRequest/$serviceRequestId"
      val organizationId = FhirTestUtils.loadOrganizations()
      val fhirOrganizationId = s"Organization/$organizationId"
      val templateMetadata = Source.fromResource("good/metadata.json").mkString
      val metadata = templateMetadata.replace("_PATIENT_ID_", patientId).replace("_SERVICE_REQUEST_ID_", serviceRequestId)
      val putMetadata = PutObjectRequest.builder().bucket(inputBucket).key(s"$inputPrefix/metadata.json").build()
      s3.putObject(putMetadata, RequestBody.fromString(metadata))

      val result = FileImport.run(inputBucket, inputPrefix, outputBucket, outputPrefix)

      println(result)
      //Validate documents that has been copied
      result.isValid shouldBe true
      val resultFiles = list(outputBucket, outputPrefix)
      println(s"Files=${resultFiles.mkString(",")}")
      resultFiles.size shouldBe 5

      // Validate specimens
      val searchSpecimens = searchFhir("Specimen")
      searchSpecimens.getTotal shouldBe 2
      searchSpecimens.getEntry.asScala.foreach { be =>
        val s = be.getResource.asInstanceOf[Specimen]
        s.getSubject.getReference shouldBe fhirPatientId
        s.getType.getCodingFirstRep.getSystem shouldBe CodingSystems.SPECIMEN_TYPE
        s.getType.getCodingFirstRep.getCode shouldBe "NBL"
      }
      val fullSpecimens = read(searchSpecimens, classOf[Specimen])

      //Validate specimen
      val optSpecimen = fullSpecimens.collectFirst { case s if !s.hasParent => s }
      optSpecimen shouldBe defined
      val specimen = optSpecimen.get
      specimen.getSubject.getReference shouldBe fhirPatientId
      specimen.getRequestFirstRep.getReference shouldBe fhirServiceRequestId
      specimen.getAccessionIdentifier.getSystem shouldBe "https://cqgc.qc.ca/labs/CHUSJ"
      specimen.getAccessionIdentifier.getValue shouldBe "submitted_specimen_id3"

      //Validate sample
      val optSample = fullSpecimens.collectFirst { case s if s.hasParent => s }
      optSample shouldBe defined
      val sample = optSample.get
      sample.getParentFirstRep.getReference shouldBe id(specimen)
      sample.getSubject.getReference shouldBe fhirPatientId
      sample.getRequestFirstRep.getReference shouldBe fhirServiceRequestId
      sample.getAccessionIdentifier.getSystem shouldBe "https://cqgc.qc.ca/labs/CHUSJ"
      sample.getAccessionIdentifier.getValue shouldBe "submitted_sample_id3"

      //Validate Service request
      val specimenIds = Seq(id(specimen), id(sample))
      val updatedSr = searchFhir("ServiceRequest")
      updatedSr.getTotal shouldBe 1
      updatedSr.getEntry.asScala.foreach { be =>
        val r = be.getResource.asInstanceOf[ServiceRequest]
        r.getSpecimen.asScala.map(_.getReference) should contain theSameElementsAs specimenIds
      }

      //Validate DocumentReference
      val searchDr = searchFhir("DocumentReference")
      searchDr.getTotal shouldBe 3
      val documentReferences = read(searchDr, classOf[DocumentReference])
      documentReferences.foreach { d =>
        val attachment = d.getContent.asScala.map { content =>
          val attachment= content.getAttachment
          val objectKey = attachment.getUrl.replace(ferloadConf.url, "").replace("/", "")
          val objectFullKey = s"$outputPrefix/$objectKey"
          //Object exist
//          assert(s3.doesObjectExist(outputBucket, objectFullKey), s"DocumentReference with key $objectKey does not exist in object store")
//          val objectMetadata = s3.getObject(outputBucket, objectFullKey).getObjectMetadata
//
//          //Size
//          attachment.getSize shouldBe objectMetadata.getContentLength
//
//          //MD5
//          new String(attachment.getHash) shouldBe objectMetadata.getETag
          d.getSubject.getReference shouldBe fhirPatientId
          d.getCustodian.getReference shouldBe fhirOrganizationId
        }
      }
      //Expected title
      documentReferences.flatMap(d => d.getContent.asScala.map(_.getAttachment.getTitle)) should contain only("file1.cram", "file1.crai", "file2.vcf", "file2.tbi", "file3.json")
      //Expected code systems
      documentReferences.flatMap(d => d.getType.getCoding.asScala.map(_.getSystem)) should contain only (CodingSystems.DR_TYPE)
      documentReferences.flatMap(d => d.getType.getCoding.asScala.map(_.getCode)) should contain only("AR", "SNV", "QC")
      documentReferences.map(d => d.getCategoryFirstRep.getCodingFirstRep.getSystem) should contain only (CodingSystems.DR_CATEGORY)
      documentReferences.map(d => d.getCategoryFirstRep.getCodingFirstRep.getCode) should contain only("SR", "SNV", "RE")
      documentReferences.flatMap(d => d.getContent.asScala.map(_.getFormat.getSystem)) should contain only (CodingSystems.DR_FORMAT)

      //Validate tasks
      val searchTasks = searchFhir("Task")
      searchTasks.getTotal shouldBe 3
      val tasks = read(searchTasks, classOf[Task])
      tasks.foreach { t =>
        t.getFor.getReference shouldBe fhirPatientId
        t.getOwner.getReference shouldBe fhirOrganizationId
        t.getFocus.getReference shouldBe fhirServiceRequestId
      }
      tasks.map(_.getCode.getText) should contain theSameElementsAs TTasks.allTypes

    }


  }
  it should "return errors" in {
    withS3Objects { (inputPrefix, outputPrefix) =>
      transferFromResources(inputPrefix, "bad")
      val result = FileImport.run(inputBucket, inputPrefix, outputBucket, outputPrefix)

      //Validate documents that has been copied
      println(result)
      result.isValid shouldBe false
    }
  }

  private def read[T <: IBaseResource](b: Bundle, theClass: Class[T]): Seq[T] = {
    b.getEntry.asScala.map { be =>
      fhirClient.read().resource(theClass).withUrl(id(be.getResource)).execute()
    }
  }

  private def id(r: Resource) = {
    IdType.of(r).toUnqualifiedVersionless.toString
  }

  private def searchFhir(resourceType: String) = {
    fhirClient.search().forResource(resourceType)
      .returnBundle(classOf[Bundle])
      .summaryMode(SummaryEnum.TRUE)
      .execute()
  }
}
