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

package org.kxml2.io

import java.io._
import java.util.Locale
import org.xmlpull.v1._

object KXmlSerializer {

  private val BUFFER_LEN = 8192

  // BEGIN Android-added
  //    static final String UNDEFINED = ":";
  private def reportInvalidCharacter(ch: Char): Unit = {
    throw new IllegalArgumentException("Illegal character (U+" + Integer.toHexString(ch.toInt) + ")")
  }
  // END Android-added
}

class KXmlSerializer extends XmlSerializer {

  final private val mText = new Array[Char](KXmlSerializer.BUFFER_LEN)

  private var mPos = 0
  private var writer: Writer = _
  private var pending = false
  private var auto = 0
  private var depth = 0

  private var elementStack = new Array[String](12)
  //nsp/prefix/name
  private var nspCounts = new Array[Int](4)
  private var nspStack = new Array[String](8)
  //prefix/nsp; both empty are ""
  private var indent = new Array[Boolean](4)
  private var unicode = false
  private var encoding: String = _

  @throws[IOException]
  private def append(c: Char): Unit = {
    if (mPos >= KXmlSerializer.BUFFER_LEN) flushBuffer()
    mText(mPos) = c
    mPos += 1
  }

  @throws[IOException]
  private def append(str: String): Unit = {
  //private def append(str: String, index: Int, length: Int): Unit = {
    var i = 0
    var maxLen = str.length

    while (maxLen > 0) {
      if (mPos == KXmlSerializer.BUFFER_LEN) flushBuffer()
      var batch = KXmlSerializer.BUFFER_LEN - mPos

      if (batch > maxLen)
        batch = maxLen

      str.getChars(i, i + batch, mText, mPos)

      i += batch
      maxLen -= batch
      mPos += batch
    }
  }

  //@throws[IOException]
  //private def append(str: String) = append(str, 0, str.length)

  @throws[IOException]
  final private def flushBuffer(): Unit = if (mPos > 0) {
    writer.write(mText, 0, mPos)
    writer.flush()
    mPos = 0
  }

  @throws[IOException]
  final private def check(close: Boolean): Unit = {
    if (!pending) return

    depth += 1
    pending = false

    if (indent.length <= depth) {
      val hlp = new Array[Boolean](depth + 4)
      System.arraycopy(indent, 0, hlp, 0, depth)
      indent = hlp
    }
    indent(depth) = indent(depth - 1)
    for (i <- nspCounts(depth - 1) until nspCounts(depth)) {
      append(" xmlns")
      if (nspStack(i * 2).nonEmpty) {
        append(':')
        append(nspStack(i * 2))
      }
      else if (getNamespace().isEmpty && nspStack(i * 2 + 1).nonEmpty) {
        throw new IllegalStateException("Cannot set default namespace for elements in no namespace")
      }
      append("=\"")
      writeEscaped(nspStack(i * 2 + 1), '"')
      append('"')
    }
    if (nspCounts.length <= depth + 1) {
      val hlp = new Array[Int](depth + 8)
      System.arraycopy(nspCounts, 0, hlp, 0, depth + 1)
      nspCounts = hlp
    }
    nspCounts(depth + 1) = nspCounts(depth)
    //   nspCounts[depth + 2] = nspCounts[depth];
    if (close) append(" />")
    else append('>')
  }

