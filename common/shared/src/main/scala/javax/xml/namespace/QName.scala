/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// $Id: QName.java 754581 2009-03-15 01:32:39Z mrglavas $

package javax.xml.namespace

import javax.xml
import javax.xml.XMLConstants

/**
 * <p><code>QName</code> represents a <strong>qualified name</strong>
 * as defined in the XML specifications: <a
 * href="http://www.w3.org/TR/xmlschema-2/#QName">XML Schema Part2:
 * Datatypes specification</a>, <a
 * href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">Namespaces
 * in XML</a>, <a
 * href="http://www.w3.org/XML/xml-names-19990114-errata">Namespaces
 * in XML Errata</a>.</p>
 *
 * <p>The value of a <code>QName</code> contains a <strong>Namespace
 * URI</strong>, <strong>local part</strong> and
 * <strong>prefix</strong>.</p>
 *
 * <p>The prefix is included in <code>QName</code> to retain lexical
 * information <strong><em>when present</em></strong> in an
 * {@link javax.xml.transform.Source XML input source}.
 * The prefix is <strong><em>NOT</em></strong> used in {@link #equals( Object )
 * QName.equals(Object)} or to compute the {@link QName#hashCode()}.
 * Equality and the hash code are defined using
 * <strong><em>only</em></strong> the Namespace URI and local part.</p>
 *
 * <p>If not specified, the Namespace URI is set to
 * {@link javax.xml.XMLConstants#NULL_NS_URI}.
 * If not specified, the prefix is set to {@link javax.xml.XMLConstants#DEFAULT_NS_PREFIX}.</p>
 *
 * <p><code>QName</code> is immutable.</p>
 *
 * @author <a href="mailto:Jeff.Suttor@Sun.com">Jeff Suttor</a>
 * @version $Revision: 754581 $, $Date: 2009-03-14 18:32:39 -0700 (Sat, 14 Mar 2009) $
 * @see <a href="http://www.w3.org/TR/xmlschema-2/#QName">XML Schema Part2: Datatypes specification</a>
 * @see <a href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">Namespaces in XML</a>
 * @see <a href="http://www.w3.org/XML/xml-names-19990114-errata">Namespaces in XML Errata</a>
 * @since 1.5
 */
object QName {

  /**
   * <p>The original default Stream Unique Identifier.</p>
   */
  private val defaultSerialVersionUID: Long = -9120448754896609940L
  /**
   * <p>The compatibility Stream Unique Identifier that was introduced
   * with Java 5 SE SDK.</p>
   */
  private val compatibilitySerialVersionUID: Long = 4418622981026545151L

  /**
   * <p>Stream Unique Identifier.</p>
   *
   * <p>To enable the compatibility <code>serialVersionUID</code>
   * set the System Property <code>org.apache.xml.namespace.QName.useCompatibleSerialVersionUID</code>
   * to a value of "1.0".</p>
   */
  private var serialVersionUID: Long = {
    val compatPropValue: String = System.getProperty("org.apache.xml.namespace.QName.useCompatibleSerialVersionUID")
    // If 1.0 use compatibility serialVersionUID
    if (!("1.0" == compatPropValue)) defaultSerialVersionUID else compatibilitySerialVersionUID
  }

