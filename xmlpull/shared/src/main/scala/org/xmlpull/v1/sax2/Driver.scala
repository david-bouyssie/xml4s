/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)
package org.xmlpull.v1.sax2

import javax.xml.namespace.QName

import java.io.InputStream
import java.io.IOException
import java.io.Reader
import java.net.URL
import java.net.MalformedURLException
import java.io.FileInputStream
import java.io.FileNotFoundException

import org.xml.sax._
import org.xml.sax.helpers.DefaultHandler
import org.xmlpull.v1._

/**
 * SAX2 Driver that pulls events from XmlPullParser
 * and converts them into SAX2 callbacks.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
object Driver {
  protected val DECLARATION_HANDLER_PROPERTY = "http://xml.org/sax/properties/declaration-handler"
  protected val LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler"
  protected val NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces"
  protected val NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes"
  protected val VALIDATION_FEATURE = "http://xml.org/sax/features/validation"
  //protected val APACHE_SCHEMA_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/schema"
  //protected val APACHE_DYNAMIC_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/dynamic"

  /** Timeout for HTTP connections (in ms) */
  private val HTTP_CONN_TIMEOUT = 20 * 1000
}

class Driver @throws[XmlPullParserException]() extends Locator with XMLReader with Attributes {

  val factory = XmlPullParserFactory.newInstance()
  factory.setNamespaceAware(true)

  protected var contentHandler: ContentHandler = new DefaultHandler()
  protected var errorHandler: ErrorHandler = new DefaultHandler()
  protected var systemId: String = _
  protected var pp = factory.newPullParser()

  def this(pp: XmlPullParser) = {
    this()
    this.pp = pp
  }

  override def getLength(): Int = pp.getAttributeCount()

  override def getURI(index: Int): String = pp.getAttributeNamespace(index)

  override def getLocalName(index: Int): String = pp.getAttributeName(index)

  override def getQName(index: Int): String = {
    val prefix = pp.getAttributePrefix(index)
    if (prefix != null) prefix + ':' + pp.getAttributeName(index)
    else pp.getAttributeName(index)
  }

  override def getType(index: Int): String = pp.getAttributeType(index)

  override def getValue(index: Int): String = pp.getAttributeValue(index)

  override def getIndex(uri: String, localName: String): Int = {
    for (i <- 0 until pp.getAttributeCount()) {
      if (pp.getAttributeNamespace(i) == uri && pp.getAttributeName(i) == localName) return i
    }
    -1
  }

  override def getIndex(qName: String): Int = {
    for (i <- 0 until pp.getAttributeCount()) {
      if (pp.getAttributeName(i) == qName) return i
    }
    -1
  }

  override def getType(uri: String, localName: String): String = {
    for (i <- 0 until pp.getAttributeCount()) {
      if (pp.getAttributeNamespace(i) == uri && pp.getAttributeName(i) == localName) return pp.getAttributeType(i)
    }
    null
  }

  override def getType(qName: String): String = {
    for (i <- 0 until pp.getAttributeCount()) {
      if (pp.getAttributeName(i) == qName) return pp.getAttributeType(i)
    }
    null
  }

  override def getValue(uri: String, localName: String): String = pp.getAttributeValue(uri, localName)

  override def getValue(qName: String): String = pp.getAttributeValue(null, qName)

  override def getPublicId(): String = null

  override def getSystemId(): String = systemId

  override def getLineNumber(): Int = pp.getLineNumber()

