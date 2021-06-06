// XMLFilterImpl.java - base SAX2 filter implementation.
// http://www.saxproject.org
// Written by David Megginson
// NO WARRANTY!  This class is in the Public Domain.
// $Id: XMLFilterImpl.java,v 1.9 2004/04/26 17:34:35 dmegginson Exp $
package org.xml.sax.helpers

import org.xml.sax._

import java.io.IOException

/**
 * Base class for deriving an XML filter.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>This class is designed to sit between an {@link XMLReader} and the client application's event handlers.
 * By default, it does nothing but pass requests up to the reader and events
 * on to the handlers unmodified, but subclasses can override
 * specific methods to modify the event stream or the configuration
 * requests as they pass through.</p>
 *
 * @since SAX 2.0
 * @author David Megginson
 * @version 2.0.1 (sax2r2)
 * @see XMLFilter
 * @see XMLReader
 * @see EntityResolver
 * @see DTDHandler
 * @see ContentHandler
 * @see ErrorHandler
 *
 * Construct an empty XML filter, with no parent.
 *
 * <p>This filter will have no parent: you must assign a parent
 * before you start a parse or do any configuration with
 * setFeature or setProperty, unless you use this as a pure event
 * consumer rather than as an {@link XMLReader}.</p>
 *
 * @see XMLReader#setFeature
 * @see XMLReader#setProperty
 * @see #setParent
 */
class XMLFilterImpl() extends XMLFilter with EntityResolver with DTDHandler with ContentHandler with ErrorHandler {

  private var parent: XMLReader = _
  private var locator: Locator = _
  private var entityResolver: EntityResolver = _
  private var dtdHandler: DTDHandler = _
  private var contentHandler: ContentHandler = _
  private var errorHandler: ErrorHandler = _

  /**
   * Construct an XML filter with the specified parent.
   *
   * @param parent the XML reader from which this filter receives its events.
   * @see #setParent
   * @see #getParent
   */
  def this(parent: XMLReader) = {
    this()
    setParent(parent)
  }

  /**
   * Set the parent reader.
   *
   * <p>This is the {@link XMLReader XMLReader} from which
   * this filter will obtain its events and to which it will pass its
   * configuration requests.  The parent may itself be another filter.</p>
   *
   * <p>If there is no parent reader set, any attempt to parse
   * or to set or get a feature or property will fail.</p>
   *
   * @param parent The parent XML reader.
   * @see #getParent
   */
  override def setParent(parent: XMLReader): Unit = {
    this.parent = parent
  }

  /**
   * Get the parent reader.
   *
   * @return The parent XML reader, or null if none is set.
   * @see #setParent
   */
  override def getParent(): XMLReader = parent

  /**
   * Set the value of a feature.
   *
   * <p>This will always fail if the parent is null.</p>
   *
   * @param name  The feature name.
   * @param value The requested feature value.
   * @exception SAXNotRecognizedException If the feature
   *            value can't be assigned or retrieved from the parent.
   * @exception SAXNotSupportedException When the
   *            parent recognizes the feature name but
   *            cannot set the requested value.
   */
  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setFeature(name: String, value: Boolean): Unit = {
    if (parent != null) parent.setFeature(name, value)
    else throw new SAXNotRecognizedException("Feature: " + name)
  }

  /**
   * Look up the value of a feature.
   *
   * <p>This will always fail if the parent is null.</p>
   *
   * @param name The feature name.
   * @return The current value of the feature.
   * @exception SAXNotRecognizedException If the feature
   *            value can't be assigned or retrieved from the parent.
   * @exception SAXNotSupportedException When the
   *            parent recognizes the feature name but
   *            cannot determine its value at this time.
   */
  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getFeature(name: String): Boolean = {
    if (parent != null) parent.getFeature(name)
    else throw new SAXNotRecognizedException("Feature: " + name)
  }

  /**
   * Set the value of a property.
   *
   * <p>This will always fail if the parent is null.</p>
   *
   * @param name  The property name.
   * @param value The requested property value.
   * @exception SAXNotRecognizedException If the property
   *            value can't be assigned or retrieved from the parent.
   * @exception SAXNotSupportedException When the
   *            parent recognizes the property name but
   *            cannot set the requested value.
   */
  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setProperty(name: String, value: Any): Unit = {
    if (parent != null) parent.setProperty(name, value)
    else throw new SAXNotRecognizedException("Property: " + name)
  }

