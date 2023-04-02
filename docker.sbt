import com.typesafe.sbt.packager.docker.DockerChmodType

enablePlugins(JavaServerAppPackaging)
enablePlugins(DockerPlugin)

Docker / maintainer := organization.toString
Docker / packageName := packageName.value
Docker / defaultLinuxInstallLocation := "/opt/docker"

dockerBaseImage := "openjdk:8"
dockerExposedPorts := Seq(8080)
dockerExposedVolumes := Seq("/opt/docker/logs")

dockerUpdateLatest := true

dockerChmodType := DockerChmodType.UserGroupWriteExecute

Docker / dockerRepository := {
  sys.props.get("publish.env") match {
    case Some("production") =>
      for {
        url <- sys.env.get("GCLOUD_URL_PRODUCTION")
        name <- sys.env.get("GCLOUD_PROJECT_PRODUCTION")
      } yield s"$url/$name"
    case Some("staging") =>
      for {
        url <- sys.env.get("GCLOUD_URL_STAGING")
        name <- sys.env.get("GCLOUD_PROJECT_STAGING")
      } yield s"$url/$name"
    case Some("dev") | _ => Some("gcr.io/fitcentive-dev-02")
  }
}
