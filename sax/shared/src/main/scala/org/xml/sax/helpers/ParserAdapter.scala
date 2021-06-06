// ParserAdapter.java - adapt a SAX1 Parser to a SAX2 XMLReader.
// http://www.saxproject.org
// Written by David Megginson
// NO WARRANTY!  This class is in the public domain.
// $Id: ParserAdapter.java,v 1.16 2004/04/26 17:34:35 dmegginson Exp $
package org.xml.sax.helpers

import org.xml.sax._

import java.io.IOException

/**
 * Adapt a SAX1 Parser as a SAX2 XMLReader.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>This class wraps a SAX1 {@link Parser Parser}
 * and makes it act as a SAX2 {@link XMLReader XMLReader},
 * with feature, property, and Namespace support.  Note
 * that it is not possible to report {@link ContentHandler#skippedEntity()} events,
 * since SAX1 does not make that information available.</p>
 *
 * <p>This adapter does not test for duplicate Namespace-qualified
 * attribute names.</p>
 *
 * @since SAX 2.0
 * @author David Megginson
 * @version 2.0.1 (sax2r2)
 * @see XMLReaderAdapter
 * @see XMLReader
 * @see Parser
 */
object ParserAdapter { //
  // Internal constants for the sake of convenience.
  private val FEATURES = "http://xml.org/sax/features/"
  private val NAMESPACES = FEATURES + "namespaces"
  private val NAMESPACE_PREFIXES = FEATURES + "namespace-prefixes"
  private val XMLNS_URIs = FEATURES + "xmlns-uris"
}

/**
 * Construct a new parser adapter.
 *
 * <p>Use the "org.xml.sax.parser" property to locate the
 * embedded SAX1 driver.</p>
 *
 * @exception SAXException If the embedded driver
 *            cannot be instantiated or if the
 *            org.xml.sax.parser property is not specified.
 */
class ParserAdapter @throws[SAXException]() extends XMLReader with DocumentHandler {

  private var nsSupport: NamespaceSupport = _
  private var attAdapter: AttributeListAdapter = _
  private var parsing = false
  private val nameParts = new Array[String](3)
  private var parser: Parser = _
  private var atts: AttributesImpl = _

  // Features
  private var namespaces = true
  private var prefixes = false
  private var uris = false

  // Handlers
  private[helpers] var locator: Locator = _
  private[helpers] var entityResolver: EntityResolver = _
  private[helpers] var dtdHandler: DTDHandler = _
  private[helpers] var contentHandler: ContentHandler = _
  private[helpers] var errorHandler: ErrorHandler = _

  val driver: String = System.getProperty("org.xml.sax.parser")

  try setup(ParserFactory.makeParser())
  catch {
    case e1: ClassNotFoundException =>
      throw new SAXException("Cannot find SAX1 driver class " + driver, e1)
    case e2: IllegalAccessException =>
      throw new SAXException("SAX1 driver class " + driver + " found but cannot be loaded", e2)
    case e3: InstantiationException =>
      throw new SAXException("SAX1 driver class " + driver + " loaded but cannot be instantiated", e3)
    case e4: ClassCastException =>
      throw new SAXException("SAX1 driver class " + driver + " does not implement Parser")
    case e5: NullPointerException =>
      throw new SAXException("System property org.xml.sax.parser not specified")
  }

  /**
   * Construct a new parser adapter.
   *
   * <p>Note that the embedded parser cannot be changed once the
   * adapter is created; to embed a different parser, allocate
   * a new ParserAdapter.</p>
   *
   * @param parser The SAX1 parser to embed.
   * @exception java.lang.NullPointerException If the parser parameter
   *            is null.
   */
  def this(parser: Parser) = {
    this()
    setup(parser)
  }

  /**
   * Internal setup method.
   *
   * @param parser The embedded parser.
   * @exception java.lang.NullPointerException If the parser parameter
   *            is null.
   */
  private def setup(parser: Parser): Unit = {
    if (parser == null) throw new NullPointerException("Parser argument must not be null")
    this.parser = parser
    atts = new AttributesImpl()
    nsSupport = new NamespaceSupport()
    attAdapter = new AttributeListAdapter()
  }

