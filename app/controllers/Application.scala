package controllers

import model.domain.{Instance, Project, Parameter}
import model.{ParameterWithData, Dao}
import play.api.libs.json.Json.obj
import play.api.mvc.{Action, _}
import play.api.libs.json._
import scala.collection.mutable
import scala.util.Random
import play.api.Logger

class Application(dao: Dao) extends Controller {


  def index () = Action {
    val projectsAndInstances = dao.getProjectsAndInstances()
    Logger.debug("projects and instances = " + projectsAndInstances)

    Ok(views.html.main("Monitoring", projectsAndInstances))
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

    Ok(views.html.testTemplates("Hi", 1 to 5 toList))
  }

  def rowGraphic(projectId: Int,instanceId: Int) = Action {
    println("projectId = " + projectId)
    println("instanceId = " + instanceId)

    val header = " " + dao.getInstancesFromDb.filter(i => i.instanceId.equals(instanceId)).head.name


    val parameterIdsOnInstance = dao.getInstancesFromDb.filter(i => i.instanceId.equals(instanceId)).head.parameters

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

  def getParameters(instanceId: Int)= Action{
    val instanceParameters = dao.getInstanceParameters(instanceId)
    Logger.debug(s"instance id $instanceId have ${instanceParameters.size} parameters = " + instanceParameters)

    Ok(views.html.paramerters(dao.getInstance(instanceId),instanceParameters))
  }


  def getData(instanceId: Int, parameterId: Int) = Action{

    val data = dao.getLastRawData(instanceId,parameterId).toList
    val jsonData = obj("x" -> JsArray(data.map(value => JsString(value._1.toString("yyyy.MM.dd  HH:mm")))), "y" -> JsArray(data.map(value => JsNumber(value._2)))).toString()
    Logger.debug("sending json data = " + jsonData)
    Ok(jsonData)
  }

}
