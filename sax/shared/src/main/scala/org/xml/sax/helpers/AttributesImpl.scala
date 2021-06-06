// AttributesImpl.java - default implementation of Attributes.
// http://www.saxproject.org
// Written by David Megginson
// NO WARRANTY!  This class is in the public domain.
// $Id: AttributesImpl.java,v 1.9 2002/01/30 20:52:24 dbrownell Exp $
package org.xml.sax.helpers

import org.xml.sax.{AttributeList, Attributes, ContentHandler}

/**
 * Default implementation of the Attributes interface.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>This class provides a default implementation of the SAX2
 * {@link Attributes Attributes} interface, with the
 * addition of manipulators so that the list can be modified or
 * reused.</p>
 *
 * <p>There are two typical uses of this class:</p>
 *
 * <ol>
 * <li>to take a persistent snapshot of an Attributes object
 * in a {@link ContentHandler# startElement startElement} event; or</li>
 * <li>to construct or modify an Attributes object in a SAX2 driver or filter.</li>
 * </ol>
 *
 * <p>This class replaces the now-deprecated SAX1 {@link
 * org.xml.sax.helpers.AttributeListImpl AttributeListImpl}
 * class; in addition to supporting the updated Attributes
 * interface rather than the deprecated {@link AttributeList
 * AttributeList} interface, it also includes a much more efficient
 * implementation using a single array rather than a set of Vectors.</p>
 *
 * @since SAX 2.0
 * @author David Megginson
 * @version 2.0.1 (sax2r2)
 *
 * Construct a new, empty AttributesImpl object.
 */
class AttributesImpl() extends Attributes {

  private[helpers] var length = 0
  private[helpers] var data: Array[String] = _

  /**
   * Copy an existing Attributes object.
   *
   * <p>This constructor is especially useful inside a
   * {@link ContentHandler# startElement startElement} event.</p>
   *
   * @param atts The existing Attributes object.
   */
  def this(atts: Attributes) {
    this()
    setAttributes(atts)
  }

  /**
   * Return the number of attributes in the list.
   *
   * @return The number of attributes in the list.
   * @see org.xml.sax.Attributes#getLength
   */
  override def getLength(): Int = length

  /**
   * Return an attribute's Namespace URI.
   *
   * @param index The attribute's index (zero-based).
   * @return The Namespace URI, the empty string if none is
   *         available, or null if the index is out of range.
   * @see org.xml.sax.Attributes#getURI
   */
  override def getURI(index: Int): String = {
    if (index >= 0 && index < length) data(index * 5)
    else null
  }

  /**
   * Return an attribute's local name.
   *
   * @param index The attribute's index (zero-based).
   * @return The attribute's local name, the empty string if
   *         none is available, or null if the index if out of range.
   * @see org.xml.sax.Attributes#getLocalName
   */
  override def getLocalName(index: Int): String = {
    if (index >= 0 && index < length) data(index * 5 + 1)
    else null
  }

  /**
   * Return an attribute's qualified (prefixed) name.
   *
   * @param index The attribute's index (zero-based).
   * @return The attribute's qualified name, the empty string if
   *         none is available, or null if the index is out of bounds.
   * @see org.xml.sax.Attributes#getQName
   */
  override def getQName(index: Int): String = {
    if (index >= 0 && index < length) data(index * 5 + 2)
    else null
  }

  /**
   * Return an attribute's type by index.
   *
   * @param index The attribute's index (zero-based).
   * @return The attribute's type, "CDATA" if the type is unknown, or null
   *         if the index is out of bounds.
   * @see org.xml.sax.Attributes#getType(int)
   */
  override def getType(index: Int): String = {
    if (index >= 0 && index < length) data(index * 5 + 3)
    else null
  }

  /**
   * Return an attribute's value by index.
   *
   * @param index The attribute's index (zero-based).
   * @return The attribute's value or null if the index is out of bounds.
   * @see org.xml.sax.Attributes#getValue(int)
   */
  override def getValue(index: Int): String = {
    if (index >= 0 && index < length) data(index * 5 + 4)
    else null
  }

  /**
   * Look up an attribute's index by Namespace name.
   *
   * <p>In many cases, it will be more efficient to look up the name once and
   * use the index query methods rather than using the name query methods
   * repeatedly.</p>
   *
   * @param uri       The attribute's Namespace URI, or the empty
   *                  string if none is available.
   * @param localName The attribute's local name.
   * @return The attribute's index, or -1 if none matches.
   * @see org.xml.sax.Attributes#getIndex(java.lang.String,java.lang.String)
   */
  override def getIndex(uri: String, localName: String): Int = {
    val max = length * 5
    var i = 0
    while (i < max) {
      if (data(i) == uri && data(i + 1) == localName) return i / 5
      i += 5
    }
    -1
  }

