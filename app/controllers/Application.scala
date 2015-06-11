package controllers

import java.text.SimpleDateFormat

import model.Dao
import org.joda.time.DateTime
import play.api.libs.json.Json.obj
import play.api.mvc.{Action, _}
import play.api.libs.json._
import play.api.Logger

class Application(dao: Dao) extends Controller {

  def index () = Action {
    val projectsAndInstances = dao.getProjectsAndInstances()
    Ok(views.html.main("Monitoring", projectsAndInstances))
  }

  def getParameters(instanceId: Int) = Action {
    val instanceParameters = dao.getInstanceParameters(instanceId)
    Ok(views.html.paramerters(dao.getInstance(instanceId),instanceParameters))
  }

  def getData(instanceId: Int, parameterId: Int, timePeriod: String, sinceTime: String, untilTime: String, valueType: String) = Action {

    val sinceDateTime = if (!sinceTime.equals("default")) new DateTime(new SimpleDateFormat("yyyy-MM-dd_HH:mm").parse(sinceTime)) else new DateTime(0)
    val untilDateTime = if (!untilTime.equals("default")) new DateTime(new SimpleDateFormat("yyyy-MM-dd_HH:mm").parse(untilTime)) else new DateTime()

    val data = if(timePeriod.equals("1m")){ dao.getLastRawData(instanceId, parameterId,sinceDateTime, untilDateTime).toList }
               else { dao.getLastAggregatedData(instanceId, parameterId, timePeriod,sinceDateTime,untilDateTime,valueType).toList }

    val jsArray = JsArray(data.map(v => obj("date" -> v._1, "value" -> v._2)))
    Logger.debug("sending json data = " + jsArray)
    Ok(jsArray)
  }

  def getProjectParameters(projectId: Int) = Action {
    val projectInstances = dao.getProjectInstances(projectId)
    val parameters = projectInstances.map(i => dao.getInstanceParameters(i.instanceId)).flatten.toSet
    Ok(views.html.projectParameters(dao.getProject(projectId),projectInstances.map(_.name),parameters))
  }

  def getProjectData(projectId: Int, parameterId: Int, timePeriod: String, sinceTime: String, untilTime: String, valueType: String) = Action {

    val sinceDateTime = if (!sinceTime.equals("default")) new DateTime(new SimpleDateFormat("yyyy-MM-dd_HH:mm").parse(sinceTime)) else new DateTime(0)
    val untilDateTime = if (!untilTime.equals("default")) new DateTime(new SimpleDateFormat("yyyy-MM-dd_HH:mm").parse(untilTime)) else new DateTime()

    val instances = dao.getProjectInstances(projectId)
    val data = instances.map(instance => {
      val data = if (timePeriod.equals("1m")) {
        dao.getLastRawData(instance.instanceId, parameterId,sinceDateTime,untilDateTime).toList
      } else {
        dao.getLastAggregatedData(instance.instanceId, parameterId, timePeriod,sinceDateTime,untilDateTime,valueType).toList
      }
      data.map(e=> (e._1,(instance.name,e._2)))
    })

    val flatten = data.flatten
    val groupBy = flatten.groupBy(e => (e._1))
    val list = groupBy.toList.sortBy(_._1)

    val jsonList = list.map(e => {
      val (date, values) = (e._1, e._2)
      val map1 = values.map(_._2)
      var jsonObj = obj("date" -> date)

      for (instance <- instances) {
        val tuple = map1.find(_._1.equals(instance.name)).getOrElse((instance.name, 0d))
        jsonObj = jsonObj + (tuple._1 -> JsNumber(tuple._2))
      }
      jsonObj
    })

    val jsArray = JsArray(jsonList)
    Logger.debug("sending json data = " + jsArray)
    Ok(jsArray)
  }
}
