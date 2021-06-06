// SAX default implementation for AttributeList.
// http://www.saxproject.org
// No warranty; no copyright -- use this as you will.
// $Id: AttributeListImpl.java,v 1.6 2002/01/30 20:52:22 dbrownell Exp $
package org.xml.sax.helpers

import org.xml.sax.{AttributeList, Attributes}

import java.util

/**
 * Default implementation for AttributeList.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>AttributeList implements the deprecated SAX1 {@link
 * AttributeList AttributeList} interface, and has been
 * replaced by the new SAX2 {@link AttributesImpl
 * AttributesImpl} interface.</p>
 *
 * <p>This class provides a convenience implementation of the SAX
 * {@link org.xml.sax.AttributeList AttributeList} interface.  This
 * implementation is useful both for SAX parser writers, who can use
 * it to provide attributes to the application, and for SAX application
 * writers, who can use it to create a persistent copy of an element's
 * attribute specifications:</p>
 *
 * <pre>
 * private AttributeList myatts;
 *
 * public void startElement (String name, AttributeList atts)
 * {
 * // create a persistent copy of the attribute list
 * // for use outside this method
 * myatts = new AttributeListImpl(atts);
 * [...]
 * }
 * </pre>
 *
 * <p>Please note that SAX parsers are not required to use this
 * class to provide an implementation of AttributeList; it is
 * supplied only as an optional convenience.  In particular,
 * parser writers are encouraged to invent more efficient
 * implementations.</p>
 *
 * @deprecated This class implements a deprecated interface,
 *             {@link AttributeList AttributeList};
 *             that interface has been replaced by
 *             {@link Attributes Attributes},
 *             which is implemented in the
 *             {@link AttributesImpl AttributesImpl} helper class.
 * @since      SAX 1.0
 * @author     David Megginson
 * @version    2.0.1 (sax2r2)
 * @see        org.xml.sax.AttributeList
 * @see        org.xml.sax.DocumentHandler#startElement
 *
 * Create an empty attribute list.
 *
 * <p>This constructor is most useful for parser writers, who
 * will use it to create a single, reusable attribute list that
 * can be reset with the clear method between elements.</p>
 *
 * @see #addAttribute
 * @see #clear
 */
@deprecated class AttributeListImpl() extends AttributeList {
// TODO: DBO => delete me?

  private val names = new util.ArrayList[String]
  private val types = new util.ArrayList[String]
  private val values = new util.ArrayList[String]

  /**
   * Construct a persistent copy of an existing attribute list.
   *
   * <p>This constructor is most useful for application writers,
   * who will use it to create a persistent copy of an existing
   * attribute list.</p>
   *
   * @param atts The attribute list to copy
   * @see org.xml.sax.DocumentHandler#startElement
   */
  def this(atts: AttributeList) = {
    this()
    setAttributeList(atts)
  }

  /**
   * Set the attribute list, discarding previous contents.
   *
   * <p>This method allows an application writer to reuse an
   * attribute list easily.</p>
   *
   * @param atts The attribute list to copy.
   */
  def setAttributeList(atts: AttributeList): Unit = {
    val count = atts.getLength()
    clear()
    for (i <- 0 until count) {
      addAttribute(atts.getName(i), atts.getType(i), atts.getValue(i))
    }
  }

  /**
   * Add an attribute to an attribute list.
   *
   * <p>This method is provided for SAX parser writers, to allow them
   * to build up an attribute list incrementally before delivering
   * it to the application.</p>
   *
   * @param name  The attribute name.
   * @param type  The attribute type ("NMTOKEN" for an enumeration).
   * @param value The attribute value (must not be null).
   * @see #removeAttribute
   * @see org.xml.sax.DocumentHandler#startElement
   */
  def addAttribute(name: String, `type`: String, value: String): Unit = {
    names.add(name)
    types.add(`type`)
    values.add(value)
  }

  /**
   * Remove an attribute from the list.
   *
   * <p>SAX application writers can use this method to filter an
   * attribute out of an AttributeList.  Note that invoking this
   * method will change the length of the attribute list and
   * some of the attribute's indices.</p>
   *
   * <p>If the requested attribute is not in the list, this is a no-op.</p>
   *
   * @param name The attribute name.
   * @see #addAttribute
   */
  def removeAttribute(name: String): Unit = {
    val i = names.indexOf(name)
    if (i != -1) {
      names.remove(i)
      types.remove(i)
      values.remove(i)
    }
  }

  /**
   * Clear the attribute list.
   *
   * <p>SAX parser writers can use this method to reset the attribute
   * list between DocumentHandler.startElement events.  Normally,
   * it will make sense to reuse the same AttributeListImpl object
   * rather than allocating a new one each time.</p>
   *
   * @see org.xml.sax.DocumentHandler#startElement
   */
  def clear(): Unit = {
    names.clear()
    types.clear()
    values.clear()
  }

  /**
   * Return the number of attributes in the list.
   *
   * @return The number of attributes in the list.
   * @see org.xml.sax.AttributeList#getLength
   */
  override def getLength(): Int = names.size

  /**
   * Get the name of an attribute (by position).
   *
   * @param i The position of the attribute in the list.
   * @return The attribute name as a string, or null if there
   *         is no attribute at that position.
   * @see org.xml.sax.AttributeList#getName(int)
   */
  override def getName(i: Int): String = {
    if (i < 0 || i >= names.size) return null
    names.get(i)
  }

  /**
   * Get the type of an attribute (by position).
   *
   * @param i The position of the attribute in the list.
   * @return The attribute type as a string ("NMTOKEN" for an
   *         enumeration, and "CDATA" if no declaration was
   *         read), or null if there is no attribute at
   *         that position.
   * @see org.xml.sax.AttributeList#getType(int)
   */
  override def getType(i: Int): String = {
    if (i < 0 || i >= types.size) return null
    types.get(i)
  }

  /**
   * Get the value of an attribute (by position).
   *
   * @param i The position of the attribute in the list.
   * @return The attribute value as a string, or null if
   *         there is no attribute at that position.
   * @see org.xml.sax.AttributeList#getValue(int)
   */
  override def getValue(i: Int): String = {
    if (i < 0 || i >= values.size) return null
    values.get(i)
  }

  /**
   * Get the type of an attribute (by name).
   *
   * @param name The attribute name.
   * @return The attribute type as a string ("NMTOKEN" for an
   *         enumeration, and "CDATA" if no declaration was
   *         read).
   * @see org.xml.sax.AttributeList#getType(java.lang.String)
   */
  override def getType(name: String): String = getType(names.indexOf(name))

  /**
   * Get the value of an attribute (by name).
   *
   * @param name The attribute name.
   * @return the named attribute's value or null, if the attribute does not
   *         exist.
   * @see org.xml.sax.AttributeList#getValue(java.lang.String)
   */
  override def getValue(name: String): String = getValue(names.indexOf(name))

}