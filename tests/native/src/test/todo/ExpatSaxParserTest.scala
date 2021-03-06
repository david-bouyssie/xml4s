/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//import com.google.mockwebserver.MockResponse
//import com.google.mockwebserver.MockWebServer
import expat.ExpatReader

import org.junit.Assert._
import org.xml.sax._
import org.xml.sax.ext.DefaultHandler2
import org.xml.sax.helpers.DefaultHandler

import java.io._
import java.util._
import scala.scalanative.junit.JUnitFramework

object ExpatSaxParserTest {
  private val SNIPPET = "<dagny dad=\"bob\">hello</dagny>"

  def validate(handler: ExpatSaxParserTest.TestHandler) = {
    assertEquals("dagny", handler.startElementName)
    assertEquals("dagny", handler.endElementName)
    assertEquals("hello", handler.text.toString)
  }

  class TestHandler extends DefaultHandler {
    var startElementName = null
    var endElementName = null
    val text = new StringBuilder

    @throws[SAXException]
    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) = {
      assertNull(this.startElementName)
      this.startElementName = localName
      // Validate attributes.
      assertEquals(1, attributes.getLength)
      assertEquals("", attributes.getURI(0))
      assertEquals("dad", attributes.getLocalName(0))
      assertEquals("bob", attributes.getValue(0))
      assertEquals(0, attributes.getIndex("", "dad"))
      assertEquals("bob", attributes.getValue("", "dad"))
    }

    @throws[SAXException]
    override def endElement(uri: String, localName: String, qName: String) = {
      assertNull(this.endElementName)
      this.endElementName = localName
    }

