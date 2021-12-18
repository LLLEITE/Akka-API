import DataBase.{Guitar, GuitarDB, GuitarStoreJsonProtocol}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._

object API extends App with GuitarStoreJsonProtocol {

  implicit val system = ActorSystem("API")
  implicit val materializer = ActorMaterializer()

  import DataBase.GuitarDB._
  import system.dispatcher

  // guitarDB is the actor that sends a message
  val guitarDB = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")

  val guitarList = List (
    Guitar("Fender", "Stratocaster"),
    Guitar("Fender", "Les Paul"),
    Guitar("Fender", "LX1")
  )

  guitarList.foreach{
    guitar => guitarDB ! CreateGuitar(guitar)
  }

  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  implicit val timeout = Timeout(2 seconds)

  val apiRouter =
    (pathPrefix("api" / "guitar") & get) {
      path("inventory") {
        parameter('inStock.as[Boolean]) { inStock =>
          // ? = ask pattern that returns a Future representing a possible reply
          val entityFuture = (guitarDB ? FindGuitarsInStock(inStock)).mapTo[List[Guitar]].map(_.toJson.prettyPrint).map(toHttpEntity)
          // HttpEntity holds all the data from teh possible response message
          complete(entityFuture)
        }
      } ~
        (path(IntNumber) | parameter('id.as[Int])) { guitarId =>
          val entityFuture = (guitarDB ? FindGuitar(guitarId)).mapTo[Option[Guitar]].map(_.toJson.prettyPrint).map(toHttpEntity)
          complete(entityFuture)
        } ~
        pathEndOrSingleSlash {
          val entityFuture = (guitarDB ? FindAllGuitars).mapTo[List[Guitar]].map(_.toJson.prettyPrint).map(toHttpEntity)
          complete(entityFuture)
        }
    }

  Http().bindAndHandle(apiRouter, "localhost", 8080)
}