  /**
   * Look up an attribute's index by qualified (prefixed) name.
   *
   * @param qName The qualified name.
   * @return The attribute's index, or -1 if none matches.
   * @see org.xml.sax.Attributes#getIndex(java.lang.String)
   */
  override def getIndex(qName: String): Int = {
    val max = length * 5
    var i = 0
    while (i < max) {
      if (data(i + 2) == qName) return i / 5
      i += 5
    }
    -1
  }

  /**
   * Look up an attribute's type by Namespace-qualified name.
   *
   * @param uri       The Namespace URI, or the empty string for a name
   *                  with no explicit Namespace URI.
   * @param localName The local name.
   * @return The attribute's type, or null if there is no
   *         matching attribute.
   * @see org.xml.sax.Attributes#getType(java.lang.String,java.lang.String)
   */
  override def getType(uri: String, localName: String): String = {
    val max = length * 5
    var i = 0
    while (i < max) {
      if (data(i) == uri && data(i + 1) == localName) return data(i + 3)
      i += 5
    }
    null
  }

  /**
   * Look up an attribute's type by qualified (prefixed) name.
   *
   * @param qName The qualified name.
   * @return The attribute's type, or null if there is no
   *         matching attribute.
   * @see org.xml.sax.Attributes#getType(java.lang.String)
   */
  override def getType(qName: String): String = {
    val max = length * 5
    var i = 0
    while (i < max) {
      if (data(i + 2) == qName) return data(i + 3)
      i += 5
    }
    null
  }

  /**
   * Look up an attribute's value by Namespace-qualified name.
   *
   * @param uri       The Namespace URI, or the empty string for a name
   *                  with no explicit Namespace URI.
   * @param localName The local name.
   * @return The attribute's value, or null if there is no
   *         matching attribute.
   * @see org.xml.sax.Attributes#getValue(java.lang.String,java.lang.String)
   */
  override def getValue(uri: String, localName: String): String = {
    val max = length * 5
    var i = 0
    while (i < max) {
      if (data(i) == uri && data(i + 1) == localName) return data(i + 4)
      i += 5
    }
    null
  }

  /**
   * Look up an attribute's value by qualified (prefixed) name.
   *
   * @param qName The qualified name.
   * @return The attribute's value, or null if there is no
   *         matching attribute.
   * @see org.xml.sax.Attributes#getValue(java.lang.String)
   */
  override def getValue(qName: String): String = {
    val max = length * 5
    var i = 0
    while (i < max) {
      if (data(i + 2) == qName) return data(i + 4)
      i += 5
    }
    null
  }

  /**
   * Clear the attribute list for reuse.
   *
   * <p>Note that little memory is freed by this call:
   * the current array is kept so it can be
   * reused.</p>
   */
  def clear(): Unit = {
    if (data != null)
      for (i <- 0 until length * 5)
        data(i) = null

    length = 0
  }

  /**
   * Copy an entire Attributes object.
   *
   * <p>It may be more efficient to reuse an existing object
   * rather than constantly allocating new ones.</p>
   *
   * @param atts The attributes to copy.
   */
  def setAttributes(atts: Attributes): Unit = {
    clear()
    length = atts.getLength()
    if (length > 0) {
      data = new Array[String](length * 5)
      for (i <- 0 until length) {
        data(i * 5) = atts.getURI(i)
        data(i * 5 + 1) = atts.getLocalName(i)
        data(i * 5 + 2) = atts.getQName(i)
        data(i * 5 + 3) = atts.getType(i)
        data(i * 5 + 4) = atts.getValue(i)
      }
    }
  }

  /**
   * Add an attribute to the end of the list.
   *
   * <p>For the sake of speed, this method does no checking
   * to see if the attribute is already in the list: that is
   * the responsibility of the application.</p>
   *
   * @param uri       The Namespace URI, or the empty string if
   *                  none is available or Namespace processing is not
   *                  being performed.
   * @param localName The local name, or the empty string if
   *                  Namespace processing is not being performed.
   * @param qName     The qualified (prefixed) name, or the empty string
   *                  if qualified names are not available.
   * @param type      The attribute type as a string.
   * @param value     The attribute value.
   */
  def addAttribute(uri: String, localName: String, qName: String, `type`: String, value: String): Unit = {
    ensureCapacity(length + 1)
    data(length * 5) = uri
    data(length * 5 + 1) = localName
    data(length * 5 + 2) = qName
    data(length * 5 + 3) = `type`
    data(length * 5 + 4) = value
    length += 1
  }