  /**
   * Look up the value of a property.
   *
   * @param name The property name.
   * @return The current value of the property.
   * @exception SAXNotRecognizedException If the property
   *            value can't be assigned or retrieved from the parent.
   * @exception SAXNotSupportedException When the
   *            parent recognizes the property name but
   *            cannot determine its value at this time.
   */
  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getProperty(name: String): Any = {
    if (parent != null) parent.getProperty(name)
    else throw new SAXNotRecognizedException("Property: " + name)
  }

  /**
   * Set the entity resolver.
   *
   * @param resolver The new entity resolver.
   */
  override def setEntityResolver(resolver: EntityResolver): Unit = {
    entityResolver = resolver
  }

  /**
   * Get the current entity resolver.
   *
   * @return The current entity resolver, or null if none was set.
   */
  override def getEntityResolver(): EntityResolver = entityResolver

  /**
   * Set the DTD event handler.
   *
   * @param handler the new DTD handler
   */
  override def setDTDHandler(handler: DTDHandler): Unit = {
    dtdHandler = handler
  }

  /**
   * Get the current DTD event handler.
   *
   * @return The current DTD handler, or null if none was set.
   */
  override def getDTDHandler(): DTDHandler = dtdHandler

  /**
   * Set the content event handler.
   *
   * @param handler the new content handler
   */
  override def setContentHandler(handler: ContentHandler): Unit = {
    contentHandler = handler
  }

  /**
   * Get the content event handler.
   *
   * @return The current content handler, or null if none was set.
   */
  override def getContentHandler(): ContentHandler = contentHandler

  /**
   * Set the error event handler.
   *
   * @param handler the new error handler
   */
  override def setErrorHandler(handler: ErrorHandler): Unit = {
    errorHandler = handler
  }

  /**
   * Get the current error event handler.
   *
   * @return The current error handler, or null if none was set.
   */
  override def getErrorHandler(): ErrorHandler = errorHandler

  /**
   * Parse a document.
   *
   * @param input The input source for the document entity.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @exception java.io.IOException An IO exception from the parser,
   *            possibly from a byte stream or character stream
   *            supplied by the application.
   */
  @throws[SAXException]
  @throws[IOException]
  override def parse(input: InputSource): Unit = {
    setupParse()
    parent.parse(input)
  }

  /**
   * Parse a document.
   *
   * @param systemId The system identifier as a fully-qualified URI.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @exception java.io.IOException An IO exception from the parser,
   *            possibly from a byte stream or character stream
   *            supplied by the application.
   */
  @throws[SAXException]
  @throws[IOException]
  override def parse(systemId: String): Unit = parse(new InputSource(systemId))

  /**
   * Filter an external entity resolution.
   *
   * @param publicId The entity's public identifier, or null.
   * @param systemId The entity's system identifier.
   * @return A new InputSource or null for the default.
   * @exception SAXException The client may throw
   *            an exception during processing.
   * @exception java.io.IOException The client may throw an
   *            I/O-related exception while obtaining the
   *            new InputSource.
   */
  @throws[SAXException]
  @throws[IOException]
  override def resolveEntity(publicId: String, systemId: String): InputSource = {
    if (entityResolver != null) entityResolver.resolveEntity(publicId, systemId)
    else null
  }

  /**
   * Filter a notation declaration event.
   *
   * @param name     The notation name.
   * @param publicId The notation's public identifier, or null.
   * @param systemId The notation's system identifier, or null.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def notationDecl(name: String, publicId: String, systemId: String): Unit = {
    if (dtdHandler != null) dtdHandler.notationDecl(name, publicId, systemId)
  }

  /**
   * Filter an unparsed entity declaration event.
   *
   * @param name         The entity name.
   * @param publicId     The entity's public identifier, or null.
   * @param systemId     The entity's system identifier, or null.
   * @param notationName The name of the associated notation.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def unparsedEntityDecl(name: String, publicId: String, systemId: String, notationName: String): Unit = {
    if (dtdHandler != null) dtdHandler.unparsedEntityDecl(name, publicId, systemId, notationName)
  }

  /**
   * Filter a new document locator event.
   *
   * @param locator The document locator.
   */
  override def setDocumentLocator(locator: Locator): Unit = {
    this.locator = locator
    if (contentHandler != null) contentHandler.setDocumentLocator(locator)
  }

