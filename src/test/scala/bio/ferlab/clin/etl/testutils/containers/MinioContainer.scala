package bio.ferlab.clin.etl.testutils.containers

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import java.time.Duration

case object MinioContainer extends OurContainer {
  val name = "clin-pipeline-minio-test"
  val port = 9000
  val accessKey = "accesskey"
  val secretKey = "secretkey"
  val container: GenericContainer = GenericContainer(
    "minio/minio",
    command = Seq("server", "/data"),
    //waitStrategy = Wait.forHttp("/").withStartupTimeout(Duration.ofSeconds(60)),
    exposedPorts = Seq(port),
    labels = Map("name" -> name),
    env = Map("MINIO_ACCESS_KEY" -> accessKey, "MINIO_SECRET_KEY" -> secretKey)
  )
}
