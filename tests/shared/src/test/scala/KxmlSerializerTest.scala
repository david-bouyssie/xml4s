/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringWriter

import org.junit.Assert._
import org.junit.Test
import org.kxml2.io.KXmlSerializer

//import junit.framework.TestCase
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text

import org.xmlpull.v1.XmlSerializer

import Support_Xml.domOf

object KxmlSerializerTest {

  private val NAMESPACE: String = null

  // Cover all the BMP code points plus a few that require us to use surrogates.
  private val MAX_TEST_CODE_POINT = 0x10008

  @throws[IOException]
  private def newSerializer(): XmlSerializer = {
    val bytesOut = new ByteArrayOutputStream()
    val serializer = new KXmlSerializer()
    serializer.setOutput(bytesOut, "UTF-8")
    serializer.startDocument("UTF-8", null)
    serializer
  }

  private def isValidXmlCodePoint(c: Int) = {
    // http://www.w3.org/TR/REC-xml/#charsets
    (c >= 0x20 && c <= 0xd7ff) || c == 0x9 || c == 0xa || c == 0xd || (c >= 0xe000 && c <= 0xfffd) || (c >= 0x10000 && c <= 0x10ffff)
  }
}

final class KxmlSerializerTest { // extends TestCase

  @throws[Exception]
  @Test def testWhitespaceInAttributeValue(): Unit = {
    val stringWriter = new StringWriter
    val serializer = new KXmlSerializer()
    serializer.setOutput(stringWriter)
    serializer.startDocument("UTF-8", null)
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "a")
    serializer.attribute(KxmlSerializerTest.NAMESPACE, "cr", "\r")
    serializer.attribute(KxmlSerializerTest.NAMESPACE, "lf", "\n")
    serializer.attribute(KxmlSerializerTest.NAMESPACE, "tab", "\t")
    serializer.attribute(KxmlSerializerTest.NAMESPACE, "space", " ")
    serializer.endTag(KxmlSerializerTest.NAMESPACE, "a")
    serializer.endDocument()
    assertXmlEquals("<a cr=\"&#13;\" lf=\"&#10;\" tab=\"&#9;\" space=\" \" />", stringWriter.toString)
  }

  @throws[Exception]
  @Test def testWriteDocument(): Unit = {
    val stringWriter = new StringWriter()
    val serializer = new KXmlSerializer()
    serializer.setOutput(stringWriter)
    serializer.startDocument("UTF-8", null)
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "foo")
    serializer.attribute(KxmlSerializerTest.NAMESPACE, "quux", "abc")
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "bar")
    serializer.endTag(KxmlSerializerTest.NAMESPACE, "bar")
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "baz")
    serializer.endTag(KxmlSerializerTest.NAMESPACE, "baz")
    serializer.endTag(KxmlSerializerTest.NAMESPACE, "foo")
    serializer.endDocument()
    assertXmlEquals("<foo quux=\"abc\"><bar /><baz /></foo>", stringWriter.toString)
  }

  // http://code.google.com/p/android/issues/detail?id=21250
  @throws[Exception]
  @Test def testWriteSpecialCharactersInText(): Unit = {
    val stringWriter = new StringWriter()
    val serializer = new KXmlSerializer()
    serializer.setOutput(stringWriter)
    serializer.startDocument("UTF-8", null)
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "foo")
    serializer.text("5'8\", 5 < 6 & 7 > 3!")
    serializer.endTag(KxmlSerializerTest.NAMESPACE, "foo")
    serializer.endDocument()
    assertXmlEquals("<foo>5'8\", 5 &lt; 6 &amp; 7 &gt; 3!</foo>", stringWriter.toString)
  }

  @throws[Exception]
  private def assertXmlEquals(expectedXml: String, actualXml: String) = {
    val declaration = "<?xml version='1.0' encoding='UTF-8' ?>"
    assertEquals(declaration + expectedXml, actualXml)
  }

  def fromCodePoint(codePoint: Int): String = {
    if (codePoint > Character.MAX_VALUE) return new String(Character.toChars(codePoint))
    Character.toString(codePoint.toChar)
  }

  /*
  // http://b/17960630
  @throws[Exception]
  @Test def testSpeakNoEvilMonkeys(): Unit = {
    val stringWriter = new StringWriter()
    val serializer = new KXmlSerializer()
    serializer.setOutput(stringWriter)
    serializer.startDocument("UTF-8", null)
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "tag")
    serializer.attribute(KxmlSerializerTest.NAMESPACE, "attr", "a\ud83d\ude4ab")
    serializer.text("c\ud83d\ude4ad")
    serializer.cdsect("e\ud83d\ude4af")
    serializer.endTag(KxmlSerializerTest.NAMESPACE, "tag")
    serializer.endDocument()
    assertXmlEquals("<tag attr=\"a&#128586;b\">" + "c&#128586;d" + "<![CDATA[e]]>&#128586;<![CDATA[f]]>" + "</tag>", stringWriter.toString)

    // Check we can parse what we just output.
    val doc = domOf(stringWriter.toString)
    val root = doc.getDocumentElement
    assertEquals("a\ud83d\ude4ab", root.getAttributes.getNamedItem("attr").getNodeValue)
    val text = root.getFirstChild.asInstanceOf[Text]
    assertEquals("c\ud83d\ude4ade\ud83d\ude4af", text.getNodeValue)
  }*/

  @throws[Exception]
  @Test def testBadSurrogates(): Unit = {
    val stringWriter = new StringWriter()
    val serializer = new KXmlSerializer()
    serializer.setOutput(stringWriter)
    serializer.startDocument("UTF-8", null)
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "tag")
    try {
      serializer.attribute(KxmlSerializerTest.NAMESPACE, "attr", "a\ud83d\u0040b")
      fail()
    } catch {
      case expected: IllegalArgumentException =>
    }
    try {
      serializer.text("c\ud83d\u0040d")
      fail()
    } catch {
      case expected: IllegalArgumentException =>
    }
    try {
      serializer.cdsect("e\ud83d\u0040f")
      fail()
    } catch {
      case expected: IllegalArgumentException =>
    }
  }

  @throws[IOException]
  @Test def testInvalidCharactersInText(): Unit = {
    val serializer = KxmlSerializerTest.newSerializer()
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "root")

    for (c <- 0 to KxmlSerializerTest.MAX_TEST_CODE_POINT) {
      val s = fromCodePoint(c)
      if (KxmlSerializerTest.isValidXmlCodePoint(c)) serializer.text("a" + s + "b")
      else try {
        serializer.text("a" + s + "b")
        fail(s)
      } catch {
        case expected: IllegalArgumentException =>
      }
    }

    serializer.endTag(KxmlSerializerTest.NAMESPACE, "root")
  }

  @throws[IOException]
  @Test def testInvalidCharactersInAttributeValues(): Unit = {
    val serializer = KxmlSerializerTest.newSerializer()
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "root")

    for (c <- 0 to KxmlSerializerTest.MAX_TEST_CODE_POINT) {
      val s = fromCodePoint(c)
      if (KxmlSerializerTest.isValidXmlCodePoint(c)) serializer.attribute(KxmlSerializerTest.NAMESPACE, "a", "a" + s + "b")
      else try {
        serializer.attribute(KxmlSerializerTest.NAMESPACE, "a", "a" + s + "b")
        fail(s)
      } catch {
        case expected: IllegalArgumentException =>
      }
    }

    serializer.endTag(KxmlSerializerTest.NAMESPACE, "root")
  }

  @throws[IOException]
  @Test def testInvalidCharactersInCdataSections(): Unit = {
    val serializer = KxmlSerializerTest.newSerializer()
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "root")
    for (c <- 0 to KxmlSerializerTest.MAX_TEST_CODE_POINT) {
      val s = fromCodePoint(c)
      if (KxmlSerializerTest.isValidXmlCodePoint(c)) serializer.cdsect("a" + s + "b")
      else try {
        serializer.cdsect("a" + s + "b")
        fail(s)
      } catch {
        case expected: IllegalArgumentException =>
      }
    }
    serializer.endTag(KxmlSerializerTest.NAMESPACE, "root")
  }

  /*
  @throws[Exception]
  @Test def testCdataWithTerminatorInside(): Unit = {
    val writer = new StringWriter()
    val serializer = new KXmlSerializer()
    serializer.setOutput(writer)
    serializer.startDocument("UTF-8", null)
    serializer.startTag(KxmlSerializerTest.NAMESPACE, "p")
    serializer.cdsect("a]]>b")
    serializer.endTag(KxmlSerializerTest.NAMESPACE, "p")
    serializer.endDocument()

    // Adjacent CDATA sections aren't merged, so let's stick them together ourselves...
    val doc = domOf(writer.toString)
    val children = doc.getFirstChild.getChildNodes

    var text = ""
    for (i <- 0 until children.getLength) {
      text += children.item(i).getNodeValue
    }

    assertEquals("a]]>b", text)
  }*/
}