  @throws[IOException]
  final private def writeEscaped(s: String, quot: Int): Unit = {
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i)
      c match {
        case '\n' | '\r' | '\t' =>
          if (quot == -1) append(c)
          else append("&#" + c.toInt + ';')
        case '&' =>
          append("&amp;")
        case '>' =>
          append("&gt;")
        case '<' =>
          append("&lt;")
        case _ =>
          if (c == quot) {
            append(if (c == '"') "&quot;" else "&apos;")
          } else {
            // BEGIN Android-changed: refuse to output invalid characters
            // See http://www.w3.org/TR/REC-xml/#charsets for definition.
            // No other Java XML writer we know of does this, but no Java
            // XML reader we know of is able to parse the bad output we'd
            // otherwise generate.
            // Note: tab, newline, and carriage return have already been
            // handled above.
            val allowedInXml = (c >= 0x20 && c <= 0xd7ff) || (c >= 0xe000 && c <= 0xfffd)
            if (allowedInXml) if (unicode || c < 127) append(c)
            else append("&#" + c.toInt + ";")
            else if (Character.isHighSurrogate(c) && i < s.length - 1) {
              writeSurrogate(c, s.charAt(i + 1))
              i += 1
            }
            else KXmlSerializer.reportInvalidCharacter(c)
          }
        // END Android-changed
      }
      i += 1
    }
  }

  /*
  private final void writeIndent() throws IOException {
      writer.write("\r\n");
      for (int i = 0; i < depth; i++)
          writer.write(' ');
  }*/

  @throws[IOException]
  def docdecl(dd: String): Unit = {
    append("<!DOCTYPE")
    append(dd)
    append('>')
  }

  @throws[IOException]
  def endDocument(): Unit = {
    while ( {depth > 0}) endTag(elementStack(depth * 3 - 3), elementStack(depth * 3 - 1))
    flush()
  }

  @throws[IOException]
  def entityRef(name: String): Unit = {
    check(false)
    append('&')
    append(name)
    append(';')
  }

  def getFeature(name: String): Boolean = { //return false;
    if ("http://xmlpull.org/v1/doc/features.html#indent-output" == name) indent(depth)
    else false
  }

  def getPrefix(namespace: String, create: Boolean): String = {
    try getPrefix(namespace, includeDefault = false, create = create)
    catch {
      case e: IOException =>
        throw new RuntimeException(e.toString)
    }
  }

  @throws[IOException]
  final private def getPrefix(namespace: String, includeDefault: Boolean, create: Boolean): String = {
    val cand = _getPrefixCandidate(namespace, includeDefault)
    if (cand != null)
      return cand
    if (!create)
      return null

    var prefix: String = null
    if (namespace.isEmpty) prefix = ""
    else {
      while (prefix == null) {
        prefix = "n" + auto
        auto += 1

        var i = nspCounts(depth + 1) * 2 - 2
        while (i >= 0) {
          if (prefix == nspStack(i)) {
            prefix = null
            i = -1 // break loop
          } else {
            i -= 2
          }
        }
      }
    }

    val p = pending
    pending = false
    setPrefix(prefix, namespace)
    pending = p
    prefix
  }

  @throws[IOException]
  final private def _getPrefixCandidate(namespace: String, includeDefault: Boolean): String = {
    var i = nspCounts(depth + 1) * 2 - 2
    while (i >= 0) {
      if (nspStack(i + 1) == namespace && (includeDefault || nspStack(i).nonEmpty)) {
        var cand = nspStack(i)
        for (j <- i + 2 until nspCounts(depth + 1) * 2) {
          if (nspStack(j) == cand) {
            return null
          }
        }
        if (cand != null)
          return cand
      }
      i -= 2
    }

    null
  }

  def getProperty(name: String): Any = throw new RuntimeException("Unsupported property")

  @throws[IOException]
  def ignorableWhitespace(s: String): Unit = text(s)

  def setFeature(name: String, value: Boolean): Unit = if ("http://xmlpull.org/v1/doc/features.html#indent-output" == name) indent(depth) = value
  else throw new RuntimeException("Unsupported Feature")

  def setProperty(name: String, value: Any): Unit = throw new RuntimeException("Unsupported Property:" + value)

  @throws[IOException]
  def setPrefix(prefix: String, namespace: String): Unit = {
    check(false)

    val nonNullPrefix = if (prefix != null) prefix else ""
    val nonNullNamespace = if (namespace != null) namespace else ""

    val defined = getPrefix(nonNullNamespace, includeDefault = true, create = false)
    // boil out if already defined
    if (nonNullPrefix == defined) return

    val pos = nspCounts(depth + 1) << 1
    nspCounts(depth + 1) += 1

    if (nspStack.length < pos + 1) {
      val hlp = new Array[String](nspStack.length + 16)
      System.arraycopy(nspStack, 0, hlp, 0, pos)
      nspStack = hlp
    }

    nspStack(pos) = nonNullPrefix
    nspStack(pos + 1) = nonNullNamespace
  }

  def setOutput(writer: Writer): Unit = {
    this.writer = writer
    // elementStack = new String[12]; //nsp/prefix/name
    //nspCounts = new int[4];
    //nspStack = new String[8]; //prefix/nsp
    //indent = new boolean[4];
    nspCounts(0) = 2
    nspCounts(1) = 2
    nspStack(0) = ""
    nspStack(1) = ""
    nspStack(2) = "xml"
    nspStack(3) = "http://www.w3.org/XML/1998/namespace"
    pending = false
    auto = 0
    depth = 0
    unicode = false
  }

  @throws[IOException]
  def setOutput(os: OutputStream, encoding: String): Unit = {
    if (os == null) throw new IllegalArgumentException("os == null")
    setOutput(if (encoding == null) new OutputStreamWriter(os)
    else new OutputStreamWriter(os, encoding)
    )
    this.encoding = encoding

    // FIXME: DBO => Locale.US should be used???
    //if (encoding != null && encoding.toLowerCase(Locale.US).startsWith("utf")) unicode = true
    if (encoding != null && encoding.toLowerCase().startsWith("utf")) unicode = true
  }

  @throws[IOException]
  def startDocument(encoding: String, standalone: java.lang.Boolean): Unit = {
    append("<?xml version='1.0' ")
    if (encoding != null) {
      this.encoding = encoding
      // FIXME: DBO => Locale.US should be used???
      //if (encoding.toLowerCase(Locale.US).startsWith("utf")) unicode = true
      if (encoding.startsWith("utf")) unicode = true
    }
    if (this.encoding != null) {
      append("encoding='")
      append(this.encoding)
      append("' ")
    }
    if (standalone != null) {
      append("standalone='")
      append(if (standalone.booleanValue) "yes"
      else "no"
      )
      append("' ")
    }
    append("?>")
  }

  @throws[IOException]
  def startTag(namespace: String, name: String): KXmlSerializer = {
    check(false)
    //        if (namespace == null)
    //            namespace = "";
    if (indent(depth)) {
      append("\r\n")
      for (i <- 0 until depth) {append("  ")}
    }
    var esp = depth * 3
    if (elementStack.length < esp + 3) {
      val hlp = new Array[String](elementStack.length + 12)
      System.arraycopy(elementStack, 0, hlp, 0, esp)
      elementStack = hlp
    }
    val prefix = if (namespace == null) ""
    else getPrefix(namespace, includeDefault = true, create = true)
    if (namespace != null && namespace.isEmpty) for (i <- nspCounts(depth) until nspCounts(depth + 1)) {
      if (nspStack(i * 2).isEmpty && nspStack(i * 2 + 1).nonEmpty)
        throw new IllegalStateException("Cannot set default namespace for elements in no namespace")
    }
    elementStack(esp) = namespace; esp += 1
    elementStack(esp) = prefix; esp += 1
    elementStack(esp) = name
    append('<')

    if (prefix.nonEmpty) {
      append(prefix)
      append(':')
    }
    append(name)
    pending = true
    this
  }

  @throws[IOException]
  def attribute(namespace: String, name: String, value: String): KXmlSerializer = {
    if (!pending) throw new IllegalStateException("illegal position for attribute")
    //        int cnt = nspCounts[depth];
    val nonNullNamespace = if (namespace == null) "" else namespace
    //        depth--;
    //        pending = false;
    val prefix = if (nonNullNamespace.isEmpty) ""
    else getPrefix(nonNullNamespace, includeDefault =  false, create = true)
    //        pending = true;
    //        depth++;
    /*        if (cnt != nspCounts[depth]) {
                        writer.write(' ');
                        writer.write("xmlns");
                        if (nspStack[cnt * 2] != null) {
                            writer.write(':');
                            writer.write(nspStack[cnt * 2]);
                        }
                        writer.write("=\"");
                        writeEscaped(nspStack[cnt * 2 + 1], '"');
                        writer.write('"');
                    }
                    */
    append(' ')

    if (prefix.nonEmpty) {
      append(prefix)
      append(':')
    }

    append(name)
    append('=')

    val q = if (value.indexOf('"') == -1) '"' else '\''

    append(q)
    writeEscaped(value, q)
    append(q)

    this
  }

  @throws[IOException]
  def flush(): Unit = {
    check(false)
    flushBuffer()
  }

  /*
          public void close() throws IOException {
              check();
              writer.close();
          }
      */
  @throws[IOException]
  def endTag(namespace: String, name: String): KXmlSerializer = {
    if (!pending) depth -= 1
    //          namespace = "";
    if ((namespace == null && elementStack(depth * 3) != null) ||
      (namespace != null && !(namespace == elementStack(depth * 3))) ||
      !(elementStack(depth * 3 + 2) == name)
    ) {
      throw new IllegalArgumentException("</{" + namespace + "}" + name + "> does not match start")
    }

    if (pending) {
      check(true)
      depth -= 1
    }
    else {
      if (indent(depth + 1)) {
        append("\r\n")
        for (i <- 0 until depth) {append("  ")}
      }
      append("</")
      val prefix = elementStack(depth * 3 + 1)
      if (prefix.nonEmpty) {
        append(prefix)
        append(':')
      }
      append(name)
      append('>')
    }

    nspCounts(depth + 1) = nspCounts(depth)

    this
  }

  def getNamespace(): String = {
    if (getDepth() == 0) null
    else elementStack(getDepth() * 3 - 3)
  }

  def getName(): String = {
    if (getDepth() == 0) null
    else elementStack(getDepth() * 3 - 1)
  }

  def getDepth(): Int = {
    if (pending) depth + 1
    else depth
  }

  @throws[IOException]
  def text(text: String): KXmlSerializer = {
    check(false)
    indent(depth) = false
    writeEscaped(text, -1)
    this
  }

  @throws[IOException]
  def text(buf: Array[Char], start: Int, len: Int): KXmlSerializer = {
    this.text(new String(buf, start, len))
    this
  }

  @throws[IOException]
  def cdsect(data: String): Unit = {
    check(false)

    // BEGIN Android-changed: ]]> is not allowed within a CDATA,
    // so break and start a new one when necessary.
    val safeData = data.replace("]]>", "]]]]><![CDATA[>")
    val safeDataLen = safeData.length

    append("<![CDATA[")

    var i = 0
    while (i < safeDataLen) {
      val ch = safeData.charAt(i)
      val allowedInCdata = (ch >= 0x20 && ch <= 0xd7ff) || (ch == '\t' || ch == '\n' || ch == '\r') || (ch >= 0xe000 && ch <= 0xfffd)

      if (allowedInCdata) append(ch)
      else if (Character.isHighSurrogate(ch) && i < safeData.length - 1) {
        // Character entities aren't valid in CDATA, so break out for this.
        append("]]>")
        i += 1
        writeSurrogate(ch, safeData.charAt(i))
        append("<![CDATA[")
      }
      else KXmlSerializer.reportInvalidCharacter(ch)

      i += 1
    }

    append("]]>")
  }

  @throws[IOException]
  private def writeSurrogate(high: Char, low: Char): Unit = {
    require(
      Character.isLowSurrogate(low),
      "Bad surrogate pair (U+" + Integer.toHexString(high.toInt) + " U+" + Integer.toHexString(low.toInt) + ")"
    )
    // Java-style surrogate pairs aren't allowed in XML. We could use the > 3-byte encodings, but that
    // seems likely to upset anything expecting modified UTF-8 rather than "real" UTF-8. It seems more
    // conservative in a Java environment to use an entity reference instead.
    val codePoint = Character.toCodePoint(high, low)
    append("&#" + codePoint + ";")
  }

  @throws[IOException]
  def comment(comment: String): Unit = {
    check(false)
    append("<!--")
    append(comment)
    append("-->")
  }

  @throws[IOException]
  def processingInstruction(pi: String): Unit = {
    check(false)
    append("<?")
    append(pi)
    append("?>")
  }
}