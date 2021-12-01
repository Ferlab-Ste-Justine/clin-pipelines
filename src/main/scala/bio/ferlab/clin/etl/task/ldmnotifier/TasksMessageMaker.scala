package bio.ferlab.clin.etl.task.ldmnotifier

import play.api.libs.mailer.AttachmentFile
import java.io.{File, PrintWriter}

object TasksMessageMaker {

  def extractFileNameFromAttachmentUrl(attachmentUrl: String): String = {
    val fileId = attachmentUrl.split("/").last
    fileId
  }

  def extractFileIdAndFileExtension(fileId: String): (String, String) = {
    val fileIdAndExtension = fileId.split("\\.")
    (fileIdAndExtension.head, fileIdAndExtension.last)
  }

  def createMetaDataAttachmentFile(urls: Seq[String]): AttachmentFile = {
    val file = new File("manifest.tsv")

    val pW = new PrintWriter(file);
    pW.write("file_id\tfile_type\n");
    urls.foreach(url => {
      val fileName = extractFileNameFromAttachmentUrl(url)
      val (fileId, fileType) = extractFileIdAndFileExtension(fileName)
      pW.write(s"${fileId}\t${fileType}\n")
    })
    pW.close()

    AttachmentFile("manifest.tsv", file)
  }

  def createMsgBody(urls: Seq[String]): String = {
    val resourcePath = getClass.getResource("/ldmEmailPreambleText.txt")
    val source = scala.io.Source.fromFile(resourcePath.getPath)
    val preamble = try source.mkString finally source.close()
    val specific = s"${urls.mkString("\n")}"
    s"$preamble \n $specific"
  }
}
