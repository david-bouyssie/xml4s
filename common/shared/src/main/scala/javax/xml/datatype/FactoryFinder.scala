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
// $Id: FactoryFinder.java 670432 2008-06-23 02:02:08Z mrglavas $
package javax.xml.datatype

import java.io.{BufferedReader, IOException, InputStream, InputStreamReader}
import java.util.Properties

/**
 * <p>Implement pluggable data types.</p>
 *
 * <p>This class is duplicated for each JAXP subpackage so keep it in
 * sync.  It is package private for secure class loading.</p>
 *
 * @author <a href="mailto:Jeff.Suttor@Sun.com">Jeff Suttor</a>
 * @version $Revision: 670432 $, $Date: 2008-06-22 19:02:08 -0700 (Sun, 22 Jun 2008) $
 * @since 1.5
 */
object FactoryFinder {

  /** <p>Name of class to display in output messages.</p> */
  private val CLASS_NAME = "javax.xml.datatype.FactoryFinder"

  /** Default columns per line. */
  private val DEFAULT_LINE_LENGTH = 80

  /** <p>Debug flag to trace loading process.</p> */
  private val debug = {
    val debugProp = System.getProperty("jaxp.debug")
    // Allow simply setting the prop to turn on debug
    debugProp != null && debugProp != "false"
  }

  /**
   * <p>Output debugging messages.</p>
   *
   * @param msg <code>String</code> to print to <code>stderr</code>.
   */
  private def debugPrintln(msg: String): Unit = {
    if (debug) System.err.println(CLASS_NAME + ":" + msg)
  }

  private val me = this

  /**
   * <p>Cache properties for performance. Use a static class to avoid double-checked locking.</p>
   */
  private object CacheHolder extends javax.xml.FactoryCacheHolder {

    @inline
    def debugPrintln(msg: String): Unit = me.debugPrintln(msg)

    @inline
    def debug: Boolean = me.debug

    private[FactoryFinder] val cacheProps: Properties = readProperties()
  }

  /**
   * <p>Find the appropriate <code>ClassLoader</code> to use.</p>
   *
   * <p>The context ClassLoader is preferred.</p>
   *
   * @return <code>ClassLoader</code> to use.
   */
  private def findClassLoader() = {
    // Figure out which ClassLoader to use for loading the provider class.
    // If there is a Context ClassLoader then use it.
    var classLoader = Thread.currentThread.getContextClassLoader
    if (debug) debugPrintln("Using context class loader: " + classLoader)

    if (classLoader == null) { // if we have no Context ClassLoader
      // so use the current ClassLoader
      classLoader = FactoryFinder.getClass.getClassLoader

      if (debug) debugPrintln("Using the class loader of FactoryFinder: " + classLoader)
    }

    classLoader
  }

  /**
   * <p>Create an instance of a class using the specified ClassLoader.</p>
   *
   * @param className   Name of class to create.
   * @param classLoader ClassLoader to use to create named class.
   * @return New instance of specified class created using the specified ClassLoader.
   * @throws ConfigurationError If class could not be created.
   */
  @throws[ConfigurationError]
  private[datatype] def newInstance(className: String, classLoader: ClassLoader): Any = {
    try {
      val spiClass = if (classLoader == null) Class.forName(className)
      else classLoader.loadClass(className)

      if (debug) debugPrintln("Loaded " + className + " from " + which(spiClass))

      spiClass.newInstance()
    } catch {
      case x: ClassNotFoundException =>
        throw new FactoryFinder.ConfigurationError("Provider " + className + " not found", x)
      case x: Exception =>
        throw new FactoryFinder.ConfigurationError("Provider " + className + " could not be instantiated: " + x, x)
    }
  }

