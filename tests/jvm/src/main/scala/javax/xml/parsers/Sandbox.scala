package javax.xml.parsers

/*import java.io.StringReader

import org.kxml2.io.KXmlParser
import org.xmlpull.v1.{XmlPullParser, XmlPullParserException}

object Sandbox extends App {

  @throws[XmlPullParserException]
  def newPullParser(): XmlPullParser = {
    val result = new KXmlParser()
    result.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, true)
    result
  }

  @throws[XmlPullParserException]
  private def newPullParser(xml: String) = {
    val result = newPullParser()
    result.setInput(new StringReader(xml))
    result
  }

  @throws[Exception]
  def testDeclaringParameterEntities(): Unit = {
    val xml = "<!DOCTYPE foo [" + "  <!ENTITY % a \"android\">" + "]><foo></foo>"
    val parser = newPullParser(xml)
    while ( {parser.next != XmlPullParser.END_DOCUMENT}) {
    }
  }

  /*@throws[Exception]
  def testUsingParameterEntitiesInDtds(): Unit = {
    assertParseFailure("<!DOCTYPE foo [" + "  <!ENTITY % a \"android\">" + "  <!ENTITY b \"%a;\">" + "]><foo></foo>")
  }

  @throws[Exception]
  def testUsingParameterInDocuments(): Unit = {
    assertParseFailure("<!DOCTYPE foo [" + "  <!ENTITY % a \"android\">" + "]><foo>&a;</foo>")
  }*/

  @throws[Exception]
  def testGeneralAndParameterEntityWithTheSameName(): Unit = {
    val xml = "<!DOCTYPE foo [" + "  <!ENTITY a \"aaa\">" + "  <!ENTITY % a \"bbb\">" + "]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assert(XmlPullParser.START_TAG == parser.next)
    assert(XmlPullParser.TEXT == parser.next)
    assert("aaa" == parser.getText)
    assert(XmlPullParser.END_TAG == parser.next)
    assert(XmlPullParser.END_DOCUMENT == parser.next)
  }

  @throws[Exception]
  def testInternalEntities(): Unit = {
    val xml = "<!DOCTYPE foo [" + "  <!ENTITY a \"android\">" + "]><foo>&a;</foo>"
    val parser = newPullParser(xml)
    assert(XmlPullParser.START_TAG == parser.next)
    assert(XmlPullParser.TEXT == parser.next)
    assert("android" == parser.getText)
    assert(XmlPullParser.END_TAG == parser.next)
    assert(XmlPullParser.END_DOCUMENT == parser.next)
  }

  @throws[Exception]
  def testExternalDtdIsSilentlyIgnored(): Unit = {
    val xml = "<!DOCTYPE foo SYSTEM \"http://127.0.0.1:1/no-such-file.dtd\"><foo></foo>"
    val parser = newPullParser(xml)
    assert(XmlPullParser.START_TAG == parser.next)
    assert("foo" == parser.getName)
    assert(XmlPullParser.END_TAG == parser.next)
    assert("foo" == parser.getName)
    assert(XmlPullParser.END_DOCUMENT == parser.next)
  }

  testDeclaringParameterEntities()
  testInternalEntities()
  testExternalDtdIsSilentlyIgnored()
}
*/