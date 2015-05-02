package model

import model.domain.Parameter
import org.joda.time.DateTime

/**
 * Created by vipmax on 02.05.2015.
 */
case class ParameterWithData(parameter: Parameter, dataJson: String)