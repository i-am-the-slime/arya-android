package co.aryaapp.communication

import android.os.AsyncTask
import android.util.Log
import com.squareup.okhttp.{Request, RequestBody, MediaType, OkHttpClient}
import argonaut._, Argonaut._
import scalaz._, Scalaz._

import scala.concurrent.{ExecutionContext, Future}

object RestClient{
  implicit val ec = ExecutionContext.fromExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
  val ClientId = "e782c0c0-cf2c-4720-929f-bdcd314028f7"
  val ClientSecret = "b4c081b1561238cf9b5e22e9c4deb67b10d39e8ebe748e2f334c7afeba6875bb"
  val ServerBase = "http://aryaapp-staging.herokuapp.com"
  val Json = MediaType.parse("application/json; charset=utf-8")
  val client = new OkHttpClient
}

sealed abstract class APIServerError(code: Int, message:String)
case class Unauthorized(msg:String) extends APIServerError(401, msg)
case class BadRequest(msg:String) extends APIServerError(400, msg)
case class UnknownError() extends APIServerError(-1, "Unknown error occurred")

class RestClient(val accessToken:String) {
  import RestClient._

  protected def getRequest(resource:String) = request(resource, None)

  protected def postRequest(resource:String, body:RequestBody) = request(resource, Some(body))

  protected def request(resource:String, post:Option[RequestBody]) = {
    val req = new Request.Builder()
      .url(ServerBase + resource)
      .header("Authorization", "Client " + ClientId)
      .header("Authorization", "Bearer " + accessToken)
      .header("Content-Type", "application/json")
    post.foreach(body => req.post(body))
    req.build()
  }

  protected def getError(responseString:String):APIServerError = {
    val decodeResult = responseString.decode[Resp]
    val error = decodeResult.getOrElse(Resp(Errors(-1, "Unknown Error"))).errors
    error.code match {
      case 401 => Unauthorized(error.message)
      case _ => UnknownError()
    }
  }

  protected def executeRequest[A](request:Request)(implicit decode:DecodeJson[A]):Future[APIServerError \/ A] = {
    Future {
      val response = client.newCall(request).execute
      val responseString = response.body.string
      Log.e("MOTHER", responseString)
      val decoded = responseString.decodeOption[A](decode)
      if(decoded.isDefined) \/-(decoded.get)
      else -\/(getError(responseString))
    }
  }

  def wrapJson(json:String, fieldName: String) = s"""{ "$fieldName":$json }"""

  def getFromServer[A](resource:String)(implicit decode:DecodeJson[A]):Future[APIServerError \/ A] = {
    executeRequest(getRequest(resource))(decode)
  }

  def postToServer[A, B](resource:String, data:A)
                        (implicit enc:EncodeJson[A], dec:DecodeJson[B]):Future[APIServerError \/ B] = {
    val name = resource.stripSuffix("/").split("/").last.dropRight(1)
    val payload = wrapJson(data.asJson.nospaces, name)
    val body = RequestBody.create(Json, payload)
    executeRequest(postRequest(resource, body))(dec)
  }

}

case class Errors(code:Int, message:String)
object Errors{
  implicit def ErrorsDecodeJson: CodecJson[Errors] = casecodec2(Errors.apply, Errors.unapply)("code", "message")
}
case class Resp(errors:Errors)
object Resp{
  implicit def RespDecodeJson: CodecJson[Resp] = casecodec1(Resp.apply, Resp.unapply)("errors")
}
