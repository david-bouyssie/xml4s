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
