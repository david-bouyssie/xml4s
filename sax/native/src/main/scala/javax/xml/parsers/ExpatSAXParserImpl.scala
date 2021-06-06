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

package javax.xml.parsers

import java.util.Collections
import java.util

import expat.ExpatReader
import org.xml.sax.Parser
import org.xml.sax.SAXException
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXNotSupportedException
import org.xml.sax.XMLReader
import org.xml.sax.helpers.XMLReaderAdapter

/**
 * A SAX parser based on Expat.
 */
final class ExpatSAXParserImpl @throws[SAXNotRecognizedException] @throws[SAXNotSupportedException] private() extends SAXParser {

  private var reader: XMLReader = _
  private var parser: Parser = _
  private var initialFeatures: util.Map[String, Boolean] = _

  private[parsers] def this(initialFeatures: util.Map[String, Boolean]) = {
    this()

    this.initialFeatures = if (initialFeatures.isEmpty) {
      Collections.emptyMap[String, Boolean]
    }
    else {
      new util.HashMap[String, Boolean](initialFeatures)
    }

    resetInternal ()
  }

  @throws[SAXNotSupportedException]
  @throws[SAXNotRecognizedException]
  private def resetInternal(): Unit = {
    reader = new ExpatReader()
    initialFeatures.entrySet.forEach { entry =>
      reader.setFeature(entry.getKey, entry.getValue)
    }
  }

  override def reset(): Unit = {
    /*
     * The exceptions are impossible. If any features are unrecognized or
     * unsupported, construction of this instance would have failed.
     */
    try resetInternal()
    catch {
      case e: SAXNotRecognizedException =>
        throw new AssertionError
      case e: SAXNotSupportedException =>
        throw new AssertionError
    }
  }

  override def getParser(): Parser = {
    if (parser == null) parser = new XMLReaderAdapter(reader)
    parser
  }

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getProperty(name: String): Any = reader.getProperty(name)

  override def getXMLReader(): XMLReader = reader

  override def isNamespaceAware(): Boolean = {
    try reader.getFeature("http://xml.org/sax/features/namespaces")
    catch {
      case ex: SAXException =>
        false
    }
  }

  override def isValidating(): Boolean = false

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setProperty(name: String, value: Any): Unit = {
    reader.setProperty(name, value)
  }

}