    @throws[SAXException]
    override def characters(ch: Array[Char], start: Int, length: Int) = this.text.append(ch, start, length)
  }

  val XML = "<one xmlns='ns:default' xmlns:n1='ns:1' a='b'>\n" + "  <n1:two c='d' n1:e='f' xmlns:n2='ns:2'>text</n1:two>\n" + "</one>"

  class NamespaceHandler extends ContentHandler {
    var locator = null
    var documentStarted = false
    var documentEnded = false
    val prefixMappings = new util.HashMap[String, String]
    var oneStarted = false
    var twoStarted = false
    var oneEnded = false
    var twoEnded = false

    def validate() = assertTrue(documentEnded)

    override def setDocumentLocator(locator: Locator) = this.locator = locator

    @throws[SAXException]
    override def startDocument() = {
      documentStarted = true
      assertNotNull(locator)
      assertEquals(0, prefixMappings.size)
      assertFalse(documentEnded)
    }

    @throws[SAXException]
    override def endDocument() = {
      assertTrue(documentStarted)
      assertTrue(oneEnded)
      assertTrue(twoEnded)
      assertEquals(0, prefixMappings.size)
      documentEnded = true
    }

    @throws[SAXException]
    override def startPrefixMapping(prefix: String, uri: String) = prefixMappings.put(prefix, uri)

    @throws[SAXException]
    override def endPrefixMapping(prefix: String) = assertNotNull(prefixMappings.remove(prefix))

    @throws[SAXException]
    override def startElement(uri: String, localName: String, qName: String, atts: Attributes): Unit = {
      if (localName eq "one") {
        assertEquals(2, prefixMappings.size)
        assertEquals(1, locator.getLineNumber)
        assertFalse(oneStarted)
        assertFalse(twoStarted)
        assertFalse(oneEnded)
        assertFalse(twoEnded)
        oneStarted = true
        assertSame("ns:default", uri)
        assertEquals("one", qName)
        // Check atts.
        assertEquals(1, atts.getLength)
        assertSame("", atts.getURI(0))
        assertSame("a", atts.getLocalName(0))
        assertEquals("b", atts.getValue(0))
        assertEquals(0, atts.getIndex("", "a"))
        assertEquals("b", atts.getValue("", "a"))
        return
      }
      if (localName eq "two") {
        assertEquals(3, prefixMappings.size)
        assertTrue(oneStarted)
        assertFalse(twoStarted)
        assertFalse(oneEnded)
        assertFalse(twoEnded)
        twoStarted = true
        assertSame("ns:1", uri)
        Assert.assertEquals("n1:two", qName)
        assertEquals(2, atts.getLength)
        assertSame("", atts.getURI(0))
        assertSame("c", atts.getLocalName(0))
        assertEquals("d", atts.getValue(0))
        assertEquals(0, atts.getIndex("", "c"))
        assertEquals("d", atts.getValue("", "c"))
        assertSame("ns:1", atts.getURI(1))
        assertSame("e", atts.getLocalName(1))
        assertEquals("f", atts.getValue(1))
        assertEquals(1, atts.getIndex("ns:1", "e"))
        assertEquals("f", atts.getValue("ns:1", "e"))
        // We shouldn't find these.
        assertEquals(-1, atts.getIndex("ns:default", "e"))
        assertEquals(null, atts.getValue("ns:default", "e"))
        return
      }
      fail
    }

    @throws[SAXException]
    override def endElement(uri: String, localName: String, qName: String): Unit = {
      if (localName eq "one") {
        assertEquals(3, locator.getLineNumber)
        assertTrue(oneStarted)
        assertTrue(twoStarted)
        assertTrue(twoEnded)
        assertFalse(oneEnded)
        oneEnded = true
        assertSame("ns:default", uri)
        assertEquals("one", qName)
        return
      }
      if (localName eq "two") {
        assertTrue(oneStarted)
        assertTrue(twoStarted)
        assertFalse(twoEnded)
        assertFalse(oneEnded)
        twoEnded = true
        assertSame("ns:1", uri)
        assertEquals("n1:two", qName)
        return
      }
      fail
    }

    @throws[SAXException]
    override def characters(ch: Array[Char], start: Int, length: Int) = {
      val s = new String(ch, start, length).trim
      if (!(s == "")) {
        assertTrue(oneStarted)
        assertTrue(twoStarted)
        assertFalse(oneEnded)
        assertFalse(twoEnded)
        assertEquals("text", s)
      }
    }

    @throws[SAXException]
    override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int) = fail

    @throws[SAXException]
    override def processingInstruction(target: String, data: String) = fail

    @throws[SAXException]
    override def skippedEntity(name: String) = fail
  }

  class TestDtdHandler extends DefaultHandler2 {
    var name = null
    var publicId = null
    var systemId = null
    var ndName = null
    var ndPublicId = null
    var ndSystemId = null
    var ueName = null
    var uePublicId = null
    var ueSystemId = null
    var ueNotationName = null
    var ended = false
    var locator = null

    override def startDTD(name: String, publicId: String, systemId: String) = {
      this.name = name
      this.publicId = publicId
      this.systemId = systemId
    }

    override def endDTD() = ended = true

    override def setDocumentLocator(locator: Locator) = this.locator = locator

    override def notationDecl(name: String, publicId: String, systemId: String) = {
      this.ndName = name
      this.ndPublicId = publicId
      this.ndSystemId = systemId
    }

    override def unparsedEntityDecl(entityName: String, publicId: String, systemId: String, notationName: String) = {
      this.ueName = entityName
      this.uePublicId = publicId
      this.ueSystemId = systemId
      this.ueNotationName = notationName
    }
  }

  class TestCdataHandler extends DefaultHandler2 {
    var startCdata = 0
    var endCdata = 0
    val buffer = new StringBuffer

    override def characters(ch: Array[Char], start: Int, length: Int) = buffer.append(ch, start, length)

    @throws[SAXException]
    override def startCDATA() = startCdata += 1

    @throws[SAXException]
    override def endCDATA() = endCdata += 1
  }

  class TestProcessingInstrutionHandler extends DefaultHandler2 {
    var target = null
    var data = null

    override def processingInstruction(target: String, data: String) = {
      this.target = target
      this.data = data
    }
  }

  /**
   * Parses the given xml string and fires events on the given SAX handler.
   */
  @throws[SAXException]
  private def parse(xml: String, contentHandler: ContentHandler) = try {
    val reader = new Nothing
    reader.setContentHandler(contentHandler)
    reader.parse(new InputSource(new StringReader(xml)))
  } catch {
    case e: IOException =>
      throw new AssertionError(e)
  }

  /**
   * Parses xml from the given reader and fires events on the given SAX
   * handler.
   */
  @throws[IOException]
  @throws[SAXException]
  private def parse(in: Reader, contentHandler: ContentHandler) = {
    val reader = new Nothing
    reader.setContentHandler(contentHandler)
    reader.parse(new InputSource(in))
  }

  /**
   * Parses xml from the given input stream and fires events on the given SAX
   * handler.
   */
  @throws[IOException]
  @throws[SAXException]
  private def parse(in: InputStream, encoding: ExpatSaxParserTest.Encoding, contentHandler: ContentHandler) = try {
    val reader = new Nothing
    reader.setContentHandler(contentHandler)
    val source = new InputSource(in)
    source.setEncoding(encoding.expatName)
    reader.parse(source)
  } catch {
    case e: IOException =>
      throw new AssertionError(e)
  }

  /**
   * Supported character encodings.
   */
  private object Encoding extends Enumeration {
    type Encoding = Value
    val US_ASCII, UTF_8, UTF_16, ISO_8859_1 = Value
    val expatName = nulldef
    this (expatName: String) {
      this ()
      this.expatName = expatName
    }
  }
}

