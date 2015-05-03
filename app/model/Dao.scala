package model

/**
 * Created by vipmax on 4/29/15.
 */


import akka.actor.ActorSystem
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.querybuilder.QueryBuilder.select
import model.domain._
import org.joda.time.{DateTimeZone, DateTime}
import play.api.Logger
import play.api.libs.json.{JsString, JsNumber, Json}
import scala.collection.JavaConversions._
import scala.concurrent.duration._


class Dao(node: String) {

  val cluster = Cluster.builder().addContactPoint(node).build()
  val session = cluster.connect("monitoring")

  private var projects = getProjects()
  private var instances = getInstances()
  private var parameters = getParameters()

  scheduleUpdateMetadata()

  def scheduleUpdateMetadata() {
    val actorSystem = ActorSystem()
    val scheduler = actorSystem.scheduler
    implicit val executor = actorSystem.dispatcher

    class UpdateMetaDataTask extends Runnable{

      override def run() {
        val actualProjects =   getProjects()
        val actualInstances =  getInstances()
        val actualParameters = getParameters()

        val newProjects =   projects.diff(actualProjects)
        val newInstances =  instances.diff(actualInstances)
        val newParameters = parameters.diff(actualParameters)


        if (newProjects.nonEmpty)   Logger.debug("Found new projects = " + newProjects)
        if (newInstances.nonEmpty)  Logger.debug("Found new instances = " + newInstances)
        if (newParameters.nonEmpty) Logger.debug("Found new parameters = " + newParameters)

        projects =   actualProjects
        instances =  actualInstances
        parameters = actualParameters
      }
    }

    scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 5 minute,
      runnable = new UpdateMetaDataTask
    )

  }

  def getLastRawData(instanceId: Int, parameterId: Int) = {

    val timeSince = DateTime.now.withZone(DateTimeZone.forOffsetHours(3)).minusHours(1).withSecondOfMinute(0).withMillisOfSecond(0).toString()
    val defaultTimePeriod = "1m"
    val query = ("select time, value from raw_data " +
      "where instance_id = %d and parameter_id = %d and time_period = '%s' and time >= '%s'")
      .format(instanceId, parameterId, defaultTimePeriod, timeSince)

    println("query = " + query)

    val lastRawData = session.execute(query).map(row => (new DateTime(row.getDate("time")), row.getDouble("value")))

    lastRawData
  }
  def getLastAggregatedData(projectId: Int,instanceId: Int, parameterId: Int, timePeriod: String) = {

    val now = DateTime.now.withZone(DateTimeZone.forOffsetHours(4)).withSecondOfMinute(0).withMillisOfSecond(0)
    val timeSince = timePeriod match {
      case "1h" => now.withMinuteOfHour(0).toString()
      case "1d" => now.withHourOfDay(0).getMillis
      case "1w" => now.withDayOfWeek(0).getMillis
      case "1m" => now.withDayOfMonth(0).getMillis
      case "1y" => now.withDayOfYear(0).getMillis
      case _ =>    now.withMinuteOfHour(0).getMillis
    }


    val query = ("select time, max_value, min_value, avg_value, sum_value " +
      "from aggregated_data" +
      " where project_id = %d and instance_id = %d and parameter_id = %d and time_period = '%s' and time >= '%s'").
      format(projectId, instanceId, parameterId, timePeriod, timeSince)

    session.execute(query).
      map(row => (row.getDate("time"), row.getDouble("max_value"), row.getDouble("min_value"), row.getDouble("avg_value"), row.getDouble("sum_value")))


  }


  def getProjects()   = getRows("projects").  map(r => Project(r.getInt("project_id"),     r.getString("name"), r.getSet("instances",  classOf[Integer]).toSet)).toSet
  def getInstances()  = getRows("instances"). map(r => Instance(r.getInt("instance_id"),   r.getString("name"), r.getSet("parameters", classOf[Integer]).toSet)).toSet
  def getParameters() = getRows("parameters").map(r => Parameter(r.getInt("parameter_id"), r.getString("name"), r.getString("unit"),   r.getDouble("min_value"), r.getDouble("max_value"))).toSet
  def getRows(table:String) = session.execute(select().all().from(table))
  def getInstances(projectId: Int) = projects.find(_.projectId.equals(projectId)).get.instances
  def getParameter(parameterId: Int) = parameters.find(_.parameterId.equals(parameterId)).getOrElse(Parameter(-1,"","",0,0))

  def getMenuInfo = {

    val menuData = MenuData()
    var menuJson = Json.obj()

    var projectMs = Set[ProjectM]()
    var projectsJ = Json.arr()

    projects.foreach(pr=> {

      var projectM = ProjectM()
      var projectJ = Json.obj()

      projectM.id = pr.projectId
      projectJ += ("projectId", JsNumber(pr.projectId.toInt))

      projectM.name = pr.name
      projectJ += ("projectName", JsString(pr.name))


      val instancesOnProject = instances.filter(i => pr.instances.contains(i.instanceId))

      var instanceMs = Set[InstanceM]()
      var instancesJ = Json.arr()


      instancesOnProject.foreach(i => {

        var instanceM = InstanceM()
        var instanceJ = Json.obj()

        instanceM.id = i.instanceId
        instanceJ += ("instanceId", JsNumber(i.instanceId.toInt))
        instanceM.name = i.name
        instanceJ += ("instanceName", JsString(i.name))


        val parametersOnInstance = parameters.filter(p => i.parameters.contains(p.parameterId))
        instanceM.parameters = parametersOnInstance
        instanceJ += ("parameters", Json.arr(parameters.map(p=>
          Json.obj(
            "parameterId" -> JsNumber(p.parameterId.toInt),
            "parameterName" -> JsString(p.name),
            "unit" -> JsString(p.unit),
            "maxValue" -> JsNumber(p.max_Value),
            "minValue" -> JsNumber(p.min_Value)
            )
        )))

        instanceMs += instanceM
        instancesJ +:= instanceJ
      })

      projectM.instances = instanceMs
      projectJ += ("instances", instancesJ)

      projectMs += projectM
      projectsJ +:= projectJ
    })

    menuData.projects = projectMs
    menuJson += ("projects",projectsJ)

    println("menuJson = " + menuJson)
    menuJson
  }


  def close() {
    session.close
    cluster.close
  }


}




object DaoTest extends App {
  val dao = new Dao("127.0.0.1")
  var info = dao.getMenuInfo
  println("info = " + info)


  dao.close
}