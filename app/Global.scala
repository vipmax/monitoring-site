import model.Dao
import play.api.{Logger, GlobalSettings}
import play.api.mvc.{Handler, RequestHeader}


/**
 * Created by vipmax on 4/29/15.
 */
object Global extends GlobalSettings  {

  private var dao: Dao = _

  private var controller: controllers.Application = _

  override def onStart(app: play.api.Application) {
    val cassandraNode = app.configuration.getString("cassandra.node").getOrElse(throw new scala.IllegalArgumentException("No 'cassandra.node' config found."))
    dao = new Dao(cassandraNode)
    controller = new controllers.Application(dao)
  }

  override def getControllerInstance[A](clazz: Class[A]): A =
    if(clazz == classOf[controllers.Application]) controller.asInstanceOf[A]
    else throw new IllegalArgumentException(s"Controller of class $clazz not yet supported")


  override def onStop(app: play.api.Application) { dao.close }


  override def onRouteRequest(request: RequestHeader) = {logRequest(request); super.onRouteRequest(request)}



  def logRequest(request: RequestHeader) {
    if (!request.toString().startsWith("GET /webjars") && !request.toString().startsWith("GET /assets") && !request.toString().startsWith("GET /stylesheets"))
      Logger.debug("request: " + request.toString)
  }
}


