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

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

import org.junit.Assert._
import org.w3c.dom.Document
import org.w3c.dom.Element

object Support_Xml {

  @throws[Exception]
  def domOf(xml: String): Document = {
    /*
    // DocumentBuilderTest assumes we're using DocumentBuilder to do this parsing!
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.setCoalescing(true)
    dbf.setExpandEntityReferences(true)
    val stream = new ByteArrayInputStream(xml.getBytes)
    val builder = dbf.newDocumentBuilder
    builder.parse(stream)*/
    null // FIXME: DBO => implement DocumentBuilderFactory
  }

  /*@throws[Exception]
  def firstChildTextOf(doc: Document): String = {
    val children = doc.getFirstChild.getChildNodes
    assertEquals(1, children.getLength)
    children.item(0).getNodeValue
  }

  @throws[Exception]
  def firstElementOf(doc: Document): Element = {
    doc.getFirstChild.asInstanceOf[Element]
  }

  @throws[Exception]
  def attrOf(e: Element): String = {
    e.getAttribute("attr")
  }*/
}