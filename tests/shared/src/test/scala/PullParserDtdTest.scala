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
 */

import java.io.StringReader
import java.util

import org.junit.Assert._
import org.junit.Test
//import junit.framework.TestCase
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/**
 * Test doctype handling in pull parsers.
 */
object PullParserDtdTest {
  private val READ_BUFFER_SIZE = 8192
}

abstract class PullParserDtdTest {

  /**
   * Android's Expat pull parser permits parameter entities to be declared,
   * but it doesn't permit such entities to be used.
   */
  @throws[Exception]
  @Test def testDeclaringParameterEntities(): Unit = {
    val xml = "<!DOCTYPE foo [  <!ENTITY % a \"android\">]><foo></foo>"
    val parser = newPullParser(xml)
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
    }
  }

  @throws[Exception]
  @Test def testUsingParameterEntitiesInDtds(): Unit = {
    assertParseFailure("<!DOCTYPE foo [  <!ENTITY % a \"android\">  <!ENTITY b \"%a;\">]><foo></foo>")
  }

  @throws[Exception]
  @Test def testUsingParameterInDocuments(): Unit = {
    assertParseFailure("<!DOCTYPE foo [  <!ENTITY % a \"android\">]><foo>&a;</foo>")
  }

  @throws[Exception]
  @Test def testGeneralAndParameterEntityWithTheSameName(): Unit = {
    val xml = "<!DOCTYPE foo [  <!ENTITY a \"aaa\">  <!ENTITY % a \"bbb\">]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("aaa", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testInternalEntities(): Unit = {
    val xml = "<!DOCTYPE foo [  <!ENTITY a \"android\">]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("android", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testExternalDtdIsSilentlyIgnored(): Unit = {
    val xml = "<!DOCTYPE foo SYSTEM \"http://127.0.0.1:1/no-such-file.dtd\"><foo></foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testExternalAndInternalDtd(): Unit = {
    val xml = "<!DOCTYPE foo SYSTEM \"http://127.0.0.1:1/no-such-file.dtd\" [  <!ENTITY a \"android\">]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("android", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testInternalEntitiesAreParsed(): Unit = {
    val xml = "<!DOCTYPE foo [" +
      "  <!ENTITY a \"&#38;#65;\">" + // &#38; expands to '&', &#65; expands to 'A'
      "]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("A", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testFoolishlyRecursiveInternalEntities(): Unit = {
    val xml = "<!DOCTYPE foo [" +
      "  <!ENTITY a \"&#38;#38;#38;#38;\">" + // expand &#38; to '&' only twice
      "]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("&#38;#38;", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  /**
   * Test that the output of {@code &#38;} is parsed, but {@code &amp;} isn't.
   * http://www.w3.org/TR/2008/REC-xml-20081126/#sec-entexpand
   */
  @throws[Exception]
  @Test def testExpansionOfEntityAndCharacterReferences(): Unit = {
    val xml = "<!DOCTYPE foo [<!ENTITY example \"<p>An ampersand (&#38;#38;) may be escaped\n" +
      "numerically (&#38;#38;#38;) or with a general entity\n(&amp;amp;).</p>\" >]><foo>&example;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("p", parser.getName())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals(
      "An ampersand (&) may be escaped\n" +
      "numerically (&#38;) or with a general entity\n(&amp;).",
      parser.getText()
    )
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("p", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testGeneralEntitiesAreStoredUnresolved(): Unit = {
    val xml = "<!DOCTYPE foo [<!ENTITY b \"&a;\" ><!ENTITY a \"android\" >]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("android", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testStructuredEntityAndNextToken(): Unit = {
    val xml = "<!DOCTYPE foo [<!ENTITY bb \"<bar>baz<!--quux--></bar>\">]><foo>a&bb;c</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.DOCDECL, parser.nextToken())
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("a", parser.getText())
    assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
    assertEquals("bb", parser.getName())
    assertEquals("", parser.getText())
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals("bar", parser.getName())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("baz", parser.getText())
    assertEquals(XmlPullParser.COMMENT, parser.nextToken())
    assertEquals("quux", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    assertEquals("bar", parser.getName())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("c", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  /**
   * Android's Expat replaces external entities with the empty string.
   */
  @throws[Exception]
  @Test def testUsingExternalEntities(): Unit = {
    val xml = "<!DOCTYPE foo [  <!ENTITY a SYSTEM \"http://localhost:1/no-such-file.xml\">]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    // &a; is dropped!
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  /**
   * Android's ExpatPullParser replaces missing entities with the empty string
   * when an external DTD is declared.
   */
  @throws[Exception]
  @Test def testExternalDtdAndMissingEntity(): Unit = {
    val xml = "<!DOCTYPE foo SYSTEM \"http://127.0.0.1:1/no-such-file.dtd\"><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testExternalIdIsCaseSensitive(): Unit = {
    // The spec requires 'SYSTEM' in upper case
    assertParseFailure("<!DOCTYPE foo [  <!ENTITY a system \"http://localhost:1/no-such-file.xml\">]><foo/>")
  }

  /**
   * Use a DTD to specify that {@code <foo>} only contains {@code <bar>} tags.
   * Validating parsers react to this by dropping whitespace between the two
   * tags.
   */
  @throws[Exception]
  @Test def testDtdDoesNotInformIgnorableWhitespace(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ELEMENT foo (bar)*>\n  <!ELEMENT bar ANY>\n]><foo>  \n  <bar></bar>  \t  </foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("  \n  ", parser.getText())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("bar", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("bar", parser.getName())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("  \t  ", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testEmptyDoesNotInformIgnorableWhitespace(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ELEMENT foo EMPTY>\n]><foo>  \n  </foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("  \n  ", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  /**
   * Test that the parser doesn't expand the entity attributes.
   */
  @throws[Exception]
  @Test def testAttributeOfTypeEntity(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ENTITY a \"android\">  <!ELEMENT foo ANY>\n  <!ATTLIST foo\n    bar ENTITY #IMPLIED>]><foo bar=\"a\"></foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals("a", parser.getAttributeValue(null, "bar"))
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testTagStructureNotValidated(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ELEMENT foo (bar)*>\n  <!ELEMENT bar ANY>\n  <!ELEMENT baz ANY>\n]><foo><bar/><bar/><baz/></foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("bar", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("bar", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("baz", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testAttributeDefaultValues(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ATTLIST bar\n    baz (a|b|c)  \"c\">]><foo><bar/><bar baz=\"a\"/></foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("bar", parser.getName())
    assertEquals("c", parser.getAttributeValue(null, "baz"))
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("bar", parser.getName())
    assertEquals("a", parser.getAttributeValue(null, "baz"))
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testAttributeDefaultValueEntitiesExpanded(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ENTITY g \"ghi\">  <!ELEMENT foo ANY>\n" +
      "  <!ATTLIST foo\n    bar CDATA \"abc &amp; def &g; jk\">]><foo></foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals("abc & def ghi jk", parser.getAttributeValue(null, "bar"))
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testAttributeDefaultValuesAndNamespaces(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ATTLIST foo\n    bar:a CDATA \"android\">]><foo xmlns:bar='http://bar'></foo>"
    val parser = newPullParser(xml)
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, state = true)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    // In Expat, namespaces don't apply to default attributes
    val index = indexOfAttributeWithName(parser, "bar:a")
    assertEquals("", parser.getAttributeNamespace(index))
    assertEquals("bar:a", parser.getAttributeName(index))
    assertEquals("android", parser.getAttributeValue(index))
    assertEquals("CDATA", parser.getAttributeType(index))
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  private def indexOfAttributeWithName(parser: XmlPullParser, name: String): Int = {
    for (i <- 0 until parser.getAttributeCount()) {
      if (parser.getAttributeName(i) == name) return i
    }
    -1
  }

  @throws[Exception]
  @Test def testAttributeEntitiesExpandedEagerly(): Unit = {
    assertParseFailure(
      "<!DOCTYPE foo [\n  <!ELEMENT foo ANY>\n" +
        "  <!ATTLIST foo\n    bar CDATA \"abc &amp; def &g; jk\">  <!ENTITY g \"ghi\">]><foo></foo>"
    )
  }

  @throws[Exception]
  @Test def testRequiredAttributesOmitted(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ELEMENT foo ANY>\n  <!ATTLIST foo\n    bar (a|b|c) #REQUIRED>]><foo></foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(null, parser.getAttributeValue(null, "bar"))
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testFixedAttributesWithConflictingValues(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!ELEMENT foo ANY>\n  <!ATTLIST foo\n    bar (a|b|c) #FIXED \"c\">]><foo bar=\"a\"></foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals("a", parser.getAttributeValue(null, "bar"))
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testParsingNotations(): Unit = {
    val xml = "<!DOCTYPE foo [\n  <!NOTATION type-a PUBLIC \"application/a\"> \n" +
      "  <!NOTATION type-b PUBLIC \"image/b\">\n" +
      "  <!NOTATION type-c PUBLIC \"-//W3C//DTD SVG 1.1//EN\"\n     \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"> \n" +
      "  <!ENTITY file          SYSTEM \"d.xml\">\n" +
      "  <!ENTITY fileWithNdata SYSTEM \"e.bin\" NDATA type-b>\n]>" +
      "<foo type=\"type-a\"/>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testCommentsInDoctype(): Unit = {
    val xml = "<!DOCTYPE foo [  <!-- ' -->]><foo>android</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("android", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testDoctypeNameOnly(): Unit = {
    val xml = "<!DOCTYPE foo><foo></foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testVeryLongEntities(): Unit = {
    val a = repeat('a', PullParserDtdTest.READ_BUFFER_SIZE + 1)
    val b = repeat('b', PullParserDtdTest.READ_BUFFER_SIZE + 1)
    val c = repeat('c', PullParserDtdTest.READ_BUFFER_SIZE + 1)
    val xml = "<!DOCTYPE foo [\n  <!ENTITY " + a + "  \"d &" + b + "; e\">" +
      "  <!ENTITY " + b + "  \"f " + c + " g\">]><foo>h &" + a + "; i</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("h d f " + c + " g e i", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testManuallyRegisteredEntitiesWithDoctypeParsing(): Unit = {
    val xml = "<foo>&a;</foo>"
    val parser = newPullParser(xml)
    try {
      parser.defineEntityReplacementText("a", "android")
      fail()
    } catch {
      case expected: UnsupportedOperationException =>
      case expected: IllegalStateException =>
    }
  }

  @throws[Exception]
  @Test def testDoctypeWithNextToken(): Unit = {
    val xml = "<!DOCTYPE foo [<!ENTITY bb \"bar baz\">]><foo>a&bb;c</foo>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.DOCDECL, parser.nextToken())
    assertEquals(" foo [<!ENTITY bb \"bar baz\">]", parser.getText())
    assertNull(parser.getName())
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("abar bazc", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testDoctypeSpansBuffers(): Unit = {
    val doctypeChars = new Array[Char](PullParserDtdTest.READ_BUFFER_SIZE + 1)
    util.Arrays.fill(doctypeChars, 'x')
    val doctypeBody = " foo [<!--" + new String(doctypeChars) + "-->]"
    val xml = "<!DOCTYPE" + doctypeBody + "><foo/>"
    val parser = newPullParser(xml)
    assertEquals(XmlPullParser.DOCDECL, parser.nextToken())
    assertEquals(doctypeBody, parser.getText())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testDoctypeInDocumentElement(): Unit = {
    assertParseFailure("<foo><!DOCTYPE foo></foo>")
  }

  @throws[Exception]
  @Test def testDoctypeAfterDocumentElement(): Unit = {
    assertParseFailure("<foo/><!DOCTYPE foo>")
  }

  @throws[Exception]
  private def assertParseFailure(xml: String): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader(xml))
    try {
      while (parser.next() != XmlPullParser.END_DOCUMENT) {
      }
      fail()
    } catch {
      case expected: XmlPullParserException =>
    }
  }

  private def repeat(c: Char, length: Int): String = {
    val chars = new Array[Char](length)
    util.Arrays.fill(chars, c)
    new String(chars)
  }

  @throws[XmlPullParserException]
  private def newPullParser(xml: String): XmlPullParser = {
    val result = newPullParser()
    result.setInput(new StringReader(xml))
    result
  }

  /**
   * Creates a new pull parser.
   */
  @throws[XmlPullParserException]
  def newPullParser(): XmlPullParser
}