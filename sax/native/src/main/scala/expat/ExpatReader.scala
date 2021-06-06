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

import java.io.{IOException, InputStream, Reader}

import libcore.io.IoUtils

import org.xml.sax._
import org.xml.sax.ext.LexicalHandler

object ExpatReader {

  private val LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler"

  private object Feature {
    val BASE_URI = "http://xml.org/sax/features/"
    val VALIDATION = BASE_URI + "validation"
    val NAMESPACES = BASE_URI + "namespaces"
    val NAMESPACE_PREFIXES = BASE_URI + "namespace-prefixes"
    val STRING_INTERNING = BASE_URI + "string-interning"
    val EXTERNAL_GENERAL_ENTITIES = BASE_URI + "external-general-entities"
    val EXTERNAL_PARAMETER_ENTITIES = BASE_URI + "external-parameter-entities"
  }
}


/**
 * SAX wrapper around Expat.
 * Interns strings.
 * Does not support validation.
 * Does not support {@link DTDHandler}.
 */
class ExpatReader() extends XMLReader {

  /*
   * ExpatParser accesses these fields directly during parsing.
   * The user should be able to safely change them during parsing.
   */
  private[expat] var contentHandler: ContentHandler = _
  private[expat] var dtdHandler: DTDHandler = _
  private[expat] var entityResolver: EntityResolver = _
  private[expat] var errorHandler: ErrorHandler = _
  private[expat] var lexicalHandler: LexicalHandler = _

  private var processNamespaces = true
  private var processNamespacePrefixes = false

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getFeature(name: String): Boolean = {
    if (name == null) throw new NullPointerException("name == null")
    if (name == ExpatReader.Feature.VALIDATION || name == ExpatReader.Feature.EXTERNAL_GENERAL_ENTITIES || name == ExpatReader.Feature.EXTERNAL_PARAMETER_ENTITIES) return false
    if (name == ExpatReader.Feature.NAMESPACES) return processNamespaces
    if (name == ExpatReader.Feature.NAMESPACE_PREFIXES) return processNamespacePrefixes
    if (name == ExpatReader.Feature.STRING_INTERNING) return true
    throw new SAXNotRecognizedException(name)
  }

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setFeature(name: String, value: Boolean): Unit = {
    if (name == null) throw new NullPointerException("name == null")

    if (
      name == ExpatReader.Feature.VALIDATION ||
      name == ExpatReader.Feature.EXTERNAL_GENERAL_ENTITIES ||
      name == ExpatReader.Feature.EXTERNAL_PARAMETER_ENTITIES
    ) {
      if (value) throw new SAXNotSupportedException("Cannot enable " + name)
      else {
        // Default.
        return
      }
    }

    if (name == ExpatReader.Feature.NAMESPACES) {
      processNamespaces = value
      return
    }

    if (name == ExpatReader.Feature.NAMESPACE_PREFIXES) {
      processNamespacePrefixes = value
      return
    }

    if (name == ExpatReader.Feature.STRING_INTERNING) {
      if (value) return
      else throw new SAXNotSupportedException("Cannot disable " + name)
    }

    throw new SAXNotRecognizedException(name)
  }

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def getProperty(name: String): Any = {
    if (name == null) throw new NullPointerException("name == null")

    if (name == ExpatReader.LEXICAL_HANDLER_PROPERTY) return lexicalHandler
    throw new SAXNotRecognizedException(name)
  }

  @throws[SAXNotRecognizedException]
  @throws[SAXNotSupportedException]
  override def setProperty(name: String, value: Any): Unit = {
    if (name == null) throw new NullPointerException("name == null")

    if (name == ExpatReader.LEXICAL_HANDLER_PROPERTY) {
      // The object must implement LexicalHandler
      if (value.isInstanceOf[LexicalHandler] || value == null) {
        this.lexicalHandler = value.asInstanceOf[LexicalHandler]
        return
      }
      throw new SAXNotSupportedException("value doesn't implement org.xml.sax.ext.LexicalHandler")
    }

    throw new SAXNotRecognizedException(name)
  }

  override def setEntityResolver(resolver: EntityResolver): Unit = {
    this.entityResolver = resolver
  }

  override def getEntityResolver(): EntityResolver = entityResolver