  /**
   * <p><code>QName</code> derived from parsing the formatted
   * <code>String</code>.</p>
   *
   * <p>If the <code>String</code> is <code>null</code> or does not conform to
   * {@link #toString() QName.toString()} formatting, an
   * <code>IllegalArgumentException</code> is thrown.</p>
   *
   * <p><em>The <code>String</code> <strong>MUST</strong> be in the
   * form returned by {@link #toString() QName.toString()}.</em></p>
   *
   * <p>The commonly accepted way of representing a <code>QName</code>
   * as a <code>String</code> was <a href="http://jclark.com/xml/xmlns.htm">defined</a>
   * by James Clark.  Although this is not a <em>standard</em>
   * specification, it is in common use,  e.g. {@link javax.xml.transform.Transformer#setParameter(String name, Object value)}.
   * This implementation parses a <code>String</code> formatted
   * as: "{" + Namespace URI + "}" + local part.  If the Namespace
   * URI <code>.equals(XMLConstants.NULL_NS_URI)</code>, only the
   * local part should be provided.</p>
   *
   * <p>The prefix value <strong><em>CANNOT</em></strong> be
   * represented in the <code>String</code> and will be set to
   * {@link javax.xml.XMLConstants# DEFAULT_NS_PREFIX}.</p>
   *
   * <p>This method does not do full validation of the resulting
   * <code>QName</code>.
   * <p>The Namespace URI is not validated as a
   * <a href="http://www.ietf.org/rfc/rfc2396.txt">URI reference</a>.
   * The local part is not validated as a
   * <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
   * as specified in
   * <a href="http://www.w3.org/TR/REC-xml-names/">Namespaces in XML</a>.</p>
   *
   * @param qNameAsString <code>String</code> representation
   * of the <code>QName</code>
   * @return <code>QName</code> corresponding to the given <code>String</code>
   * @see #toString() QName.toString()
   */
  def valueOf(qNameAsString: String): QName = {
    // null is not valid
    require(qNameAsString != null, "cannot create QName from null String")

    // "" local part is valid to preserve compatible behavior with QName 1.0
    // if this is an empty string or it does not start with '{', treat this as a local part alone.
    if (qNameAsString.isEmpty || qNameAsString.charAt(0) != '{')
      return new QName(XMLConstants.NULL_NS_URI, qNameAsString, XMLConstants.DEFAULT_NS_PREFIX)

    // Namespace URI improperly specified?
    require(
      !qNameAsString.startsWith(s"{${XMLConstants.NULL_NS_URI}}"),
      s"""Namespace URI .equals(XMLConstants.NULL_NS_URI), .equals(${XMLConstants.NULL_NS_URI}), """ +
      s"""only the local part, "${qNameAsString.substring(2 + XMLConstants.NULL_NS_URI.length())}" should be provided."""
    )

    // Namespace URI and local part specified
    val endOfNamespaceURI: Int = qNameAsString.indexOf('}')
    require(endOfNamespaceURI != -1, s"""cannot create QName from "$qNameAsString", missing closing "}"""")

    new QName(
      qNameAsString.substring(1, endOfNamespaceURI),
      qNameAsString.substring(endOfNamespaceURI + 1),
      XMLConstants.DEFAULT_NS_PREFIX
    )
  }

}

/*
 * Default private constructor.
 *

 *
 * @param namespaceURI
 * @param localPart
 */

/**
 * QName implementation.
 * A case class is used to extend java.io.Serializable, and use auto-implemented equals()/hashCode() methods.
 * <p>Two <code>QName</code>s are considered equal if and only if both the Namespace URI and local part are equal.
 * The prefix is <strong><em>NOT</em></strong> used to determine equality.</p>
 *
 * <p><code>QName</code> constructor specifying the Namespace URI and local part.</p>
 *
 * <p>If the Namespace URI is <code>null</code>, it is set to {@link javax.xml.XMLConstants# NULL_NS_URI}.
 * This value represents no explicitly defined Namespace as defined by the
 * <a href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">Namespaces in XML</a> specification.
 * This action preserves compatible behavior with QName 1.0.
 * Explicitly providing the {@link javax.xml.XMLConstants#NULL_NS_URI} value is the preferred coding style.</p>
 *
 * <p>If the local part is <code>null</code> an <code>IllegalArgumentException</code> is thrown.
 * A local part of "" is allowed to preserve compatible behavior with QName 1.0. </p>
 *
 * <p>When using this constructor, the prefix is set to
 * {@link javax.xml.XMLConstants#DEFAULT_NS_PREFIX}.</p>
 *
 * <p>The Namespace URI is not validated as a <a href="http://www.ietf.org/rfc/rfc2396.txt">URI reference</a>.
 * The local part is not validated as a <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
 * as specified in <a href="http://www.w3.org/TR/REC-xml-names/">Namespaces in XML</a>.</p>
 *
 * @param namespaceURI Namespace URI of the <code>QName</code>
 * @param localPart    local part of the <code>QName</code>
 * @see #QName(String namespaceURI, String localPart, String prefix)
 */
case class QName(
  /**
   * <p>Namespace URI of this <code>QName</code>.</p>
   */
  private var namespaceURI: String,

  /**
   * <p>local part of this <code>QName</code>.</p>
   */
  private val localPart: String
) {

  // map null Namespace URI to default to preserve compatibility with QName 1.0
  if (namespaceURI == null)
    namespaceURI = XMLConstants.NULL_NS_URI

  // local part is required. "" is allowed to preserve compatibility with QName 1.0
  require(localPart != null, "local part cannot be \"null\" when creating a QName")

  /**
   * <p>prefix of this <code>QName</code>.</p>
   */
  private var prefix: String = XMLConstants.DEFAULT_NS_PREFIX

  /**
   * <p><code>QName</code> constructor specifying the Namespace URI, local part and prefix.</p>
   *
   * <p>If the Namespace URI is <code>null</code>, it is set to
   * {@link javax.xml.XMLConstants#NULL_NS_URI}.
   * This value represents no explicitly defined Namespace as defined by the <a
   * href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">Namespaces
   * in XML</a> specification. This action preserves compatible
   * behavior with QName 1.0. Explicitly providing the
   * {@link javax.xml.XMLConstants#NULL_NS_URI}
   * value is the preferred coding style.</p>
   *
   * <p>If the local part is <code>null</code> an
   * <code>IllegalArgumentException</code> is thrown.
   * A local part of "" is allowed to preserve
   * compatible behavior with QName 1.0. </p>
   *
   * <p>If the prefix is <code>null</code>, an
   * <code>IllegalArgumentException</code> is thrown.
   * Use {@link javax.xml.XMLConstants#DEFAULT_NS_PREFIX}
   * to explicitly indicate that no prefix is present or the prefix is not relevant.</p>
   *
   * <p>The Namespace URI is not validated as a
   * <a href="http://www.ietf.org/rfc/rfc2396.txt">URI reference</a>.
   * The local part and prefix are not validated as a
   * <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
   * as specified in <a href="http://www.w3.org/TR/REC-xml-names/">Namespaces
   * in XML</a>.</p>
   *
   * @param namespaceURI Namespace URI of the <code>QName</code>
   * @param localPart    local part of the <code>QName</code>
   * @param prefix       prefix of the <code>QName</code>
   */
  def this(namespaceURI: String, localPart: String, prefix: String) = {
    this(namespaceURI, localPart)

    // prefix is required
    require(prefix != null, "prefix cannot be \"null\" when creating a QName")

    this.prefix = prefix
  }

  /**
   * <p><code>QName</code> constructor specifying the Namespace URI and local part.</p>
   *
   * <p>If the Namespace URI is <code>null</code>, it is set to
   * {@link javax.xml.XMLConstants# NULL_NS_URI}.  This value represents no
   * explicitly defined Namespace as defined by the <a
   * href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">Namespaces
   * in XML</a> specification.  This action preserves compatible behavior with QName 1.0.
   * Explicitly providing the {@link javax.xml.XMLConstants#NULL_NS_URI} value is the preferred coding style.</p>
   *
   * <p>If the local part is <code>null</code> an
   * <code>IllegalArgumentException</code> is thrown.
   * A local part of "" is allowed to preserve
   * compatible behavior with QName 1.0. </p>
   *
   * <p>When using this constructor, the prefix is set to
   * {@link javax.xml.XMLConstants#DEFAULT_NS_PREFIX}.</p>
   *
   * <p>The Namespace URI is not validated as a
   * <a href="http://www.ietf.org/rfc/rfc2396.txt">URI reference</a>.
   * The local part is not validated as a
   * <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
   * as specified in <a href="http://www.w3.org/TR/REC-xml-names/">Namespaces
   * in XML</a>.</p>
   *
   * @param namespaceURI Namespace URI of the <code>QName</code>
   * @param localPart    local part of the <code>QName</code>
   * @see #QName(String namespaceURI, String localPart, String prefix)
   */
  /*def this(namespaceURI: String, localPart: String) = {
    this(namespaceURI, localPart, XMLConstants.DEFAULT_NS_PREFIX)
  }*/

  /**
   * <p><code>QName</code> constructor specifying the local part.</p>
   *
   * <p>If the local part is <code>null</code> an
   * <code>IllegalArgumentException</code> is thrown.
   * A local part of "" is allowed to preserve
   * compatible behavior with QName 1.0. </p>
   *
   * <p>When using this constructor, the Namespace URI is set to
   * {@link javax.xml.XMLConstants#NULL_NS_URI} and the prefix is set to
   * {@link javax.xml.XMLConstants#DEFAULT_NS_PREFIX}.</p>
   *
   * <p><em>In an XML context, all Element and Attribute names exist
   * in the context of a Namespace.  Making this explicit during the
   * construction of a <code>QName</code> helps prevent hard to
   * diagnosis XML validity errors.
   * The constructors {@link #QName(String namespaceURI, String localPart)} and
   * {@link #QName(String namespaceURI, String localPart, String prefix)}
   * are preferred.</em></p>
   *
   * <p>The local part is not validated as a
   * <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
   * as specified in <a href="http://www.w3.org/TR/REC-xml-names/">Namespaces
   * in XML</a>.</p>
   *
   * @param localPart local part of the <code>QName</code>
   * @see #QName(String namespaceURI, String localPart) QName(String
   *      namespaceURI, String localPart)
   * @see #QName(String namespaceURI, String localPart, String
   *      prefix) QName(String namespaceURI, String localPart, String
   *      prefix)
   */
  def this(localPart: String) = {
    this(XMLConstants.NULL_NS_URI, localPart, XMLConstants.DEFAULT_NS_PREFIX)
  }

  /**
   * <p>Get the Namespace URI of this <code>QName</code>.</p>
   *
   * @return Namespace URI of this <code>QName</code>
   */
  def getNamespaceURI(): String = namespaceURI

  /**
   * <p>Get the local part of this <code>QName</code>.</p>
   *
   * @return local part of this <code>QName</code>
   */
  def getLocalPart(): String = localPart

  /**
   * <p>Get the prefix of this <code>QName</code>.</p>
   *
   * <p>The prefix assigned to a <code>QName</code> might
   * <strong><em>NOT</em></strong> be valid in a different
   * context. For example, a <code>QName</code> may be assigned a
   * prefix in the context of parsing a document but that prefix may
   * be invalid in the context of a different document.</p>
   *
   * @return prefix of this <code>QName</code>
   */
  def getPrefix(): String = prefix

  /**
   * <p><code>String</code> representation of this <code>QName</code>.</p>
   */
  private lazy val qNameAsString: String = {
    val nsLength: Int = namespaceURI.length
    if (nsLength == 0) localPart
    else {
      val buffer: StringBuilder = new StringBuilder(nsLength + localPart.length + 2)
      buffer.append('{')
      buffer.append(namespaceURI)
      buffer.append('}')
      buffer.append(localPart)
      buffer.toString
    }
  }

  /**
   * <p><code>String</code> representation of this <code>QName</code>.</p>
   *
   * <p>The commonly accepted way of representing a <code>QName</code>
   * as a <code>String</code> was <a href="http://jclark.com/xml/xmlns.htm">defined</a>
   * by James Clark.  Although this is not a <em>standard</em>
   * specification, it is in common use,  e.g. {@link javax.xml.transform.Transformer# setParameter(String name, Object value)}.
   * This implementation represents a <code>QName</code> as:
   * "{" + Namespace URI + "}" + local part.  If the Namespace URI
   * <code>.equals(XMLConstants.NULL_NS_URI)</code>, only the
   * local part is returned.  An appropriate use of this method is
   * for debugging or logging for human consumption.</p>
   *
   * <p>Note the prefix value is <strong><em>NOT</em></strong>
   * returned as part of the <code>String</code> representation.</p>
   *
   * <p>This method satisfies the general contract of
   * {@link java.lang.Object#toString()}.</p>
   *
   * @return <code>String</code> representation of this <code>QName</code>
   */
  override def toString: String = qNameAsString

}

/*
class QName private() extends Equals with Serializable {

  /**
   * <p>Namespace URI of this <code>QName</code>.</p>
   */
  private var namespaceURI: String = _

