/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// $Id: DatatypeConstants.java 446598 2006-09-15 12:55:40Z jeremias $
package javax.xml.datatype

import javax.xml
import javax.xml.namespace.QName

/**
 * <p>Utility class to contain basic Datatype values as constants.</p>
 *
 * @author <a href="mailto:Jeff.Suttor@Sun.com">Jeff Suttor</a>
 * @version $Revision: 446598 $, $Date: 2006-09-15 05:55:40 -0700 (Fri, 15 Sep 2006) $
 * @since 1.5
 */
object DatatypeConstants {
  /**
   * Value for first month of year.
   */
  val JANUARY = 1
  /**
   * Value for second month of year.
   */
  val FEBRUARY = 2
  /**
   * Value for third month of year.
   */
  val MARCH = 3
  /**
   * Value for fourth month of year.
   */
  val APRIL = 4
  /**
   * Value for fifth month of year.
   */
  val MAY = 5
  /**
   * Value for sixth month of year.
   */
  val JUNE = 6
  /**
   * Value for seventh month of year.
   */
  val JULY = 7
  /**
   * Value for eighth month of year.
   */
  val AUGUST = 8
  /**
   * Value for ninth month of year.
   */
  val SEPTEMBER = 9
  /**
   * Value for tenth month of year.
   */
  val OCTOBER = 10
  /**
   * Value for eleven month of year.
   */
  val NOVEMBER = 11
  /**
   * Value for twelve month of year.
   */
  val DECEMBER = 12
  /**
   * <p>Comparison result.</p>
   */
  val LESSER = -1
  val EQUAL = 0
  val GREATER = 1
  val INDETERMINATE = 2
  /**
   * Designation that an "int" field is not set.
   */
  val FIELD_UNDEFINED = Integer.MIN_VALUE
  /**
   * <p>A constant that represents the years field.</p>
   */
  case object YEARS extends Field { protected val str = "YEARS"; protected val id = 0}
  //val YEARS = new DatatypeConstants.Field("YEARS", 0)
  /**
   * <p>A constant that represents the months field.</p>
   */
  case object MONTHS extends Field { protected val str = "MONTHS"; protected val id = 1}
  //val MONTHS = new DatatypeConstants.Field("MONTHS", 1)
  /**
   * <p>A constant that represents the days field.</p>
   */
  case object DAYS extends Field { protected val str = "DAYS"; protected val id = 2}
  //val DAYS = new DatatypeConstants.Field("DAYS", 2)
  /**
   * <p>A constant that represents the hours field.</p>
   */
  case object HOURS extends Field { protected val str = "HOURS"; protected val id = 3}
  //val HOURS = new DatatypeConstants.Field("HOURS", 3)
  /**
   * <p>A constant that represents the minutes field.</p>
   */
  case object MINUTES extends Field { protected val str = "MINUTES"; protected val id = 4}
  //val MINUTES = new DatatypeConstants.Field("MINUTES", 4)
  /**
   * <p>A constant that represents the seconds field.</p>
   */
  case object SECONDS extends Field { protected val str = "SECONDS"; protected val id = 5}
  //val SECONDS = new DatatypeConstants.Field("SECONDS", 5)

  /**
   * Type-safe enum class that represents six fields of the {@link Duration} class.
   */
  sealed trait Field {
    /**
     * <p><code>String</code> representation of <ode>Field</code>.</p>
     */
    protected def str: String

    /**
     * <p>Unique id of the field.</p>
     *
     * <p>This value allows the {@link Duration} class to use switch statements to process fields.</p>
     */
    protected def id: Int

    /**
     * Returns a field name in English. This method
     * is intended to be used for debugging/diagnosis
     * and not for display to end-users.
     *
     * @return
     * a non-null valid String constant.
     */
    override def toString = str

    /**
     * <p>Get id of this Field.</p>
     *
     * @return Id of field.
     */
    def getId(): Int = id
  }

  /*

  /**
   * <p>Construct a <code>Field</code> with specified values.</p>
   *
   * @param str <code>String</code> representation of <code>Field</code>
   * @param id  <code>int</code> representation of <code>Field</code>
   */
  final case class Field private(

    /**
     * <p><code>String</code> representation of <ode>Field</code>.</p>
     */
    str: String,

    /**
     * <p>Unique id of the field.</p>
     *
     * <p>This value allows the {@link Duration} class to use switch
     * statements to process fields.</p>
     */
    id: Int
  ) {
    /**
     * Returns a field name in English. This method
     * is intended to be used for debugging/diagnosis
     * and not for display to end-users.
     *
     * @return
     * a non-null valid String constant.
     */
    override def toString = str

    /**
     * <p>Get id of this Field.</p>
     *
     * @return Id of field.
     */
    def getId = id
  }*/

  /**
   * <p>Fully qualified name for W3C XML Schema 1.0 datatype <code>dateTime</code>.</p>
   */
  val DATETIME = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "dateTime")
  /**
   * <p>Fully qualified name for W3C XML Schema 1.0 datatype <code>time</code>.</p>
   */
  val TIME = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "time")
  /**
   * <p>Fully qualified name for W3C XML Schema 1.0 datatype <code>date</code>.</p>
   */
  val DATE = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "date")
  /**
   * <p>Fully qualified name for W3C XML Schema 1.0 datatype <code>gYearMonth</code>.</p>
   */
  val GYEARMONTH = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "gYearMonth")
  /**
   * <p>Fully qualified name for W3C XML Schema 1.0 datatype <code>gMonthDay</code>.</p>
   */
  val GMONTHDAY = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "gMonthDay")
  /**
   * <p>Fully qualified name for W3C XML Schema 1.0 datatype <code>gYear</code>.</p>
   */
  val GYEAR = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "gYear")
  /**
   * <p>Fully qualified name for W3C XML Schema 1.0 datatype <code>gMonth</code>.</p>
   */
  val GMONTH = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "gMonth")
  /**
   * <p>Fully qualified name for W3C XML Schema 1.0 datatype <code>gDay</code>.</p>
   */
  val GDAY = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "gDay")
  /**
   * <p>Fully qualified name for W3C XML Schema datatype <code>duration</code>.</p>
   */
  val DURATION = new QName(xml.XMLConstants.W3C_XML_SCHEMA_NS_URI, "duration")
  /**
   * <p>Fully qualified name for XQuery 1.0 and XPath 2.0 datatype <code>dayTimeDuration</code>.</p>
   */
  val DURATION_DAYTIME = new QName(xml.XMLConstants.W3C_XPATH_DATATYPE_NS_URI, "dayTimeDuration")
  /**
   * <p>Fully qualified name for XQuery 1.0 and XPath 2.0 datatype <code>yearMonthDuration</code>.</p>
   */
  val DURATION_YEARMONTH = new QName(xml.XMLConstants.W3C_XPATH_DATATYPE_NS_URI, "yearMonthDuration")
  /**
   * W3C XML Schema max timezone offset is -14:00. Zone offset is in minutes.
   */
  val MAX_TIMEZONE_OFFSET = -14 * 60
  /**
   * W3C XML Schema min timezone offset is +14:00. Zone offset is in minutes.
   */
  val MIN_TIMEZONE_OFFSET = 14 * 60
}