  /**
   * Set a feature flag for the parser.
   *
   * <p>The only features recognized are namespaces and
   * namespace-prefixes.</p>
   *
   * @param name  The feature name, as a complete URI.
   * @param value The requested feature value.
   * @exception SAXNotRecognizedException If the feature
   *            can't be assigned or retrieved.
   * @exception SAXNotSupportedException If the feature
   *            can't be assigned that value.
   * @see XMLReader#setFeature
   */
  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setFeature(name: String, value: Boolean): Unit = {
    if (name == ParserAdapter.NAMESPACES) {
      checkNotParsing("feature", name)
      namespaces = value
      if (!namespaces && !prefixes) prefixes = true
    }
    else if (name == ParserAdapter.NAMESPACE_PREFIXES) {
      checkNotParsing("feature", name)
      prefixes = value
      if (!prefixes && !namespaces) namespaces = true
    }
    else if (name == ParserAdapter.XMLNS_URIs) {
      checkNotParsing("feature", name)
      uris = value
    }
    else throw new SAXNotRecognizedException("Feature: " + name)
  }

  /**
   * Check a parser feature flag.
   *
   * <p>The only features recognized are namespaces and
   * namespace-prefixes.</p>
   *
   * @param name The feature name, as a complete URI.
   * @return The current feature value.
   * @exception SAXNotRecognizedException If the feature
   *            value can't be assigned or retrieved.
   * @exception SAXNotSupportedException If the
   *            feature is not currently readable.
   * @see XMLReader#setFeature
   */
  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getFeature(name: String): Boolean = {
    if (name == ParserAdapter.NAMESPACES) namespaces
    else if (name == ParserAdapter.NAMESPACE_PREFIXES) prefixes
    else if (name == ParserAdapter.XMLNS_URIs) uris
    else throw new SAXNotRecognizedException("Feature: " + name)
  }

  /**
   * Set a parser property.
   *
   * <p>No properties are currently recognized.</p>
   *
   * @param name  The property name.
   * @param value The property value.
   * @exception SAXNotRecognizedException If the property
   *            value can't be assigned or retrieved.
   * @exception SAXNotSupportedException If the property
   *            can't be assigned that value.
   * @see XMLReader#setProperty
   */
  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setProperty(name: String, value: Any): Unit = {
    throw new SAXNotRecognizedException("Property: " + name)
  }

  /**
   * Get a parser property.
   *
   * <p>No properties are currently recognized.</p>
   *
   * @param name The property name.
   * @return The property value.
   * @exception SAXNotRecognizedException If the property
   *            value can't be assigned or retrieved.
   * @exception SAXNotSupportedException If the property
   *            value is not currently readable.
   * @see XMLReader#getProperty
   */
  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getProperty(name: String): String = {
    throw new SAXNotRecognizedException("Property: " + name)
  }

  /**
   * Set the entity resolver.
   *
   * @param resolver The new entity resolver.
   * @see XMLReader#setEntityResolver
   */
  override def setEntityResolver(resolver: EntityResolver): Unit = {
    entityResolver = resolver
  }

  /**
   * Return the current entity resolver.
   *
   * @return The current entity resolver, or null if none was supplied.
   * @see XMLReader#getEntityResolver
   */
  override def getEntityResolver(): EntityResolver = entityResolver

  /**
   * Set the DTD handler.
   *
   * @param handler the new DTD handler
   * @see XMLReader#setEntityResolver
   */
  override def setDTDHandler(handler: DTDHandler): Unit = {
    dtdHandler = handler
  }

  /**
   * Return the current DTD handler.
   *
   * @return the current DTD handler, or null if none was supplied
   * @see XMLReader#getEntityResolver
   */
  override def getDTDHandler(): DTDHandler = dtdHandler

  /**
   * Set the content handler.
   *
   * @param handler the new content handler
   * @see XMLReader#setEntityResolver
   */
  override def setContentHandler(handler: ContentHandler): Unit = {
    contentHandler = handler
  }

  /**
   * Return the current content handler.
   *
   * @return The current content handler, or null if none was supplied.
   * @see XMLReader#getEntityResolver
   */
  override def getContentHandler(): ContentHandler = contentHandler

  /**
   * Set the error handler.
   *
   * @param handler The new error handler.
   * @see XMLReader#setEntityResolver
   */
  override def setErrorHandler(handler: ErrorHandler): Unit = {
    errorHandler = handler
  }

  /**
   * Return the current error handler.
   *
   * @return The current error handler, or null if none was supplied.
   * @see XMLReader#getEntityResolver
   */
  override def getErrorHandler(): ErrorHandler = errorHandler

  /**
   * Parse an XML document.
   *
   * @param systemId The absolute URL of the document.
   * @exception java.io.IOException If there is a problem reading
   *            the raw content of the document.
   * @exception SAXException If there is a problem
   *            processing the document.
   * @see #parse(InputSource)
   * @see Parser#parse(java.lang.String)
   */
  @throws[IOException]
  @throws[SAXException]
  override def parse(systemId: String): Unit = parse(new InputSource(systemId))