  /**
   * <p>local part of this <code>QName</code>.</p>
   */
  private var localPart: String = _

  /**
   * <p>prefix of this <code>QName</code>.</p>
   */
  private var prefix: String = _

  /**
   * <p><code>String</code> representation of this <code>QName</code>.</p>
   */
  private var qNameAsString: String = _

  /**
   * <p><code>QName</code> constructor specifying the Namespace URI,
   * local part and prefix.</p>
   *
   * <p>If the Namespace URI is <code>null</code>, it is set to
   * {@link javax.xml.XMLConstants# NULL_NS_URI
 * XMLConstants.NULL_NS_URI}.  This value represents no
   * explicitly defined Namespace as defined by the <a
   * href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">Namespaces
   * in XML</a> specification.  This action preserves compatible
   * behavior with QName 1.0.  Explicitly providing the {@link
 * javax.xml.XMLConstants#NULL_NS_URI
 * XMLConstants.NULL_NS_URI} value is the preferred coding style.</p>
   *
   * <p>If the local part is <code>null</code> an
   * <code>IllegalArgumentException</code> is thrown.
   * A local part of "" is allowed to preserve
   * compatible behavior with QName 1.0. </p>
   *
   * <p>If the prefix is <code>null</code>, an
   * <code>IllegalArgumentException</code> is thrown.  Use {@link
 * javax.xml.XMLConstants#DEFAULT_NS_PREFIX
     * XMLConstants.DEFAULT_NS_PREFIX} to explicitly indicate that no
   * prefix is present or the prefix is not relevant.</p>
   *
   * <p>The Namespace URI is not validated as a
   * <a href="http://www.ietf.org/rfc/rfc2396.txt">URI reference</a>.
   * The local part and prefix are not validated as a
   * <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
   * as specified in <a href="http://www.w3.org/TR/REC-xml-names/">Namespaces
   * in XML</a>.</p>
   *
   * @param namespaceURI Namespace URI of the <code>QName</code>
   * @param localPart    local part of the <code>QName</code>
   * @param prefix       prefix of the <code>QName</code>
   */
  def this(namespaceURI: String, localPart: String, prefix: String) {
    this()

    // map null Namespace URI to default to preserve compatibility with QName 1.0
    if (namespaceURI == null) {
      this.namespaceURI = XMLConstants.NULL_NS_URI
    }
    else {
      this.namespaceURI = namespaceURI
    }

    // local part is required.  "" is allowed to preserve compatibility with QName 1.0
    require(localPart != null,"local part cannot be \"null\" when creating a QName")

    // prefix is required
    require(prefix != null,"prefix cannot be \"null\" when creating a QName")
  }

