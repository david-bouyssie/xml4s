/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */

// Contributors: Paul Hackenberger (unterminated entity handling in relaxed mode)

package org.kxml2.io

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util

import scala.collection.mutable.HashMap

import libcore.util.StringPool

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/**
 * An XML pull parser with limited support for parsing internal DTDs.
 */
object KXmlParser {

  private val PROPERTY_XMLDECL_VERSION = "http://xmlpull.org/v1/doc/properties.html#xmldecl-version"
  private val PROPERTY_XMLDECL_STANDALONE = "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone"
  private val PROPERTY_LOCATION = "http://xmlpull.org/v1/doc/properties.html#location"
  /** "relaxed" mode: for compatibility with HTML or SGML files (that are not well-formed XML documents) */
  private val FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed"

  private val DEFAULT_ENTITIES = new util.HashMap[String, String]
  private val ELEMENTDECL = 11
  private val ENTITYDECL = 12
  private val ATTLISTDECL = 13
  private val NOTATIONDECL = 14
  private val PARAMETER_ENTITY_REF = 15
  private val START_COMMENT = Array('<', '!', '-', '-')
  private val END_COMMENT = Array('-', '-', '>')
  private val COMMENT_DOUBLE_DASH = Array('-', '-')
  private val START_CDATA = Array('<', '!', '[', 'C', 'D', 'A', 'T', 'A', '[')
  private val END_CDATA = Array(']', ']', '>')
  private val START_PROCESSING_INSTRUCTION = Array('<', '?')
  private val END_PROCESSING_INSTRUCTION = Array('?', '>')
  private val START_DOCTYPE = Array('<', '!', 'D', 'O', 'C', 'T', 'Y', 'P', 'E')
  private val SYSTEM = Array('S', 'Y', 'S', 'T', 'E', 'M')
  private val PUBLIC = Array('P', 'U', 'B', 'L', 'I', 'C')
  private val START_ELEMENT = Array('<', '!', 'E', 'L', 'E', 'M', 'E', 'N', 'T')
  private val START_ATTLIST = Array('<', '!', 'A', 'T', 'T', 'L', 'I', 'S', 'T')
  private val START_ENTITY = Array('<', '!', 'E', 'N', 'T', 'I', 'T', 'Y')
  private val START_NOTATION = Array('<', '!', 'N', 'O', 'T', 'A', 'T', 'I', 'O', 'N')
  private val EMPTY = Array[Char]('E', 'M', 'P', 'T', 'Y')
  private val ANY = Array[Char]('A', 'N', 'Y')
  private val NDATA = Array[Char]('N', 'D', 'A', 'T', 'A')
  private val NOTATION = Array[Char]('N', 'O', 'T', 'A', 'T', 'I', 'O', 'N')
  private val REQUIRED = Array[Char]('R', 'E', 'Q', 'U', 'I', 'R', 'E', 'D')
  private val IMPLIED = Array[Char]('I', 'M', 'P', 'L', 'I', 'E', 'D')
  private val FIXED = Array[Char]('F', 'I', 'X', 'E', 'D')
  private val UNEXPECTED_EOF = "Unexpected EOF"
  private val ILLEGAL_TYPE = "Wrong event type"
  private val XML_DECLARATION = 998
  private val SINGLE_QUOTE = Array[Char]('\'')
  private val DOUBLE_QUOTE = Array[Char]('"')

  /**
   * Where a value is found impacts how that value is interpreted. For
   * example, in attributes, "\n" must be replaced with a space character. In
   * text, "]]>" is forbidden. In entity declarations, named references are
   * not resolved.
   */
  private[io] object ValueContext extends Enumeration {
    type ValueContext = Value
    val ATTRIBUTE, TEXT, ENTITY_DECLARATION = Value
  }
  private[io] type ValueContext = ValueContext.ValueContext

  /**
   * A chain of buffers containing XML content. Each content source contains
   * the parser's primary read buffer or the characters of entities actively
   * being parsed.
   *
   * <p>For example, note the buffers needed to parse this document:
   * <pre>   {@code
   * <!DOCTYPE foo [
   * <!ENTITY baz "ghi">
   * <!ENTITY bar "def &baz; jkl">
   * ]>
   * <foo>abc &bar; mno</foo>
   * }</pre>
   *
   * <p>Things get interesting when the bar entity is encountered. At that
   * point two buffers are active:
   * <ol>
   * <li>The value for the bar entity, containing {@code "def &baz; jkl"}
   * <li>The parser's primary read buffer, containing {@code " mno</foo>"}
   * </ol>
   * <p>The parser will return the characters {@code "def "} from the bar
   * entity's buffer, and then it will encounter the baz entity. To handle
   * that, three buffers will be active:
   * <ol>
   * <li>The value for the baz entity, containing {@code "ghi"}
   * <li>The remaining value for the bar entity, containing {@code " jkl"}
   * <li>The parser's primary read buffer, containing {@code " mno</foo>"}
   * </ol>
   * <p>The parser will then return the characters {@code ghi jkl mno} in that
   * sequence by reading each buffer in sequence.
   */
  private[io] case class ContentSource private[io](
    next: KXmlParser.ContentSource,
    buffer: Array[Char],
    position: Int,
    limit: Int
  )

  DEFAULT_ENTITIES.put("lt", "<")
  DEFAULT_ENTITIES.put("gt", ">")
  DEFAULT_ENTITIES.put("amp", "&")
  DEFAULT_ENTITIES.put("apos", "'")
  DEFAULT_ENTITIES.put("quot", "\"")
}

class KXmlParser extends XmlPullParser with Closeable {

  import org.xmlpull.v1.XmlPullParser._

  // general
  private var location: String = _
  private var version: String = _
  private var standalone = java.lang.Boolean.FALSE
  private var rootElementName: String = _
  private var systemId: String = _
  private var publicId: String = _

  /**
   * True if the {@code <!DOCTYPE>} contents are handled. The DTD defines
   * entity values and default attribute values. These values are parsed at
   * inclusion time and may contain both tags and entity references.
   *
   * <p>If this is false, the user must {@link #defineEntityReplacementText
   * define entity values manually}. Such entity values are literal strings
   * and will not be parsed. There is no API to define default attributes
   * manually.
   */
  private[io] var processDocDecl = false
  private[io] var processNsp = false
  private var relaxed = false
  private var keepNamespaceAttrs = false

  /**
   * If non-null, the contents of the read buffer must be copied into this
   * string builder before the read buffer is overwritten. This is used to
   * capture the raw DTD text while parsing the DTD.
   */
  private var bufferCapture: StringBuilder = _

  /**
   * Entities defined in or for this document. This map is created lazily.
   */
  private var documentEntities: HashMap[String, Array[Char]] = _

  /**
   * Default attributes in this document. The outer map's key is the element
   * name; the inner map's key is the attribute name. Both keys should be
   * without namespace adjustments. This map is created lazily.
   */
  private var defaultAttributes: HashMap[String, HashMap[String, String]] = _

  private var depth = 0
  private var elementStack = new Array[String](16)
  private var nspStack = new Array[String](8)
  private var nspCounts = new Array[Int](4)

  // source

  private var reader: Reader = _
  private var encoding: String = _
  private var nextContentSource: KXmlParser.ContentSource = _
  private var buffer = new Array[Char](8192)
  private var position = 0
  private var limit = 0

  /**
   * Track the number of newlines and columns preceding the current buffer. To
   * compute the line and column of a position in the buffer, compute the line
   * and column in the buffer and add the preceding values.
   */
  private var bufferStartLine = 0
  private var bufferStartColumn = 0

