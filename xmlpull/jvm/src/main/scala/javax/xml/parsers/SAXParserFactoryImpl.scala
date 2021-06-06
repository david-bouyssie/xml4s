package javax.xml.parsers

import org.xml.sax.SAXNotRecognizedException

import java.util.HashMap

//class SAXParserFactoryImpl extends com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl

/*object SAXParserFactoryImpl {

  private val NAMESPACES = "http://xml.org/sax/features/namespaces"
  private val VALIDATION = "http://xml.org/sax/features/validation"

  // Default SAXParserImpl
  private var saxParserImplProvider: HashMap[String, Boolean] => SAXParser = { features =>
    throw new Exception("SAXParserFactoryImpl.setParserImplProvider() must first be called")
  }

  def setParserImplProvider(provider: HashMap[String, Boolean] => SAXParser): Unit = {
    saxParserImplProvider = provider
  }

}
*/

class SAXParserFactoryImpl2 extends CustomSAXParserFactoryImpl {
  def createImpl(features: HashMap[String, Boolean]): SAXParser = {
    new KxmlSAXParserImpl(features)
  }
}

// FIXME: remove code redundancy with Sax shared file
object CustomSAXParserFactoryImpl {

  private val NAMESPACES = "http://xml.org/sax/features/namespaces"
  private val VALIDATION = "http://xml.org/sax/features/validation"

}

abstract class CustomSAXParserFactoryImpl extends SAXParserFactory {

  def createImpl(features: HashMap[String, Boolean]): SAXParser

  private val features = new HashMap[String, Boolean]

  @throws[SAXNotRecognizedException]
  def getFeature(name: String): Boolean = {
    if (name == null) throw new NullPointerException("name == null")
    if (!name.startsWith("http://xml.org/sax/features/")) throw new SAXNotRecognizedException(name)
    java.lang.Boolean.TRUE == features.get(name)
  }

  override def isNamespaceAware(): Boolean = {
    try getFeature(CustomSAXParserFactoryImpl.NAMESPACES)
    catch {
      case ex: SAXNotRecognizedException =>
        throw new AssertionError(ex)
    }
  }

  override def isValidating(): Boolean = {
    try getFeature(CustomSAXParserFactoryImpl.VALIDATION)
    catch {
      case ex: SAXNotRecognizedException =>
        throw new AssertionError(ex)
    }
  }

  @throws[ParserConfigurationException]
  def newSAXParser(): SAXParser = {
    if (isValidating())
      throw new ParserConfigurationException("No validating SAXParser implementation available")

    try createImpl(features)
    catch {
      case ex: Exception =>
        throw new ParserConfigurationException(ex.toString)
    }
  }

  @throws[SAXNotRecognizedException]
  def setFeature(name: String, value: Boolean): Unit = {
    if (name == null) throw new NullPointerException("name == null")
    if (!name.startsWith("http://xml.org/sax/features/")) throw new SAXNotRecognizedException(name)

    if (value) features.put(name, java.lang.Boolean.TRUE)
    else {
      // This is needed to disable features that are enabled by default.
      features.put(name, java.lang.Boolean.FALSE)
    }
  }

  override def setNamespaceAware(value: Boolean): Unit = {
    try setFeature(CustomSAXParserFactoryImpl.NAMESPACES, value)
    catch {
      case ex: SAXNotRecognizedException =>
        throw new AssertionError(ex)
    }
  }

  override def setValidating(value: Boolean): Unit = {
    try setFeature(CustomSAXParserFactoryImpl.VALIDATION, value)
    catch {
      case ex: SAXNotRecognizedException =>
        throw new AssertionError(ex)
    }
  }
}