  /**
   * <p><code>QName</code> constructor specifying the Namespace URI
   * and local part.</p>
   *
   * <p>If the Namespace URI is <code>null</code>, it is set to
   * {@link javax.xml.XMLConstants# NULL_NS_URI
   * XMLConstants.NULL_NS_URI}.  This value represents no
   * explicitly defined Namespace as defined by the <a
   * href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">Namespaces
   * in XML</a> specification.  This action preserves compatible
   * behavior with QName 1.0.  Explicitly providing the {@link
 * javax.xml.XMLConstants#NULL_NS_URI
     * XMLConstants.NULL_NS_URI} value is the preferred coding
   * style.</p>
   *
   * <p>If the local part is <code>null</code> an
   * <code>IllegalArgumentException</code> is thrown.
   * A local part of "" is allowed to preserve
   * compatible behavior with QName 1.0. </p>
   *
   * <p>When using this constructor, the prefix is set to {@link
 * javax.xml.XMLConstants#DEFAULT_NS_PREFIX
     * XMLConstants.DEFAULT_NS_PREFIX}.</p>
   *
   * <p>The Namespace URI is not validated as a
   * <a href="http://www.ietf.org/rfc/rfc2396.txt">URI reference</a>.
   * The local part is not validated as a
   * <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
   * as specified in <a href="http://www.w3.org/TR/REC-xml-names/">Namespaces
   * in XML</a>.</p>
   *
   * @param namespaceURI Namespace URI of the <code>QName</code>
   * @param localPart    local part of the <code>QName</code>
   * @see #QName(String namespaceURI, String localPart, String
   *      prefix) QName(String namespaceURI, String localPart, String
   *      prefix)
   */
  def this(namespaceURI: String, localPart: String) {
    this(namespaceURI, localPart, XMLConstants.DEFAULT_NS_PREFIX)
  }

