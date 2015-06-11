package model

/**
 * Created by vipmax on 4/29/15.
 */



import akka.actor.ActorSystem
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.querybuilder.QueryBuilder.select
import model.domain._
import org.joda.time.{DateTimeZone, DateTime}
import play.api.libs.json.{JsString, JsNumber, Json}
import scala.collection.JavaConversions._
import scala.concurrent.duration._


class Dao(node: String) {


  val cluster = Cluster.builder().addContactPoint(node).build()
  val session = cluster.connect("monitoring")

  private var projects = getProjectsFromDb()
  private var instances = getInstancesFromDb()
  private var parameters = getParametersFromDb()

  scheduleUpdateMetadata()

  def scheduleUpdateMetadata() {
    val actorSystem = ActorSystem()
    val scheduler = actorSystem.scheduler
    implicit val executor = actorSystem.dispatcher

    class UpdateMetaDataTask extends Runnable{

      override def run() {

        val (actualProjects,actualInstances,actualParameters) = {
          try {
            (getProjectsFromDb(),
              getInstancesFromDb(),
              getParametersFromDb())
          }
          catch {
            case e: com.datastax.driver.core.exceptions.NoHostAvailableException =>(Set[Project](),Set[Instance](),Set[Parameter]())
          }
        }


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

  def getLastRawData(instanceId: Int, parameterId: Int, sinceDateTime: DateTime, untilTime: DateTime) = {

    val (startTime,endTime) = {
      if(sinceDateTime.equals(new DateTime(0)))
        (DateTime.now.withZone(DateTimeZone.forOffsetHours(3)).minusHours(1).withSecondOfMinute(0).withMillisOfSecond(0).toString(), DateTime.now)
      else (sinceDateTime,untilTime)
    }
    val defaultTimePeriod = "1m"
    val query = s"select time, value from raw_data where instance_id = $instanceId and parameter_id = $parameterId and time_period = '$defaultTimePeriod' and time >= '$startTime' and time <= '$endTime'"

    println("query = " + query)

    val lastRawData = session.execute(query).map(row => (new DateTime(row.getDate("time")).toString("yyyy-MM-dd HH:mm"), row.getDouble("value")))

    lastRawData.toList.sortBy(_._1)
  }
  def getLastAggregatedData(instanceId: Int, parameterId: Int, timePeriod: String, sinceDateTime: DateTime,untilTime: DateTime, valueType: String) = {
    val projectId = getProjectId(instanceId)


    val timeSince = {
      val now = DateTime.now.withZone(DateTimeZone.forOffsetHours(4)).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)

      if(sinceDateTime.equals(new DateTime(0)))
        timePeriod match {
        case "1h" => now.minusDays(1).toString()
        case "1d" => now.withDayOfWeek(1).withHourOfDay(0).toString()
        case "1w" => now.withDayOfMonth(1).withHourOfDay(0).toString()
        case "1M" => now.withDayOfMonth(1).withHourOfDay(0).toString()
        case "1Y" => now.withDayOfYear(1).withHourOfDay(0).toString()
        case _ => now.withHourOfDay(0).withMinuteOfHour(0).toString()
      }
      else {
        sinceDateTime
      }
    }

    val query = s"select time, ${valueType}_value from aggregated_data where project_id = $projectId and instance_id = $instanceId and parameter_id = $parameterId and time_period = '$timePeriod' and time >= '$timeSince' and time <= '$untilTime'"
    println("query = " + query)
    val data = session.execute(query).map(row =>(new DateTime(row.getDate("time")).toString("yyyy-MM-dd HH:mm"),  row.getDouble(s"${valueType}_value")))
    data.toList.sortBy(_._1)
  }


  def getProjectId(instanceId: Int) = projects.find(_.instances.contains(instanceId)).get.projectId
  def getProjectsAndInstances() =   projects.map(p => (p, getProjectInstances(p.projectId)))

  def getProjectsFromDb()   = getRowsFromDb("projects").  map(r =>  Project(r.getInt("project_id"),    r.getString("name"), asScalaSet(r.getSet("instances",  classOf[Integer]).map(p=> p.intValue())).toSet)).toSet
  def getInstancesFromDb()  = getRowsFromDb("instances"). map(r => Instance(r.getInt("instance_id"),   r.getString("name"), asScalaSet(r.getSet("parameters", classOf[Integer]).map(p=> p.intValue())).toSet)).toSet
  def getParametersFromDb() = getRowsFromDb("parameters").map(r => Parameter(r.getInt("parameter_id"), r.getString("name"), r.getString("unit"),   r.getDouble("min_value"), r.getDouble("max_value"))).toSet
  def getRowsFromDb(table:String) = session.execute(select().all().from(table))

  def getInstanceParameters(instanceId: Int) = parameters.filter(p => getInstance(instanceId).parameters.contains(p.parameterId))
  def getProjectInstances(projectId: Int) = instances.filter(i =>  getProject(projectId).instances.contains(i.instanceId))

  def getParameter(parameterId: Int) = parameters.find(_.parameterId.equals(parameterId)).getOrElse(Parameter(-1,"","",0,0))
  def getInstance(instanceId: Int) = instances.find(_.instanceId.equals(instanceId)).getOrElse(Instance(-1,"",Set()))
  def getProject(projectId: Int) = projects.find(_.projectId.equals(projectId)).getOrElse(Project(-1,"",Set()))



  def close() {
    session.close
    cluster.close
  }

}