  // the current token
  private var `type` = 0
  private var _isWhitespace = false
  private var namespace: String = _
  private var prefix: String = _
  private var name: String = _
  private var text: String = _

  private var degenerated = false
  private var attributeCount = 0

  // true iff. we've encountered the START_TAG of an XML element at depth == 0;
  private var parsedTopLevelStartTag = false

  /**
   * The current element's attributes arranged in groups of 4:
   * i + 0 = attribute namespace URI
   * i + 1 = attribute namespace prefix
   * i + 2 = attribute qualified name (may contain ":", as in "html:h1")
   * i + 3 = attribute value
   */
  private var attributes = new Array[String](16)

  private var error: String = _

  private var unresolved = false

  final val stringPool = new StringPool()

  /**
   * Retains namespace attributes like {@code xmlns="http://foo"} or {@code xmlns:foo="http:foo"}
   * in pulled elements. Most applications will only be interested in the effective namespaces of
   * their elements, so these attributes aren't useful. But for structure preserving wrappers like
   * DOM, it is necessary to keep the namespace data around.
   */
  def keepNamespaceAttributes(): Unit = {
    this.keepNamespaceAttrs = true
  }

  @throws[XmlPullParserException]
  private def adjustNsp(): Boolean = {
    var any = false

    var i = 0
    while (i < (attributeCount << 2)) {
      var attrName = attributes(i + 2)
      val cut = attrName.indexOf(':')
      var prefix: String = null

      if (cut != -1) {
        prefix = attrName.substring(0, cut)
        attrName = attrName.substring(cut + 1)
      }
      else if (attrName == "xmlns") {
        prefix = attrName
        attrName = null
      }

      if (prefix != null) {
        if (prefix != "xmlns") any = true
        else {
          val j = nspCounts(depth) << 1
          nspCounts(depth) += 1

          nspStack = ensureCapacity(nspStack, j + 2)
          nspStack(j) = attrName
          nspStack(j + 1) = attributes(i + 3)

          if (attrName != null && attributes(i + 3).isEmpty)
            checkRelaxed("illegal empty namespace")

          if (keepNamespaceAttrs) {
            // explicitly set the namespace for unprefixed attributes
            // such as xmlns="http://foo"
            attributes(i) = "http://www.w3.org/2000/xmlns/"
            any = true
          }
          else {
            attributeCount -= 1
            System.arraycopy(
              attributes,
              i + 4,
              attributes,
              i,
              (attributeCount << 2) - i
            )

            i -= 4
          }
        }
      }

      i += 4
    }

    if (any) {
      var i = (attributeCount << 2) - 4
      while (i >= 0) {
        var attrName = attributes(i + 2)
        val cut = attrName.indexOf(':')

        if (cut == 0 && !relaxed) {
          throw new RuntimeException("illegal attribute name: " + attrName + " at " + this)
        }
        else if (cut != -1) {
          val attrPrefix = attrName.substring(0, cut)
          attrName = attrName.substring(cut + 1)
          val attrNs = getNamespace(attrPrefix)

          if (attrNs == null && !relaxed)
            throw new RuntimeException("Undefined Prefix: " + attrPrefix + " in " + this)

          attributes(i) = attrNs
          attributes(i + 1) = attrPrefix
          attributes(i + 2) = attrName
        }
        i -= 4
      }
    }

    val cut = name.indexOf(':')
    if (cut == 0)
      checkRelaxed("illegal tag name: " + name)

    if (cut != -1) {
      prefix = name.substring(0, cut)
      name = name.substring(cut + 1)
    }

    this.namespace = getNamespace(prefix)

    if (this.namespace == null) {
      if (prefix != null) checkRelaxed("undefined prefix: " + prefix)
      this.namespace = NO_NAMESPACE
    }

    any
  }

  private def ensureCapacity(arr: Array[String], required: Int): Array[String] = {
    if (arr.length >= required) return arr
    val bigger = new Array[String](required + 16)
    System.arraycopy(arr, 0, bigger, 0, arr.length)
    bigger
  }

  @throws[XmlPullParserException]
  private def checkRelaxed(errorMessage: String) = {
    if (!relaxed) throw new XmlPullParserException(errorMessage, this, null)
    if (error == null) error = "Error: " + errorMessage
  }

  @throws[XmlPullParserException]
  @throws[IOException]
  override def next(): Int = next(false)

  @throws[XmlPullParserException]
  @throws[IOException]
  override def nextToken(): Int = next(true)

  @throws[IOException]
  @throws[XmlPullParserException]
  private def next(justOneToken: Boolean): Int = {
    if (reader == null)
      throw new XmlPullParserException("setInput() must be called first.", this, null)

    if (`type` == END_TAG) depth -= 1

    // degenerated needs to be handled before error because of possible
    // processor expectations(!)
    if (degenerated) {
      degenerated = false
      `type` = END_TAG
      return `type`
    }

    if (error != null) {
      if (!justOneToken) error = null
      else {
        text = error
        `type` = COMMENT
        error = null
        return `type`
      }
    }

    `type` = peekType(false)
    if (`type` == KXmlParser.XML_DECLARATION) {
      readXmlDeclaration()
      `type` = peekType(false)
    }

    text = null
    _isWhitespace = true
    prefix = null
    name = null
    namespace = null
    attributeCount = -1

    val throwOnResolveFailure = !justOneToken
    while (true) {
      `type` match {
        /* Return immediately after encountering a start tag, end tag, or the end of the document. */
        case START_TAG =>
          parseStartTag(false, throwOnResolveFailure)
          return `type`
        case END_TAG =>
          readEndTag()
          return `type`
        case END_DOCUMENT =>
          return `type`
        /*
         * Return after any text token when we're looking for a single token.
         * Otherwise concatenate all text between tags.
         */
        //case ENTITY_REF if justOneToken => {
        // FIXME: DBO => the initial Java code is breaking only when justOneToken is true
        case ENTITY_REF => {
          if (justOneToken) {
            val entityTextBuilder = new java.lang.StringBuilder()
            readEntity(entityTextBuilder, true, throwOnResolveFailure, KXmlParser.ValueContext.TEXT)
            text = entityTextBuilder.toString
          } else {
            text = readValue('<', !justOneToken, throwOnResolveFailure, KXmlParser.ValueContext.TEXT)
            if (depth == 0 && _isWhitespace) `type` = IGNORABLE_WHITESPACE
          }
        }
        case TEXT =>
          text = readValue('<', !justOneToken, throwOnResolveFailure, KXmlParser.ValueContext.TEXT)
          if (depth == 0 && _isWhitespace) `type` = IGNORABLE_WHITESPACE
        case CDSECT =>
          read(KXmlParser.START_CDATA)
          text = readUntil(KXmlParser.END_CDATA, returnText = true)
        /*
         * Comments, processing instructions and declarations are returned
         * when we're looking for a single token. Otherwise they're skipped.
         */
        case COMMENT =>
          val commentText = readComment(justOneToken)
          if (justOneToken) text = commentText
        case PROCESSING_INSTRUCTION =>
          read(KXmlParser.START_PROCESSING_INSTRUCTION)
          val processingInstruction = readUntil(KXmlParser.END_PROCESSING_INSTRUCTION, justOneToken)
          if (justOneToken) text = processingInstruction
        case DOCDECL =>
          readDoctype(justOneToken)
          if (parsedTopLevelStartTag)
            throw new XmlPullParserException("Unexpected token", this, null)
        case _ =>
          throw new XmlPullParserException("Unexpected token", this, null)
      }

      if (depth == 0 && (`type` == ENTITY_REF || `type` == TEXT || `type` == CDSECT))
        throw new XmlPullParserException("Unexpected token", this, null)

      if (justOneToken) return `type`

      if (`type` == IGNORABLE_WHITESPACE) text = null

      /*
       * We've read all that we can of a non-empty text block.
       * Always report this as text, even if it was a CDATA block or entity reference.
       */
      val peek = peekType(false)
      if (text != null && text.nonEmpty && peek < TEXT) {
        `type` = TEXT
        return `type`
      }

      `type` = peek
    }

    throw new XmlPullParserException("Unexpected token", this, null)
  }

