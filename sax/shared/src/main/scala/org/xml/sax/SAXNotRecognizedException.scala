// SAXNotRecognizedException.java - unrecognized feature or value.
// http://www.saxproject.org
// Written by David Megginson
// NO WARRANTY!  This class is in the Public Domain.
// $Id: SAXNotRecognizedException.java,v 1.7 2002/01/30 21:13:48 dbrownell Exp $
package org.xml.sax

/**
 * Exception class for an unrecognized identifier.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>An XMLReader will throw this exception when it finds an
 * unrecognized feature or property identifier; SAX applications and
 * extensions may use this class for other, similar purposes.</p>
 *
 * @since SAX 2.0
 * @author David Megginson
 * @version 2.0.1 (sax2r2)
 * @see SAXNotSupportedException
 *
 * Construct a new exception with the given message.
 * @param message The text message of the exception.
 */
class SAXNotRecognizedException(
  private val message: String,
) extends SAXException(message, null) {

  /**
   * No argument constructor.
   */
  // FIXME: keep me ?
  def this() = {
    this(null: String)
  }
}