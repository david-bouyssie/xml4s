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

import java.util.HashMap

/*object SAXParserFactoryImpl {

  private val NAMESPACES = "http://xml.org/sax/features/namespaces"
  private val VALIDATION = "http://xml.org/sax/features/validation"

  // Default SAXParserImpl
  private var saxParserImplProvider: HashMap[String, Boolean] => SAXParser = { features =>
    new ExpatSAXParserImpl(features)
  }

  def setParserImplProvider(provider: HashMap[String, Boolean] => SAXParser): Unit = {
    saxParserImplProvider = provider
  }

}
*/

/**
 * Provides a straightforward SAXParserFactory implementation based on Expat.
 * The class is used internally only, thus only notable members
 * that are not already in the abstract superclass are documented.
 */
class SAXParserFactoryImpl extends CustomSAXParserFactoryImpl {
  def createImpl(features: HashMap[String, Boolean]): SAXParser = {
    new ExpatSAXParserImpl(features)
  }
}