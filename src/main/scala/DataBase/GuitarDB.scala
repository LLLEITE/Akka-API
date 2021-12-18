package DataBase

import DataBase.GuitarDB._
import akka.actor.{Actor, ActorLogging}
import spray.json._

case class Guitar(make: String, model: String, quantity: Int = 0)

object GuitarDB {

  case class CreateGuitar(guitar: Guitar)
  case class GuitarCreated(id: Int)
  case class FindGuitar(id: Int)
  case object FindAllGuitars
  case class AddQuantity(id: Int, quantity: Int)
  case class FindGuitarsInStock(inStock: Boolean)
}

// Ator que irá responder ás requisições da API
class GuitarDB extends Actor with ActorLogging {

  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarId: Int = 0

  // Comportamento do ator
  override def receive: Receive = {
    case FindAllGuitars =>
      sender() ! guitars.values.toList

    case FindGuitar(id) =>
      sender() ! guitars.get(id)

    case CreateGuitar(guitar) =>
      guitars = guitars + (currentGuitarId -> guitar)
      sender() ! GuitarCreated(currentGuitarId)
      currentGuitarId += 1

    case AddQuantity(id, quantity) =>
      val guitar: Option[Guitar] = guitars.get(id)
      val newGuitar: Option[Guitar] = guitar.map {
        case Guitar(make, model, q) => Guitar(make, model, q + quantity)
      }

      newGuitar.foreach(guitar => guitars = guitars + (id -> guitar))

      sender() ! newGuitar

    // Encontra as guitarras que estão ou não em estock (true ou false)
    case FindGuitarsInStock(inStock) =>
      if (inStock) {
        // Filtra os elementos do array conforme a expresão booleana
        sender() ! guitars.values.filter(_.quantity > 0)
      } else
        sender() ! guitars.values.filter(_.quantity == 0)
  }
}

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat = jsonFormat3(Guitar)
}
