package model.domain

import org.joda.time.DateTime

/**
 * Created by i00 on 4/12/15.
 */

case class AggregatedData (projectId: Int, instanceId: Int, parameterId: Int, time: DateTime, timePeriod: String,
                           max_Value: Double, min_Value: Double, avg_Value: Double, sum_Value: Double)

case class RawData (instanceId: Int, parameterId: Int, time: DateTime, timePeriod: String, value: Double)
case class Project (projectId: Int, name: String, instances: Set[Int])
case class Instance (instanceId: Int, name: String, parameters: Set[Int])
case class Parameter (parameterId: Int, name: String, unit: String, max_Value: Double, min_Value: Double)