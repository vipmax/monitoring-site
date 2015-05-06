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




  def getParameters(instanceId: Int)= Action{
    val instanceParameters = dao.getInstanceParameters(instanceId)
    Logger.debug(s"instance id $instanceId have ${instanceParameters.size} parameters = " + instanceParameters)

    Ok(views.html.paramerters(dao.getInstance(instanceId),instanceParameters))
  }


  def getData(instanceId: Int, parameterId: Int, timePeriod: String) = Action {


    val data = if(timePeriod.equals("1m")){dao.getLastRawData(instanceId, parameterId).toList} else { dao.getLastAggregatedData(instanceId, parameterId, timePeriod).toList}


    val jsObjects = data.map(v => obj("date" -> v._1, "value" -> v._2))
    val jsArray = JsArray(jsObjects)
    Logger.debug("sending json data = " + jsArray)
    Ok(jsArray)
  }

}