  override def setDTDHandler(dtdHandler: DTDHandler): Unit = {
    this.dtdHandler = dtdHandler
  }

  override def getDTDHandler(): DTDHandler = dtdHandler

  override def setContentHandler(handler: ContentHandler): Unit = {
    this.contentHandler = handler
  }

  override def getContentHandler(): ContentHandler = this.contentHandler

  override def setErrorHandler(handler: ErrorHandler): Unit = {
    this.errorHandler = handler
  }

  override def getErrorHandler(): ErrorHandler = errorHandler

  /**
   * Returns the current lexical handler.
   *
   * @return the current lexical handler, or null if none has been registered
   * @see #setLexicalHandler
   */
  def getLexicalHandler(): LexicalHandler = lexicalHandler

  /**
   * Registers a lexical event handler. Supports neither
   * {@link LexicalHandler# startEntity ( String )} nor
   * {@link LexicalHandler# endEntity ( String )}.
   *
   * <p>If the application does not register a lexical handler, all
   * lexical events reported by the SAX parser will be silently
   * ignored.</p>
   *
   * <p>Applications may register a new or different handler in the
   * middle of a parse, and the SAX parser must begin using the new
   * handler immediately.</p>
   *
   * @param lexicalHandler listens for lexical events
   * @see #getLexicalHandler()
   */
  def setLexicalHandler(lexicalHandler: LexicalHandler): Unit = {
    this.lexicalHandler = lexicalHandler
  }

  /**
   * Returns true if this SAX parser processes namespaces.
   *
   * @see #setNamespaceProcessingEnabled(boolean)
   */
  def isNamespaceProcessingEnabled(): Boolean = processNamespaces

  /**
   * Enables or disables namespace processing. Set to true by default. If you
   * enable namespace processing, the parser will invoke
   * {@link ContentHandler# startPrefixMapping ( String, String)} and
   * {@link ContentHandler# endPrefixMapping ( String )}, and it will filter
   * out namespace declarations from element attributes.
   *
   * @see #isNamespaceProcessingEnabled()
   */
  def setNamespaceProcessingEnabled(processNamespaces: Boolean): Unit = {
    this.processNamespaces = processNamespaces
  }

  @throws[IOException]
  @throws[SAXException]
  override def parse(input: InputSource): Unit = {
    if (processNamespacePrefixes && processNamespaces) {
      /*
       * Expat has XML_SetReturnNSTriplet, but that still doesn't
       * include xmlns attributes like this feature requires. We may
       * have to implement namespace processing ourselves if we want
       * this (not too difficult). We obviously "support" namespace
       * prefixes if namespaces are disabled.
       */
      throw new SAXNotSupportedException("The 'namespace-prefix' feature is not supported while the 'namespaces' feature is enabled.")
    }

    // Try the character stream.
    val reader = input.getCharacterStream()
    if (reader != null) {
      try parse(reader, input.getPublicId(), input.getSystemId())
      finally IoUtils.closeQuietly(reader)
      return
    }

    // Try the byte stream.
    var in = input.getByteStream()
    val encoding = input.getEncoding()
    if (in != null) {
      try parse(in, encoding, input.getPublicId(), input.getSystemId())
      finally IoUtils.closeQuietly(in)
      return
    }

    val systemId = input.getSystemId()
    if (systemId == null) throw new SAXException("No input specified.")

    // Try the system id.
    in = IoUtils.openUrl(systemId, ExpatParser.TIMEOUT)

    try parse(in, encoding, input.getPublicId(), systemId)
    finally IoUtils.closeQuietly(in)
  }

  @throws[IOException]
  @throws[SAXException]
  private def parse(in: Reader, publicId: String, systemId: String): Unit = {
    val parser = new ExpatParser(ExpatParser.CHARACTER_ENCODING, this, processNamespaces, publicId, systemId)
    parser.parseDocument(in)
  }

  @throws[IOException]
  @throws[SAXException]
  private def parse(in: InputStream, charsetName: String, publicId: String, systemId: String): Unit = {
    val parser = new ExpatParser(charsetName, this, processNamespaces, publicId, systemId)
    parser.parseDocument(in)
  }

  @throws[IOException]
  @throws[SAXException]
  override def parse(systemId: String): Unit = {
    parse(new InputSource(systemId))
  }
}