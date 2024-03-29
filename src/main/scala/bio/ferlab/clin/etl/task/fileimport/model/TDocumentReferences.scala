package bio.ferlab.clin.etl.task.fileimport.model

import bio.ferlab.clin.etl.conf.FerloadConf
import org.hl7.fhir.r4.model.{Reference, Resource}

case class TDocumentReferences(sequencingAlignment: SequencingAlignment, variantCalling: VariantCalling, copyNumberVariant: CopyNumberVariant, structuralVariant: Option[StructuralVariant], supplement: SupplementDocument,
                               exomiser: Option[Exomiser], igvTrack: Option[IgvTrack], cnvVisualization: Option[CnvVisualization], coverageByGene: Option[CoverageByGene], qcMetrics: Option[QcMetrics]) {

  def buildResources(subject: Reference, custodian: Reference, sample: Reference)(implicit ferloadConf: FerloadConf): DocumentReferencesResources = {
    val sequencingAlignmentR = sequencingAlignment.buildResource(subject, custodian, Seq(sample))
    val variantCallingR = variantCalling.buildResource(subject, custodian, Seq(sample))
    val copyNumberVariantR = copyNumberVariant.buildResource(subject, custodian, Seq(sample))
    val structuralVariantR = structuralVariant.map(_.buildResource(subject, custodian, Seq(sample))).orNull
    val supplementR = supplement.buildResource(subject, custodian, Seq(sample))
    val exomiserR = exomiser.map(_.buildResource(subject, custodian, Seq(sample))).orNull
    val igvTrackR = igvTrack.map(_.buildResource(subject, custodian, Seq(sample))).orNull
    val cnvVisualizationR = cnvVisualization.map(_.buildResource(subject, custodian, Seq(sample))).orNull
    val coverageByGeneR = coverageByGene.map(_.buildResource(subject, custodian, Seq(sample))).orNull
    val qcMetricsR = qcMetrics.map(_.buildResource(subject, custodian, Seq(sample))).orNull
    DocumentReferencesResources(sequencingAlignmentR, variantCallingR, copyNumberVariantR, structuralVariantR, supplementR,
      exomiserR, igvTrackR, cnvVisualizationR, coverageByGeneR, qcMetricsR)
  }

}

case class DocumentReferencesResources(sequencingAlignment: Resource, variantCalling: Resource, copyNumberVariant:Resource, structuralVariant:Resource, supplement: Resource,
                                       exomiser: Resource, igvTrack: Resource, cnvVisualization: Resource, coverageByGene: Resource, qcMetrics: Resource) {
  def resources() = Seq(sequencingAlignment, variantCalling, copyNumberVariant, structuralVariant, supplement, exomiser, igvTrack, cnvVisualization, coverageByGene, qcMetrics)
    .filter(_!=null)
}
