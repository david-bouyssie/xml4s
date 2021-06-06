/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)
package org.xmlpull.v1

import java.util.ArrayList
import java.util.HashMap

import scala.collection.mutable.ArrayBuffer

/**
 * This class is used to create implementations of XML Pull Parser defined in XMPULL V1 API.
 *
 * @see XmlPullParser
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 * @author Stefan Haustein
 */
object XmlPullParserFactory {

  // FIXME: try to be less obtrusive
  private val DEFAULT_PARSER_CLASS = classOf[org.kxml2.io.KXmlParser]
  private val DEFAULT_SERIALIZER_CLASS = classOf[org.kxml2.io.KXmlSerializer]

  val PROPERTY_NAME = "org.xmlpull.v1.XmlPullParserFactory"

  private def newInstantiationException(message: String, exceptions: ArrayBuffer[Exception]) = {
    if (exceptions == null || exceptions.isEmpty) new XmlPullParserException(message)
    else {
      val exception = new XmlPullParserException(message)

      exceptions.foreach { ex: Exception =>
        exception.addSuppressed(ex)
      }

      exception
    }
  }

  /**
   * Creates a new instance of a PullParserFactory that can be used
   * to create XML pull parsers. The factory will always return instances
   * of Android's built-in {@link XmlPullParser} and {@link XmlSerializer}.
   */
  @throws[XmlPullParserException]
  def newInstance(): XmlPullParserFactory = new XmlPullParserFactory

  /**
   * Creates a factory that always returns instances of Android's built-in
   * {@link XmlPullParser} and {@link XmlSerializer} implementation. This
   * <b>does not</b> support factories capable of creating arbitrary parser
   * and serializer implementations. Both arguments to this method are unused.
   */
  @throws[XmlPullParserException]
  def newInstance(unused: String, unused2: Class[_]): XmlPullParserFactory = newInstance()

}

/**
 * Protected constructor to be called by factory implementations.
 */
class XmlPullParserFactory protected() {

  protected val parserClasses = new ArrayList[Class[_]]
  protected val serializerClasses =  new ArrayList[Class[_]]
  /** Unused, but we have to keep it because it's public API. */
  protected var classNamesLocation: String = _
  // features are kept there
  // TODO: This can't be made final because it's a public API.
  protected var features = new HashMap[String, Boolean]

  try {
    //parserClasses.add(Class.forName("com.android.org.kxml2.io.KXmlParser"))
    //serializerClasses.add(Class.forName("com.android.org.kxml2.io.KXmlSerializer"))
    parserClasses.add(XmlPullParserFactory.DEFAULT_PARSER_CLASS)
    serializerClasses.add(XmlPullParserFactory.DEFAULT_SERIALIZER_CLASS)
  } catch {
    case e: ClassNotFoundException => throw new AssertionError()
  }

  /**
   * Set the features to be set when XML Pull Parser is created by this factory.
   * <p><b>NOTE:</b> factory features are not used for XML Serializer.
   *
   * @param name  string with URI identifying feature
   * @param state if true feature will be set; if false will be ignored
   */
  @throws[XmlPullParserException]
  def setFeature(name: String, state: Boolean) = features.put(name, state): Unit

  /**
   * Return the current value of the feature with given name.
   * <p><b>NOTE:</b> factory features are not used for XML Serializer.
   *
   * @param name The name of feature to be retrieved.
   * @return The value of named feature.
   *         Unknown features are <string>always</strong> returned as false
   */
  def getFeature(name: String): Boolean = features.getOrDefault(name, false)

  /**
   * Specifies that the parser produced by this factory will provide
   * support for XML namespaces.
   * By default the value of this is set to false.
   *
   * @param awareness true if the parser produced by this code
   *                  will provide support for XML namespaces;  false otherwise.
   */
  def setNamespaceAware(awareness: Boolean): Unit = features.put(XmlPullParser.FEATURE_PROCESS_NAMESPACES, awareness)

  /**
   * Indicates whether or not the factory is configured to produce
   * parsers which are namespace aware
   * (it simply set feature XmlPullParser.FEATURE_PROCESS_NAMESPACES to true or false).
   *
   * @return true if the factory is configured to produce parsers
   *         which are namespace aware; false otherwise.
   */
  def isNamespaceAware(): Boolean = getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES)

  /**
   * Specifies that the parser produced by this factory will be validating
   * (it simply set feature XmlPullParser.FEATURE_VALIDATION to true or false).
   *
   * By default the value of this is set to false.
   *
   * @param validating - if true the parsers created by this factory  must be validating.
   */
  def setValidating(validating: Boolean): Unit = features.put(XmlPullParser.FEATURE_VALIDATION, validating)

  /**
   * Indicates whether or not the factory is configured to produce parsers
   * which validate the XML content during parse.
   *
   * @return true if the factory is configured to produce parsers
   *         which validate the XML content during parse; false otherwise.
   */
  def isValidating = getFeature(XmlPullParser.FEATURE_VALIDATION): Boolean

  /**
   * Creates a new instance of a XML Pull Parser
   * using the currently configured factory features.
   *
   * @return A new instance of a XML Pull Parser.
   */
  @throws[XmlPullParserException]
  def newPullParser(): XmlPullParser = {
    val pp = getParserInstance()

    features.entrySet.forEach { entry =>
      // NOTE: This test is needed for compatibility reasons. We guarantee
      // that we only set a feature on a parser if its value is true.
      if (entry.getValue)
        pp.setFeature(entry.getKey, entry.getValue)
    }

    pp
  }

  @throws[XmlPullParserException]
  private def getParserInstance(): XmlPullParser = {

    var exceptions: ArrayBuffer[Exception] = null
    if (parserClasses != null && !parserClasses.isEmpty) {
      exceptions = new ArrayBuffer[Exception]

      parserClasses.forEach { parserClass =>
        try {
          if (parserClass != null) {
            // FIXME: this is a workaround to avoid crashing SN => try to be less obtrusive
            if (parserClass == XmlPullParserFactory.DEFAULT_PARSER_CLASS) {
              return new org.kxml2.io.KXmlParser()
            } else {
               parserClass.newInstance.asInstanceOf[XmlPullParser]
            }
          }
        }
        catch {
          case e: InstantiationException => exceptions += e
          case e: IllegalAccessException => exceptions += e
          case e: ClassCastException => exceptions += e
        }
      }
    }

    throw XmlPullParserFactory.newInstantiationException("Invalid parser class list", exceptions)
  }

  @throws[XmlPullParserException]
  private def getSerializerInstance(): XmlSerializer = {

    var exceptions: ArrayBuffer[Exception] = null
    if (serializerClasses != null && !serializerClasses.isEmpty) {
      exceptions = new ArrayBuffer[Exception]

      serializerClasses.forEach { serializerClass: Class[_] =>
        try {
          if (serializerClass != null) {
            return serializerClass.newInstance().asInstanceOf[XmlSerializer]
          }
        }
        catch {
          case e: InstantiationException => exceptions += e
          case e: IllegalAccessException => exceptions += e
          case e: ClassCastException => exceptions += e
        }
      }
    }

    throw XmlPullParserFactory.newInstantiationException("Invalid serializer class list", exceptions)
  }

  /**
   * Creates a new instance of a XML Serializer.
   *
   * <p><b>NOTE:</b> factory features are not used for XML Serializer.
   *
   * @return A new instance of a XML Serializer.
   * @throws XmlPullParserException if a parser cannot be created which satisfies the
   *                                requested configuration.
   */
  @throws[XmlPullParserException]
  def newSerializer(): XmlSerializer = getSerializerInstance()
}