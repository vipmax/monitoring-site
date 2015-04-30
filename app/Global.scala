import model.Dao
import play.api.GlobalSettings

/**
 * Created by vipmax on 4/29/15.
 */
object Global extends GlobalSettings {

  private var dao: Dao = _

  private var controller: controllers.Application = _

  override def onStart(app: play.api.Application) {
    println("onStart")
    val cassandraNode = app.configuration.getString("cassandra.node").getOrElse(throw new scala.IllegalArgumentException("No 'cassandra.node' config found."))
    dao = new Dao(cassandraNode)
    controller = new controllers.Application(dao)
  }

  override def getControllerInstance[A](clazz: Class[A]): A = {
    // as simple as possible, nothing else needed for now...
    if(clazz == classOf[controllers.Application])
      controller.asInstanceOf[A]
    else
      throw new IllegalArgumentException(s"Controller of class $clazz not yet supported")
  }
  override def onStop(app: play.api.Application) {
    dao.close
  }
}
