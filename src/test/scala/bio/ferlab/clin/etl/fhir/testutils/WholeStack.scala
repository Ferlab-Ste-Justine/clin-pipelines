package bio.ferlab.clin.etl.fhir.testutils

import bio.ferlab.clin.etl.conf.FerloadConf

trait WholeStack extends MinioServer with FhirServer

trait WholeStackSuite extends MinioServer with FhirServerSuite{

}

object StartWholeStack extends App with MinioServer with FhirServer {
  println("Whole stack is started")
  while (true) {

  }
}




