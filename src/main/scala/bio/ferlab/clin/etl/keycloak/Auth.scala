package bio.ferlab.clin.etl.keycloak

import bio.ferlab.clin.etl.task.KeycloakConf
import org.keycloak.authorization.client.{AuthzClient, Configuration}
import org.keycloak.common.util.Time
import org.keycloak.representations.idm.authorization.AuthorizationRequest

import scala.collection.JavaConverters._

class Auth(conf: KeycloakConf) {

  private val config = new Configuration()
  config.setRealm(conf.realm)
  config.setAuthServerUrl(conf.url)
  config.setResource(conf.clientKey)
  config.setCredentials(Map("secret" -> conf.clientSecret).toMap[String, Object].asJava)
  private val authzClient = AuthzClient.create(config)

  private val req = new AuthorizationRequest()
  req.setAudience(conf.audience)

  private var expiresAt = 0L
  private var token = ""

  def withToken[T](f: String => T): T = {
    if (expiresAt == 0 || expiresAt > Time.currentTime()) {
      val resp = authzClient.authorization().authorize(req)
      expiresAt = Time.currentTime + resp.getExpiresIn - 5
      token = resp.getToken
    }
    f(token)
  }


}