  /**
   * Set an attribute in the list.
   *
   * <p>For the sake of speed, this method does no checking
   * for name conflicts or well-formedness: such checks are the
   * responsibility of the application.</p>
   *
   * @param index     The index of the attribute (zero-based).
   * @param uri       The Namespace URI, or the empty string if
   *                  none is available or Namespace processing is not
   *                  being performed.
   * @param localName The local name, or the empty string if
   *                  Namespace processing is not being performed.
   * @param qName     The qualified name, or the empty string
   *                  if qualified names are not available.
   * @param type      The attribute type as a string.
   * @param value     The attribute value.
   * @exception java.lang.ArrayIndexOutOfBoundsException When the
   *            supplied index does not point to an attribute
   *            in the list.
   */
  def setAttribute(index: Int, uri: String, localName: String, qName: String, `type`: String, value: String): Unit = {
    if (index >= 0 && index < length) {
      data(index * 5) = uri
      data(index * 5 + 1) = localName
      data(index * 5 + 2) = qName
      data(index * 5 + 3) = `type`
      data(index * 5 + 4) = value
    } else badIndex(index)
  }

  /**
   * Remove an attribute from the list.
   *
   * @param index The index of the attribute (zero-based).
   * @exception java.lang.ArrayIndexOutOfBoundsException When the
   *            supplied index does not point to an attribute
   *            in the list.
   */
  def removeAttribute(index: Int): Unit = {
    if (index >= 0 && index < length) {
      if (index < length - 1)
        System.arraycopy(data, (index + 1) * 5, data, index * 5, (length - index - 1) * 5)

      var newIndex = (length - 1) * 5
      val maxIdx = newIndex + 5
      while (newIndex < maxIdx) {
        data(newIndex) = null
        newIndex += 1
      }

      length -= 1
    }
    else badIndex(index)
  }

  /**
   * Set the Namespace URI of a specific attribute.
   *
   * @param index The index of the attribute (zero-based).
   * @param uri   The attribute's Namespace URI, or the empty
   *              string for none.
   * @exception java.lang.ArrayIndexOutOfBoundsException When the
   *            supplied index does not point to an attribute
   *            in the list.
   */
  def setURI(index: Int, uri: String): Unit = {
    if (index >= 0 && index < length) data(index * 5) = uri
    else badIndex(index)
  }

  /**
   * Set the local name of a specific attribute.
   *
   * @param index     The index of the attribute (zero-based).
   * @param localName The attribute's local name, or the empty
   *                  string for none.
   * @exception java.lang.ArrayIndexOutOfBoundsException When the
   *            supplied index does not point to an attribute
   *            in the list.
   */
  def setLocalName(index: Int, localName: String): Unit = {
    if (index >= 0 && index < length) data(index * 5 + 1) = localName
    else badIndex(index)
  }

  /**
   * Set the qualified name of a specific attribute.
   *
   * @param index The index of the attribute (zero-based).
   * @param qName The attribute's qualified name, or the empty
   *              string for none.
   * @exception java.lang.ArrayIndexOutOfBoundsException When the
   *            supplied index does not point to an attribute
   *            in the list.
   */
  def setQName(index: Int, qName: String): Unit = {
    if (index >= 0 && index < length) data(index * 5 + 2) = qName
    else badIndex(index)
  }

  /**
   * Set the type of a specific attribute.
   *
   * @param index The index of the attribute (zero-based).
   * @param type  The attribute's type.
   * @exception java.lang.ArrayIndexOutOfBoundsException When the
   *            supplied index does not point to an attribute
   *            in the list.
   */
  def setType(index: Int, `type`: String): Unit = {
    if (index >= 0 && index < length) data(index * 5 + 3) = `type`
    else badIndex(index)
  }

  /**
   * Set the value of a specific attribute.
   *
   * @param index The index of the attribute (zero-based).
   * @param value The attribute's value.
   * @exception java.lang.ArrayIndexOutOfBoundsException When the
   *            supplied index does not point to an attribute
   *            in the list.
   */
  def setValue(index: Int, value: String): Unit = {
    if (index >= 0 && index < length) data(index * 5 + 4) = value
    else badIndex(index)
  }

  /**
   * Ensure the internal array's capacity.
   *
   * @param n The minimum number of attributes that the array must
   *          be able to hold.
   */
  private def ensureCapacity(n: Int): Unit = {
    if (n <= 0) return

    var max = 0
    if (data == null || data.length == 0) max = 25
    else if (data.length >= n * 5) return
    else max = data.length

    while (max < n * 5)
      max *= 2

    val newData = new Array[String](max)

    if (length > 0)
      System.arraycopy(data, 0, newData, 0, length * 5)

    data = newData
  }

  /**
   * Report a bad array index in a manipulator.
   *
   * @param index The index to report.
   * @exception java.lang.ArrayIndexOutOfBoundsException Always.
   */
  @throws[ArrayIndexOutOfBoundsException]
  private def badIndex(index: Int): Unit = {
    val msg = s"Attempt to modify attribute at illegal index: $index"
    throw new ArrayIndexOutOfBoundsException(msg)
  }

}