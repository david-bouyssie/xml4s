// DefaultHandler.java - default implementation of the core handlers.
// http://www.saxproject.org
// Written by David Megginson
// NO WARRANTY!  This class is in the public domain.
// $Id: DefaultHandler.java,v 1.9 2004/04/26 17:34:35 dmegginson Exp $
package org.xml.sax.helpers

import org.xml.sax._

import java.io.IOException

/**
 * Default base class for SAX2 event handlers.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>This class is available as a convenience base class for SAX2
 * applications: it provides default implementations for all of the
 * callbacks in the four core SAX2 handler classes:</p>
 *
 * <ul>
 * <li>{@link EntityResolver EntityResolver}</li>
 * <li>{@link DTDHandler DTDHandler}</li>
 * <li>{@link ContentHandler ContentHandler}</li>
 * <li>{@link ErrorHandler ErrorHandler}</li>
 * </ul>
 *
 * <p>Application writers can extend this class when they need to
 * implement only part of an interface; parser writers can
 * instantiate this class to provide default handlers when the
 * application has not supplied its own.</p>
 *
 * <p>This class replaces the deprecated SAX1
 * {@link HandlerBase HandlerBase} class.</p>
 *
 * @since SAX 2.0
 * @author David Megginson,
 * @version 2.0.1 (sax2r2)
 * @see EntityResolver
 * @see DTDHandler
 * @see ContentHandler
 * @see ErrorHandler
 */
class DefaultHandler extends EntityResolver with DTDHandler with ContentHandler with ErrorHandler {
  /**
   * Resolve an external entity.
   *
   * <p>Always return null, so that the parser will use the system
   * identifier provided in the XML document.  This method implements
   * the SAX default behaviour: application writers can override it
   * in a subclass to do special translations such as catalog lookups
   * or URI redirection.</p>
   *
   * @param publicId The public identifier, or null if none is
   *                 available.
   * @param systemId The system identifier provided in the XML
   *                 document.
   * @return The new input source, or null to require the
   *         default behaviour.
   * @exception java.io.IOException If there is an error setting
   *            up the new input source.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see EntityResolver#resolveEntity
   */
  @throws[IOException]
  @throws[SAXException]
  override def resolveEntity(publicId: String, systemId: String): InputSource = null

  /**
   * Receive notification of a notation declaration.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass if they wish to keep track of the notations
   * declared in a document.</p>
   *
   * @param name     The notation name.
   * @param publicId The notation public identifier, or null if not
   *                 available.
   * @param systemId The notation system identifier.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see DTDHandler#notationDecl
   */
  @throws[SAXException]
  override def notationDecl(name: String, publicId: String, systemId: String): Unit = {
    // no op
  }

  /**
   * Receive notification of an unparsed entity declaration.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass to keep track of the unparsed entities
   * declared in a document.</p>
   *
   * @param name         The entity name.
   * @param publicId     The entity public identifier, or null if not
   *                     available.
   * @param systemId     The entity system identifier.
   * @param notationName The name of the associated notation.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see DTDHandler#unparsedEntityDecl
   */
  @throws[SAXException]
  override def unparsedEntityDecl(name: String, publicId: String, systemId: String, notationName: String): Unit = {
  }

  /**
   * Receive a Locator object for document events.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass if they wish to store the locator for use
   * with other document events.</p>
   *
   * @param locator A locator for all SAX document events.
   * @see ContentHandler#setDocumentLocator
   * @see Locator
   */
  override def setDocumentLocator(locator: Locator): Unit = {
  }

  /**
   * Receive notification of the beginning of the document.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass to take specific actions at the beginning
   * of a document (such as allocating the root node of a tree or
   * creating an output file).</p>
   *
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#startDocument
   */
  @throws[SAXException]
  override def startDocument(): Unit = {
  }

  /**
   * Receive notification of the end of the document.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass to take specific actions at the end
   * of a document (such as finalising a tree or closing an output
   * file).</p>
   *
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#endDocument
   */
  @throws[SAXException]
  override def endDocument(): Unit = {
  }

  /**
   * Receive notification of the start of a Namespace mapping.
   *
   * <p>By default, do nothing. Application writers may override this
   * method in a subclass to take specific actions at the start of
   * each Namespace prefix scope (such as storing the prefix mapping).</p>
   *
   * @param prefix The Namespace prefix being declared.
   * @param uri    The Namespace URI mapped to the prefix.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#startPrefixMapping
   */
  @throws[SAXException]
  override def startPrefixMapping(prefix: String, uri: String): Unit = {
  }