  override def getColumnNumber(): Int = pp.getColumnNumber()

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getFeature(name: String): Boolean = {
    import Driver._
    try {
      name match {
        case NAMESPACES_FEATURE => pp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES)
        case NAMESPACE_PREFIXES_FEATURE => pp.getFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES)
        case VALIDATION_FEATURE => pp.getFeature(XmlPullParser.FEATURE_VALIDATION)
        case _ => {
          pp.getFeature(name)
          //throw new SAXNotRecognizedException("unrecognized feature "+name)
        }
      }
    } catch {
      case e: XmlPullParserException => throw new SAXNotRecognizedException(e.getMessage)
    }
  }

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setFeature(name: String, value: Boolean): Unit = {
    import Driver._
    try {
      name match {
        case NAMESPACES_FEATURE => pp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, value)
        case NAMESPACE_PREFIXES_FEATURE => {
          if (pp.getFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES) != value)
            pp.setFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES, value)
        }
        case VALIDATION_FEATURE => pp.setFeature(XmlPullParser.FEATURE_VALIDATION, value)
        case _ => pp.setFeature(name, value)
      }
    }
    catch {
      case ex: XmlPullParserException =>
        //throw new SAXNotSupportedException(s"problem with setting feature $name: $ex")
    }
  }

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getProperty(name: String): Any = {
    if (Driver.DECLARATION_HANDLER_PROPERTY == name) null
    else if (Driver.LEXICAL_HANDLER_PROPERTY == name) null
    else {
      pp.getProperty(name)
      throw new SAXNotRecognizedException(s"not recognized get property $name")
    }
  }

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setProperty(name: String, value: Any): Unit = { //
    if (Driver.DECLARATION_HANDLER_PROPERTY == name) throw new SAXNotSupportedException("not supported setting property " + name) //+" to "+value);
    else if (Driver.LEXICAL_HANDLER_PROPERTY == name) throw new SAXNotSupportedException("not supported setting property " + name)
    else {
      try pp.setProperty(name, value)
      catch {
        case ex: XmlPullParserException =>
          throw new SAXNotSupportedException("not supported set property " + name + ": " + ex)
      }
      //throw new SAXNotRecognizedException("not recognized set property "+name);
    }
  }

  override def setEntityResolver(resolver: EntityResolver): Unit = {}
  override def getEntityResolver(): EntityResolver = null

  override def setDTDHandler(handler: DTDHandler): Unit = {}
  override def getDTDHandler(): DTDHandler = null

  override def setContentHandler(handler: ContentHandler): Unit = {
    this.contentHandler = handler
  }
  override def getContentHandler = contentHandler

  override def setErrorHandler(handler: ErrorHandler): Unit = {
    this.errorHandler = handler
  }
  override def getErrorHandler(): ErrorHandler = errorHandler

  @throws[SAXException]
  @throws[IOException]
  override def parse(source: InputSource): Unit = {
    systemId = source.getSystemId()
    contentHandler.setDocumentLocator(this)

    // Try first the character stream.
    val reader = source.getCharacterStream()

    try {
      if (reader != null) pp.setInput(reader)
      else {
        // Else try the byte stream.
        var stream = source.getByteStream()
        val encoding = source.getEncoding()

        if (stream == null) {

          // Else try the system id.
          systemId = source.getSystemId()
          if (systemId == null) {
            val saxException = new SAXParseException("null source systemId", this)
            errorHandler.fatalError(saxException)
            return
          }

          // FIXME: remove me when Scala Native has HTTP IO support
          throw new Exception("can't use the source system ID (URL stream loading is not supported on Scala Native)")

          // NOTE: DBO replaced openStream() by openConnection() to run in J2ME environment (see IoUtils.openUrl)
          try {
            val url = new URL(systemId)
            stream = libcore.io.IoUtils.openUrl(systemId, Driver.HTTP_CONN_TIMEOUT)
            //stream = url.openStream()
          } catch {
            case mue: MalformedURLException =>
              try stream = new FileInputStream(systemId)
              catch {
                case fnfe: FileNotFoundException =>
                  val saxException = new SAXParseException("could not open file with systemId " + systemId, this, fnfe)
                  errorHandler.fatalError(saxException)
                  return
              }
          }
        }

        pp.setInput(stream, encoding)
      }
    } catch {
      case ex: XmlPullParserException => {
        val saxException = new SAXParseException("parsing initialization error: " + ex, this, ex)
        //if(DEBUG) ex.printStackTrace();
        errorHandler.fatalError(saxException)
        return
      }
    }

    try {

    } catch {
        case ex: XmlPullParserException => {
          val saxException = new SAXParseException("parsing initialization error: " + ex, this, ex)
          //if(DEBUG) ex.printStackTrace();
          errorHandler.fatalError(saxException)
          return
        }
      }

    // start parsing - move to first start tag
    try {
      contentHandler.startDocument()

      // get first event
      pp.next()

      // it should be start tag...
      if (pp.getEventType() != XmlPullParser.START_TAG) {
        val saxException = new SAXParseException("expected start tag not" + pp.getPositionDescription(), this)
        //throw saxException;
        errorHandler.fatalError(saxException)
        return
      }

    } catch {
      case ex: XmlPullParserException => {
        val saxException = new SAXParseException("parsing initialization error: " + ex, this, ex)
        //ex.printStackTrace();
        errorHandler.fatalError(saxException)
        return
      }
    }

    // now real parsing can start!
    parseSubTree(pp)

    // and finished ...
    contentHandler.endDocument()
  }

  @throws[SAXException]
  @throws[IOException]
  override def parse(systemId: String): Unit = parse(new InputSource(systemId))

  @throws[SAXException]
  @throws[IOException]
  def parseSubTree(pp: XmlPullParser): Unit = {

    this.pp = pp
    val namespaceAware = pp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES)

    try {
      if (pp.getEventType() != XmlPullParser.START_TAG)
        throw new SAXException("start tag must be read before skiping subtree" + pp.getPositionDescription())

      val holderForStartAndLength = new Array[Int](2)
      val rawName = new StringBuilder(16)
      var prefix: String = null
      var name: String = null
      val level = pp.getDepth() - 1
      var eventType = XmlPullParser.START_TAG

      while (level <= pp.getDepth()) {

        eventType match {
          case XmlPullParser.START_TAG =>
            if (!namespaceAware) startElement(pp.getNamespace(), pp.getName(), pp.getName())
            else {
              val depth = pp.getDepth() - 1
              val countPrev = if (level > depth) pp.getNamespaceCount(depth) else 0
              //int countPrev = pp.getNamespaceCount(pp.getDepth() - 1)

              val count = pp.getNamespaceCount(depth + 1)
              for (i <- countPrev until count) {
                contentHandler.startPrefixMapping(pp.getNamespacePrefix(i), pp.getNamespaceUri(i))
              }

              name = pp.getName()
              prefix = pp.getPrefix()
              if (prefix != null) {
                rawName.setLength(0)
                rawName.append(prefix)
                rawName.append(':')
                rawName.append(name)
              }

              startElement(
                pp.getNamespace(),
                name, // TODO Fixed this. Was "not equals".
                if (prefix == null) name else rawName.toString
              )
            }
          //++level;

          case XmlPullParser.TEXT =>
            val chars = pp.getTextCharacters(holderForStartAndLength)
            contentHandler.characters(
              chars,
              holderForStartAndLength(0), //start
              holderForStartAndLength(1) //len
            )
          case XmlPullParser.END_TAG =>
            //--level;
            if (namespaceAware) {

              name = pp.getName()
              prefix = pp.getPrefix()

              if (prefix != null) {
                rawName.setLength(0)
                rawName.append(prefix)
                rawName.append(':')
                rawName.append(name)
              }

              contentHandler.endElement(
                pp.getNamespace(),
                name,
                if (prefix != null) name else rawName.toString
              )

              // when entering show prefixes for all levels!!!!
              val depth = pp.getDepth()
              val countPrev = if (level > depth) pp.getNamespaceCount(pp.getDepth())
              else 0
              val count = pp.getNamespaceCount(pp.getDepth() - 1)

              // undeclare them in reverse order
              for (i <- count - 1 to countPrev by -1) {
                contentHandler.endPrefixMapping(pp.getNamespacePrefix(i))
              }
            }
            else contentHandler.endElement(pp.getNamespace(), pp.getName(), pp.getName())
          case XmlPullParser.END_DOCUMENT =>
            return // break the while loop by exiting the method
        }

        eventType = pp.next()
      }

    } catch {
      case ex: XmlPullParserException =>
        val saxException = new SAXParseException("parsing error: " + ex, this, ex)
        ex.printStackTrace()
        errorHandler.fatalError(saxException)
    }
  }

  /**
   * Calls {@link ContentHandler# startElement ( String, String, String, Attributes) startElement}
   * on the <code>ContentHandler</code> with <code>this</code> driver object as the
   * {@link Attributes} implementation. In default implementation
   * {@link Attributes} object is valid only during this method call and may not
   * be stored. Sub-classes can overwrite this method to cache attributes.
   */
  @throws[SAXException]
  protected def startElement(namespace: String, localName: String, qName: String): Unit = {
    contentHandler.startElement(namespace, localName, qName, this)
  }
}