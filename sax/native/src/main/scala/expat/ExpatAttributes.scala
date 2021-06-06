/*
 * Copyright (C) 2008 The Android Open Source Project
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

package expat

import scala.scalanative.unsafe._
import org.xml.sax.Attributes

import Expat.XML_Parser
import ExpatWrapper.ExpatElementName

/**
 * Wraps native attribute array.
 */
object ExpatAttributes {

  /**
   * Since we don't do validation, pretty much everything is CDATA type.
   */
  private val CDATA = "CDATA"

}

private[expat] abstract class ExpatAttributes extends Attributes {

  private val _elemNames = new Array[ExpatElementName](getLength())

  /** Lazy loading of single ExpatElementName. */
  private def _getOrCreateElemName(pointer: XML_Parser, attributePointer: Ptr[CString], index: Int): ExpatElementName = {
    val elemName = _elemNames(index)
    if (elemName != null) elemName
    else {
      val parsingContext = ExpatWrapper.ParsingContext.fromParserPointer(pointer)
      val newElemName = new ExpatElementName(parsingContext, attributePointer, index)
      _elemNames(index) = newElemName
      newElemName
    }
  }

  /** Lazy loading of all ExpatElementName. */
  private def _getElemNames(): Array[ExpatElementName] = {
    val nAttrs = getLength()
    var i = 0
    while (i < nAttrs) {
      _getOrCreateElemName(getParserPointer(), getPointer(), i)
      i += 1
    }

    _elemNames
  }

  /**
   * Gets the number of attributes.
   */
  override def getLength(): Int

  /**
   * Gets the pointer to the parser.
   * We need this so we can get to the interned string pool.
   */
  private[expat] def getParserPointer(): XML_Parser

  /**
   * Gets the pointer to the underlying attribute array.
   * Can be 0 if the length is 0.
   */
  private[expat] def getPointer(): Ptr[CString]

  override def getURI(index: Int): String = {
    if (index < 0 || index >= getLength()) null
    //ExpatWrapper.Attributes.getURI(getParserPointer(), getPointer(), index)
    else _getOrCreateElemName(getParserPointer(), getPointer(), index).getURI()
  }

  override def getLocalName(index: Int): String = {
    if (index < 0 || index >= getLength()) null
    //else ExpatWrapper.Attributes.getLocalName(getParserPointer(), getPointer(), index)
    else _getOrCreateElemName(getParserPointer(), getPointer(), index).getLocalName()
  }

  override def getQName(index: Int): String = {
    if (index < 0 || index >= getLength()) null
    //else ExpatWrapper.Attributes.getQName(getParserPointer(), getPointer(), index)
    else _getOrCreateElemName(getParserPointer(), getPointer(), index).getQName()
  }

  override def getType(index: Int): String = {
    if (index < 0 || index >= getLength()) null
    else ExpatAttributes.CDATA
  }

  override def getType(uri: String, localName: String): String = {
    if (uri == null) throw new NullPointerException("uri == null")
    if (localName == null) throw new NullPointerException("localName == null")

    if (getIndex(uri, localName) == -1) null else ExpatAttributes.CDATA
  }

  override def getType(qName: String): String = if (getIndex(qName) == -1) null
  else ExpatAttributes.CDATA

  override def getIndex(uri: String, localName: String): Int = {
    if (uri == null) throw new NullPointerException("uri == null")
    if (localName == null) throw new NullPointerException("localName == null")

    val pointer = getPointer()
    if (pointer == null) return -1

    //ExpatWrapper.Attributes.getIndex(pointer, uri, localName)

    val elNames = _getElemNames()
    val nAttrs = getLength()
    var i = 0
    while (i < nAttrs) {
      val elName = elNames(i)
      if (elName.getURI() == uri && elName.getLocalName() == localName)
        return i
      i += 1
    }

    -1
  }

  override def getIndex(qName: String): Int = {
    if (qName == null) throw new NullPointerException("qName == null")
    val pointer = getPointer()
    if (pointer == null) return -1

    //ExpatWrapper.Attributes.getIndexForQName(pointer, qName)
    val elNames = _getElemNames()
    val nAttrs = getLength()
    var i = 0
    while (i < nAttrs) {
      val elName = elNames(i)
      if (elName.getQName() == qName)
        return i
      i += 1
    }

    -1
  }

  override def getValue(index: Int): String = {
    if (index < 0 || index >= getLength()) null
    else ExpatWrapper.Attributes.getValueByIndex(getPointer(), index)
  }

  override def getValue(uri: String, localName: String): String = {
    if (uri == null) throw new NullPointerException("uri == null")
    if (localName == null) throw new NullPointerException("localName == null")

    val pointer = getPointer()
    if (pointer == null) return null

    //ExpatWrapper.Attributes.getValue(pointer, uri, localName)
    getValue(getIndex(uri, localName))
  }

  override def getValue(qName: String): String = {
    if (qName == null) throw new NullPointerException("qName == null")
    val pointer = getPointer()
    if (pointer == null) return null

    //ExpatWrapper.Attributes.getValueForQName(pointer, qName)
    getValue(getIndex(qName))
  }

}