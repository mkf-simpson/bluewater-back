package bluewater.config

case class Configuration(
  http: HttpConfiguration,
  hydroServing: HydroServingConfiguration,
  slack: SlackConfiguration
)
