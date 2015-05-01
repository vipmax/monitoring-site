package controllers

import model.Dao
import play.api.libs.json.Json.obj
import play.api.mvc.{Action, _}
import play.api.libs.json._
import scala.util.Random

class Application(dao: Dao) extends Controller {


  def index () = Action {
    val menuJson = dao.getMenuInfo

    Ok(views.html.main("Monitoring", menuJson.toString()))
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

    Ok(views.html.graph("Graph for "+header,dao.getMenuInfo.toString(),json))
  }

  def rowGraphic(instanceId: Int, parameterId: Int) = Action {
    println("instanceId = " + instanceId)
    println("parameterId = " + parameterId)


    var parameter = dao.getParameters.filter(p => p.parameterId.equals(parameterId)).head
    val header = " "+ dao.getInstances.filter(i=>i.instanceId.equals(instanceId)).head.name

    val lastRawData = dao.getLastRawData(instanceId,parameterId).toSeq

    val json = obj("x" -> JsArray(lastRawData.map(value => JsString(value._1.toString("yyyy.MM.dd  HH:mm")))),
      "y" -> JsArray(lastRawData.map(value => JsNumber(value._2)))).toString()


    Ok(views.html.graph("Graphic for " + header,dao.getMenuInfo.toString(), json))
  }

  def ajaxCall = Action { implicit request =>
    Ok("Ajax Call!")
  }
}
