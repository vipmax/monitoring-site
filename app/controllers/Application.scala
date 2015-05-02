package controllers

import model.domain.Parameter
import model.{ParameterWithData, Dao}
import play.api.libs.json.Json.obj
import play.api.mvc.{Action, _}
import play.api.libs.json._
import scala.collection.mutable
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

    Ok("")
  }

  def rowGraphic(projectId: Int,instanceId: Int) = Action {
    println("projectId = " + projectId)
    println("instanceId = " + instanceId)

    val header = " " + dao.getInstances.filter(i => i.instanceId.equals(instanceId)).head.name


    val parameterIdsOnInstance: Set[Integer] = dao.getInstances.filter(i => i.instanceId.equals(instanceId)).head.parameters

    val parametersDataList = mutable.MutableList[ParameterWithData]()

    for (parameterId <- parameterIdsOnInstance) {
      val parameter = dao.getParameter(parameterId)
      val data = dao.getLastRawData(instanceId, parameterId).toList
      val jsonData = obj("x" -> JsArray(data.map(value => JsString(value._1.toString("yyyy.MM.dd  HH:mm")))), "y" -> JsArray(data.map(value => JsNumber(value._2)))).toString()

      parametersDataList += ParameterWithData(parameter, jsonData)
    }

    println("parametersDataList = " + parametersDataList.foreach(println))

    Ok(views.html.graph(header, dao.getMenuInfo.toString(),parametersDataList))
  }




}