import org.junit.Test

class ExpatSaxParserTest {

  @throws[Exception]
  @Test def testGlobalReferenceTableOverflow() = { // We used to use a JNI global reference per interned string.
    // Framework apps have a limit of 2000 JNI global references per VM.
    val xml = new StringBuilder
    xml.append("<root>")
    for (i <- 0 until 4000) {
      xml.append("<tag" + i + ">")
      xml.append("</tag" + i + ">")
    }
    xml.append("</root>")
    ExpatSaxParserTest.parse(xml.toString, new DefaultHandler)
  }

  @Test def testExceptions() = {
    // From startElement().
    var contentHandler = new DefaultHandler() {
      @throws[SAXException]
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) = throw new SAXException
    }
    try {
      ExpatSaxParserTest.parse(ExpatSaxParserTest.SNIPPET, contentHandler)
      fail
    } catch {
      case checked: SAXException =>

      /* expected */
    }
    // From endElement().
    contentHandler = new DefaultHandler() {
      @throws[SAXException]
      override def endElement(uri: String, localName: String, qName: String) = throw new SAXException
    }
    try {
      ExpatSaxParserTest.parse(ExpatSaxParserTest.SNIPPET, contentHandler)
      fail
    } catch {
      case checked: SAXException =>
    }
    // From characters().
    contentHandler = new DefaultHandler() {
      @throws[SAXException]
      override def characters(ch: Array[Char], start: Int, length: Int) = throw new SAXException
    }
    try {
      ExpatSaxParserTest.parse(ExpatSaxParserTest.SNIPPET, contentHandler)
      fail
    } catch {
      case checked: SAXException =>
    }
  }

  @Test def testSax() = try { // Parse String.
    var handler = new ExpatSaxParserTest.TestHandler
    ExpatSaxParserTest.parse(ExpatSaxParserTest.SNIPPET, handler)
    ExpatSaxParserTest.validate(handler)
    // Parse Reader.
    handler = new ExpatSaxParserTest.TestHandler
    ExpatSaxParserTest.parse(new StringReader(ExpatSaxParserTest.SNIPPET), handler)
    ExpatSaxParserTest.validate(handler)
    // Parse InputStream.
    handler = new ExpatSaxParserTest.TestHandler
    ExpatSaxParserTest.parse(new ByteArrayInputStream(ExpatSaxParserTest.SNIPPET.getBytes), ExpatSaxParserTest.Encoding.UTF_8, handler)
    ExpatSaxParserTest.validate(handler)
  } catch {
    case e: SAXException =>
      throw new RuntimeException(e)
    case e: IOException =>
      throw new RuntimeException(e)
  }

  @Test def testNamespaces() = try {
    val handler = new ExpatSaxParserTest.NamespaceHandler
    ExpatSaxParserTest.parse(ExpatSaxParserTest.XML, handler)
    handler.validate()
  } catch {
    case e: SAXException =>
      throw new RuntimeException(e)
  }

  @throws[Exception]
  private def runDtdTest(s: String) = {
    val in = new StringReader(s)
    val reader = new Nothing
    val handler = new ExpatSaxParserTest.TestDtdHandler
    reader.setContentHandler(handler)
    reader.setDTDHandler(handler)
    reader.setLexicalHandler(handler)
    reader.parse(new InputSource(in))
    handler
  }

  @throws[Exception]
  @Test def testDtdDoctype() = {
    val handler = runDtdTest("<?xml version=\"1.0\"?><!DOCTYPE foo PUBLIC 'bar' 'tee'><a></a>")
    assertEquals("foo", handler.name)
    assertEquals("bar", handler.publicId)
    assertEquals("tee", handler.systemId)
    assertTrue(handler.ended)
  }

  @throws[Exception]
  @Test def testDtdUnparsedEntity_system() = {
    val handler = runDtdTest("<?xml version=\"1.0\"?><!DOCTYPE foo PUBLIC 'bar' 'tee' [ <!ENTITY ent SYSTEM 'blah' NDATA poop> ]><a></a>")
    assertEquals("ent", handler.ueName)
    assertEquals(null, handler.uePublicId)
    assertEquals("blah", handler.ueSystemId)
    assertEquals("poop", handler.ueNotationName)
  }

  @throws[Exception]
  @Test def testDtdUnparsedEntity_public() = {
    val handler = runDtdTest("<?xml version=\"1.0\"?><!DOCTYPE foo PUBLIC 'bar' 'tee' [ <!ENTITY ent PUBLIC 'a' 'b' NDATA poop> ]><a></a>")
    assertEquals("ent", handler.ueName)
    assertEquals("a", handler.uePublicId)
    assertEquals("b", handler.ueSystemId)
    assertEquals("poop", handler.ueNotationName)
  }

  @throws[Exception]
  @Test def testDtdNotation_system() = {
    val handler = runDtdTest("<?xml version=\"1.0\"?><!DOCTYPE foo PUBLIC 'bar' 'tee' [ <!NOTATION sn SYSTEM 'nf2'> ]><a></a>")
    assertEquals("sn", handler.ndName)
    assertEquals(null, handler.ndPublicId)
    assertEquals("nf2", handler.ndSystemId)
  }

  @throws[Exception]
  @Test def testDtdNotation_public() = {
    val handler = runDtdTest("<?xml version=\"1.0\"?><!DOCTYPE foo PUBLIC 'bar' 'tee' [ <!NOTATION pn PUBLIC 'nf1'> ]><a></a>")
    assertEquals("pn", handler.ndName)
    assertEquals("nf1", handler.ndPublicId)
    assertEquals(null, handler.ndSystemId)
  }

  @throws[Exception]
  def testCdata() = {
    val in = new StringReader("<a><![CDATA[<b></b>]]> <![CDATA[<c></c>]]></a>")
    val reader = new Nothing
    val handler = new ExpatSaxParserTest.TestCdataHandler
    reader.setContentHandler(handler)
    reader.setLexicalHandler(handler)
    reader.parse(new InputSource(in))
    assertEquals(2, handler.startCdata)
    assertEquals(2, handler.endCdata)
    assertEquals("<b></b> <c></c>", handler.buffer.toString)
  }

  @throws[IOException]
  @throws[SAXException]
  @Test def testProcessingInstructions() = {
    val in = new StringReader("<?bob lee?><a></a>")
    val reader = new Nothing
    val handler = new ExpatSaxParserTest.TestProcessingInstrutionHandler
    reader.setContentHandler(handler)
    reader.parse(new InputSource(in))
    assertEquals("bob", handler.target)
    assertEquals("lee", handler.data)
  }

  @throws[IOException]
  @throws[SAXException]
  @Test def testExternalEntity() = {
    class Handler extends DefaultHandler {
      val elementNames = new util.ArrayList[String]
      val text = new StringBuilder

      @throws[IOException]
      @throws[SAXException]
      override def resolveEntity(publicId: String, systemId: String): InputSource = {
        if (publicId == "publicA" && systemId == "systemA") return new InputSource(new StringReader("<a/>"))
        else if (publicId == "publicB" && systemId == "systemB") {
          /*
                              * Explicitly set the encoding here or else the parser will
                              * try to use the parent parser's encoding which is utf-16.
                              */ val inputSource = new InputSource(new ByteArrayInputStream("bob".getBytes("utf-8")))
          inputSource.setEncoding("utf-8")
          return inputSource
        }
        throw new AssertionError
      }

      @throws[SAXException]
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) = elementNames.add(localName)

      @throws[SAXException]
      override def endElement(uri: String, localName: String, qName: String) = elementNames.add("/" + localName)

      @throws[SAXException]
      override def characters(ch: Array[Char], start: Int, length: Int) = text.append(ch, start, length)
    }
    val in = new StringReader("<?xml version=\"1.0\"?>\n" + "<!DOCTYPE foo [\n" + "  <!ENTITY a PUBLIC 'publicA' 'systemA'>\n" + "  <!ENTITY b PUBLIC 'publicB' 'systemB'>\n" + "]>\n" + "<foo>\n" + "  &a;<b>&b;</b></foo>")
    val reader = new Nothing
    val handler = new Handler
    reader.setContentHandler(handler)
    reader.setEntityResolver(handler)
    reader.parse(new InputSource(in))
    assertEquals(util.Arrays.asList("foo", "a", "/a", "b", "/b", "/foo"), handler.elementNames)
    assertEquals("bob", handler.text.toString.trim)
  }

  @throws[IOException]
  @throws[SAXException]
  @Test def testExternalEntityDownload() = {
    val server = new Nothing
    server.enqueue(new Nothing().setBody("<bar></bar>"))
    server.play
    class Handler extends DefaultHandler {
      final val elementNames = new util.ArrayList[String]

      @throws[IOException]
      override def resolveEntity(publicId: String, systemId: String) = { // The parser should have resolved the systemId.
        assertEquals(server.getUrl("/systemBar").toString, systemId)
        new InputSource(systemId)
      }

      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) = elementNames.add(localName)

      override def endElement(uri: String, localName: String, qName: String) = elementNames.add("/" + localName)
    }
    // 'systemBar', the external entity, is relative to 'systemFoo':
    val in = new StringReader("<?xml version=\"1.0\"?>\n" + "<!DOCTYPE foo [\n" + "  <!ENTITY bar SYSTEM 'systemBar'>\n" + "]>\n" + "<foo>&bar;</foo>")
    val reader = new Nothing
    val handler = new Handler
    reader.setContentHandler(handler)
    reader.setEntityResolver(handler)
    val source = new InputSource(in)
    source.setSystemId(server.getUrl("/systemFoo").toString)
    reader.parse(source)
    assertEquals(util.Arrays.asList("foo", "bar", "/bar", "/foo"), handler.elementNames)
    server.shutdown
  }

  /**
   * A little endian UTF-16 file with an odd number of bytes.
   */
  @throws[Exception]
  @Test def testBug28698301_1() = checkBug28698301("bug28698301-1.xml", "At line 19, column 18: no element found")

  /**
   * A little endian UTF-16 file with an even number of bytes that didn't exhibit the problem
   * reported in the bug.
   */
  @throws[Exception]
  @Test def testBug28698301_2() = checkBug28698301("bug28698301-2.xml", "At line 3, column 18: no element found")

  /**
   * A big endian UTF-16 file with an odd number of bytes.
   */
  @throws[Exception]
  @Test def testBug28698301_3() = checkBug28698301("bug28698301-3.xml", "At line 97, column 21: not well-formed (invalid token)")

  /**
   * This tests what happens when UTF-16 input (little and big endian) that has an odd number of
   * bytes (and hence is invalid UTF-16) is parsed by Expat.
   *
   * <p>Prior to the patch the files would cause the pointer into the byte buffer to jump past
   * the end of the buffer and keep reading. Once it had jumped past it would continue reading
   * from memory until it hit a check that caused it to stop or caused a SIGSEGV. If a SIGSEGV
   * was not thrown that lead to spurious and misleading errors being reported.
   *
   * <p>The initial jump was caused because it was not checking to make sure that there were
   * enough bytes to read a whole UTF-16 character. It kept reading because most of the buffer
   * range checks used == and != rather than >= and <. The patch fixes the initial jump and then
   * uses inequalities in the range check to fail fast in the event of another overflow bug.
   */
  @throws[IOException]
  @throws[SAXException]
  private def checkBug28698301(name: String, expectedMessage: String) = {
    val is = getClass.getResourceAsStream(name)
    try ExpatSaxParserTest.parse(is, ExpatSaxParserTest.Encoding.UTF_16, new ExpatSaxParserTest.TestHandler)
    catch {
      case exception: SAXParseException =>
        val message = exception.getMessage
        if (!(message == expectedMessage)) fail("Expected '" + expectedMessage + "' exception, found: '" + message + "'")
    }
  }
}