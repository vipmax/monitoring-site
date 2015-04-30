package model.domain

import org.joda.time.DateTime

/**
 * Created by i00 on 4/12/15.
 */

case class AggregatedData (rojectId: Integer, instanceId: Integer, parameterId: Integer, time: DateTime, timePeriod: String,
                           max_Value: Double, min_Value: Double, avg_Value: Double, sum_Value: Double)

case class RawData (instanceId: Integer, parameterId: Integer, time: DateTime, timePeriod: String, value: Double)
case class Project (projectId: Integer, name: String, instances: Set[Integer])
case class Instance (instanceId: Integer, name: String, parameters: Set[Integer])
case class Parameter (parameterId: Integer, name: String, unit: String, max_Value: Double, min_Value: Double)