  /**
   * Finds the implementation Class object in the specified order.  Main
   * entry point.
   * Package private so this code can be shared.
   *
   * @param factoryId         Name of the factory to find, same as a property name
   * @param fallbackClassName Implementation class name, if nothing else is found.  Use null to mean no fallback.
   * @return Class Object of factory, never null
   * @throws ConfigurationError If Class cannot be found.
   */
  @throws[ConfigurationError]
  private[datatype] def find(factoryId: String, fallbackClassName: String): Any = {
    val classLoader = findClassLoader()

    // Use the system property first
    val systemProp = System.getProperty(factoryId)
    if (systemProp != null && systemProp.nonEmpty) {
      if (debug) debugPrintln("found " + systemProp + " in the system property " + factoryId)
      return newInstance(systemProp, classLoader)
    }

    // try to read from $java.home/lib/jaxp.properties
    try {
      val factoryClassName = CacheHolder.cacheProps.getProperty(factoryId)
      if (debug) debugPrintln("found " + factoryClassName + " in $java.home/jaxp.properties")
      if (factoryClassName != null) return newInstance(factoryClassName, classLoader)
    } catch {
      case ex: Exception =>
        if (debug) ex.printStackTrace()
    }

    // Try Jar Service Provider Mechanism
    val provider = findJarServiceProvider(factoryId)
    if (provider != null) return provider

    if (fallbackClassName == null)
      throw new FactoryFinder.ConfigurationError("Provider for " + factoryId + " cannot be found", null)

    if (debug) debugPrintln("loaded from fallback value: " + fallbackClassName)

    newInstance(fallbackClassName, classLoader)
  }

  /**
   * Try to find provider using Jar Service Provider Mechanism
   *
   * @return instance of provider class if found or null
   */
  @throws[ConfigurationError]
  // FIXME: should only work on the JVM
  private def findJarServiceProvider(factoryId: String): Any = {
    val serviceId = "META-INF/services/" + factoryId

    // First try the Context ClassLoader
    val cl = findClassLoader()
    val is: InputStream = cl.getResourceAsStream(serviceId)
    if (is == null) { // No provider found
      return null
    }

    if (debug) debugPrintln("found jar resource=" + serviceId + " using ClassLoader: " + cl)

    val rd = try {
      new BufferedReader(new InputStreamReader(is, "UTF-8"), DEFAULT_LINE_LENGTH)
    }
    catch {
      case e: java.io.UnsupportedEncodingException =>
        new BufferedReader(new InputStreamReader(is), DEFAULT_LINE_LENGTH)
    }

    val factoryClassName = try {
      // XXX Does not handle all possible input as specified by the
      // Jar Service Provider specification
      rd.readLine()
    } catch {
      case x: IOException => return null
    } finally libcore.io.IoUtils.closeQuietly(rd)

    if (factoryClassName == null || factoryClassName.nonEmpty) null
    else {
      if (debug) debugPrintln("found in resource, value=" + factoryClassName)
      newInstance(factoryClassName, cl)
    }
  }

  /**
   * <p>Configuration Error.</p>
   *
   * <p>Construct a new instance with the specified detail string and
   * exception.</p>
   *
   * @param msg       Detail message for this error.
   * @param exception Exception that caused the error.
   */
  @SerialVersionUID(-3644413026244211347L)
  private[datatype] class ConfigurationError private[datatype](
    private val msg: String,

    /**
     * <p>Exception that caused the error.</p>
     */
    private var exception: Exception
  ) extends Error(msg) {
    /**
     * <p>Get the Exception that caused the error.</p>
     *
     * @return Exception that caused the error.
     */
    private[datatype] def getException = exception
  }

  /**
   * Returns the location where the given Class is loaded from.
   */
  private def which(clazz: Class[_]): String = {
    try {
      val classnameAsResource = clazz.getName.replace('.', '/') + ".class"
      val loader = clazz.getClassLoader

      val it = if (loader != null) loader.getResource(classnameAsResource)
      else ClassLoader.getSystemResource(classnameAsResource)

      if (it != null)
        return it.toString

    } catch {
      case vme: VirtualMachineError =>
        throw vme
      case td: ThreadDeath =>
        throw td
      case t: Throwable =>
        // work defensively.
        if (debug) t.printStackTrace()
    }

    "unknown location"
  }

}