  /**
   * Reads text until the specified delimiter is encountered.
   * Consumes the text and the delimiter.
   *
   * @param returnText true to return the read text excluding the delimiter;
   *                   false to return null.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readUntil(delimiter: Array[Char], returnText: Boolean): String = {
    var start = position
    var result: StringBuilder = null
    if (returnText && text != null) {
      result = new StringBuilder()
      result.append(text)
    }

    var foundDelimiter = false
    while (!foundDelimiter) {
      if (position + delimiter.length > limit) {
        if (start < position && returnText) {
          if (result == null) result = new StringBuilder()
          result.appendAll(buffer, start, position - start)
        }
        if (!fillBuffer(delimiter.length)) {
          checkRelaxed(KXmlParser.UNEXPECTED_EOF)
          `type` = COMMENT
          return null
        }
        start = position
      }

      // Set foundDelimiter to true (we will break the following while loop if chars don't match)
      foundDelimiter = true

      // TODO: replace with Arrays.equals(buffer, position, delimiter, 0, delimiter.length)
      // when the VM has better method inlining
      var i = 0
      val delimiterLen =  delimiter.length
      while (foundDelimiter && i < delimiterLen) {
        if (buffer(position + i) != delimiter(i)) {
          position += 1
          foundDelimiter = false
        }
        i += 1
      }
    }

    val end = position
    position += delimiter.length

    if (!returnText) null
    else if (result == null) stringPool.get(buffer, start, end - start)
    else {
      result.appendAll(buffer, start, end - start)
      result.toString
    }
  }

  /**
   * Returns true if an XML declaration was read.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readXmlDeclaration(): Unit = {
    if (bufferStartLine != 0 || bufferStartColumn != 0 || position != 0)
      checkRelaxed("processing instructions must not start with xml")

    read(KXmlParser.START_PROCESSING_INSTRUCTION)
    parseStartTag(xmldecl = true, throwOnResolveFailure = true)

    if (attributeCount < 1 || attributes(2) != "version") checkRelaxed("version expected")
    version = attributes(3)

    var pos = 1
    if (pos < attributeCount && "encoding" == attributes(2 + 4)) {
      encoding = attributes(3 + 4)
      pos += 1
    }

    if (pos < attributeCount && "standalone" == attributes(4 * pos + 2)) {
      val st = attributes(3 + 4 * pos)
      if ("yes" == st) standalone = java.lang.Boolean.TRUE
      else if ("no" == st) standalone = java.lang.Boolean.FALSE
      else checkRelaxed("illegal standalone value: " + st)
      pos += 1
    }

    if (pos != attributeCount)
      checkRelaxed("unexpected attributes in XML declaration")

    _isWhitespace = true
    text = null
  }

  @throws[IOException]
  @throws[XmlPullParserException]
  private def readComment(returnText: Boolean): String = {
    read(KXmlParser.START_COMMENT)
    if (relaxed)
      return readUntil(KXmlParser.END_COMMENT, returnText)

    val commentText = readUntil(KXmlParser.COMMENT_DOUBLE_DASH, returnText)

    if (peekCharacter() != '>')
      throw new XmlPullParserException("Comments may not contain --", this, null)

    position += 1
    commentText
  }

  /**
   * Read the document's DTD. Although this parser is non-validating, the DTD
   * must be parsed to capture entity values and default attribute values.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readDoctype(saveDtdText: Boolean): Unit = {
    read(KXmlParser.START_DOCTYPE)
    var startPosition = -1
    if (saveDtdText) {
      bufferCapture = new StringBuilder()
      startPosition = position
    }

    try {
      skip()

      rootElementName = readName()
      readExternalId(requireSystemName = true, assignFields = true)
      skip()

      if (peekCharacter() == '[')
        readInternalSubset()

      skip()
    } finally if (saveDtdText) {
      bufferCapture.appendAll(buffer, 0, position)
      bufferCapture.delete(0, startPosition)
      text = bufferCapture.toString
      bufferCapture = null
    }

    read('>')
    skip()
  }

  /**
   * Reads an external ID of one of these two forms:
   * SYSTEM "quoted system name"
   * PUBLIC "quoted public id" "quoted system name"
   *
   * If the system name is not required, this also supports lone public IDs of
   * this form:
   * PUBLIC "quoted public id"
   *
   * Returns true if any ID was read.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readExternalId(requireSystemName: Boolean, assignFields: Boolean): Boolean = {
    skip()

    val c = peekCharacter()
    if (c == 'S') read(KXmlParser.SYSTEM)
    else if (c == 'P') {
      read(KXmlParser.PUBLIC)
      skip()
      if (assignFields) publicId = readQuotedId(true)
      else readQuotedId(false)
    }
    else return false

    skip()
    if (!requireSystemName) {
      val delimiter = peekCharacter()
      if (delimiter != '"' && delimiter != '\'') return true // no system name!
    }

    if (assignFields) systemId = readQuotedId(true)
    else readQuotedId(false)

    true
  }

  /**
   * Reads a quoted string, performing no entity escaping of the contents.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readQuotedId(returnText: Boolean): String = {
    val quote = peekCharacter()

    val delimiter = if (quote == '"') KXmlParser.DOUBLE_QUOTE
    else if (quote == '\'') KXmlParser.SINGLE_QUOTE
    else throw new XmlPullParserException("Expected a quoted string", this, null)

    position += 1

    readUntil(delimiter, returnText)
  }

  @throws[IOException]
  @throws[XmlPullParserException]
  private def readInternalSubset(): Unit = {
    read('[')
    while (true) {
      skip()

      if (peekCharacter() == ']') {
        position += 1
        return
      }

      val declarationType = peekType(true)
      declarationType match {
        case KXmlParser.ELEMENTDECL =>
          readElementDeclaration()
        case KXmlParser.ATTLISTDECL =>
          readAttributeListDeclaration()
        case KXmlParser.ENTITYDECL =>
          readEntityDeclaration()
        case KXmlParser.NOTATIONDECL =>
          readNotationDeclaration()
        case PROCESSING_INSTRUCTION =>
          read(KXmlParser.START_PROCESSING_INSTRUCTION)
          readUntil(KXmlParser.END_PROCESSING_INSTRUCTION, returnText = false)
        case COMMENT =>
          readComment(false)
        case KXmlParser.PARAMETER_ENTITY_REF =>
          throw new XmlPullParserException("Parameter entity references are not supported", this, null)
        case _ =>
          throw new XmlPullParserException("Unexpected token", this, null)
      }
    }
  }

  /**
   * Read an element declaration. This contains a name and a content spec.
   * <!ELEMENT foo EMPTY >
   * <!ELEMENT foo (bar?,(baz|quux)) >
   * <!ELEMENT foo (#PCDATA|bar)* >
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readElementDeclaration() = {
    read(KXmlParser.START_ELEMENT)
    skip()
    readName()
    readContentSpec()
    skip()
    read('>')
  }

  /**
   * Read an element content spec. This is a regular expression-like pattern
   * of names or other content specs. The following operators are supported:
   * sequence:    (a,b,c)
   * choice:      (a|b|c)
   * optional:    a?
   * one or more: a+
   * any number:  a*
   *
   * The special name '#PCDATA' is permitted but only if it is the first
   * element of the first group:
   * (#PCDATA|a|b)
   *
   * The top-level element must be either a choice, a sequence, or one of the
   * special names EMPTY and ANY.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readContentSpec() = { // this implementation is very lenient; it scans for balanced parens only
    skip()
    var c = peekCharacter()
    if (c == '(') {
      var depth = 0
      do {
        if (c == '(') depth += 1
        else if (c == ')') depth -= 1
        else if (c == -1) throw new XmlPullParserException("Unterminated element content spec", this, null)
        position += 1
        c = peekCharacter()
      } while (depth > 0)
      if (c == '*' || c == '?' || c == '+') position += 1
    }
    else if (c == KXmlParser.EMPTY(0)) read(KXmlParser.EMPTY)
    else if (c == KXmlParser.ANY(0)) read(KXmlParser.ANY)
    else throw new XmlPullParserException("Expected element content spec", this, null)
  }

  /**
   * Reads an attribute list declaration such as the following:
   * <!ATTLIST foo
   * bar CDATA #IMPLIED
   * quux (a|b|c) "c"
   * baz NOTATION (a|b|c) #FIXED "c">
   *
   * Each attribute has a name, type and default.
   *
   * Types are one of the built-in types (CDATA, ID, IDREF, IDREFS, ENTITY,
   * ENTITIES, NMTOKEN, or NMTOKENS), an enumerated type "(list|of|options)"
   * or NOTATION followed by an enumerated type.
   *
   * The default is either #REQUIRED, #IMPLIED, #FIXED, a quoted value, or
   * #FIXED with a quoted value.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readAttributeListDeclaration(): Unit = {
    read(KXmlParser.START_ATTLIST)
    skip()

    val elementName = readName()
    while (true) {
      skip()

      var c = peekCharacter()
      if (c == '>') {
        position += 1
        return
      }

      // attribute name
      val attributeName = readName()
      // attribute type
      skip()

      if (position + 1 >= limit && !fillBuffer(2)) throw new XmlPullParserException("Malformed attribute list", this, null)
      if (buffer(position) == KXmlParser.NOTATION(0) && buffer(position + 1) == KXmlParser.NOTATION(1)) {
        read(KXmlParser.NOTATION)
        skip()
      }

      c = peekCharacter()

      if (c == '(') {
        position += 1
        var reachedEndingParenthesis = false
        while (!reachedEndingParenthesis) {
          skip()
          readName()
          skip()
          c = peekCharacter()
          if (c == ')') {
            position += 1
            reachedEndingParenthesis = true
          }
          else if (c == '|') position += 1
          else throw new XmlPullParserException("Malformed attribute type", this, null)
        }
      }
      else readName()

      // default value
      skip()
      c = peekCharacter()
      if (c == '#') {
        position += 1
        c = peekCharacter()
        if (c == 'R') read(KXmlParser.REQUIRED)
        else if (c == 'I') read(KXmlParser.IMPLIED)
        else if (c == 'F') read(KXmlParser.FIXED)
        else throw new XmlPullParserException("Malformed attribute type", this, null)
        skip()
        c = peekCharacter()
      }

      if (c == '"' || c == '\'') {
        position += 1
        // TODO: does this do escaping correctly?
        val value = readValue(c.toChar, true, true, KXmlParser.ValueContext.ATTRIBUTE)
        if (peekCharacter() == c) position += 1
        defineAttributeDefault(elementName, attributeName, value)
      }
    }
  }

  private def defineAttributeDefault(elementName: String, attributeName: String, value: String): Unit = {
    if (defaultAttributes == null) defaultAttributes = new HashMap[String, HashMap[String, String]]()
    var elementAttributes = defaultAttributes.getOrElse(elementName, null)
    if (elementAttributes == null) {
      elementAttributes = new HashMap[String, String]()
      defaultAttributes.put(elementName, elementAttributes)
    }
    elementAttributes.put(attributeName, value)
  }

  /**
   * Read an entity declaration. The value of internal entities are inline:
   * <!ENTITY foo "bar">
   *
   * The values of external entities must be retrieved by URL or path:
   * <!ENTITY foo SYSTEM "http://host/file">
   * <!ENTITY foo PUBLIC "-//Android//Foo//EN" "http://host/file">
   * <!ENTITY foo SYSTEM "../file.png" NDATA png>
   *
   * Entities may be general or parameterized. Parameterized entities are
   * marked by a percent sign. Such entities may only be used in the DTD:
   * <!ENTITY % foo "bar">
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readEntityDeclaration(): Unit = {
    read(KXmlParser.START_ENTITY)
    var generalEntity = true

    skip()
    if (peekCharacter() == '%') {
      generalEntity = false
      position += 1
      skip()
    }

    val name = readName()

    skip()
    val quote = peekCharacter()
    var entityValue: String = null
    if (quote == '"' || quote == '\'') {
      position += 1
      entityValue = readValue(quote.toChar, true, false, KXmlParser.ValueContext.ENTITY_DECLARATION)
      if (peekCharacter() == quote) position += 1
    }
    else if (readExternalId(requireSystemName = true, assignFields = false)) {
      /*
       * Map external entities to the empty string. This is dishonest,
       * but it's consistent with Android's Expat pull parser.
       */
      entityValue = ""
      skip()
      if (peekCharacter() == KXmlParser.NDATA(0)) {
        read(KXmlParser.NDATA)
        skip()
        readName()
      }
    }
    else throw new XmlPullParserException("Expected entity value or external ID", this, null)

    if (generalEntity && processDocDecl) {
      if (documentEntities == null) documentEntities = new HashMap[String, Array[Char]]
      documentEntities.put(name, entityValue.toCharArray)
    }

    skip()
    read('>')
  }

  @throws[IOException]
  @throws[XmlPullParserException]
  private def readNotationDeclaration(): Unit = {
    read(KXmlParser.START_NOTATION)
    skip()
    readName()

    if (!readExternalId(requireSystemName = false, assignFields = false))
      throw new XmlPullParserException("Expected external ID or public ID for notation", this, null)

    skip()
    read('>')
  }

  @throws[IOException]
  @throws[XmlPullParserException]
  private def readEndTag(): Unit = {
    read('<')
    read('/')
    name = readName() // TODO: pass the expected name in as a hint?
    skip()
    read('>')

    val sp = (depth - 1) * 4
    if (depth == 0) {
      checkRelaxed("read end tag " + name + " with no tags open")
      `type` = COMMENT
      return
    }

    if (name == elementStack(sp + 3)) {
      namespace = elementStack(sp)
      prefix = elementStack(sp + 1)
      name = elementStack(sp + 2)
    }
    else if (!relaxed)
      throw new XmlPullParserException(
        "expected: /" + elementStack(sp + 3) + " read: " + name,
        this,
        null
      )
  }

  /**
   * Returns the type of the next token.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def peekType(inDeclaration: Boolean): Int = {
    if (position >= limit && !fillBuffer(1))
      return END_DOCUMENT

    buffer(position) match {
      case '&' =>
        ENTITY_REF // &
      case '<' =>
        if (position + 3 >= limit && !fillBuffer(4))
          throw new XmlPullParserException("Dangling <", this, null)

        buffer(position + 1) match {
          case '/' =>
            END_TAG // </
          case '?' =>
            // we're looking for "<?xml " with case insensitivity
            if (
              (position + 5 < limit || fillBuffer(6)) &&
              (buffer(position + 2) == 'x' || buffer(position + 2) == 'X') &&
              (buffer(position + 3) == 'm' || buffer(position + 3) == 'M') &&
              (buffer(position + 4) == 'l' || buffer(position + 4) == 'L') &&
              (buffer(position + 5) == ' ')
            ) {
              KXmlParser.XML_DECLARATION // <?xml
            }
            else {
              PROCESSING_INSTRUCTION // <?
            }
          case '!' =>
            buffer(position + 2) match {
              case 'D' =>
                return DOCDECL // <!D
              case '[' =>
                return CDSECT // <![
              case '-' =>
                return COMMENT // <!-
              case 'E' =>
                buffer(position + 3) match {
                  case 'L' =>
                    return KXmlParser.ELEMENTDECL // <!EL
                  case 'N' =>
                    return KXmlParser.ENTITYDECL // <!EN
                }
              case 'A' =>
                return KXmlParser.ATTLISTDECL // <!A
              case 'N' =>
                return KXmlParser.NOTATIONDECL // <!N
            }
            throw new XmlPullParserException("Unexpected <!", this, null)
          case _ =>
            START_TAG // <
        }
      case '%' =>
        if (inDeclaration) KXmlParser.PARAMETER_ENTITY_REF
        else TEXT
      case _ =>
        TEXT
    }
  }

  /**
   * Sets name and attributes
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def parseStartTag(xmldecl: Boolean, throwOnResolveFailure: Boolean): Unit = {
    if (!xmldecl) read('<')
    name = readName()
    attributeCount = 0

    var continue = true
    while (continue) {
      skip()

      if (position >= limit && !fillBuffer(1)) {
        checkRelaxed(KXmlParser.UNEXPECTED_EOF)
        return
      }

      val c = buffer(position)

      if (xmldecl) {
        if (c == '?') {
          position += 1
          read('>')
          return
        }
      } else {
        if (c == '/') {
          degenerated = true
          position += 1
          skip()
          read('>')
          continue = false
        }
        else if (c == '>') {
          position += 1
          continue = false
        }
      }

      if (continue) {
        val attrName = readName()
        val i = attributeCount * 4
        attributeCount += 1
        attributes = ensureCapacity(attributes, i + 4)
        attributes(i) = ""
        attributes(i + 1) = null
        attributes(i + 2) = attrName

        skip()
        if (position >= limit && !fillBuffer(1)) {
          checkRelaxed(KXmlParser.UNEXPECTED_EOF)
          return
        }
        if (buffer(position) == '=') {
          position += 1
          skip()

          if (position >= limit && !fillBuffer(1)) {
            checkRelaxed(KXmlParser.UNEXPECTED_EOF)
            return
          }

          var delimiter = buffer(position)
          if (delimiter == '\'' || delimiter == '"') position += 1
          else if (relaxed) delimiter = ' '
          else throw new XmlPullParserException("attr value delimiter missing!", this, null)

          attributes(i + 3) = readValue(delimiter, true, throwOnResolveFailure, KXmlParser.ValueContext.ATTRIBUTE)

          if (delimiter != ' ' && peekCharacter() == delimiter) position += 1 // end quote
        }
        else if (relaxed) attributes(i + 3) = attrName
        else {
          checkRelaxed("Attr.value missing f. " + attrName)
          attributes(i + 3) = attrName
        }
      }
    }

    val sp = depth * 4
    depth += 1

    if (depth == 1) parsedTopLevelStartTag = true
    elementStack = ensureCapacity(elementStack, sp + 4)
    elementStack(sp + 3) = name

    if (depth >= nspCounts.length) {
      val bigger = new Array[Int](depth + 4)
      System.arraycopy(nspCounts, 0, bigger, 0, nspCounts.length)
      nspCounts = bigger
    }

    nspCounts(depth) = nspCounts(depth - 1)

    if (processNsp) adjustNsp()
    else namespace = ""

    // For consistency with Expat, add default attributes after fixing namespaces.
    if (defaultAttributes != null) {
      val elementDefaultAttributes = defaultAttributes.getOrElse(name, null)
      if (elementDefaultAttributes != null) {
        for ((k,v) <- elementDefaultAttributes) {
          if (getAttributeValue(null, k) != null) {
            // an explicit value overrides the default
          } else {
            val i = attributeCount * 4
            attributeCount += 1
            attributes = ensureCapacity(attributes, i + 4)
            attributes(i) = ""
            attributes(i + 1) = null
            attributes(i + 2) = k
            attributes(i + 3) = v
          }
        }
      }
    }

    elementStack(sp) = namespace
    elementStack(sp + 1) = prefix
    elementStack(sp + 2) = name
  }

  /**
   * Reads an entity reference from the buffer, resolves it, and writes the
   * resolved entity to {@code out}. If the entity cannot be read or resolved,
   * {@code out} will contain the partial entity reference.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readEntity(
    out: java.lang.StringBuilder,
    isEntityToken: Boolean,
    throwOnResolveFailure: Boolean,
    valueContext: KXmlParser.ValueContext
  ): Unit = {

    val start = out.length
    if (buffer({position += 1; position - 1}) != '&')
      throw new AssertionError()

    out.append('&')

    var reachedSemiColon = false
    while (!reachedSemiColon) {
      val c = peekCharacter()
      if (c == ';') {
        out.append(';')
        position += 1
        reachedSemiColon = true
      }
      else if (c >= 128 || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-' || c == '#') {
        position += 1
        out.append(c.toChar)
      }
      else if (relaxed) {
        // intentionally leave the partial reference in 'out'
        return
      }
      else throw new XmlPullParserException("unterminated entity ref", this, null)
    }

    val code = out.substring(start + 1, out.length - 1)

    if (isEntityToken) name = code
    if (code.startsWith("#")) {
      try {
        val c = if (code.startsWith("#x"))
          Integer.parseInt(code.substring(2), 16)
        else
          code.substring(1).toInt

        out.delete(start, out.length)
        //out.append(c.toChar)
        out.appendCodePoint(c)

        unresolved = false
        return
      } catch {
        case notANumber: NumberFormatException =>
          throw new XmlPullParserException("Invalid character reference: &" + code)
        case invalidCodePoint: IllegalArgumentException =>
          throw new XmlPullParserException("Invalid character reference: &" + code)
      }
    }

    if (valueContext == KXmlParser.ValueContext.ENTITY_DECLARATION) { // keep the unresolved &code; in the text to resolve later
      // keep the unresolved &code; in the text to resolve later
      return
    }

    val defaultEntity = KXmlParser.DEFAULT_ENTITIES.get(code)
    if (defaultEntity != null) {
      out.delete(start, out.length)
      unresolved = false
      out.append(defaultEntity)
      return
    }

    var resolved: Array[Char] = null
    if (documentEntities != null) {
      resolved = documentEntities.getOrElse(code, null)
      if (resolved != null) {
        out.delete(start, out.length)
        unresolved = false

        if (processDocDecl) pushContentSource(resolved) // parse the entity as XML
        else out.append(resolved) // include the entity value as text

        return
      }
    }

    /*
     * The parser skipped an external DTD, and now we've encountered an
     * unknown entity that could have been declared there.
     * Map it to the empty string.
     * This is dishonest, but it's consistent with Android's old ExpatPullParser.
     */
    if (systemId != null) {
      out.delete(start, out.length)
      return
    }

    // keep the unresolved entity "&code;" in the text for relaxed clients
    unresolved = true
    if (throwOnResolveFailure)
      checkRelaxed(s"unresolved: &$code;")
  }

  /**
   * Returns the current text or attribute value. This also has the side
   * effect of setting isWhitespace to false if a non-whitespace character is
   * encountered.
   *
   * @param delimiter {@code <} for text, {@code "} and {@code '} for quoted
   *                  attributes, or a space for unquoted attributes.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def readValue(
    delimiter: Char,
    resolveEntities: Boolean,
    throwOnResolveFailure: Boolean,
    valueContext: KXmlParser.ValueContext
  ): String = {

    /*
     * This method returns all of the characters from the current position
     * through to an appropriate delimiter.
     *
     * If we're lucky (which we usually are), we'll return a single slice of the buffer.
     * This fast path avoids allocating a string builder.
     *
     * There are 6 unlucky characters we could encounter:
     *  - "&":  entities must be resolved.
     *  - "%":  parameter entities are unsupported in entity values.
     *  - "<":  this isn't permitted in attributes unless relaxed.
     *  - "]":  this requires a lookahead to defend against the forbidden
     *          CDATA section delimiter "]]>".
     *  - "\r": If a "\r" is followed by a "\n", we discard the "\r". If it
     *          isn't followed by "\n", we replace "\r" with either a "\n"
     *          in text nodes or a space in attribute values.
     *  - "\n": In attribute values, "\n" must be replaced with a space.
     *
     * We could also get unlucky by needing to refill the buffer midway through the text.
     */
    var start = position
    var result: java.lang.StringBuilder = null

    // if a text section was already started, prefix the start
    if (valueContext == KXmlParser.ValueContext.TEXT && text != null) {
      result = new java.lang.StringBuilder()
      result.append(text)
    }

    var continue = true
    while (continue) {
      /*
       * Make sure we have at least a single character to read from the
       * buffer. This mutates the buffer, so save the partial result
       * to the slow path string builder first.
       */
      if (position >= limit) {
        if (start < position) {
          if (result == null) result = new java.lang.StringBuilder()
          result.append(buffer, start, position - start)
        }

        if (!fillBuffer(1))
          return if (result != null) result.toString else ""

        start = position
      }

      var c = buffer(position)

      if (
        c == delimiter ||
        (delimiter == ' ' && (c <= ' ' || c == '>')) ||
        c == '&' && !resolveEntities
      ) {
        continue = false
      } else {
        if (
          c != '\r' &&
          (c != '\n' || valueContext != KXmlParser.ValueContext.ATTRIBUTE) &&
          c != '&' &&
          c != '<' &&
          (c != ']' || valueContext != KXmlParser.ValueContext.TEXT) &&
          (c != '%' || valueContext != KXmlParser.ValueContext.ENTITY_DECLARATION)
        ) {
          _isWhitespace &= (c <= ' ')
          position += 1
        } else {
          /*
           * We've encountered an unlucky character!
           * Convert from fast path to slow path if we haven't done so already.
           */
          if (result == null) result = new java.lang.StringBuilder()
          result.append(buffer, start, position - start)

          var charIsAmpersand = false
          if (c == '\r') {
            if ((position + 1 < limit || fillBuffer(2)) && buffer(position + 1) == '\n') position += 1
            c = if (valueContext == KXmlParser.ValueContext.ATTRIBUTE) ' '
            else '\n'
          }
          else if (c == '\n') c = ' '
          else if (c == '&') {
            _isWhitespace = false // TODO: what if the entity resolves to whitespace?
            readEntity(result, false, throwOnResolveFailure, valueContext)
            start = position
            charIsAmpersand = true
          }
          else if (c == '<') {
            if (valueContext == KXmlParser.ValueContext.ATTRIBUTE) checkRelaxed("Illegal: \"<\" inside attribute value")
            _isWhitespace = false
          }
          else if (c == ']') {
            if ((position + 2 < limit || fillBuffer(3)) && buffer(position + 1) == ']' && buffer(position + 2) == '>') checkRelaxed("Illegal: \"]]>\" outside CDATA section")
            _isWhitespace = false
          }
          else if (c == '%') throw new XmlPullParserException("This parser doesn't support parameter entities", this, null)
          else throw new AssertionError

          if (!charIsAmpersand) {
            position += 1
            result.append(c)
            start = position
          }
        }
      }

    } // ends while loop

    if (result == null) stringPool.get(buffer, start, position - start)
    else {
      result.append(buffer, start, position - start)
      result.toString
    }
  }

  @throws[IOException]
  @throws[XmlPullParserException]
  private def read(expected: Char): Unit = {
    val c = peekCharacter()
    if (c != expected) {
      checkRelaxed("expected: '" + expected + "' actual: '" + c.toChar + "'")
      if (c == -1) return // On EOF, don't move position beyond limit
    }
    position += 1
  }

  @throws[IOException]
  @throws[XmlPullParserException]
  private def read(chars: Array[Char]): Unit = {
    if (position + chars.length > limit && !fillBuffer(chars.length)) {
      checkRelaxed("expected: '" + new String(chars) + "' but was EOF")
      return
    }
    for (i <- 0 until chars.length) {
      if (buffer(position + i) != chars(i)) checkRelaxed("expected: \"" + new String(chars) + "\" but was \"" + new String(buffer, position, chars.length) + "...\"")
    }
    position += chars.length
  }

  @throws[IOException]
  @throws[XmlPullParserException]
  private def peekCharacter(): Int = {
    if (position < limit || fillBuffer(1))
      return buffer(position)
    -1
  }

  /**
   * Returns true once {@code limit - position >= minimum}. If the data is
   * exhausted before that many characters are available, this returns
   * false.
   */
  @throws[IOException]
  @throws[XmlPullParserException]
  private def fillBuffer(minimum: Int): Boolean = {

    // If we've exhausted the current content source, remove it
    while (nextContentSource != null) {
      if (position < limit)
        throw new XmlPullParserException("Unbalanced entity!", this, null)

      /**
       * Replaces the current exhausted buffer with the next buffer in the chain.
       */
      buffer = nextContentSource.buffer
      position = nextContentSource.position
      limit = nextContentSource.limit
      nextContentSource = nextContentSource.next

      if ( (limit - position) >= minimum)
        return true
    }

    // Before clobbering the old characters, update where buffer starts
    var i = 0
    while (i < position) {
      if (buffer(i) == '\n') {
        bufferStartLine += 1
        bufferStartColumn = 0
      }
      else bufferStartColumn += 1

      i += 1
    }

    if (bufferCapture != null)
      bufferCapture.appendAll(buffer, 0, position)

    if (limit != position) {
      limit -= position
      System.arraycopy(buffer, position, buffer, 0, limit)
    }
    else limit = 0

    position = 0

    var total = 0
    while ( {total = reader.read(buffer, limit, buffer.length - limit); total != -1}) {
      limit += total
      if (limit >= minimum)
        return true
    }

    false
  }

  /**
   * Returns an element or attribute name.
   * This is always non-empty for non-relaxed parsers.
   */

  @throws[IOException]
  @throws[XmlPullParserException]
  private def readName(): String = {
    if (position >= limit && !fillBuffer(1)) {
      checkRelaxed("name expected")
      return ""
    }

    var start = position
    var result: StringBuilder = null

    // read the first character
    var c = buffer(position)
    if (
      (c >= 'a' && c <= 'z') ||
      (c >= 'A' && c <= 'Z') ||
      c == '_' ||
      c == ':' ||
      c >= '\u00c0' || // TODO: check the XML spec
      relaxed
    ) {
      position += 1
    } else {
      checkRelaxed("name expected")
      return ""
    }

    while (true) {
      /*
       * Make sure we have at least a single character to read from the
       * buffer. This mutates the buffer, so save the partial result
       * to the slow path string builder first.
       */
      if (position >= limit) {
        if (result == null) {
          result = new StringBuilder()
        }

        result.appendAll(buffer, start, position - start)
        if (!fillBuffer(1)) return result.toString
        start = position
      }

      // read another character
      c = buffer(position)

      if (
        (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z') ||
        (c >= '0' && c <= '9') ||
        c == '_' ||
        c == '-' ||
        c == ':' ||
        c == '.' ||
        c >= '\u00b7' // TODO: check the XML spec
      ) {
        position += 1
      } else {
        // we encountered a non-name character. done!
        if (result == null)
          return stringPool.get(buffer, start, position - start)
        else {
          result.appendAll(buffer, start, position - start)
          return result.toString
        }
      }
    }

    null // should not happen
  }

  @throws[IOException]
  @throws[XmlPullParserException]
  private def skip(): Unit = {
    while (position < limit || fillBuffer(1)) {
      val c = buffer(position)
      if (c > ' ') return
      position += 1
    }
  }

  //  public part starts here...

  @throws[XmlPullParserException]
  def setInput(reader: Reader): Unit = {
    this.reader = reader

    `type` = START_DOCUMENT
    parsedTopLevelStartTag = false
    name = null
    namespace = null
    degenerated = false
    attributeCount = -1
    encoding = null
    version = null
    standalone = null

    if (reader == null)
      return

    position = 0
    limit = 0
    bufferStartLine = 0
    bufferStartColumn = 0
    depth = 0
    documentEntities = null
  }

  @throws[XmlPullParserException]
  def setInput(is: InputStream, charset: String): Unit = {
    position = 0
    limit = 0

    scala.Predef.require(is != null, "is == null")

    val detectCharset = charset == null
    var detectedCharset = charset

    try {
      if (detectCharset) {
        // read the four bytes looking for an indication of the encoding in use
        var firstFourBytes = 0
        var i = 0
        while (limit < 4 && i != -1) {
          i = is.read()
          if (i != -1) {
            firstFourBytes = (firstFourBytes << 8) | i
            buffer(limit) = i.toChar
            limit += 1
          }
        }

        if (limit == 4) {
          firstFourBytes match {
            case 0x00000FEFF => // UTF-32BE BOM
              detectedCharset = "UTF-32BE"
              limit = 0
            case 0x0FFFE0000 => // UTF-32LE BOM
              detectedCharset = "UTF-32LE"
              limit = 0
            case 0x0000003c => // '<' in UTF-32BE
              detectedCharset = "UTF-32BE"
              buffer(0) = '<'
              limit = 1
            case 0x03c000000 => // '<' in UTF-32LE
              detectedCharset = "UTF-32LE"
              buffer(0) = '<'
              limit = 1
            case 0x0003c003f => // "<?" in UTF-16BE
              detectedCharset = "UTF-16BE"
              buffer(0) = '<'
              buffer(1) = '?'
              limit = 2
            case 0x03c003f00 => // "<?" in UTF-16LE
              detectedCharset = "UTF-16LE"
              buffer(0) = '<'
              buffer(1) = '?'
              limit = 2
            case 0x03c3f786d => { // "<?xm" in ASCII etc.
              i = is.read()

              while (i != -1) {
                buffer({limit += 1; limit - 1}) = i.toChar
                if (i == '>') {
                  val s = new String(buffer, 0, limit)
                  var i0 = s.indexOf("encoding")
                  if (i0 != -1) {
                    while (s.charAt(i0) != '"' && s.charAt(i0) != '\'') i0 += 1
                    val deli = s.charAt({i0 += 1; i0 - 1})
                    val i1 = s.indexOf(deli, i0)
                    detectedCharset = s.substring(i0, i1)
                  }
                  i = -1
                } else {
                  i = is.read()
                }
              }
            }
            case _ =>
              // handle a byte order mark followed by something other than <?
              if ((firstFourBytes & 0x0ffff0000) == 0x0feff0000) {
                detectedCharset = "UTF-16BE"
                buffer(0) = ((buffer(2) << 8) | buffer(3)).asInstanceOf[Char]
                limit = 1
              }
              else if ((firstFourBytes & 0x0ffff0000) == 0x0fffe0000) {
                detectedCharset = "UTF-16LE"
                buffer(0) = ((buffer(3) << 8) | buffer(2)).asInstanceOf[Char]
                limit = 1
              }
              else if ((firstFourBytes & 0x0ffffff00) == 0x0efbbbf00) {
                detectedCharset = "UTF-8"
                buffer(0) = buffer(3)
                limit = 1
              }
          }
        } // ends if (limit == 4)
      }

      if (detectedCharset == null) detectedCharset = "UTF-8"

      val savedLimit = limit
      setInput(new InputStreamReader(is, detectedCharset))
      encoding = detectedCharset
      limit = savedLimit

      /*
       * Skip the optional BOM if we didn't above. This decrements limit
       * rather than incrementing position so that <?xml version='1.0'?>
       * is still at character 0.
       */
      if (!detectCharset && peekCharacter() == 0xfeff) {
        limit -= 1
        System.arraycopy(buffer, 1, buffer, 0, limit)
      }
    } catch {
      case e: Exception =>
        throw new XmlPullParserException("Invalid stream or encoding: " + e, this, e)
    }
  }

  @throws[IOException]
  def close(): Unit = {
    if (reader != null) reader.close()
  }

  def getFeature(feature: String): Boolean = {
    if (feature == FEATURE_PROCESS_NAMESPACES) processNsp
    else if (feature == KXmlParser.FEATURE_RELAXED) relaxed
    else if (feature == FEATURE_PROCESS_DOCDECL) processDocDecl
    // Note: added by DBO
    else if (feature == FEATURE_REPORT_NAMESPACE_ATTRIBUTES) keepNamespaceAttrs
    // Note: DBO replaced false by XmlPullParserException
    //else false
    else throw new XmlPullParserException("unsupported feature: " + feature, this, null)
  }

  @throws[XmlPullParserException]
  def setFeature(feature: String, value: Boolean): Unit = {
    if (feature == FEATURE_PROCESS_NAMESPACES) processNsp = value
    else if (feature == FEATURE_PROCESS_DOCDECL) processDocDecl = value
    else if (feature == KXmlParser.FEATURE_RELAXED) relaxed = value
    // Note: added by DBO
    else if (feature == FEATURE_REPORT_NAMESPACE_ATTRIBUTES) keepNamespaceAttrs = value
    else throw new XmlPullParserException("unsupported feature: " + feature, this, null)
  }

  def getInputEncoding(): String = encoding

  @throws[XmlPullParserException]
  def defineEntityReplacementText(entity: String, value: String): Unit = {
    if (processDocDecl) throw new IllegalStateException("Entity replacement text may not be defined with DOCTYPE processing enabled.")
    if (reader == null) throw new IllegalStateException("Entity replacement text must be defined after setInput()")
    if (documentEntities == null) documentEntities = new HashMap[String, Array[Char]]()
    documentEntities.put(entity, value.toCharArray)
  }

  def getProperty(property: String): Any = {
    if (property == KXmlParser.PROPERTY_XMLDECL_VERSION) version
    else if (property == KXmlParser.PROPERTY_XMLDECL_STANDALONE) standalone
    else if (property == KXmlParser.PROPERTY_LOCATION) if (location != null) location
    else reader.toString
    else null
  }

  @throws[XmlPullParserException]
  def setProperty(property: String, value: Any): Unit = {
    if (property == KXmlParser.PROPERTY_LOCATION) location = String.valueOf(value)
    else throw new XmlPullParserException("unsupported property: " + property)
  }

  /**
   * Returns the root element's name if it was declared in the DTD. This
   * equals the first tag's name for valid documents.
   */
  def getRootElementName(): String = rootElementName

  /**
   * Returns the document's system ID if it was declared. This is typically a
   * string like {@code http://www.w3.org/TR/html4/strict.dtd}.
   */
  def getSystemId(): String = systemId

  /**
   * Returns the document's public ID if it was declared. This is typically a
   * string like {@code -//W3C//DTD HTML 4.01//EN}.
   */
  def getPublicId(): String = publicId

  def getNamespaceCount(depth: Int): Int = {
    if (depth > this.depth) throw new IndexOutOfBoundsException
    nspCounts(depth)
  }

  def getNamespacePrefix(pos: Int): String = nspStack(pos * 2)

  def getNamespaceUri(pos: Int): String = nspStack((pos * 2) + 1)

  def getNamespace(prefix: String): String = {
    if (prefix == "xml") return "http://www.w3.org/XML/1998/namespace"
    if (prefix == "xmlns") return "http://www.w3.org/2000/xmlns/"

    var i = (getNamespaceCount(depth) << 1) - 2
    while (i >= 0) {
      if (prefix == null) {
        if (nspStack(i) == null)
          return nspStack(i + 1)
      }
      else if (prefix == nspStack(i))
        return nspStack(i + 1)

      i -= 2
    }

    null
  }

  def getDepth(): Int = depth

  def getPositionDescription(): String = {
    val buf = new StringBuilder(
      if (`type` < TYPES.length) TYPES(`type`) else "unknown"
    )

    buf.append(' ')

    if (`type` == START_TAG || `type` == END_TAG) {
      if (degenerated) buf.append("(empty) ")
      buf.append('<')

      if (`type` == END_TAG) buf.append('/')
      if (prefix != null) buf.append(s"{$namespace}$prefix:")
      buf.append(name)

      val cnt = attributeCount * 4

      var i = 0
      while (i < cnt) {
        buf.append(' ')
        if (attributes(i + 1) != null) buf.append(s"{${attributes(i)}${attributes(i + 1)}:")
        buf.append(s"${attributes(i + 2)}='${attributes(i + 3)}'")

        i += 4
      }

      buf.append('>')
    }
    else if (`type` == IGNORABLE_WHITESPACE) {}
    else if (`type` != TEXT) buf.append(getText())
    else if (_isWhitespace) buf.append("(whitespace)")
    else {
      var text = getText()
      if (text.length > 16) text = text.substring(0, 16) + "..."
      buf.append(text)
    }

    buf.append("@" + getLineNumber() + ":" + getColumnNumber())
    if (location != null) {
      buf.append(" in ")
      buf.append(location)
    }
    else if (reader != null) {
      buf.append(" in ")
      buf.append(reader.toString)
    }

    buf.toString
  }

  def getLineNumber(): Int = {
    var result = bufferStartLine
    var i = 0
    while (i < position) {
      if (buffer(i) == '\n') result += 1
      i += 1
    }

    result + 1 // the first line is '1'
  }

  def getColumnNumber(): Int = {
    var result = bufferStartColumn

    var i = 0
    while (i < position) {
      if (buffer(i) == '\n') result = 0
      else result += 1

      i += 1
    }

    result + 1 // the first column is '1'
  }

  @throws[XmlPullParserException]
  def isWhitespace(): Boolean = {
    if (`type` != TEXT && `type` != IGNORABLE_WHITESPACE && `type` != CDSECT)
      throw new XmlPullParserException(KXmlParser.ILLEGAL_TYPE, this, null)
    _isWhitespace
  }

  def getText(): String = {
    if (`type` < TEXT || (`type` == ENTITY_REF && unresolved)) null
    else if (text == null) ""
    else text
  }

  def getTextCharacters(poslen: Array[Int]): Array[Char] = {
    val text = getText()
    if (text == null) {
      poslen(0) = -1
      poslen(1) = -1
      return null
    }
    val result = text.toCharArray
    poslen(0) = 0
    poslen(1) = result.length
    result
  }

  def getNamespace(): String = namespace

  def getName(): String = name

  def getPrefix(): String = prefix

  @throws[XmlPullParserException]
  def isEmptyElementTag(): Boolean = {
    if (`type` != START_TAG)
      throw new XmlPullParserException(KXmlParser.ILLEGAL_TYPE, this, null)
    degenerated
  }

  def getAttributeCount(): Int = attributeCount

  def getAttributeType(index: Int) = "CDATA"

  def isAttributeDefault(index: Int) = false

  def getAttributeNamespace(index: Int): String = {
    if (index >= attributeCount) throw new IndexOutOfBoundsException
    attributes(index * 4)
  }

  def getAttributeName(index: Int): String = {
    if (index >= attributeCount) throw new IndexOutOfBoundsException
    attributes((index * 4) + 2)
  }

  def getAttributePrefix(index: Int): String = {
    if (index >= attributeCount) throw new IndexOutOfBoundsException
    attributes((index * 4) + 1)
  }

  def getAttributeValue(index: Int): String = {
    if (index >= attributeCount) throw new IndexOutOfBoundsException
    attributes((index * 4) + 3)
  }

  def getAttributeValue(namespace: String, name: String): String = {
    var i = (attributeCount * 4) - 4
    while ( {i >= 0}) {
      if (attributes(i + 2).equals(name) && (namespace == null || attributes(i).equals(namespace))) return attributes(i + 3)

      i -= 4
    }
    null
  }

  @throws[XmlPullParserException]
  def getEventType(): Int = `type`

  // utility methods to make XML parsing easier ...

  @throws[XmlPullParserException]
  @throws[IOException]
  def nextTag(): Int = {
    next()

    if (`type` == TEXT && _isWhitespace)
      next()

    if (`type` != END_TAG && `type` != START_TAG)
      throw new XmlPullParserException("unexpected type", this, null)

    `type`
  }

  @throws[XmlPullParserException]
  @throws[IOException]
  def require(`type`: Int, namespace: String, name: String): Unit = {
    if (
      `type` != this.`type` ||
      (namespace != null && namespace != getNamespace()) ||
      (name != null && name != getName())
    ) {
      throw new XmlPullParserException(
        "expected: " + TYPES(`type`) + " {" + namespace + "}" + name,
        this,
        null
      )
    }
  }

  @throws[XmlPullParserException]
  @throws[IOException]
  def nextText(): String = {
    if (`type` != START_TAG)
      throw new XmlPullParserException("precondition: START_TAG", this, null)

    next()

    var result: String = null
    if (`type` == TEXT) {
      result = getText()
      next()
    }
    else result = ""

    if (`type` != END_TAG)
      throw new XmlPullParserException("END_TAG expected", this, null)

    result
  }

  /**
   * Prepends the characters of {@code newBuffer} to be read before the
   * current buffer.
   */
  private def pushContentSource(newBuffer: Array[Char]): Unit = {
    nextContentSource = new KXmlParser.ContentSource(nextContentSource, buffer, position, limit)
    buffer = newBuffer
    position = 0
    limit = newBuffer.length
  }

}