  /**
   * Receive notification of the end of a Namespace mapping.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass to take specific actions at the end of
   * each prefix mapping.</p>
   *
   * @param prefix The Namespace prefix being declared.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#endPrefixMapping
   */
  @throws[SAXException]
  override def endPrefixMapping(prefix: String): Unit = {
  }

  /**
   * Receive notification of the start of an element.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass to take specific actions at the start of
   * each element (such as allocating a new tree node or writing
   * output to a file).</p>
   *
   * @param uri        The Namespace URI, or the empty string if the
   *                   element has no Namespace URI or if Namespace
   *                   processing is not being performed.
   * @param localName  The local name (without prefix), or the
   *                   empty string if Namespace processing is not being
   *                   performed.
   * @param qName      The qualified name (with prefix), or the
   *                   empty string if qualified names are not available.
   * @param attributes The attributes attached to the element.  If
   *                   there are no attributes, it shall be an empty
   *                   Attributes object.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#startElement
   */
  @throws[SAXException]
  override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
  }

  /**
   * Receive notification of the end of an element.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass to take specific actions at the end of
   * each element (such as finalising a tree node or writing
   * output to a file).</p>
   *
   * @param uri       The Namespace URI, or the empty string if the
   *                  element has no Namespace URI or if Namespace
   *                  processing is not being performed.
   * @param localName The local name (without prefix), or the
   *                  empty string if Namespace processing is not being
   *                  performed.
   * @param qName     The qualified name (with prefix), or the
   *                  empty string if qualified names are not available.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#endElement
   */
  @throws[SAXException]
  override def endElement(uri: String, localName: String, qName: String): Unit = {
  }

  /**
   * Receive notification of character data inside an element.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method to take specific actions for each chunk of character data
   * (such as adding the data to a node or buffer, or printing it to
   * a file).</p>
   *
   * @param ch     The characters.
   * @param start  The start position in the character array.
   * @param length The number of characters to use from the
   *               character array.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#characters
   */
  @throws[SAXException]
  override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
  }

  /**
   * Receive notification of ignorable whitespace in element content.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method to take specific actions for each chunk of ignorable
   * whitespace (such as adding data to a node or buffer, or printing
   * it to a file).</p>
   *
   * @param ch     The whitespace characters.
   * @param start  The start position in the character array.
   * @param length The number of characters to use from the
   *               character array.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#ignorableWhitespace
   */
  @throws[SAXException]
  override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = {
  }

  /**
   * Receive notification of a processing instruction.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass to take specific actions for each
   * processing instruction, such as setting status variables or
   * invoking other methods.</p>
   *
   * @param target The processing instruction target.
   * @param data   The processing instruction data, or null if
   *               none is supplied.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#processingInstruction
   */
  @throws[SAXException]
  override def processingInstruction(target: String, data: String): Unit = {
  }

  /**
   * Receive notification of a skipped entity.
   *
   * <p>By default, do nothing.  Application writers may override this
   * method in a subclass to take specific actions for each
   * processing instruction, such as setting status variables or
   * invoking other methods.</p>
   *
   * @param name The name of the skipped entity.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ContentHandler#processingInstruction
   */
  @throws[SAXException]
  override def skippedEntity(name: String): Unit = {
  }

  /**
   * Receive notification of a parser warning.
   *
   * <p>The default implementation does nothing.  Application writers
   * may override this method in a subclass to take specific actions
   * for each warning, such as inserting the message in a log file or
   * printing it to the console.</p>
   *
   * @param e The warning information encoded as an exception.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ErrorHandler#warning
   * @see SAXParseException
   */
  @throws[SAXException]
  override def warning(e: SAXParseException): Unit = {
  }

  /**
   * Receive notification of a recoverable parser error.
   *
   * <p>The default implementation does nothing.  Application writers
   * may override this method in a subclass to take specific actions
   * for each error, such as inserting the message in a log file or
   * printing it to the console.</p>
   *
   * @param e The warning information encoded as an exception.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ErrorHandler#warning
   * @see SAXParseException
   */
  @throws[SAXException]
  override def error(e: SAXParseException): Unit = {
  }

  /**
   * Report a fatal XML parsing error.
   *
   * <p>The default implementation throws a SAXParseException.
   * Application writers may override this method in a subclass if
   * they need to take specific actions for each fatal error (such as
   * collecting all of the errors into a single report): in any case,
   * the application must stop all regular processing when this
   * method is invoked, since the document is no longer reliable, and
   * the parser may no longer report parsing events.</p>
   *
   * @param e The error information encoded as an exception.
   * @exception SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see ErrorHandler#fatalError
   * @see SAXParseException
   */
  @throws[SAXException]
  override def fatalError(e: SAXParseException) = throw e
}