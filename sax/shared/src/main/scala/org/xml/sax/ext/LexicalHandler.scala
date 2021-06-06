// LexicalHandler.java - optional handler for lexical parse events.
// http://www.saxproject.org
// Public Domain: no warranty.
// $Id: LexicalHandler.java,v 1.5 2002/01/30 21:00:44 dbrownell Exp $
package org.xml.sax.ext

import org.xml.sax.ext.DeclHandler
import org.xml.sax.SAXException

/**
 * SAX2 extension handler for lexical events.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>This is an optional extension handler for SAX2 to provide
 * lexical information about an XML document, such as comments
 * and CDATA section boundaries.
 * XML readers are not required to recognize this handler, and it
 * is not part of core-only SAX2 distributions.</p>
 *
 * <p>The events in the lexical handler apply to the entire document,
 * not just to the document element, and all lexical handler events
 * must appear between the content handler's startDocument and
 * endDocument events.</p>
 *
 * <p>To set the LexicalHandler for an XML reader, use the
 * {@link XMLReader# setProperty setProperty} method
 * with the property name
 * <code>http://xml.org/sax/properties/lexical-handler</code>
 * and an object implementing this interface (or null) as the value.
 * If the reader does not report lexical events, it will throw a
 * {@link SAXNotRecognizedException SAXNotRecognizedException}
 * when you attempt to register the handler.</p>
 *
 * @since SAX 2.0 (extensions 1.0)
 * @author David Megginson
 * @version 2.0.1 (sax2r2)
 */
trait LexicalHandler {

  /**
   * Report the start of DTD declarations, if any.
   *
   * <p>This method is intended to report the beginning of the
   * DOCTYPE declaration; if the document has no DOCTYPE declaration,
   * this method will not be invoked.</p>
   *
   * <p>All declarations reported through
   * {@link DTDHandler DTDHandler} or
   * {@link DeclHandler DeclHandler} events must appear
   * between the startDTD and {@link #endDTD endDTD} events.
   * Declarations are assumed to belong to the internal DTD subset
   * unless they appear between {@link #startEntity startEntity}
   * and {@link #endEntity endEntity} events.  Comments and
   * processing instructions from the DTD should also be reported
   * between the startDTD and endDTD events, in their original
   * order of (logical) occurrence; they are not required to
   * appear in their correct locations relative to DTDHandler
   * or DeclHandler events, however.</p>
   *
   * <p>Note that the start/endDTD events will appear within
   * the start/endDocument events from ContentHandler and
   * before the first
   * {@link ContentHandler# startElement startElement}
   * event.</p>
   *
   * @param name     The document type name.
   * @param publicId The declared public identifier for the
   *                 external DTD subset, or null if none was declared.
   * @param systemId The declared system identifier for the
   *                 external DTD subset, or null if none was declared.
   *                 (Note that this is not resolved against the document
   *                 base URI.)
   * @exception SAXException The application may raise an
   *            exception.
   * @see #endDTD
   * @see #startEntity
   */
  @throws[SAXException]
  def startDTD(name: String, publicId: String, systemId: String): Unit

  /**
   * Report the end of DTD declarations.
   *
   * <p>This method is intended to report the end of the
   * DOCTYPE declaration; if the document has no DOCTYPE declaration,
   * this method will not be invoked.</p>
   *
   * @exception SAXException The application may raise an exception.
   * @see #startDTD
   */
  @throws[SAXException]
  def endDTD(): Unit

  /**
   * Report the beginning of some internal and external XML entities.
   *
   * <p>The reporting of parameter entities (including
   * the external DTD subset) is optional, and SAX2 drivers that
   * report LexicalHandler events may not implement it; you can use the
   * <code
   * >http://xml.org/sax/features/lexical-handler/parameter-entities</code>
   * feature to query or control the reporting of parameter entities.</p>
   *
   * <p>General entities are reported with their regular names,
   * parameter entities have '%' prepended to their names, and
   * the external DTD subset has the pseudo-entity name "[dtd]".</p>
   *
   * <p>When a SAX2 driver is providing these events, all other
   * events must be properly nested within start/end entity
   * events.  There is no additional requirement that events from
   * {@link DeclHandler DeclHandler} or
   * {@link DTDHandler DTDHandler} be properly ordered.</p>
   *
   * <p>Note that skipped entities will be reported through the
   * {@link ContentHandler# skippedEntity skippedEntity}
   * event, which is part of the ContentHandler interface.</p>
   *
   * <p>Because of the streaming event model that SAX uses, some
   * entity boundaries cannot be reported under any
   * circumstances:</p>
   *
   * <ul>
   * <li>general entities within attribute values</li>
   * <li>parameter entities within declarations</li>
   * </ul>
   *
   * <p>These will be silently expanded, with no indication of where
   * the original entity boundaries were.</p>
   *
   * <p>Note also that the boundaries of character references (which
   * are not really entities anyway) are not reported.</p>
   *
   * <p>All start/endEntity events must be properly nested.
   *
   * @param name The name of the entity.  If it is a parameter
   *             entity, the name will begin with '%', and if it is the
   *             external DTD subset, it will be "[dtd]".
   * @exception SAXException The application may raise an exception.
   * @see #endEntity
   * @see DeclHandler#internalEntityDecl
   * @see DeclHandler#externalEntityDecl
   */
  @throws[SAXException]
  def startEntity(name: String): Unit

  /**
   * Report the end of an entity.
   *
   * @param name The name of the entity that is ending.
   * @exception SAXException The application may raise an exception.
   * @see #startEntity
   */
  @throws[SAXException]
  def endEntity(name: String): Unit

  /**
   * Report the start of a CDATA section.
   *
   * <p>The contents of the CDATA section will be reported through
   * the regular {@link ContentHandler# characters
   * characters} event; this event is intended only to report
   * the boundary.</p>
   *
   * @exception SAXException The application may raise an exception.
   * @see #endCDATA
   */
  @throws[SAXException]
  def startCDATA(): Unit

  /**
   * Report the end of a CDATA section.
   *
   * @exception SAXException The application may raise an exception.
   * @see #startCDATA
   */
  @throws[SAXException]
  def endCDATA(): Unit

  /**
   * Report an XML comment anywhere in the document.
   *
   * <p>This callback will be used for comments inside or outside the
   * document element, including comments in the external DTD
   * subset (if read).  Comments in the DTD must be properly
   * nested inside start/endDTD and start/endEntity events (if
   * used).</p>
   *
   * @param ch     An array holding the characters in the comment.
   * @param start  The starting position in the array.
   * @param length The number of characters to use from the array.
   * @exception SAXException The application may raise an exception.
   */
  @throws[SAXException]
  def comment(ch: Array[Char], start: Int, length: Int): Unit
}