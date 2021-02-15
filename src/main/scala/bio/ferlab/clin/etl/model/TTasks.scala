package bio.ferlab.clin.etl.model

import bio.ferlab.clin.etl.ResourceExtension
import org.hl7.fhir.r4.model.Task.{ParameterComponent, TaskOutputComponent}
import org.hl7.fhir.r4.model.{CodeableConcept, Reference, Resource}

case class TTasks(sequencingAlignment: TTask, variantCall: TTask, qualityControl: TTask) {
  private val SEQUENCING_ALIGNMENT_ANALYSIS = "Sequencing Alignment Analysis"

  private val ANALYSED_SAMPLE = "Analysed sample"

  private val CRAM_FILE = "CRAM File"

  private val CRAI_FILE = "CRAI File"

  private val VCF_FILE = "VCF File"

  private val TBI_FILE = "TBI File"

  private val VARIANT_CALLING_ANALYSIS = "Variant Calling Analysis"

  private val SEQUENCING_QC_ANALYSIS = "Sequencing QC Analysis"

  private val QC_FILE = "QC File"

  def buildResources(serviceRequest: Reference, patient: Reference, organization: Reference, sample: Reference, drr: DocumentReferencesResources): Seq[Resource] = {

    val sequencingExperimentInput = {
      val p = new ParameterComponent()
      p.setType(new CodeableConcept().setText(ANALYSED_SAMPLE))
      Seq(p.setValue(sample))
    }
    val sequencingExperimentOutput = {
      val cram = new TaskOutputComponent()
        .setType(new CodeableConcept().setText(CRAM_FILE)) //TODO Use a terminology
        .setValue(drr.cram.toReference())
      val crai = new TaskOutputComponent()
        .setType(new CodeableConcept().setText(CRAI_FILE)) //TODO Use a terminology
        .setValue(drr.crai.toReference())
      Seq(cram, crai)
    }
    val sequencingAlignmentR =
      sequencingAlignment.buildResource(SEQUENCING_ALIGNMENT_ANALYSIS,serviceRequest, patient, organization, sequencingExperimentInput, sequencingExperimentOutput)

    val variantCallInput = {
      val p = new ParameterComponent()
        .setType(new CodeableConcept().setText(SEQUENCING_ALIGNMENT_ANALYSIS)) //TODO Use a terminology
      Seq(p.setValue(sequencingAlignmentR.toReference()))
    }
    val variantCallOutput = {
      val vcf = new TaskOutputComponent()
        .setType(new CodeableConcept().setText(VCF_FILE)) //TODO Use a terminology
        .setValue(drr.cram.toReference())
      val tbi = new TaskOutputComponent()
        .setType(new CodeableConcept().setText(TBI_FILE)) //TODO Use a terminology
        .setValue(drr.crai.toReference())
      Seq(vcf, tbi)
    }
    val variantCallR = variantCall.buildResource(VARIANT_CALLING_ANALYSIS,serviceRequest, patient, organization, variantCallInput, variantCallOutput)

    val qualityControlInput = {
      val sq = new ParameterComponent()
        .setType(new CodeableConcept().setText(SEQUENCING_ALIGNMENT_ANALYSIS)) //TODO Use a terminology
        .setValue(sequencingAlignmentR.toReference())
      val variantCall = new ParameterComponent()
        .setType(new CodeableConcept().setText(VARIANT_CALLING_ANALYSIS)) //TODO Use a terminology
        .setValue(variantCallR.toReference())
      Seq(sq, variantCall)
    }

    val qualityControlOutput = {
      val p = new TaskOutputComponent()
        .setType(new CodeableConcept().setText(QC_FILE)) //TODO Use a terminology
        .setValue(drr.qc.toReference())
      Seq(p.setValue(sequencingAlignmentR.toReference()))
    }
    val qualityControlR = qualityControl.buildResource(SEQUENCING_QC_ANALYSIS,serviceRequest, patient, organization, qualityControlInput, qualityControlOutput)

    Seq(sequencingAlignmentR, variantCallR, qualityControlR)

  }

}
