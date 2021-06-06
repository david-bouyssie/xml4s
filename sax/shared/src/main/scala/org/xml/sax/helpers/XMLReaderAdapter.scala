// XMLReaderAdapter.java - adapt an SAX2 XMLReader to a SAX1 Parser
// http://www.saxproject.org
// Written by David Megginson
// NO WARRANTY!  This class is in the public domain.
// $Id: XMLReaderAdapter.java,v 1.9 2004/04/26 17:34:35 dmegginson Exp $
package org.xml.sax.helpers

import org.xml.sax._

import java.io.IOException
import java.util.Locale

/**
 * Adapt a SAX2 XMLReader as a SAX1 Parser.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>This class wraps a SAX2 {@link XMLReader XMLReader}
 * and makes it act as a SAX1 {@link Parser Parser}.  The XMLReader
 * must support a true value for the
 * http://xml.org/sax/features/namespace-prefixes property or parsing will fail
 * with a {@link SAXException SAXException}; if the XMLReader
 * supports a false value for the http://xml.org/sax/features/namespaces
 * property, that will also be used to improve efficiency.</p>
 *
 * @since SAX 2.0
 * @author David Megginson
 * @version 2.0.1 (sax2r2)
 * @see Parser
 * @see XMLReader
 */
object XMLReaderAdapter {
  /**
   * Internal class to wrap a SAX2 Attributes object for SAX1.
   */
  final private[helpers] class AttributesAdapter private[helpers]() extends AttributeList {

    private var attributes: Attributes = _

    /**
     * Set the embedded Attributes object.
     *
     * @param The embedded SAX2 Attributes.
     */
    private[helpers] def setAttributes(attributes: Attributes): Unit = {
      this.attributes = attributes
    }

    /**
     * Return the number of attributes.
     *
     * @return The length of the attribute list.
     * @see AttributeList#getLength
     */
    override def getLength() = attributes.getLength()

    /**
     * Return the qualified (prefixed) name of an attribute by position.
     *
     * @return The qualified name.
     * @see AttributeList#getName
     */
    override def getName(i: Int) = attributes.getQName(i)

    /**
     * Return the type of an attribute by position.
     *
     * @return The type.
     * @see AttributeList#getType(int)
     */
    override def getType(i: Int) = attributes.getType(i)

    /**
     * Return the value of an attribute by position.
     *
     * @return The value.
     * @see AttributeList#getValue(int)
     */
    override def getValue(i: Int) = attributes.getValue(i)

    /**
     * Return the type of an attribute by qualified (prefixed) name.
     *
     * @return The type.
     * @see AttributeList#getType(java.lang.String)
     */
    override def getType(qName: String) = attributes.getType(qName)

    /**
     * Return the value of an attribute by qualified (prefixed) name.
     *
     * @return The value.
     * @see AttributeList#getValue(java.lang.String)
     */
    override def getValue(qName: String) = attributes.getValue(qName)
  }
}

/**
 * Create a new adapter.
 *
 * <p>Use the "org.xml.sax.driver" property to locate the SAX2
 * driver to embed.</p>
 *
 * @exception SAXException If the embedded driver
 *            cannot be instantiated or if the
 *            org.xml.sax.driver property is not specified.
 */
class XMLReaderAdapter @throws[SAXException]() extends Parser with ContentHandler {

  private[helpers] var xmlReader: XMLReader = _
  private[helpers] var documentHandler: DocumentHandler = _
  private[helpers] var qAtts: XMLReaderAdapter.AttributesAdapter = _

  setup(XMLReaderFactory.createXMLReader())

  /**
   * Create a new adapter.
   *
   * <p>Create a new adapter, wrapped around a SAX2 XMLReader.
   * The adapter will make the XMLReader act like a SAX1
   * Parser.</p>
   *
   * @param xmlReader The SAX2 XMLReader to wrap.
   * @exception java.lang.NullPointerException If the argument is null.
   */
  def this(xmlReader: XMLReader) = {
    this()
    setup(xmlReader)
  }

  /**
   * Internal setup.
   *
   * @param xmlReader The embedded XMLReader.
   */
  private def setup(xmlReader: XMLReader) = {
    require(xmlReader != null, "XMLReader must not be null")
    this.xmlReader = xmlReader
    qAtts = new XMLReaderAdapter.AttributesAdapter()
  }

  /**
   * Set the locale for error reporting.
   *
   * <p>This is not supported in SAX2, and will always throw
   * an exception.</p>
   *
   * @param locale the locale for error reporting.
   * @see Parser#setLocale
   * @exception SAXException Thrown unless overridden.
   */
  @throws[SAXException]
  override def setLocale(locale: Locale): Unit = {
    throw new SAXNotSupportedException("setLocale not supported")
  }

  /**
   * Register the entity resolver.
   *
   * @param resolver The new resolver.
   * @see Parser#setEntityResolver
   */
  override def setEntityResolver(resolver: EntityResolver): Unit = {
    xmlReader.setEntityResolver(resolver)
  }

  /**
   * Register the DTD event handler.
   *
   * @param handler The new DTD event handler.
   * @see Parser#setDTDHandler
   */
  override def setDTDHandler(handler: DTDHandler): Unit = {
    xmlReader.setDTDHandler(handler)
  }