  /**
   * Parse an XML document.
   *
   * @param input An input source for the document.
   * @exception java.io.IOException If there is a problem reading
   *            the raw content of the document.
   * @exception SAXException If there is a problem
   *            processing the document.
   * @see #parse(java.lang.String)
   * @see Parser#parse(InputSource)
   */
  @throws[IOException]
  @throws[SAXException]
  override def parse(input: InputSource): Unit = {
    if (parsing) throw new SAXException("Parser is already in use")
    setupParser()
    parsing = true
    try parser.parse(input)
    finally parsing = false
    parsing = false
  }

  /**
   * Adapter implementation method; do not call.
   * Adapt a SAX1 document locator event.
   *
   * @param locator A document locator.
   * @see ContentHandler#setDocumentLocator
   */
  override def setDocumentLocator(locator: Locator): Unit = {
    this.locator = locator
    if (contentHandler != null) contentHandler.setDocumentLocator(locator)
  }

  /**
   * Adapter implementation method; do not call.
   * Adapt a SAX1 start document event.
   *
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see DocumentHandler#startDocument
   */
  @throws[SAXException]
  override def startDocument(): Unit = {
    if (contentHandler != null) contentHandler.startDocument()
  }

  /**
   * Adapter implementation method; do not call.
   * Adapt a SAX1 end document event.
   *
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see DocumentHandler#endDocument
   */
  @throws[SAXException]
  override def endDocument(): Unit = {
    if (contentHandler != null) contentHandler.endDocument()
  }

  /**
   * Adapter implementation method; do not call.
   * Adapt a SAX1 startElement event.
   *
   * <p>If necessary, perform Namespace processing.</p>
   *
   * @param qName The qualified (prefixed) name.
   * @param qAtts The XML attribute list (with qnames).
   * @exception SAXException The client may raise a
   *            processing exception.
   */
  @throws[SAXException]
  override def startElement(qName: String, qAtts: AttributeList): Unit = {

    // If we're not doing Namespace processing, dispatch this quickly
    if (!namespaces) {
      if (contentHandler != null) {
        attAdapter.setAttributeList(qAtts)
        contentHandler.startElement("", "", qName.intern, attAdapter)
      }
      return
    }

    // OK, we're doing Namespace processing.
    nsSupport.pushContext()

    // These are exceptions from the first pass;
    // they should be ignored if there's a second pass, but reported otherwise.
    var exceptions: collection.mutable.ArrayBuffer[SAXParseException] = null

    val length = qAtts.getLength()

    // First pass: handle NS decls
    for (i <- 0 until length) {
      val attQName = qAtts.getName(i)
      if (attQName.startsWith("xmlns")) {

        // Could be a declaration...
        val n = attQName.indexOf(':')

        // xmlns=...
        val prefix = if (n == -1 && attQName.length == 5) ""
        // xmlns:foo=...
        else if (n == 5) attQName.substring(6)
        // XML namespaces spec doesn't discuss "xmlnsf:oo"
        // (and similarly named) attributes ... at most, warn
        else null

        if (prefix != null) {
          val value = qAtts.getValue(i)
          if (!nsSupport.declarePrefix(prefix, value)) {
            reportError("Illegal Namespace prefix: " + prefix)
          } else if (contentHandler != null) {
            contentHandler.startPrefixMapping(prefix, value)
          }
        }
      }
    }

    // Second pass: copy all relevant
    // attributes into the SAX2 AttributeList
    // using updated prefix bindings
    atts.clear()

    for (i <- 0 until length) {
      val attQName = qAtts.getName(i)
      val `type` = qAtts.getType(i)
      val value = qAtts.getValue(i)

      // Declaration?
      val isDeclaration = if (!attQName.startsWith("xmlns")) false
      else {
        val n = attQName.indexOf(':')

        val prefix: String = if (n == -1 && attQName.length == 5) ""
        // (and similarly named) attributes ... ignore
        else if (n == 5) attQName.substring(6)
        else null

        // Yes, decl:  report or prune
        if (prefix == null) false
        else {
          if (prefixes) if (uris) { // note funky case:  localname can be null
            // when declaring the default prefix, and
            // yet the uri isn't null.
            atts.addAttribute(NamespaceSupport.XMLNS, prefix, attQName.intern, `type`, value)
          }
          else atts.addAttribute("", "", attQName.intern, `type`, value)
          true
        }
      }

      if (!isDeclaration) {
        // Not a declaration -- report
        try {
          val attName = processName(attQName, true, true)
          atts.addAttribute(attName(0), attName(1), attName(2), `type`, value)
        } catch {
          case e: SAXException =>
            if (exceptions == null) exceptions = new collection.mutable.ArrayBuffer[SAXParseException]
            exceptions += e.asInstanceOf[SAXParseException]
            atts.addAttribute("", attQName, attQName, `type`, value)
        }
      }
    }

    // now handle the deferred exception reports
    if (exceptions != null && errorHandler != null) {
      for (ex <- exceptions) {
        errorHandler.error(ex)
      }
    }

    // OK, finally report the event.
    if (contentHandler != null) {
      val name = processName(qName, false, false)
      contentHandler.startElement(name(0), name(1), name(2), atts)
    }
  }

