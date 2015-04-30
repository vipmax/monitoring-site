package controllers

import model.Dao
import play.api.libs.json.Json.obj
import play.api.mvc.{Action, _}
import play.api.libs.json._
import scala.util.Random

class Application(dao: Dao) extends Controller {


  def index () = Action {
    val menuInfo = dao.getInfo
//    val project = project(("projectName", JsString("")), ("id", JsString("")), ("instances", project()))
    //    val json = obj(("projects",Json.arr(obj(())menuInfo.map(mi => JsString(mi.project.name))))).toString()
//    val json = project(("projects",Json.arr(project))).toString()


    Ok(views.html.main("Monitoring", ""))
  }


  def aggregatedGraphic(projectId: Int,instanceId: Int, parameterId: Int, timePeriod: String) = Action {
    println("projectId = " + projectId)
    println("instanceId = " + instanceId)
    println("parameterId = " + parameterId)
    println("timePeriod = " + timePeriod)

    val header = projectId + " " + instanceId + " " + parameterId + " " + timePeriod
    val lastData = dao.getLastAggregatedData(projectId, instanceId, parameterId, timePeriod)
    
    println("lastData = " + lastData)

    val json = obj("x" -> JsArray(1.to(10).map(JsNumber(_))), "y" -> JsArray(1.to(10).map(value => JsString(Random.nextInt(10).toString)))).toString()

    Ok(views.html.graph("Graph for "+header,json))
  }

  def rowGraphic(instanceId: Int, parameterId: Int) = Action {
    println("instanceId = " + instanceId)
    println("parameterId = " + parameterId)

    val header = + instanceId + " " + parameterId

    val lastRawData = dao.getLastRawData(instanceId,parameterId).toSeq

    val json = obj("x" -> JsArray(lastRawData.map(value => JsString(value._1.toString("yyyy.MM.dd  HH:mm")))),
      "y" -> JsArray(lastRawData.map(value => JsNumber(value._2)))).toString()


    Ok(views.html.graph("Graphic for " + header,json))
  }
}
