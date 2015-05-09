package controllers

import model.Dao
import play.api.libs.json.Json.obj
import play.api.mvc.{Action, _}
import play.api.libs.json._
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

    val function: ((String, Double)) => JsObject = v => obj("date" -> v._1, "value" -> v._2) + ("date" -> JsString(v._1))
    val jsArray = JsArray(data.map(v => obj("date" -> v._1, "value" -> v._2) + ("date" -> JsString(v._1))))
    Logger.debug("sending json data = " + jsArray)
    Ok(jsArray)
  }

  def getProjectParameters(projectId: Int) = Action {
    val projectInstances = dao.getProjectInstances(projectId)
    projectInstances.foreach(i => println(i + " "+dao.getInstanceParameters(i.instanceId)))
    val parameters = projectInstances.map(i => dao.getInstanceParameters(i.instanceId)).flatten.toSet
    println("parameters = " + parameters)
    Ok(views.html.projectParameters(dao.getProject(projectId),projectInstances.map(_.name),parameters))
  }

  def getProjectData(projectId: Int, parameterId: Int, timePeriod: String) = Action {
    val function: ((String, Double)) => JsObject = v => obj("date" -> JsString(v._1), "value" -> JsNumber(v._2))

    val instances = dao.getProjectInstances(projectId)
    val datas = instances.map(instance => {
      val data = if (timePeriod.equals("1m")) {
        dao.getLastRawData(instance.instanceId, parameterId).toList
      } else {
        dao.getLastAggregatedData(instance.instanceId, parameterId, timePeriod).toList
      }
      data.map(e=> (e._1,(instance.name,e._2)))
    })

    val flatten = datas.flatten
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
      println("jsonObj = " + jsonObj)
      jsonObj
      //      print("date = " + date + " val = " + values.map(_._2).mkString(", ") + "; ")
    })

    val jsArray = JsArray(jsonList)
    Logger.debug("sending json data = " + jsArray)
    Ok(jsArray)
  }
}
