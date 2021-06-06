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
// $Id: DatatypeConfigurationException.java 569987 2007-08-27 04:08:46Z mrglavas $
package javax.xml.datatype

import java.io.{PrintStream, PrintWriter}

/**
 * <p>Indicates a serious configuration error.</p>
 *
 * @author <a href="mailto:Jeff.Suttor@Sun.com">Jeff Suttor</a>
 * @version $Revision: 569987 $, $Date: 2007-08-26 21:08:46 -0700 (Sun, 26 Aug 2007) $
 * @since 1.5
 *
 * <p>Create a new <code>DatatypeConfigurationException</code> with
 * the specified detail message.</p>
 *
 * @param message The detail message.
 */
@SerialVersionUID(-1699373159027047238L)
class DatatypeConfigurationException(private val message: String) extends Exception(message) {

  /** This field is required to store the cause on JDK 1.3 and below. */
  private var causeOnJDK13OrBelow: Throwable = _

  /** Indicates whether this class is being used in a JDK 1.4 context. */
  private var isJDK14OrAbove = false

  /**
   * <p>Create a new <code>DatatypeConfigurationException</code> with
   * no specified detail message and cause.</p>
   */
  def this() = {
    this(null: String)
  }

  /**
   * <p>Create a new <code>DatatypeConfigurationException</code> with
   * the specified detail message and cause.</p>
   *
   * @param message The detail message.
   * @param cause   The cause.  A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  def this(message: String, cause: Throwable) = {
    this(message)
    initCauseByReflection(cause)
  }

  /**
   * <p>Create a new <code>DatatypeConfigurationException</code> with
   * the specified cause.</p>
   *
   * @param cause The cause.  A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  def this(cause: Throwable) = {
    this(if (cause == null) null else cause.toString)
    initCauseByReflection(cause)
  }

  /**
   * Print the the trace of methods from where the error
   * originated.  This will trace all nested exception
   * objects, as well as this object.
   */
  override def printStackTrace(): Unit = {
    if (!isJDK14OrAbove && causeOnJDK13OrBelow != null) printStackTrace0(new PrintWriter(System.err, true))
    else super.printStackTrace()
  }

  /**
   * Print the the trace of methods from where the error
   * originated.  This will trace all nested exception
   * objects, as well as this object.
   *
   * @param s The stream where the dump will be sent to.
   */
  override def printStackTrace(s: PrintStream): Unit = {
    if (!isJDK14OrAbove && causeOnJDK13OrBelow != null) printStackTrace0(new PrintWriter(s))
    else super.printStackTrace(s)
  }

  /**
   * Print the the trace of methods from where the error
   * originated.  This will trace all nested exception
   * objects, as well as this object.
   *
   * @param s The writer where the dump will be sent to.
   */
  override def printStackTrace(s: PrintWriter): Unit = {
    if (!isJDK14OrAbove && causeOnJDK13OrBelow != null) printStackTrace0(s)
    else super.printStackTrace(s)
  }

  private def printStackTrace0(s: PrintWriter): Unit = {
    causeOnJDK13OrBelow.printStackTrace(s)
    s.println("------------------------------------------")
    super.printStackTrace(s)
  }

  private def initCauseByReflection(cause: Throwable): Unit = {
    causeOnJDK13OrBelow = cause
    try {
      val m = this.getClass.getMethod("initCause", classOf[Array[Throwable]])
      m.invoke(this, Array[AnyRef](cause))
      isJDK14OrAbove = true
    } catch {
      case e: Exception => // Ignore exception
    }
  }

  // TODO: unsused, delete me
  /*
  @throws[IOException]
  @throws[ClassNotFoundException]
  private def readObject(in: ObjectInputStream) = {
    in.defaultReadObject()
    try {
      val m1 = this.getClass.getMethod("getCause", new Array[Class[_]])
      val cause = m1.invoke(this, new Array[AnyRef]).asInstanceOf[Throwable]
      if (causeOnJDK13OrBelow == null) causeOnJDK13OrBelow = cause
      else if (cause == null) {
        val m2 = this.getClass.getMethod("initCause", Array[Class[_]](classOf[Throwable]))
        m2.invoke(this, Array[AnyRef](causeOnJDK13OrBelow))
      }
      isJDK14OrAbove = true
    } catch {
      case e: Exception =>
    }
  }*/
}