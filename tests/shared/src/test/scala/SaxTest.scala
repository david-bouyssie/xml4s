/*
 * Copyright (C) 2010 The Android Open Source Project
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
 *//*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.io.StringReader
import java.util

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

import org.junit.Assert._
import org.junit.Test

//import junit.framework.AssertionFailedError

import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.XMLReader
import org.xml.sax.helpers.DefaultHandler

/**
 * Initiate and observe a SAX parse session.
 */
object SaxTest {

  // FIXME: this doesn't work as expected
  System.setProperty("javax.xml.parsers.SAXParserFactory", "javax.xml.parsers.SAXParserFactoryImpl2");

  /*javax.xml.parsers.SAXParserFactoryImpl.setParserImplProvider( { features: java.util.HashMap[String, Boolean] =>
    new javax.xml.parsers.KxmlSAXParserImpl(features)
  })*/

  type AssertionFailedError = AssertionError

  /**
   * This SAX handler throws on everything but startDocument, endDocument,
   * and setDocumentLocator(). Override the methods that are expected to be
   * called.
   */
  class ThrowingHandler extends DefaultHandler {
    override def resolveEntity(publicId: String, systemId: String): InputSource = throw new AssertionFailedError

    override def notationDecl(name: String, publicId: String, systemId: String): Unit = throw new AssertionFailedError

    override def unparsedEntityDecl(name: String, publicId: String, systemId: String, notationName: String): Unit = throw new AssertionFailedError

    override def startPrefixMapping(prefix: String, uri: String): Unit = throw new AssertionFailedError

    override def endPrefixMapping(prefix: String): Unit = throw new AssertionFailedError

    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = throw new AssertionFailedError

    override def endElement(uri: String, localName: String, qName: String): Unit = throw new AssertionFailedError

    override def characters(ch: Array[Char], start: Int, length: Int): Unit = throw new AssertionFailedError

    override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = throw new AssertionFailedError

    override def processingInstruction(target: String, data: String): Unit = throw new AssertionFailedError

    override def skippedEntity(name: String): Unit = throw new AssertionFailedError

    override def warning(e: SAXParseException): Unit = throw new AssertionFailedError

    override def error(e: SAXParseException): Unit = throw new AssertionFailedError

    override def fatalError(e: SAXParseException): Unit = throw new AssertionFailedError
  }
}

final class SaxTest {