  /**
   * <p><code>QName</code> constructor specifying the local part.</p>
   *
   * <p>If the local part is <code>null</code> an
   * <code>IllegalArgumentException</code> is thrown.
   * A local part of "" is allowed to preserve
   * compatible behavior with QName 1.0. </p>
   *
   * <p>When using this constructor, the Namespace URI is set to
   * {@link javax.xml.XMLConstants# NULL_NS_URI
   * XMLConstants.NULL_NS_URI} and the prefix is set to {@link
 * javax.xml.XMLConstants#DEFAULT_NS_PREFIX
     * XMLConstants.DEFAULT_NS_PREFIX}.</p>
   *
   * <p><em>In an XML context, all Element and Attribute names exist
   * in the context of a Namespace.  Making this explicit during the
   * construction of a <code>QName</code> helps prevent hard to
   * diagnosis XML validity errors.  The constructors {@link
 * #QName(String namespaceURI, String localPart) QName(String
     * namespaceURI, String localPart)} and
   * {@link #QName ( String namespaceURI, String localPart, String prefix)}
   * are preferred.</em></p>
   *
   * <p>The local part is not validated as a
   * <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
   * as specified in <a href="http://www.w3.org/TR/REC-xml-names/">Namespaces
   * in XML</a>.</p>
   *
   * @param localPart local part of the <code>QName</code>
   * @see #QName(String namespaceURI, String localPart) QName(String
   *      namespaceURI, String localPart)
   * @see #QName(String namespaceURI, String localPart, String
   *      prefix) QName(String namespaceURI, String localPart, String
   *      prefix)
   */
  def this(localPart: String) {
    this(XMLConstants.NULL_NS_URI, localPart, XMLConstants.DEFAULT_NS_PREFIX)
  }