  /**
   * Register the SAX1 document event handler.
   *
   * <p>Note that the SAX1 document handler has no Namespace
   * support.</p>
   *
   * @param handler The new SAX1 document event handler.
   * @see Parser#setDocumentHandler
   */
  override def setDocumentHandler(handler: DocumentHandler): Unit = {
    documentHandler = handler
  }

  /**
   * Register the error event handler.
   *
   * @param handler The new error event handler.
   * @see Parser#setErrorHandler
   */
  override def setErrorHandler(handler: ErrorHandler): Unit = {
    xmlReader.setErrorHandler(handler)
  }

  /**
   * Parse the document.
   *
   * <p>This method will throw an exception if the embedded
   * XMLReader does not support the
   * http://xml.org/sax/features/namespace-prefixes property.</p>
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
   * Parse the document.
   *
   * <p>This method will throw an exception if the embedded
   * XMLReader does not support the
   * http://xml.org/sax/features/namespace-prefixes property.</p>
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
    setupXMLReader()
    xmlReader.parse(input)
  }

  /**
   * Set up the XML reader.
   */
  @throws[SAXException]
  private def setupXMLReader(): Unit = {
    xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
    try xmlReader.setFeature("http://xml.org/sax/features/namespaces", false)
    catch {
      case e: SAXException =>
      // NO OP: it's just extra information, and we can ignore it
    }
    xmlReader.setContentHandler(this)
  }

  /**
   * Set a document locator.
   *
   * @param locator The document locator.
   * @see ContentHandler#setDocumentLocator
   */
  override def setDocumentLocator(locator: Locator): Unit = {
    if (documentHandler != null) documentHandler.setDocumentLocator(locator)
  }

  /**
   * Start document event.
   *
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see ContentHandler#startDocument
   */
  @throws[SAXException]
  override def startDocument(): Unit = {
    if (documentHandler != null) documentHandler.startDocument()
  }

  /**
   * End document event.
   *
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see ContentHandler#endDocument
   */
  @throws[SAXException]
  override def endDocument(): Unit = {
    if (documentHandler != null) documentHandler.endDocument()
  }

  /**
   * Adapt a SAX2 start prefix mapping event.
   *
   * @param prefix The prefix being mapped.
   * @param uri    The Namespace URI being mapped to.
   * @see ContentHandler#startPrefixMapping
   */
  override def startPrefixMapping(prefix: String, uri: String): Unit = {
  }

  /**
   * Adapt a SAX2 end prefix mapping event.
   *
   * @param prefix The prefix being mapped.
   * @see ContentHandler#endPrefixMapping
   */
  override def endPrefixMapping(prefix: String): Unit = {
  }

  /**
   * Adapt a SAX2 start element event.
   *
   * @param uri       The Namespace URI.
   * @param localName The Namespace local name.
   * @param qName     The qualified (prefixed) name.
   * @param atts      The SAX2 attributes.
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see ContentHandler#endDocument
   */
  @throws[SAXException]
  override def startElement(uri: String, localName: String, qName: String, atts: Attributes): Unit = {
    if (documentHandler != null) {
      qAtts.setAttributes(atts)
      documentHandler.startElement(qName, qAtts)
    }
  }

  /**
   * Adapt a SAX2 end element event.
   *
   * @param uri       The Namespace URI.
   * @param localName The Namespace local name.
   * @param qName     The qualified (prefixed) name.
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see ContentHandler#endElement
   */
  @throws[SAXException]
  override def endElement(uri: String, localName: String, qName: String): Unit = {
    if (documentHandler != null) documentHandler.endElement(qName)
  }

  /**
   * Adapt a SAX2 characters event.
   *
   * @param ch     An array of characters.
   * @param start  The starting position in the array.
   * @param length The number of characters to use.
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see ContentHandler#characters
   */
  @throws[SAXException]
  override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
    if (documentHandler != null) documentHandler.characters(ch, start, length)
  }

  /**
   * Adapt a SAX2 ignorable whitespace event.
   *
   * @param ch     An array of characters.
   * @param start  The starting position in the array.
   * @param length The number of characters to use.
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see ContentHandler#ignorableWhitespace
   */
  @throws[SAXException]
  override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = {
    if (documentHandler != null) documentHandler.ignorableWhitespace(ch, start, length)
  }

  /**
   * Adapt a SAX2 processing instruction event.
   *
   * @param target The processing instruction target.
   * @param data   The remainder of the processing instruction
   * @exception SAXException The client may raise a
   *            processing exception.
   * @see ContentHandler#processingInstruction
   */
  @throws[SAXException]
  override def processingInstruction(target: String, data: String): Unit = {
    if (documentHandler != null) documentHandler.processingInstruction(target, data)
  }

  /**
   * Adapt a SAX2 skipped entity event.
   *
   * @param name The name of the skipped entity.
   * @see ContentHandler#skippedEntity
   * @exception SAXException Throwable by subclasses.
   */
  @throws[SAXException]
  override def skippedEntity(name: String): Unit = {
  }

}