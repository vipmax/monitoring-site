package model

import model.domain.{Parameter, Project}


/**
 * Created by vipmax on 4/30/15.
 */
case class MenuData(var projects: Set[ProjectM] = Set())

//case class ParameterM (var parameterId: Int = -1, var name: String = "", var unit: String = "", var max_Value: Double = 0, var min_Value: Double = 0)
case class InstanceM(var id: Int = -1, var name:String = "", var parameters: Set[Parameter] = Set())
case class ProjectM(var id: Int = -1, var name:String = "", var instances: Set[InstanceM] = Set())
