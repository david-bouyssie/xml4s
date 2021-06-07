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
import java.util.Properties

/**
 * Common trait for {@link FactoryFinder#CacheHolder} and {@link SchemaFactoryFinder#CacheHolder}
 */
private[xml] trait FactoryCacheHolder {

  def debug: Boolean
  def debugPrintln(msg: String): Unit

  protected def readProperties(): Properties = {
    val javah = System.getProperty("java.home")
    val configFile = javah + File.separator + "lib" + File.separator + "jaxp.properties"

    val properties = if (debug)
      FactoryNameDiscovery.readProperties(configFile, Some(debugPrintln))
    else
      FactoryNameDiscovery.readProperties(configFile, None)

    properties
  }


}