  /**
   * <p>Get the Namespace URI of this <code>QName</code>.</p>
   *
   * @return Namespace URI of this <code>QName</code>
   */
  def getNamespaceURI(): String = namespaceURI

  /**
   * <p>Get the local part of this <code>QName</code>.</p>
   *
   * @return local part of this <code>QName</code>
   */
  def getLocalPart(): String = localPart

  /**
   * <p>Get the prefix of this <code>QName</code>.</p>
   *
   * <p>The prefix assigned to a <code>QName</code> might
   * <strong><em>NOT</em></strong> be valid in a different
   * context. For example, a <code>QName</code> may be assigned a
   * prefix in the context of parsing a document but that prefix may
   * be invalid in the context of a different document.</p>
   *
   * @return prefix of this <code>QName</code>
   */
  def getPrefix(): String = prefix

  /**
   * <p>Test this <code>QName</code> for equality with another
   * <code>Object</code>.</p>
   *
   * <p>If the <code>Object</code> to be tested is not a
   * <code>QName</code> or is <code>null</code>, then this method
   * returns <code>false</code>.</p>
   *
   * <p>Two <code>QName</code>s are considered equal if and only if
   * both the Namespace URI and local part are equal. This method
   * uses <code>String.equals()</code> to check equality of the
   * Namespace URI and local part. The prefix is
   * <strong><em>NOT</em></strong> used to determine equality.</p>
   *
   * <p>This method satisfies the general contract of {@link
 * java.lang.Object#equals(Object) Object.equals(Object)}</p>
   *
   * @param objectToTest the <code>Object</code> to test for
   *                     equality with this <code>QName</code>
   * @return <code>true</code> if the given <code>Object</code> is
   *         equal to this <code>QName</code> else <code>false</code>
   */
  final def equals(objectToTest: AnyRef): Boolean = {
    // Is this the same object?
    if (objectToTest eq this) {
      return true
    }
    // Is this a QName?
    objectToTest match {
      case qName: QName => localPart == qName.localPart && namespaceURI == qName.namespaceURI
      case _ => false
    }
  }

