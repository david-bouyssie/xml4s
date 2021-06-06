package javax.xml.parsers

import org.xml.sax.SAXNotRecognizedException

import java.util.HashMap

class SAXParserFactoryImpl extends CustomSAXParserFactoryImpl {
  override def setFeature(name: String, value: Boolean): Unit = {
    try {
      super.setFeature(name, value)
    } catch {
      // silent errors
      case e: SAXNotRecognizedException =>
    }
  }

  override def getFeature(name: String): Boolean = {
    try {
      super.getFeature(name)
    } catch {
      // silent errors
      case e: SAXNotRecognizedException => false
    }
  }

  def createImpl(features: HashMap[String, Boolean]): SAXParser = {
    new KxmlSAXParserImpl(features)
  }
}

class SAXParserFactoryImpl2 extends CustomSAXParserFactoryImpl {
  def createImpl(features: HashMap[String, Boolean]): SAXParser = {
    new KxmlSAXParserImpl(features)
  }
}
