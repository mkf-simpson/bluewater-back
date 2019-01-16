package bluewater.services

import bluewater.Logging
import bluewater.config.Configuration
import bluewater.terms.Intent
import cats.effect._
import cats.implicits._
import io.circe.generic.auto._
import io.grpc.{ClientInterceptors, ManagedChannelBuilder}
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, Headers}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.api.model.ModelSpec
import io.hydrosphere.serving.tensorflow.api.predict.PredictRequest
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.tensorflow.tensor.StringTensor
import javax.net.ssl.SSLContext
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.impl.EntityRequestGenerator
import org.http4s.{Method, Uri}

import scala.concurrent.duration._

case class NLPResult(intent: Intent, confidence: Double)

trait NLPService[F[_]] {
  def getIntent(msg: String): F[NLPResult]
}

class NLPServiceHydroServingGrpcInterpreter[F[_] : Async](config: Configuration) extends NLPService[F] {

  private lazy val grpcChannel = {
    val deadline = 2 minutes
    val builder = ManagedChannelBuilder.forAddress(config.hydroServing.host, 443)
    builder.enableRetry()
    builder.usePlaintext()
    builder.keepAliveTimeout(deadline.length, deadline.unit)
    ClientInterceptors.intercept(builder.build(), new AuthorityReplacerInterceptor +: Headers.interceptors: _*)
  }

  private lazy val predictionClient = PredictionServiceGrpc.blockingStub(grpcChannel)


  override def getIntent(msg: String): F[NLPResult] = Async[F].delay {
    val response = predictionClient
      .withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, "gateway")
      .predict(PredictRequest(
        modelSpec = Some(ModelSpec(config.hydroServing.appName, signatureName = config.hydroServing.appSignature)),
        inputs = Map(
          "msg" -> StringTensor(TensorShape.vector(1), Seq(msg)).toProto
        )
      ))
    val maybeResult = for {
      intents <- response.outputs.get("intent")
      confidences <- response.outputs.get("confidence")
      intent <- intents.stringVal.headOption
      confidence <- confidences.doubleVal.headOption
    } yield NLPResult(Intent.withName(intent.toString), confidence)
    maybeResult.getOrElse(NLPResult(Intent.Fail, 100d))
  }
}

class NLPServiceHydroServingHttpInterpreter[F[_]: ConcurrentEffect](config: Configuration, sslContext: SSLContext)(implicit cs: ContextShift[F]) extends NLPService[F] with Logging {
  import org.http4s.circe.CirceEntityEncoder._

  case class HydroServingInput(msg: Seq[String])
  case class HydroServingOutput(intents: Seq[Intent], confidences: Seq[Double], input_length: Seq[Int])

  val uriStr = s"https://${config.hydroServing.host}/gateway/applications/${config.hydroServing.appName}/${config.hydroServing.appSignature}"
  
  // POST.apply returns IO[Request[IO]] instead of F[Request[F]]
  private val PostGenerator = new EntityRequestGenerator[F] {
    override def method: Method = Method.POST
  }

  override def getIntent(msg: String): F[NLPResult] = BlazeClientBuilder[F](scala.concurrent.ExecutionContext.global, Some(sslContext)).resource.use { client =>
    for {
      uri <- Effect[F].fromEither(Uri.fromString(uriStr))
      req <- PostGenerator.apply(HydroServingInput(Seq(msg)), uri)
      output <- client.expect(req)(jsonOf[F, HydroServingOutput])
      result <- Effect[F].fromOption(for {
        intent <- output.intents.headOption
        confidence <- output.confidences.headOption
      } yield NLPResult(intent, confidence), new Exception(s"Empty intents or confidence from HS: $output"))
    } yield result
  }
}
