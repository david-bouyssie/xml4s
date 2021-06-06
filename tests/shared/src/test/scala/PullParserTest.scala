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

import java.io.ByteArrayInputStream
import java.io.StringReader
//import junit.framework.TestCase
import org.junit.Assert._
import org.junit.Test
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

object PullParserTest {
  
  @throws[Exception]
  def validate(parser: XmlPullParser): Unit = {
    assertEquals(XmlPullParser.START_DOCUMENT, parser.getEventType())
    assertEquals(0, parser.getDepth())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(1, parser.getDepth())
    assertEquals("dagny", parser.getName())
    assertEquals(1, parser.getAttributeCount())
    assertEquals("dad", parser.getAttributeName(0))
    assertEquals("bob", parser.getAttributeValue(0))
    assertEquals("bob", parser.getAttributeValue(null, "dad"))
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals(1, parser.getDepth())
    assertEquals("hello", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(1, parser.getDepth())
    assertEquals("dagny", parser.getName())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
    assertEquals(0, parser.getDepth())
  }
}

abstract class PullParserTest { // extends TestCase
  
  @throws[Exception]
  @Test def testAttributeNoValueWithRelaxed(): Unit = {
    val parser = newPullParser()
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    parser.setInput(new StringReader("<input checked></input>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("input", parser.getName())
    assertEquals("checked", parser.getAttributeName(0))
    assertEquals("checked", parser.getAttributeValue(0))
  }

  @throws[Exception]
  @Test def testAttributeUnquotedValueWithRelaxed(): Unit = {
    val parser = newPullParser()
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    parser.setInput(new StringReader("<input checked=true></input>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("input", parser.getName())
    assertEquals("checked", parser.getAttributeName(0))
    assertEquals("true", parser.getAttributeValue(0))
  }

  @throws[Exception]
  @Test def testUnterminatedEntityWithRelaxed(): Unit = {
    val parser = newPullParser()
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    parser.setInput(new StringReader("<foo bar='A&W'>mac&cheese</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals("bar", parser.getAttributeName(0))
    assertEquals("A&W", parser.getAttributeValue(0))
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("mac&cheese", parser.getText())
  }

  @throws[Exception]
  @Test def testEntitiesAndNamespaces(): Unit = {
    val parser = newPullParser()
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", true)
    parser.setInput(new StringReader("<foo:a xmlns:foo='http://foo' xmlns:bar='http://bar'><bar:b/></foo:a>"))
    testNamespace(parser)
  }

  @throws[Exception]
  @Test def testEntitiesAndNamespacesWithRelaxed(): Unit = {
    val parser = newPullParser()
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", true)
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    parser.setInput(new StringReader("<foo:a xmlns:foo='http://foo' xmlns:bar='http://bar'><bar:b/></foo:a>"))
    testNamespace(parser)
  }

  @throws[Exception]
  private def testNamespace(parser: XmlPullParser): Unit = {
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("http://foo", parser.getNamespace())
    assertEquals("a", parser.getName())
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("http://bar", parser.getNamespace())
    assertEquals("b", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("http://bar", parser.getNamespace())
    assertEquals("b", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("http://foo", parser.getNamespace())
    assertEquals("a", parser.getName())
  }

  @throws[Exception]
  @Test def testRegularNumericEntities(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>&#65;</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
    assertEquals("#65", parser.getName())
    assertEquals("A", parser.getText())
  }

  @throws[Exception]
  @Test def testNumericEntitiesLargerThanChar(): Unit = {
    assertParseFailure("<foo>&#2147483647; &#-2147483648;</foo>")
  }

  @throws[Exception]
  @Test def testNumericEntitiesLargerThanInt(): Unit = {
    assertParseFailure("<foo>&#2147483648;</foo>")
  }

  @throws[Exception]
  @Test def testCharacterReferenceOfHexUtf16Surrogates(): Unit = {
    testCharacterReferenceOfUtf16Surrogates("<foo>&#x10000; &#x10381; &#x10FFF0;</foo>")
  }

  @throws[Exception]
  @Test def testCharacterReferenceOfDecimalUtf16Surrogates(): Unit = {
    testCharacterReferenceOfUtf16Surrogates("<foo>&#65536; &#66433; &#1114096;</foo>")
  }

  @throws[Exception]
  private def testCharacterReferenceOfUtf16Surrogates(xml: String): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader(xml))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals(new String(Array[Int](65536, ' ', 66433, ' ', 1114096), 0, 5), parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testCharacterReferenceOfLastUtf16Surrogate(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>&#x10FFFF;</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals(new String(Array[Int](0x10FFFF), 0, 1), parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testOmittedNumericEntities(): Unit = {
    assertParseFailure("<foo>&#;</foo>")
  }

  /**
   * Carriage returns followed by line feeds are silently discarded.
   */
  @throws[Exception]
  @Test def testCarriageReturnLineFeed(): Unit = {
    testLineEndings("\r\n<foo\r\na='b\r\nc'\r\n>d\r\ne</foo\r\n>\r\n")
  }

  /**
   * Lone carriage returns are treated like newlines.
   */
  @throws[Exception]
  @Test def testLoneCarriageReturn(): Unit = {
    testLineEndings("\r<foo\ra='b\rc'\r>d\re</foo\r>\r")
  }

  @throws[Exception]
  @Test def testLoneNewLine(): Unit = {
    testLineEndings("\n<foo\na='b\nc'\n>d\ne</foo\n>\n")
  }

  @throws[Exception]
  private def testLineEndings(xml: String) = {
    val parser = newPullParser()
    parser.setInput(new StringReader(xml))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals("b c", parser.getAttributeValue(0))
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("d\ne", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testXmlDeclaration(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<?xml version='1.0' encoding='UTF-8' standalone='no'?><foo/>"))
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals("1.0", parser.getProperty("http://xmlpull.org/v1/doc/properties.html#xmldecl-version"))
    assertEquals(java.lang.Boolean.FALSE, parser.getProperty("http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone"))
    assertEquals("UTF-8", parser.getInputEncoding())
  }

  @throws[Exception]
  @Test def testXmlDeclarationExtraAttributes(): Unit = {
    assertParseFailure("<?xml version='1.0' encoding='UTF-8' standalone='no' a='b'?><foo/>")
  }

  @throws[Exception]
  @Test def testCustomEntitiesUsingNext(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo a='cd&aaaaaaaaaa;ef'>wx&aaaaaaaaaa;yz</foo>"))
    parser.defineEntityReplacementText("aaaaaaaaaa", "b")
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("cdbef", parser.getAttributeValue(0))
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("wxbyz", parser.getText())
  }

  @throws[Exception]
  @Test def testCustomEntitiesUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo a='cd&aaaaaaaaaa;ef'>wx&aaaaaaaaaa;yz</foo>"))
    parser.defineEntityReplacementText("aaaaaaaaaa", "b")
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals("cdbef", parser.getAttributeValue(0))
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("wx", parser.getText())
    assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
    assertEquals("aaaaaaaaaa", parser.getName())
    assertEquals("b", parser.getText())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("yz", parser.getText())
  }

  @throws[Exception]
  @Test def testCustomEntitiesAreNotEvaluated(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo a='&a;'>&a;</foo>"))
    parser.defineEntityReplacementText("a", "&amp; &a;")
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("&amp; &a;", parser.getAttributeValue(0))
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("&amp; &a;", parser.getText())
  }

  @throws[Exception]
  @Test def testMissingEntities(): Unit = {
    assertParseFailure("<foo>&aaa;</foo>")
  }

  @throws[Exception]
  @Test def testMissingEntitiesWithRelaxed(): Unit = {
    val parser = newPullParser()
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    parser.setInput(new StringReader("<foo>&aaa;</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals(null, parser.getName())
    assertEquals("Expected unresolved entities to be left in-place. The old parser " + "would resolve these to the empty string.", "&aaa;", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testMissingEntitiesUsingNextToken(): Unit = {
    val parser = newPullParser()
    testMissingEntitiesUsingNextToken(parser)
  }

  @throws[Exception]
  @Test def testMissingEntitiesUsingNextTokenWithRelaxed(): Unit = {
    val parser = newPullParser()
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    testMissingEntitiesUsingNextToken(parser)
  }

  @throws[Exception]
  private def testMissingEntitiesUsingNextToken(parser: XmlPullParser) = {
    parser.setInput(new StringReader("<foo>&aaa;</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
    assertEquals("aaa", parser.getName())
    assertEquals(null, parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testEntityInAttributeUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo bar=\"&amp;\"></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals("foo", parser.getName())
    assertEquals("&", parser.getAttributeValue(null, "bar"))
  }

  @throws[Exception]
  @Test def testMissingEntitiesInAttributesUsingNext(): Unit = assertParseFailure("<foo b='&aaa;'></foo>")

  @throws[Exception]
  @Test def testMissingEntitiesInAttributesUsingNextWithRelaxed(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo b='&aaa;'></foo>"))
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals(1, parser.getAttributeCount())
    assertEquals("b", parser.getAttributeName(0))
    assertEquals("Expected unresolved entities to be left in-place. The old parser " + "would resolve these to the empty string.", "&aaa;", parser.getAttributeValue(0))
  }

  @throws[Exception]
  @Test def testMissingEntitiesInAttributesUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo b='&aaa;'></foo>"))
    testMissingEntitiesInAttributesUsingNextToken(parser)
  }

  @throws[Exception]
  @Test def testMissingEntitiesInAttributesUsingNextTokenWithRelaxed(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo b='&aaa;'></foo>"))
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    testMissingEntitiesInAttributesUsingNextToken(parser)
  }

  @throws[Exception]
  private def testMissingEntitiesInAttributesUsingNextToken(parser: XmlPullParser) = {
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals(1, parser.getAttributeCount())
    assertEquals("b", parser.getAttributeName(0))
    assertEquals("Expected unresolved entities to be left in-place. The old parser " + "would resolve these to the empty string.", "&aaa;", parser.getAttributeValue(0))
  }

  @throws[Exception]
  @Test def testGreaterThanInText(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals(">", parser.getText())
  }

  @throws[Exception]
  @Test def testGreaterThanInAttribute(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo a='>'></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(">", parser.getAttributeValue(0))
  }

  @throws[Exception]
  @Test def testLessThanInText(): Unit = {
    assertParseFailure("<foo><</foo>")
  }

  @throws[Exception]
  @Test def testLessThanInAttribute(): Unit = {
    assertParseFailure("<foo a='<'></foo>")
  }

  @throws[Exception]
  @Test def testQuotesInAttribute(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo a='\"' b=\"'\"></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("\"", parser.getAttributeValue(0))
    assertEquals("'", parser.getAttributeValue(1))
  }

  @throws[Exception]
  @Test def testQuotesInText(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>\" '</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("\" '", parser.getText())
  }

  @throws[Exception]
  @Test def testCdataDelimiterInAttribute(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo a=']]>'></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("]]>", parser.getAttributeValue(0))
  }

  @throws[Exception]
  @Test def testCdataDelimiterInText(): Unit = {
    assertParseFailure("<foo>]]></foo>")
  }

  @throws[Exception]
  @Test def testUnexpectedEof(): Unit = {
    assertParseFailure("<foo><![C")
  }

  @throws[Exception]
  @Test def testUnexpectedSequence(): Unit = {
    assertParseFailure("<foo><![Cdata[bar]]></foo>")
  }

  @throws[Exception]
  @Test def testThreeDashCommentDelimiter(): Unit = {
    assertParseFailure("<foo><!--a---></foo>")
  }

  @throws[Exception]
  @Test def testTwoDashesInComment(): Unit = {
    assertParseFailure("<foo><!-- -- --></foo>")
  }

  @throws[Exception]
  @Test def testEmptyComment(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo><!----></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.COMMENT, parser.nextToken())
    assertEquals("", parser.getText())
  }

  /**
   * Close braces require lookaheads because we need to defend against "]]>".
   */
  @throws[Exception]
  @Test def testManyCloseBraces(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>]]]]]]]]]]]]]]]]]]]]]]]</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("]]]]]]]]]]]]]]]]]]]]]]]", parser.getText())
  }

  @throws[Exception]
  @Test def testCommentUsingNext(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>ab<!-- comment! -->cd</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("abcd", parser.getText())
  }

  @throws[Exception]
  @Test def testCommentUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>ab<!-- comment! -->cd</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("ab", parser.getText())
    assertEquals(XmlPullParser.COMMENT, parser.nextToken())
    assertEquals(" comment! ", parser.getText())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("cd", parser.getText())
  }

  @throws[Exception]
  @Test def testCdataUsingNext(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>ab<![CDATA[cdef]]gh&amp;i]]>jk</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("abcdef]]gh&amp;ijk", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testCdataUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>ab<![CDATA[cdef]]gh&amp;i]]>jk</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("ab", parser.getText())
    assertEquals(XmlPullParser.CDSECT, parser.nextToken())
    assertEquals("cdef]]gh&amp;i", parser.getText())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("jk", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.nextToken())
  }

  @throws[Exception]
  @Test def testEntityLooksLikeCdataClose(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>&#93;&#93;></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("]]>", parser.getText())
  }

  @throws[Exception]
  @Test def testProcessingInstructionUsingNext(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>ab<?cd efg hij?>kl</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.TEXT, parser.next())
    assertEquals("abkl", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testProcessingInstructionUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>ab<?cd efg hij?>kl</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("ab", parser.getText())
    assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
    assertEquals("cd efg hij", parser.getText())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("kl", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testWhitespaceUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("  \n  <foo> \n </foo>   \n   "))
    assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
    assertEquals(true, parser.isWhitespace())
    assertEquals("  \n  ", parser.getText())
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals(true, parser.isWhitespace())
    assertEquals(" \n ", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
    assertEquals(true, parser.isWhitespace())
    assertEquals("   \n   ", parser.getText())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken())
  }

  @throws[Exception]
  @Test def testLinesAndColumns(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("\n" + "  <foo><bar a='\n" + "' b='cde'></bar\n" + "><!--\n" + "\n" + "--><baz/>fg\n" + "</foo>"))
    assertEquals("1,1", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
    assertEquals("2,3", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals("2,8", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals("3,11", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    assertEquals("4,2", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.COMMENT, parser.nextToken())
    assertEquals("6,4", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals("6,10", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    assertEquals("6,10", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.TEXT, parser.nextToken())
    assertEquals("7,1", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    assertEquals("7,7", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken())
    assertEquals("7,7", "" + parser.getLineNumber() + "," + parser.getColumnNumber())
  }

  @throws[Exception]
  @Test def testEmptyEntityReferenceUsingNext(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>&empty;</foo>"))
    parser.defineEntityReplacementText("empty", "")
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testEmptyEntityReferenceUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>&empty;</foo>"))
    parser.defineEntityReplacementText("empty", "")
    assertEquals(XmlPullParser.START_TAG, parser.nextToken())
    assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
    assertEquals("empty", parser.getName())
    assertEquals("", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.nextToken())
  }

  @throws[Exception]
  @Test def testEmptyCdataUsingNext(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo><![CDATA[]]></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testEmptyCdataUsingNextToken(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo><![CDATA[]]></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.CDSECT, parser.nextToken())
    assertEquals("", parser.getText())
    assertEquals(XmlPullParser.END_TAG, parser.next())
  }

  @throws[Exception]
  @Test def testParseReader(): Unit = {
    val snippet = "<dagny dad=\"bob\">hello</dagny>"
    val parser = newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(new StringReader(snippet))
    PullParserTest.validate(parser)
  }

  @throws[Exception]
  @Test def testParseInputStream(): Unit = {
    val snippet = "<dagny dad=\"bob\">hello</dagny>"
    val parser = newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(new ByteArrayInputStream(snippet.getBytes), "UTF-8")
    PullParserTest.validate(parser)
  }

  @throws[Exception]
  @Test def testNextAfterEndDocument(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo></foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testNamespaces(): Unit = {
    val xml = "<one xmlns='ns:default' xmlns:n1='ns:1' a='b'>\n" +
      "  <n1:two c='d' n1:e='f' xmlns:n2='ns:2'>text</n1:two>\n" +
      "</one>"

    val parser = newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, state = true)
    parser.setInput(new StringReader(xml))
    assertEquals(0, parser.getDepth())
    assertEquals(0, parser.getNamespaceCount(0))

    try {
      parser.getNamespaceCount(1)
      fail()
    } catch {
      case e: IndexOutOfBoundsException =>

      /* expected */
    }

    // one
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals(1, parser.getDepth())
    checkNamespacesInOne(parser)
    // n1:two
    assertEquals(XmlPullParser.START_TAG, parser.nextTag())
    assertEquals(2, parser.getDepth())
    checkNamespacesInTwo(parser)
    // Body of two.
    assertEquals(XmlPullParser.TEXT, parser.next())
    // End of two.
    assertEquals(XmlPullParser.END_TAG, parser.nextTag())
    // Depth should still be 2.
    assertEquals(2, parser.getDepth())
    // We should still be able to see the namespaces from two.
    checkNamespacesInTwo(parser)
    // End of one.
    assertEquals(XmlPullParser.END_TAG, parser.nextTag())
    // Depth should be back to 1.
    assertEquals(1, parser.getDepth())
    // We can still see the namespaces in one.
    checkNamespacesInOne(parser)
    // We shouldn't be able to see the namespaces in two anymore.
    try {
      parser.getNamespaceCount(2)
      fail()
    } catch {
      case e: IndexOutOfBoundsException =>
    }
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
    // We shouldn't be able to see the namespaces in one anymore.
    try {
      parser.getNamespaceCount(1)
      fail()
    } catch {
      case e: IndexOutOfBoundsException =>
    }
    assertEquals(0, parser.getNamespaceCount(0))
  }

  @throws[XmlPullParserException]
  private def checkNamespacesInOne(parser: XmlPullParser) = {
    assertEquals(2, parser.getNamespaceCount(1))
    // Prefix for default namespace is null.
    assertNull(parser.getNamespacePrefix(0))
    assertEquals("ns:default", parser.getNamespaceUri(0))
    assertEquals("n1", parser.getNamespacePrefix(1))
    assertEquals("ns:1", parser.getNamespaceUri(1))
    assertEquals("ns:default", parser.getNamespace(null))
    // KXML returns null.
    // assertEquals("ns:default", parser.getNamespace(""));
  }

  @throws[XmlPullParserException]
  private def checkNamespacesInTwo(parser: XmlPullParser) = {
    // These should still be valid.
    checkNamespacesInOne(parser)
    assertEquals(3, parser.getNamespaceCount(2))
    // Default ns should still be in the stack
    assertNull(parser.getNamespacePrefix(0))
    assertEquals("ns:default", parser.getNamespaceUri(0))
  }

  @throws[Exception]
  @Test def testTextBeforeDocumentElement(): Unit = {
    assertParseFailure("not xml<foo/>")
  }

  @throws[Exception]
  @Test def testTextAfterDocumentElement(): Unit = {
    assertParseFailure("<foo/>not xml")
  }

  @throws[Exception]
  @Test def testTextNoDocumentElement(): Unit = {
    assertParseFailure("not xml")
  }

  @throws[Exception]
  @Test def testBomAndByteInput(): Unit = {
    val xml = "\ufeff<?xml version='1.0'?><input/>".getBytes("UTF-8")
    val parser = newPullParser()
    parser.setInput(new ByteArrayInputStream(xml), null)
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("input", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("input", parser.getName())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testBomAndByteInputWithExplicitCharset(): Unit = {
    val xml = "\ufeff<?xml version='1.0'?><input/>".getBytes("UTF-8")
    val parser = newPullParser()
    parser.setInput(new ByteArrayInputStream(xml), "UTF-8")
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("input", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.next())
    assertEquals("input", parser.getName())
    assertEquals(XmlPullParser.END_DOCUMENT, parser.next())
  }

  @throws[Exception]
  @Test def testBomAndCharacterInput(): Unit = assertParseFailure("\ufeff<?xml version='1.0'?><input/>")

  // http://code.google.com/p/android/issues/detail?id=21425
  @throws[Exception]
  @Test def testNextTextAdvancesToEndTag(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo>bar</foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.next())
    assertEquals("bar", parser.nextText())
    assertEquals(XmlPullParser.END_TAG, parser.getEventType())
  }

  @throws[Exception]
  @Test def testNextTag(): Unit = {
    val parser = newPullParser()
    parser.setInput(new StringReader("<foo> <bar></bar> </foo>"))
    assertEquals(XmlPullParser.START_TAG, parser.nextTag())
    assertEquals("foo", parser.getName())
    assertEquals(XmlPullParser.START_TAG, parser.nextTag())
    assertEquals("bar", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.nextTag())
    assertEquals("bar", parser.getName())
    assertEquals(XmlPullParser.END_TAG, parser.nextTag())
    assertEquals("foo", parser.getName())
  }

  @throws[Exception]
  @Test def testEofInElementSpecRelaxed(): Unit = {
    assertRelaxedParseFailure("<!DOCTYPE foo [<!ELEMENT foo (unterminated")
  }

  @throws[Exception]
  @Test def testEofInAttributeValue(): Unit = {
    assertParseFailure("<!DOCTYPE foo [<!ATTLIST foo x y \"unterminated")
  }

  @throws[Exception]
  @Test def testEofInEntityValue(): Unit = {
    assertParseFailure("<!DOCTYPE foo [<!ENTITY aaa \"unterminated")
  }

  @throws[Exception]
  @Test def testEofInStartTagAttributeValue(): Unit = {
    assertParseFailure("<long foo=\"unterminated")
  }

  @throws[Exception]
  @Test def testEofInReadCharRelaxed(): Unit = {
    assertRelaxedParseFailure("<!DOCTYPE foo [<!ELEMENT foo ()")
  } // EOF in read('>')

  @throws[Exception]
  @Test def testEofAfterReadCharArrayRelaxed(): Unit = {
    assertRelaxedParseFailure("<!DOCTYPE foo [<!ELEMENT foo EMPTY")
  }

  @throws[Exception]
  @Test def testWhitespacesAfterDOCTYPE(): Unit = {
    val parser = newPullParser()
    val test = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE root [\n" + "<!ENTITY fake \"fake\">\n" + "]>  \n" + "<root></root>"
    assertParseSuccess(test, parser)
  }

  // Regression test for https://code.google.com/p/android/issues/detail?id=182605
  @throws[Exception]
  @Test def testSetInputParserReuse(): Unit = {
    val parser = newPullParser()
    val test = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE root [\n" + "<!ENTITY fake \"fake\">\n" + "]>  \n" + "<root></root>"
    assertParseSuccess(test, parser)
    // A second call to parser.setInput() on a parser should result in a fully-reset parser.
    assertParseSuccess(test, parser)
  }

  @throws[Exception]
  private def assertParseFailure(xml: String): Unit = {
    val parser = newPullParser()
    assertParseFailure(xml, parser)
  }

  @throws[Exception]
  private def assertRelaxedParseFailure(xml: String) = {
    val parser = newPullParser()
    parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
    assertParseFailure(xml, parser)
  }

  @throws[Exception]
  private def assertParseFailure(xml: String, parser: XmlPullParser): Unit = {
    parser.setInput(new StringReader(xml))
    try {
      while ( {parser.next() != XmlPullParser.END_DOCUMENT}) {
      }
      fail()
    } catch {
      case expected: XmlPullParserException =>
    }
  }

  @throws[Exception]
  private def assertParseSuccess(xml: String, parser: XmlPullParser) = {
    parser.setInput(new StringReader(xml))
    while ( {parser.next() != XmlPullParser.END_DOCUMENT}) {
    }
  }

  /**
   * Creates a new pull parser.
   */
  def newPullParser(): XmlPullParser
}