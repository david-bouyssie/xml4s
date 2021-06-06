// Locator2Impl.java - extended LocatorImpl
// http://www.saxproject.org
// Public Domain: no warranty.
// $Id: Locator2Impl.java,v 1.3 2004/04/26 17:34:35 dmegginson Exp $

package org.xml.sax.ext

import org.xml.sax.Locator
import org.xml.sax.ext.Locator2
import org.xml.sax.helpers.LocatorImpl

/**
 * SAX2 extension helper for holding additional Entity information,
 * implementing the {@link Locator2} interface.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p> This is not part of core-only SAX2 distributions.</p>
 *
 * @since SAX 2.0.2
 * @author David Brownell
 * @version TBS
 *
 * Construct a new, empty Locator2Impl object.
 * This will not normally be useful, since the main purpose
 * of this class is to make a snapshot of an existing Locator.
 */
class Locator2Impl() extends LocatorImpl with Locator2 {

  private var encoding: String = _
  private var version: String = _

  /**
   * Copy an existing Locator or Locator2 object.
   * If the object implements Locator2, values of the
   * <em>encoding</em> and <em>version</em>strings are copied,
   * otherwise they set to <em>null</em>.
   *
   * @param locator The existing Locator object.
   */
  def this(locator: Locator) = {
    this()
    locator match {
      case l2: Locator2 =>
        version = l2.getXMLVersion()
        encoding = l2.getEncoding()
      case _ =>
    }
  }

  /**
   * Returns the current value of the version property.
   *
   * @return the current value of the version property.
   * @see #setXMLVersion
   */
  override def getXMLVersion(): String = version

  /**
   * Returns the current value of the encoding property.
   *
   * @return the current value of the encoding property.
   * @see #setEncoding
   */
  override def getEncoding(): String = encoding

  /**
   * Assigns the current value of the version property.
   *
   * @param version the new "version" value
   * @see #getXMLVersion
   */
  def setXMLVersion(version: String): Unit = {
    this.version = version
  }

  /**
   * Assigns the current value of the encoding property.
   *
   * @param encoding the new "encoding" value
   * @see #getEncoding
   */
  def setEncoding(encoding: String): Unit = {
    this.encoding = encoding
  }
}