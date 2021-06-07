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
package javax.xml

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

object FactoryNameDiscovery {

  /**
   * Perform a service class discovery by looking for a property
   * in a target properties file located in the java.home directory.
   *
   * @param path     The relative path to the desired properties file.
   * @param property The name of the required property.
   * @return The value of the named property within the properties file.  Returns
   *         null if the property doesn't exist or the properties file doesn't exist.
   */
  @throws[IOException]
  def lookupByJREPropertyFile(path: String, property: String): String = {
    val jreDirectory = System.getProperty("java.home")
    val properties = readProperties(jreDirectory + File.separator + path, logger = None)
    properties.getProperty(property)
  }

  private[xml] def readProperties(filePath: String, logger: Option[String => Unit]): Properties = {

    /*val configurationFile = new File(filePath)
    if (configurationFile.exists && configurationFile.canRead) {
      val properties = new Nothing
      var in = null
      try {
        in = new FileInputStream(configurationFile)
        properties.load(in)
        return properties.getProperty(property)
      } finally if (in != null) try in.close
      catch {
        case e: Exception =>

      }
    }
    null*/

    val tmpProps = new Properties()
    val configFile = new File(filePath)

    if (configFile.isFile && configFile.canRead) {
      logger.foreach(print => print("Read properties file " + configFile))

      val inputStream = new FileInputStream(configFile)

      try tmpProps.load(inputStream)
      catch {
        case ex: Exception =>
          if (logger.isDefined) ex.printStackTrace()
      } finally {
        if (inputStream != null) inputStream.close()
      }
    }

    tmpProps
  }
}