  @throws[Exception]
  @Test def testNoPrefixesNoNamespaces(): Unit = {
    parse(false, false, "<foo bar=\"baz\"/>", new DefaultHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("", uri)
        assertTrue(localName == "" || localName == "foo") // FIXME: inconsistency between Xerces and Kxml2
        assertEquals("foo", qName)
        assertEquals(1, attributes.getLength)
        assertEquals("", attributes.getURI(0))
        assertOneOf("bar", "", attributes.getLocalName(0))
        assertEquals("bar", attributes.getQName(0))
      }
    }
    )
    parse(false, false, "<a:foo a:bar=\"baz\"/>", new DefaultHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("", uri)
        assertTrue(localName == "" || localName == "a:foo") // FIXME: inconsistency between Xerces and Kxml2
        assertEquals("a:foo", qName)
        assertEquals(1, attributes.getLength)
        assertEquals("", attributes.getURI(0))
        assertOneOf("a:bar", "", attributes.getLocalName(0))
        assertEquals("a:bar", attributes.getQName(0))
      }
    }
    )
  }

  @throws[Exception]
  @Test def testNoPrefixesYesNamespaces(): Unit = {
    parse(false, true, "<foo bar=\"baz\"/>", new DefaultHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("", uri)
        assertEquals("foo", localName)
        assertEquals("foo", qName)
        assertEquals(1, attributes.getLength)
        assertEquals("", attributes.getURI(0))
        assertEquals("bar", attributes.getLocalName(0))
        assertEquals("bar", attributes.getQName(0))
      }
    }
    )
    parse(false, true, "<a:foo a:bar=\"baz\" xmlns:a=\"http://quux\"/>", new DefaultHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("http://quux", uri)
        assertEquals("foo", localName)
        assertEquals("a:foo", qName)
        assertEquals(1, attributes.getLength)
        assertEquals("http://quux", attributes.getURI(0))
        assertEquals("bar", attributes.getLocalName(0))
        assertEquals("a:bar", attributes.getQName(0))
      }
    }
    )
  }

  /**
   * Android's Expat-based SAX parser fails this test because Expat doesn't
   * supply us with our much desired {@code xmlns="http://..."} attributes.
   */
  @throws[Exception]
  @Test def testYesPrefixesYesNamespaces(): Unit = {
    parse(true, true, "<foo bar=\"baz\"/>", new DefaultHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("", uri)
        assertEquals("foo", localName)
        assertEquals("foo", qName)
        assertEquals(1, attributes.getLength)
        assertEquals("", attributes.getURI(0))
        assertEquals("bar", attributes.getLocalName(0))
        assertEquals("bar", attributes.getQName(0))
      }
    })
    parse(true, true, "<a:foo a:bar=\"baz\" xmlns:a=\"http://quux\"/>", new DefaultHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("http://quux", uri)
        assertEquals("foo", localName)
        assertEquals("a:foo", qName)
        assertEquals(2, attributes.getLength())

        assertEquals("http://quux", attributes.getURI(0))
        assertEquals("bar", attributes.getLocalName(0))
        assertEquals("a:bar", attributes.getQName(0))

        val xmlnsURI = "http://www.w3.org/2000/xmlns/"
        val attr2Uri = attributes.getURI(1)

        // FIXME: inconsistency between JDK Xerces and Kxml2
        if (attr2Uri != xmlnsURI) {
          // these assertions are only valid with Xerces
          assertEquals("", attr2Uri)
          assertEquals("", attributes.getLocalName(1))
        }

        assertEquals("xmlns:a", attributes.getQName(1))
      }
    })
  }

  @throws[Exception]
  @Test def testYesPrefixesNoNamespaces(): Unit = {
    parse(true, false, "<foo bar=\"baz\"/>", new DefaultHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("", uri)
        assertTrue(localName == "" || localName == "foo") // FIXME: inconsistency between Xerces and Kxml2
        assertEquals("foo", qName)
        assertEquals(1, attributes.getLength)
        assertEquals("", attributes.getURI(0))
        assertOneOf("bar", "", attributes.getLocalName(0))
        assertEquals("bar", attributes.getQName(0))
      }
    }
    )
    parse(true, false, "<a:foo a:bar=\"baz\"/>", new DefaultHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("", uri)
        assertTrue(localName == "" || localName == "a:foo") // FIXME: inconsistency between Xerces and Kxml2
        assertEquals("a:foo", qName)
        assertEquals(1, attributes.getLength)
        assertEquals("", attributes.getURI(0))
        assertOneOf("a:bar", "", attributes.getLocalName(0))
        assertEquals("a:bar", attributes.getQName(0))
      }
    }
    )
  }

  /**
   * Test that the external-general-entities feature can be disabled.
   * http://code.google.com/p/android/issues/detail?id=9493
   */
  @throws[Exception]
  // FIXME: fails on JDK (Xerces)
  // FIXME: "http://xml.org/sax/features/external-general-entities not implemented in Kxml2
  def testDisableExternalGeneralEntities(): Unit = {
    val xml = "<!DOCTYPE foo [" + "  <!ENTITY bar SYSTEM \"/no-such-document.xml\">" + "]>" + "<foo>&bar;</foo>"
    testDisableExternalEntities("http://xml.org/sax/features/external-general-entities", xml)
  }

  /**
   * Test that the external-parameter-entities feature can be disabled.
   * http://code.google.com/p/android/issues/detail?id=9493
   */
  @throws[Exception]
  // FIXME: "http://xml.org/sax/features/external-parameter-entities not implemented in Kxml2
  def testDisableExternalParameterEntities(): Unit = {
    val xml = "<!DOCTYPE foo [" + "  <!ENTITY % bar SYSTEM \"/no-such-document.xml\">" + "  %bar;" + "]>" + "<foo/>"
    testDisableExternalEntities("http://xml.org/sax/features/external-parameter-entities", xml)
  }

  /**
   * Disables the named feature and then parses the supplied XML.
   * The content is expected to be equivalent to "<foo/>".
   */
  @throws[Exception]
  private def testDisableExternalEntities(feature: String, xml: String): Unit = {
    //val parser = SAXParserFactory.newInstance.newSAXParser
    val parser = new javax.xml.parsers.SAXParserFactoryImpl2().newSAXParser()
    val reader = parser.getXMLReader()
    reader.setFeature(feature, false)
    assertFalse(reader.getFeature(feature))

    reader.setContentHandler(new SaxTest.ThrowingHandler() {
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        assertEquals("foo", qName)
      }

      override def endElement(uri: String, localName: String, qName: String): Unit = {
        assertEquals("foo", qName)
      }
    })

    reader.parse(new InputSource(new StringReader(xml)))
  }

  @throws[Exception]
  private def parse(prefixes: Boolean, namespaces: Boolean, xml: String, handler: ContentHandler): Unit = {
    // FIXME: we should be able to instantiate the desired SAXParser from SAXParserFactory
    //val parser = SAXParserFactory.newInstance.newSAXParser
    val parser = new javax.xml.parsers.SAXParserFactoryImpl2().newSAXParser()
    val reader = parser.getXMLReader()
    reader.setFeature("http://xml.org/sax/features/namespace-prefixes", prefixes)
    reader.setFeature("http://xml.org/sax/features/namespaces", namespaces)
    reader.setContentHandler(handler)
    reader.parse(new InputSource(new StringReader(xml)))
  }

  /**
   * @param expected an optional value that may or may have not been supplied
   * @param sentinel a marker value that means the expected value was omitted
   */
  private def assertOneOf(expected: String, sentinel: String, actual: String): Unit = {
    val optionsList = util.Arrays.asList(sentinel, expected)
    assertTrue("Expected one of " + optionsList + " but was " + actual, optionsList.contains(actual))
  }
}