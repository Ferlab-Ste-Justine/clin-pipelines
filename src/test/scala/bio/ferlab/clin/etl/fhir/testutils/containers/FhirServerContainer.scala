package bio.ferlab.clin.etl.fhir.testutils.containers

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import java.time.Duration

case object FhirServerContainer extends OurContainer {

  val fhirEnv: Map[String, String] = Map(
    "HAPI_FHIR_GRAPHQL_ENABLED" -> "true",
    "SPRING_DATASOURCE_URL" -> "jdbc:h2:mem:hapi",
    "SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT" -> "org.hibernate.dialect.H2Dialect",
    "SPRING_DATASOURCE_DRIVER_CLASS_NAME" -> "org.h2.Driver",
    "SPRING_DATASOURCE_USERNAME" -> "",
    "SPRING_DATASOURCE_PASSWORD" -> "",
    "BIO_AUTH_ENABLED" -> "false",
    "BIO_AUTH_AUTHORIZATION_ENABLED" -> "false",
    "BIO_AUTH_AUTHORIZATION_CLIENT_ID" -> "",
    "BIO_AUTH_AUTHORIZATION_CLIENT_SECRET" -> "",
    "BIO_AUDITS_ENABLED" -> "false",
    "BIO_ELASTICSEARCH_ENABLED" -> "false",
    "HAPI_LOGGING_INTERCEPTOR_SERVER_ENABLED" -> "false",
    "HAPI_LOGGING_INTERCEPTOR_CLIENT_ENABLED" -> "false",
    "HAPI_VALIDATE_RESPONSES_ENABLED" -> "false",
    "KEYCLOAK_ENABLED"-> "false"
  )
  val name = "clin-pipeline-fhir-test"

  val port = 8080
  val container: GenericContainer = GenericContainer(
    "hapiproject/hapi:v5.4.1",
    waitStrategy = Wait.forHttp("/").withStartupTimeout(Duration.ofSeconds(120)),
    exposedPorts = Seq(port),
    env = fhirEnv,
    labels = Map("name" -> name)
  )

}