  /**
   * <p>Generate the hash code for this <code>QName</code>.</p>
   *
   * <p>The hash code is calculated using both the Namespace URI and
   * the local part of the <code>QName</code>.  The prefix is
   * <strong><em>NOT</em></strong> used to calculate the hash
   * code.</p>
   *
   * <p>This method satisfies the general contract of {@link
 * java.lang.Object#hashCode() Object.hashCode()}.</p>
   *
   * @return hash code for this <code>QName</code> <code>Object</code>
   */
  override final def hashCode(): Int = {
    // See: https://stackoverflow.com/questions/7370925/what-is-the-standard-idiom-for-implementing-equals-and-hashcode-in-scala
    31 * namespaceURI.## + localPart.##
    //namespaceURI.hashCode ^ localPart.hashCode
  }

  /**
   * <p><code>String</code> representation of this
   * <code>QName</code>.</p>
   *
   * <p>The commonly accepted way of representing a <code>QName</code>
   * as a <code>String</code> was <a href="http://jclark.com/xml/xmlns.htm">defined</a>
   * by James Clark.  Although this is not a <em>standard</em>
   * specification, it is in common use,  e.g. {@link javax.xml.transform.Transformer# setParameter ( String name, Object value)}.
   * This implementation represents a <code>QName</code> as:
   * "{" + Namespace URI + "}" + local part.  If the Namespace URI
   * <code>.equals(XMLConstants.NULL_NS_URI)</code>, only the
   * local part is returned.  An appropriate use of this method is
   * for debugging or logging for human consumption.</p>
   *
   * <p>Note the prefix value is <strong><em>NOT</em></strong>
   * returned as part of the <code>String</code> representation.</p>
   *
   * <p>This method satisfies the general contract of {@link
 * java.lang.Object#toString() Object.toString()}.</p>
   *
   * @return <code>String</code> representation of this <code>QName</code>
   */
  override def toString: String = {
    var _qNameAsString: String = qNameAsString
    if (_qNameAsString == null) {
      val nsLength: Int = namespaceURI.length
      if (nsLength == 0) {
        _qNameAsString = localPart
      }
      else {
        val buffer: StringBuilder = new StringBuilder(nsLength + localPart.length + 2)
        buffer.append('{')
        buffer.append(namespaceURI)
        buffer.append('}')
        buffer.append(localPart)
        _qNameAsString = buffer.toString
      }
      qNameAsString = _qNameAsString
    }
    _qNameAsString
  }

}*/