  /**
   * Adapter implementation method; do not call.
   * Adapt a SAX1 end element event.
   *
   * @param qName The qualified (prefixed) name.
   * @exception SAXException The client may raise a processing exception.
   * @see DocumentHandler#endElement
   */
  @throws[SAXException]
  override def endElement(qName: String): Unit = {
    if (!namespaces) {
      if (contentHandler != null) contentHandler.endElement("", "", qName.intern)
      return
    }

    // Split the name.
    val names = processName(qName, false, false)
    if (contentHandler != null) {
      contentHandler.endElement(names(0), names(1), names(2))
      val prefixes = nsSupport.getDeclaredPrefixes()
      while (prefixes.hasMoreElements) {
        val prefix = prefixes.nextElement.asInstanceOf[String]
        contentHandler.endPrefixMapping(prefix)
      }
    }
    nsSupport.popContext()
  }

  /**
   * Adapter implementation method; do not call.
   * Adapt a SAX1 characters event.
   *
   * @param ch     An array of characters.
   * @param start  The starting position in the array.
   * @param length The number of characters to use.
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see DocumentHandler#characters
   */
  @throws[SAXException]
  override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
    if (contentHandler != null) contentHandler.characters(ch, start, length)
  }

  /**
   * Adapter implementation method; do not call.
   * Adapt a SAX1 ignorable whitespace event.
   *
   * @param ch     An array of characters.
   * @param start  The starting position in the array.
   * @param length The number of characters to use.
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see DocumentHandler#ignorableWhitespace
   */
  @throws[SAXException]
  override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = {
    if (contentHandler != null) contentHandler.ignorableWhitespace(ch, start, length)
  }

  /**
   * Adapter implementation method; do not call.
   * Adapt a SAX1 processing instruction event.
   *
   * @param target The processing instruction target.
   * @param data   The remainder of the processing instruction
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see DocumentHandler#processingInstruction
   */
  @throws[SAXException]
  override def processingInstruction(target: String, data: String): Unit = {
    if (contentHandler != null) contentHandler.processingInstruction(target, data)
  }

  /**
   * Initialize the parser before each run.
   */
  private def setupParser(): Unit = {
    // catch an illegal "nonsense" state.
    if (!prefixes && !namespaces) throw new IllegalStateException

    nsSupport.reset()

    if (uris) nsSupport.setNamespaceDeclUris(true)
    if (entityResolver != null) parser.setEntityResolver(entityResolver)
    if (dtdHandler != null) parser.setDTDHandler(dtdHandler)
    if (errorHandler != null) parser.setErrorHandler(errorHandler)

    parser.setDocumentHandler(this)
    locator = null
  }

  /**
   * Process a qualified (prefixed) name.
   *
   * <p>If the name has an undeclared prefix, use only the qname
   * and make an ErrorHandler.error callback in case the app is
   * interested.</p>
   *
   * @param qName       The qualified (prefixed) name.
   * @param isAttribute true if this is an attribute name.
   * @return The name split into three parts.
   * @exception SAXException The client may throw
   *            an exception if there is an error callback.
   */
  @throws[SAXException]
  private def processName(qName: String, isAttribute: Boolean, useException: Boolean): Array[String] = {
    var parts = nsSupport.processName(qName, nameParts, isAttribute)

    if (parts == null) {
      if (useException) throw makeException("Undeclared prefix: " + qName)
      reportError("Undeclared prefix: " + qName)
      parts = new Array[String](3)
      parts(0) = ""
      parts(1) = ""
      parts(2) = qName.intern
    }

    parts
  }

  /**
   * Report a non-fatal error.
   *
   * @param message The error message.
   * @exception SAXException The client may throw
   *            an exception.
   */
  @throws[SAXException]
  private[helpers] def reportError(message: String): Unit = {
    if (errorHandler != null) errorHandler.error(makeException(message))
  }

  /**
   * Construct an exception for the current context.
   *
   * @param message The error message.
   */
  private def makeException(message: String): SAXParseException = {
    if (locator != null) new SAXParseException(message, locator)
    else new SAXParseException(message, null, null, -1, -1)
  }

  /**
   * Throw an exception if we are parsing.
   *
   * <p>Use this method to detect illegal feature or
   * property changes.</p>
   *
   * @param type The type of thing (feature or property).
   * @param name The feature or property name.
   * @exception SAXNotSupportedException If a
   *            document is currently being parsed.
   */
  @throws[SAXNotSupportedException]
  private def checkNotParsing(`type`: String, name: String): Unit = {
    if (parsing) throw new SAXNotSupportedException("Cannot change " + `type` + ' ' + name + " while parsing")
  }

  /**
   * Adapt a SAX1 AttributeList as a SAX2 Attributes object.
   *
   * <p>This class is in the Public Domain, and comes with NO
   * WARRANTY of any kind.</p>
   *
   * <p>This wrapper class is used only when Namespace support
   * is disabled -- it provides pretty much a direct mapping
   * from SAX1 to SAX2, except that names and types are
   * interned whenever requested.</p>
   *
   * Construct a new adapter.
   */
  final private[helpers] class AttributeListAdapter private[helpers]() extends Attributes {

    private var qAtts: AttributeList = _

    /**
     * Set the embedded AttributeList.
     *
     * <p>This method must be invoked before any of the others
     * can be used.</p>
     *
     * @param qAtts The SAX1 attribute list (with qnames).
     */
    private[helpers] def setAttributeList(qAtts: AttributeList): Unit = {
      this.qAtts = qAtts
    }

    /**
     * Return the length of the attribute list.
     *
     * @return The number of attributes in the list.
     * @see Attributes#getLength
     */
    override def getLength() = qAtts.getLength()

    /**
     * Return the Namespace URI of the specified attribute.
     *
     * @param i The attribute's index.
     * @return Always the empty string.
     * @see Attributes#getURI
     */
    override def getURI(i: Int): String = ""

    /**
     * Return the local name of the specified attribute.
     *
     * @param i The attribute's index.
     * @return Always the empty string.
     * @see Attributes#getLocalName
     */
    override def getLocalName(i: Int): String = ""

    /**
     * Return the qualified (prefixed) name of the specified attribute.
     *
     * @param i The attribute's index.
     * @return The attribute's qualified name, internalized.
     */
    override def getQName(i: Int): String = qAtts.getName(i).intern

    /**
     * Return the type of the specified attribute.
     *
     * @param i The attribute's index.
     * @return The attribute's type as an internalized string.
     */
    override def getType(i: Int): String = qAtts.getType(i).intern

    /**
     * Return the value of the specified attribute.
     *
     * @param i The attribute's index.
     * @return The attribute's value.
     */
    override def getValue(i: Int): String = qAtts.getValue(i)

    /**
     * Look up an attribute index by Namespace name.
     *
     * @param uri       The Namespace URI or the empty string.
     * @param localName The local name.
     * @return The attributes index, or -1 if none was found.
     * @see Attributes#getIndex(java.lang.String,java.lang.String)
     */
    override def getIndex(uri: String, localName: String) = -1

    /**
     * Look up an attribute index by qualified (prefixed) name.
     *
     * @param qName The qualified name.
     * @return The attributes index, or -1 if none was found.
     * @see Attributes#getIndex(java.lang.String)
     */
    override def getIndex(qName: String): Int = {
      val max = atts.getLength()
      for (i <- 0 until max) {
        if (qAtts.getName(i) == qName) return i
      }
      -1
    }

    /**
     * Look up the type of an attribute by Namespace name.
     *
     * @param uri       The Namespace URI
     * @param localName The local name.
     * @return The attribute's type as an internalized string.
     */
    override def getType(uri: String, localName: String) = null

    /**
     * Look up the type of an attribute by qualified (prefixed) name.
     *
     * @param qName The qualified name.
     * @return The attribute's type as an internalized string.
     */
    override def getType(qName: String) = qAtts.getType(qName).intern

    /**
     * Look up the value of an attribute by Namespace name.
     *
     * @param uri       The Namespace URI
     * @param localName The local name.
     * @return The attribute's value.
     */
    override def getValue(uri: String, localName: String) = null

    /**
     * Look up the value of an attribute by qualified (prefixed) name.
     *
     * @param qName The qualified name.
     * @return The attribute's value.
     */
    override def getValue(qName: String) = qAtts.getValue(qName)

  }
}