  /**
   * Filter a start document event.
   *
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def startDocument(): Unit = {
    if (contentHandler != null) contentHandler.startDocument()
  }

  /**
   * Filter an end document event.
   *
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def endDocument(): Unit = {
    if (contentHandler != null) contentHandler.endDocument()
  }

  /**
   * Filter a start Namespace prefix mapping event.
   *
   * @param prefix The Namespace prefix.
   * @param uri    The Namespace URI.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def startPrefixMapping(prefix: String, uri: String): Unit = {
    if (contentHandler != null) contentHandler.startPrefixMapping(prefix, uri)
  }

  /**
   * Filter an end Namespace prefix mapping event.
   *
   * @param prefix The Namespace prefix.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def endPrefixMapping(prefix: String): Unit = {
    if (contentHandler != null) contentHandler.endPrefixMapping(prefix)
  }

  /**
   * Filter a start element event.
   *
   * @param uri       The element's Namespace URI, or the empty string.
   * @param localName The element's local name, or the empty string.
   * @param qName     The element's qualified (prefixed) name, or the empty
   *                  string.
   * @param atts      The element's attributes.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def startElement(uri: String, localName: String, qName: String, atts: Attributes): Unit = {
    if (contentHandler != null) contentHandler.startElement(uri, localName, qName, atts)
  }

  /**
   * Filter an end element event.
   *
   * @param uri       The element's Namespace URI, or the empty string.
   * @param localName The element's local name, or the empty string.
   * @param qName     The element's qualified (prefixed) name, or the empty
   *                  string.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def endElement(uri: String, localName: String, qName: String): Unit = {
    if (contentHandler != null) contentHandler.endElement(uri, localName, qName)
  }

  /**
   * Filter a character data event.
   *
   * @param ch     An array of characters.
   * @param start  The starting position in the array.
   * @param length The number of characters to use from the array.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
    if (contentHandler != null) contentHandler.characters(ch, start, length)
  }

  /**
   * Filter an ignorable whitespace event.
   *
   * @param ch     An array of characters.
   * @param start  The starting position in the array.
   * @param length The number of characters to use from the array.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = {
    if (contentHandler != null) contentHandler.ignorableWhitespace(ch, start, length)
  }

  /**
   * Filter a processing instruction event.
   *
   * @param target The processing instruction target.
   * @param data   The text following the target.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def processingInstruction(target: String, data: String): Unit = {
    if (contentHandler != null) contentHandler.processingInstruction(target, data)
  }

  /**
   * Filter a skipped entity event.
   *
   * @param name The name of the skipped entity.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def skippedEntity(name: String): Unit = {
    if (contentHandler != null) contentHandler.skippedEntity(name)
  }

  /**
   * Filter a warning event.
   *
   * @param e The warning as an exception.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def warning(e: SAXParseException): Unit = {
    if (errorHandler != null) errorHandler.warning(e)
  }

  /**
   * Filter an error event.
   *
   * @param e The error as an exception.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def error(e: SAXParseException): Unit = {
    if (errorHandler != null) errorHandler.error(e)
  }

  /**
   * Filter a fatal error event.
   *
   * @param e The error as an exception.
   * @exception SAXException The client may throw
   *            an exception during processing.
   */
  @throws[SAXException]
  override def fatalError(e: SAXParseException): Unit = {
    if (errorHandler != null) errorHandler.fatalError(e)
  }

  /**
   * Set up before a parse.
   *
   * <p>Before every parse, check whether the parent is
   * non-null, and re-register the filter for all of the
   * events.</p>
   */
  private def setupParse(): Unit = {
    if (parent == null) throw new NullPointerException("No parent for filter")
    parent.setEntityResolver(this)
    parent.setDTDHandler(this)
    parent.setContentHandler(this)
    parent.setErrorHandler(this)
  }

}