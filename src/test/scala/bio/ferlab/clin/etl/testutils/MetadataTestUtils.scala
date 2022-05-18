package bio.ferlab.clin.etl.testutils

import MetadataTestUtils.defaultMetadata.analyses
import bio.ferlab.clin.etl.task.fileimport.model._

object MetadataTestUtils {

  def defaultPatient(ptId1: String, firstName: String = "John", lastName: String = "Doe", sex: String = "male"): SimplePatient = {
    SimplePatient(ptId1, firstName, lastName, sex)
  }

  def defaultFullPatient(firstName: String = "John",
                         lastName: String = "Doe",
                         sex: String = "male",
                         ddn:String = "10/12/2000",
                         ramq: Option[String] = Some("DOWJ1234"),
                         ndm: Option[String] = None,
                         ep: String = "CHUSJ",
                         designFamily: String = "SOLO",
                         position: String = "PROB",
                         familyId: Option[String] = None,
                         status: String = "AFF"
                        ): FullPatient = {
    FullPatient(firstName = firstName, lastName = lastName, sex = sex, birthDate=ddn,
      ramq = ramq, mrn = ndm, ep = ep, designFamily = designFamily,
      familyMember = position, familyId = familyId, status = status)
  }

  val defaultFullAnalysis: FullAnalysis = FullAnalysis(
    patient = defaultFullPatient(),
    ldm = "LDM-CHUSJ",
    ldmSampleId = "submitted_sample_id",
    ldmSpecimenId = "submitted_specimen_id",
    specimenType = "NBL",
    sampleType = Some("DNA"),
    bodySite = "2053",
    ldmServiceRequestId = "clin_prescription_id",
    labAliquotId = "nanuq_sample_id",
    panelCode = "MMG",
    files = defaultFilesAnalysis
  )

  val defaultFilesAnalysis: FilesAnalysis = FilesAnalysis(
    cram = "file1.cram",
    crai = "file1.crai",
    snv_vcf = "file2.vcf",
    snv_tbi = "file2.tbi",
    cnv_vcf = "file4.vcf",
    cnv_tbi = "file4.tbi",
    qc = "file3.tgz"
  )
  val defaultAnalysis: SimpleAnalysis = SimpleAnalysis(
    patient = defaultPatient("clin_id"),
    ldm = "CHUSJ",
    ldmSampleId = "submitted_sample_id",
    ldmSpecimenId = "submitted_specimen_id",
    specimenType = "NBL",
    sampleType = Some("DNA"),
    bodySite = "2053",
    clinServiceRequestId = "clin_prescription_id",
    labAliquotId = "nanuq_sample_id",
    files = defaultFilesAnalysis
  )
  val defaultExperiment: Experiment = Experiment(
    platform = Some("Illumina"),
    sequencerId = Some("NB552318"),
    runName = Some("runNameExample"),
    runDate = Some("2014-09-21T11:50:23-05:00"),
    runAlias = Some("runAliasExample"),
    flowcellId = Some("0"),
    isPairedEnd = Some(true),
    fragmentSize = Some(100),
    experimentalStrategy = Some("WXS"),
    captureKit = Some("RocheKapaHyperExome"),
    baitDefinition = Some("KAPA_HyperExome_hg38_capture_targets")
  )
  val defaultWorkflow: Workflow = Workflow(
    name = Some("Dragen"),
    version = Some("1.1.0"),
    genomeBuild = Some("GRCh38")
  )
  val defaultMetadata: SimpleMetadata = SimpleMetadata(
    defaultExperiment,
    defaultWorkflow,
    analyses = Seq(
      defaultAnalysis
    )

  )

}