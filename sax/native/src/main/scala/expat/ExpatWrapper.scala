package expat

import scala.collection.mutable.Stack
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._
import scala.scalanative.libc.stdlib
import scala.scalanative.libc.string.memcpy
import scala.scalanative.libc.string.strlen

import Expat._
import Constants._

// See: ./libcore/luni/src/main/native/org_apache_harmony_xml_ExpatParser.cpp
private[expat] object ExpatWrapper {

  private val BUCKET_COUNT = 128

  // FIXME: check if this is correct (see: https://github.com/libexpat/libexpat/blob/master/expat/lib/expat.h#L664)
  def XML_GetUserData(parser: XML_Parser): Ptr[Byte] = {
    val p = parser.asInstanceOf[Ptr[Ptr[Byte]]]
    !p
  }

  object ParsingContext {
    def fromParserPointer(pointer: XML_Parser): ParsingContext = {
      XML_GetUserData(pointer).asInstanceOf[ExpatWrapper.ParsingContext]
    }
    def fromPointer(data: Ptr[Byte]): ParsingContext = {
      data.asInstanceOf[ExpatWrapper.ParsingContext]
    }
  }

  class ParsingContext(
    /** The Java parser object. */
    var parserInstance: ExpatParser
  ) {

    /** Keep track of exceptions in handlers. */
    var handlerException: Throwable = _

    /** Buffer for text events. */
    var buffer: Array[Char] = _
    /** Current attributes. */
    var attributes: Ptr[CString] = _
    /** Number of attributes. */
    var attributeCount: Int = 0
    /** True if namespace support is enabled. */
    var processNamespaces: Boolean = false
    /** Keep track of names. */
    var stringStack: Stack[String] = new Stack[String]
    /** Cache of interned strings. */
    var internedStrings: Array[String] = new Array[String](BUCKET_COUNT)

    // TODO: reuse libcore.util.StringPool? or implement static jstring internString(JNIEnv* env, ParsingContext* parsingContext, const char* s)
    def getInternedString(s: String): String = {
      // FIXME: s.intern() returns s on SN
      s.intern()
    }

    private def ensureCapacity(length: Int): Array[Char] = {
      if (buffer == null || buffer.length < length) {
        buffer = new Array[Char](length)
      }
      buffer
    }

    /**
     * Copies UTF-8 characters into the buffer.
     * Returns the number of Java chars which were buffered.
     *
     * @returns number of UTF-16 characters which were copied
     */
    def fillBuffer(utf8: CString, byteLength: Int): Int = {

      // Grow buffer if necessary (the length in bytes is always >= the length in chars).
      val javaChars = ensureCapacity(byteLength)
      if (javaChars == null) {
        return -1
      }

      // Decode UTF-8 characters into our char[].
      val utf8Chars = fromCString(utf8).toCharArray
      val nChars = utf8Chars.length
      assert(nChars <= byteLength)

      System.arraycopy(utf8Chars, 0, javaChars, 0, nChars)

      nChars
    }

    // FIXME: maybe we should free attributes before setting to null???
    // what about interned string references?
    def release(): Unit = {
      buffer = null
      attributes = null
      attributeCount = -1
      stringStack = null
      internedStrings = null
    }
  }

  /**
   * The component parts of an attribute or element name.
   */
  class ExpatElementName private(private val parsingContext: ExpatWrapper.ParsingContext) {

    private var mUri: String = _
    private var mLocalName: String = _
    private var mPrefix: String = _

    def this(
      parsingContext: ExpatWrapper.ParsingContext,
      attributes: Ptr[CString],
      index: Int
      //val attributePointer: Ptr[Byte],
      //val index: UInt
    ) = {
      this(parsingContext)
      val attrName = attributes(index * 2)
      init(attrName)
    }

    def this(
      parsingContext: ExpatWrapper.ParsingContext,
      elemName: CString
    ) = {
      this(parsingContext)
      init(elemName)
    }

    /**
     * Decodes an Expat-encoded name of one of these three forms:
     * - "uri|localName|prefix" (example: "http://www.w3.org/1999/xhtml|h1|html")
     * - "uri|localName" (example: "http://www.w3.org/1999/xhtml|h1")
     * - "localName" (example: "h1")
     */
    // TODO: implement me using a lower level C implem (https://android.googlesource.com/platform/libcore/+/cff1616/luni/src/main/native/org_apache_harmony_xml_ExpatParser.cpp#555)
    def init(attrName: CString): Unit = {

      // split the input into up to 3 parts: a|b|c
      val parts = fromCString(attrName).split('|')

      parts.length match {
        case 3 => { // input of the form "uri|localName|prefix"
          mUri = parts(0)
          mLocalName = parts(1)
          mPrefix = parts(2)
        }
        case 2 => {  // input of the form "uri|localName"
          mUri = parts(0)
          mLocalName = parts(1)
          mPrefix = ""
        }
        case _ => { // input of the form "localName"
          mLocalName = parts(0)
          mUri = ""
          mPrefix = ""
        }
      }

    }

    /**
     * Returns the namespace URI, like "http://www.w3.org/1999/xhtml".
     * Possibly empty.
     */
    def getURI(): String = {
      parsingContext.getInternedString(this.mUri)
    }

    /**
     * Returns the element or attribute local name, like "h1". Never empty.
     * When namespace processing is disabled, this may contain a prefix, yielding a
     * local name like "html:h1". In such cases, the qName will always be empty.
     */
    def getLocalName(): String = {
      parsingContext.getInternedString(this.mLocalName)
    }

    def getQName(): String = {
      if (this.mPrefix.isEmpty) this.getLocalName()
      else {
        parsingContext.getInternedString(
          s"${this.mPrefix}:${this.getLocalName()}"
        )
      }
    }
  }

  /** List of the different parsing handlers */
  /*private val startElement = Callback.startElement _
  private val endElement = Callback.endElement _
  private val text = Callback.text _
  private val comment = Callback.comment _
  private val startNamespace = Callback.startNamespace _
  private val endNamespace = Callback.endNamespace _
  private val startCdata = Callback.startCdata _
  private val endCdata = Callback.endCdata _
  private val startDtd = Callback.startDtd _
  private val endDtd = Callback.endDtd _
  private val processingInstruction = Callback.processingInstruction _
  private val handleExternalEntity = Callback.handleExternalEntity _
  private val unparsedEntityDecl = Callback.unparsedEntityDecl _
  private val notationDecl = Callback.notationDecl _*/

  /**
   * Creates a new Expat parser. Called from the Java ExpatParser constructor.
   *
   * @param parserInstance the Java ExpatParser instance
   * @param javaEncoding the character encoding name
   * @param processNamespaces true if the parser should handle namespaces
   * @returns the pointer to the C Expat parser
   */
  def initializeParser(parserInstance: ExpatParser, javaEncoding: String, processNamespaces: Boolean): XML_Parser = {

    // Create the parsing context.
    val context = new ParsingContext(parserInstance)
    context.processNamespaces = processNamespaces

    // Create a parser.
    /*ScopedUtfChars encoding(env, javaEncoding);
    if (encoding.c_str() == NULL) {
      return 0;
    }*/
    val encoding_c_str = Zone { implicit z =>
      toCString(javaEncoding)
    }

    val parser: XML_Parser = if (processNamespaces) {
      // Use '|' to separate URIs from local names.
      XML_ParserCreateNS(encoding_c_str, '|');
    } else {
      XML_ParserCreate(encoding_c_str);
    }
    require(parser != null, "can't create an XML_Parser")

    // FIXME: enable the handlers
    /*
    if (processNamespaces) {
      XML_SetNamespaceDeclHandler(parser, startNamespace, endNamespace)
      XML_SetReturnNSTriplet(parser, 1)
    }

    XML_SetCdataSectionHandler(parser, startCdata, endCdata)
    XML_SetCharacterDataHandler(parser, text)
    XML_SetCommentHandler(parser, comment)
    XML_SetDoctypeDeclHandler(parser, startDtd, endDtd)
    XML_SetElementHandler(parser, startElement, endElement)
    XML_SetExternalEntityRefHandler(parser, handleExternalEntity)
    XML_SetNotationDeclHandler(parser, notationDecl)
    XML_SetProcessingInstructionHandler(parser, processingInstruction)
    XML_SetUnparsedEntityDeclHandler(parser, unparsedEntityDecl)
    XML_SetUserData(parser, context.asInstanceOf[Ptr[Byte]])
     */

    parser
  }

  /**
   * Decodes the bytes as characters and parse the characters as XML. This
   * performs character decoding using the charset specified at XML_Parser
   * creation.
   * For Java chars, that charset must be UTF-16 so that a Java char[]
   * can be reinterpreted as a UTF-16 encoded byte[].
   * appendBytes, appendChars and appendString all call through this method.
   */
  private def _append(
    parserInstance: ExpatParser,
    parser: XML_Parser,
    bytes: Ptr[Byte],
    byteOffset: CSize,
    byteCount: Int,
    isFinal: XML_Bool
  ): Unit = {
    val context = ParsingContext.fromParserPointer(parser)
    context.parserInstance = parserInstance

    val rc = XML_Parse(parser, bytes + byteOffset, byteCount, isFinal.toInt)

    if (rc == enum_XML_Status.XML_STATUS_ERROR) {
      throw new ExpatException(s"XML_Parse failed (error code=${XML_GetErrorCode(parser)})", context.handlerException)
    }

    context.parserInstance = null
  }

  def appendBytes(
    parserInstance: ExpatParser,
    parser: XML_Parser,
    xml: Array[Byte],
    byteOffset: Int,
    byteCount: Int
  ): Unit = {
    if (xml == null) return
    val bytesPtr = xml.asInstanceOf[scala.scalanative.runtime.ByteArray].at(0)
    _append(parserInstance, parser, bytesPtr, byteOffset.toUInt, byteCount, XML_FALSE)
  }

  def appendChars(
    parserInstance: ExpatParser,
    parser: XML_Parser,
    xml: Array[Char],
    charOffset: Int,
    charCount: Int
  ): Unit = {
    if (xml == null) return
    val bytesPtr = xml.asInstanceOf[scala.scalanative.runtime.CharArray].at(0).asInstanceOf[Ptr[Byte]]
    val byteOffset = 2 * charOffset
    val byteCount = 2 * charCount

    _append(parserInstance, parser, bytesPtr, byteOffset.toUInt, byteCount, XML_FALSE)
  }

  private val NULL_OFFSET: CSize = 0.toUInt

  def appendString(
    parserInstance: ExpatParser,
    parser: XML_Parser,
    xml: String,
    isFinal: Boolean
  ): Unit = {
    if (xml == null) return

    val bytes = xml.getBytes(java.nio.charset.StandardCharsets.UTF_16)
    val bytesPtr = bytes.asInstanceOf[scala.scalanative.runtime.ByteArray].at(0)
    val byteCount = 2 * xml.length
    assert(bytes.length == byteCount)

    _append(parserInstance, parser, bytesPtr, NULL_OFFSET, byteCount, if (isFinal) XML_TRUE else XML_FALSE)
  }

  /**
   * Releases parser only.
   */
  def releaseParser(parser: XML_Parser): Unit = {
    XML_ParserFree(parser)
  }

  /**
   * Cleans up after the parser. Called at garbage collection time.
   */
  def release(parser: XML_Parser): Unit = {
    val context = ParsingContext.fromParserPointer(parser)
    context.release()

    XML_ParserFree(parser)
  }


  def line(parser: XML_Parser): Int = {
    XML_GetCurrentLineNumber(parser).toInt
  }

  def column(parser: XML_Parser): Int = {
    XML_GetCurrentColumnNumber(parser).toInt
  }

  /**
   * Creates a new entity parser.
   *
   * @param parentParser pointer
   * @param context that was provided to handleExternalEntity
   * @returns the pointer to the C Expat entity parser
   */
  def createEntityParser(parentPointer: XML_Parser, context: String): XML_Parser = {
    Zone { implicit z =>
      Expat.XML_ExternalEntityParserCreate(
        parentPointer,
        toCString(context),
        null
      )
    }
  }

  private val NULL_CHAR: CChar = 0.toByte

  object Callback {

    val emptyString = "".intern()

    /**
     * Called by Expat at the start of an element. Delegates to the same method
     * on the Java parser.
     *
     * @param data parsing context
     * @param elementName "uri|localName" or "localName" for the current element
     * @param attributes alternating attribute names and values. Like element
     * names, attribute names follow the format "uri|localName" or "localName".
     */
    def startElement(data: Ptr[Byte], elementName: CString, attributes: Ptr[CString]): Unit = {

      val parsingContext = data.asInstanceOf[ParsingContext]

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        // Count the number of attributes.
        var count = 0
        while (attributes(count * 2) != null) {
          count += 1
        }

        // Make the attributes available for the duration of this call.
        parsingContext.attributes = attributes
        parsingContext.attributeCount = count

        val javaParser = parsingContext.parserInstance

        val elName = new ExpatElementName(parsingContext, elementName)
        val uri = if (parsingContext.processNamespaces) elName.getURI() else emptyString
        val localName = if (parsingContext.processNamespaces) elName.getLocalName() else emptyString
        val qName = elName.getQName()

        parsingContext.stringStack.push(qName)
        parsingContext.stringStack.push(uri)
        parsingContext.stringStack.push(localName)

        javaParser.startElement(
          uri,
          localName,
          qName,
          attributes,
          count
        )

        parsingContext.attributes = null
        parsingContext.attributeCount = -1
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Called by Expat at the end of an element. Delegates to the same method
     * on the Java parser.
     *
     * @param data parsing context
     * @param elementName "uri|localName" or "localName" for the current element;
     *         we assume that this matches the last data on our stack.
     */
    def endElement(data: Ptr[Byte], _elementName: CString): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        val javaParser = parsingContext.parserInstance

        val localName = parsingContext.stringStack.pop()
        val uri = parsingContext.stringStack.pop()
        val qName = parsingContext.stringStack.pop()

        javaParser.endElement(uri,localName, qName)

      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Called by Expat when it encounters text. Delegates to the same method
     * on the Java parser. This may be called mutiple times with incremental pieces
     * of the same contiguous block of text.
     *
     * @param data parsing context
     * @param characters buffer containing encountered text
     * @param length number of characters in the buffer
     */
    def text(data: Ptr[Byte], characters: CString, length: Int): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        val utf16length = parsingContext.fillBuffer(characters, length)
        parsingContext.parserInstance.text(parsingContext.buffer, utf16length)
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Called by Expat when it encounters a comment. Delegates to the same method
     * on the Java parser.

     * @param data parsing context
     * @param comment 0-terminated
     */
    def comment(data: Ptr[Byte], comment: CString): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        val utf16length = parsingContext.fillBuffer(comment, strlen(comment).toInt)
        parsingContext.parserInstance.comment(parsingContext.buffer, utf16length)
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Called by Expat at the beginning of a namespace mapping.
     *
     * @param data parsing context
     * @param prefix null-terminated namespace prefix used in the XML
     * @param uri of the namespace
     */
    def startNamespace(data: Ptr[Byte], prefix: CString, uri: CString): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        val internedPrefix = if (prefix == null) emptyString
        else parsingContext.getInternedString(fromCString(prefix))

        val internedUri = if (uri == null) emptyString
        else parsingContext.getInternedString(fromCString(uri))

        parsingContext.stringStack.push(internedPrefix)

        parsingContext.parserInstance.startNamespace(internedPrefix, internedUri)
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }

    }

    /**
     * Called by Expat at the end of a namespace mapping.
     *
     * @param data parsing context
     * @param prefix null-terminated namespace prefix used in the XML;
     *         we assume this is the same as the last prefix on the stack.
     */
    def endNamespace(data: Ptr[Byte], _prefix: CString): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        val internedPrefix = parsingContext.stringStack.pop()

        parsingContext.parserInstance.endNamespace(internedPrefix)
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }

    }

    /**
     * Called by Expat at the beginning of a CDATA section.
     *
     * @param data parsing context
     */
    def startCdata(data: Ptr[Byte]): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        parsingContext.parserInstance.startCdata()
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Called by Expat at the end of a CDATA section.
     *
     * @param data parsing context
     */
    def endCdata(data: Ptr[Byte]): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        parsingContext.parserInstance.endCdata()
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }

    }

    /**
     * Called by Expat at the beginning of a DOCTYPE section.
     * Expat gives us 'hasInternalSubset', but the Java API doesn't expect it, so we don't need it.
     */
    def startDtd(
      data: Ptr[Byte],
      name: CString,
      systemId: CString,
      publicId: CString,
      _hasInternalSubset: Int
    ): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        require(name != null, "name is null")
        require(systemId != null, "systemId is null")
        require(publicId != null, "publicId is null")

        val internedName = parsingContext.getInternedString(fromCString(name))
        val internedSystemId = parsingContext.getInternedString(fromCString(systemId))
        val internedPublicId = parsingContext.getInternedString(fromCString(publicId))

        parsingContext.parserInstance.startDtd(internedName, internedPublicId, internedSystemId)
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Called by Expat at the end of a DOCTYPE section.
     *
     * @param data parsing context
     */
    def endDtd(data: Ptr[Byte]): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        parsingContext.parserInstance.endDtd()
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Called by Expat when it encounters processing instructions.
     *
     * @param data parsing context
     * @param target of the instruction
     * @param instructionData
     */
    def processingInstruction(data: Ptr[Byte], target: CString, instructionData: CString): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        require(target != null, "target is null")
        require(instructionData != null, "instructionData is null")

        val internedTarget = parsingContext.getInternedString(fromCString(target))
        val javaInstructionData = fromCString(instructionData)

        parsingContext.parserInstance.processingInstruction(internedTarget, javaInstructionData)
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Handles external entities. We ignore the "base" URI and keep track of it ourselves.
     */
    def handleExternalEntity(
      parser: XML_Parser,
      context: CString,
      _base: CString,
      systemId: CString,
      publicId: CString
    ): CInt = {
      val parsingContext = ParsingContext.fromParserPointer(parser)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return enum_XML_Status.XML_STATUS_ERROR.toInt

      // Save parserInstance
      val savedParserInstance = parsingContext.parserInstance

      try {
        require(context != null, "context is null")
        require(systemId != null, "systemId is null")
        require(publicId != null, "publicId is null")

        val javaContext = fromCString(context)
        val javaSystemId = fromCString(systemId)
        val javaPublicId = fromCString(publicId)

        savedParserInstance.handleExternalEntity(javaContext, javaPublicId, javaSystemId)
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
          return enum_XML_Status.XML_STATUS_ERROR.toInt
        }
      } finally {
        /*
         * Parsing the external entity leaves parsingContext.parserInstance NULL, so we need to restore it.
         *
         * TODO: consider restoring it in the append() functions instead of setting it to NULL.
         */
        parsingContext.parserInstance = savedParserInstance
      }

      enum_XML_Status.XML_STATUS_OK.toInt
    }

    /**
     * Expat gives us 'base', but the Java API doesn't expect it, so we don't need it.
     */
    def unparsedEntityDecl(
      data: Ptr[Byte],
      name: CString,
      _base: CString,
      systemId: CString,
      publicId: CString,
      notationName: CString
    ): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        require(name != null, "name is null")
        require(systemId != null, "systemId is null")
        require(publicId != null, "publicId is null")
        require(notationName != null, "notationName is null")

        val javaName = fromCString(name)
        val javaSystemId = fromCString(systemId)
        val javaPublicId = fromCString(publicId)
        val javaNotationName = fromCString(notationName)

        parsingContext.parserInstance.unparsedEntityDecl(javaName, javaPublicId, javaSystemId, javaNotationName)
      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }

    /**
     * Expat gives us 'base', but the Java API doesn't expect it, so we don't need it.
     */
    def notationDecl(data: Ptr[Byte], name: CString, _base: CString, systemId: CString, publicId: CString): Unit = {
      val parsingContext = ParsingContext.fromPointer(data)

      // Bail out if a previously called handler threw an exception.
      if (parsingContext.handlerException != null) return

      try {
        require(name != null, "name is null")
        require(systemId != null, "systemId is null")
        require(publicId != null, "publicId is null")

        val javaName = fromCString(name)
        val javaSystemId = fromCString(systemId)
        val javaPublicId = fromCString(publicId)

        parsingContext.parserInstance.notationDecl(javaName, javaPublicId, javaSystemId)

      } catch {
        case t: Throwable => {
          parsingContext.handlerException = t
        }
      }
    }
  }

  object Attributes {

    @inline
    private def _parseElementName(pointer: XML_Parser, attributePointer: Ptr[CString], index: Int): ExpatElementName = {
      val parsingContext = ExpatWrapper.ParsingContext.fromParserPointer(pointer)
      new ExpatElementName(parsingContext, attributePointer, index)
    }

    /**
     * Gets the URI of the attribute at the given index.
     *
     * @param pointer          to the parser
     * @param attributePointer to the attribute array
     * @param index            of the attribute
     * @returns interned Java string containing attribute's URI
     */
    def getURI(pointer: XML_Parser, attributePointer: Ptr[CString], index: Int): String = {
      _parseElementName(pointer, attributePointer, index).getURI()
    }

    /**
     * Gets the local name of the attribute at the given index.
     *
     * @param pointer          to the parser
     * @param attributePointer to the attribute array
     * @param index of the attribute
     * @returns interned Java string containing attribute's local name
     */
    def getLocalName(pointer: XML_Parser, attributePointer: Ptr[CString], index: Int): String = {
      _parseElementName(pointer, attributePointer, index).getLocalName()
    }

    /**
     * Gets the qualified name of the attribute at the given index.
     *
     * @param pointer          to the parser
     * @param attributePointer to the attribute array
     * @param index of the attribute
     * @returns interned Java string containing attribute's local name
     */
    def getQName(pointer: XML_Parser, attributePointer: Ptr[CString], index: Int): String = {
      _parseElementName(pointer, attributePointer, index).getQName()
    }

    /**
     * Gets the value of the attribute at the given index.
     *
     * @param attributePointer to the attribute array
     * @param index of the attribute
     * @returns Java string containing attribute's value
     */
    def getValueByIndex(attributePointer: Ptr[CString], index: Int): String = {
      val i = (index * 2) + 1
      fromCString(attributePointer(i))
    }

    // Note: these functions were not ported since we took a different approach,
    // where ExpatElementName objects are cached to avoid parsing attributes several times (see: ExpatAttributes._getOrCreateElemName)
    /*

    /**
     * Gets the index of the attribute with the given URI and name.
     *
     * @param attributePointer to the attribute array
     * @param uri to look for
     * @param localName to look for
     * @returns index of attribute with the given uri and local name or -1 if not found
     */
    def getIndex(attributePointer: Ptr[CString], uri: String, localName: String): Int = {
    }

    /**
     * Gets the index of the attribute with the given qualified name.
     *
     * @param attributePointer to the attribute array
     * @param qName to look for
     * @returns index of attribute with the given uri and local name or -1 if not found
     */
    def getIndexForQName(attributePointer: Ptr[CString], qName: String): Int = {
    }

    /**
     * Gets the value of the attribute with the given URI and name.
     *
     * @param attributePointer to the attribute array
     * @param uri to look for
     * @param localName to look for
     * @returns value of attribute with the given uri and local name or NULL if not found
     */
    def getValue(attributePointer: Ptr[CString], uri: String, localName: String): String

    /**
     * Gets the value of the attribute with the given qualified name.
     *
     * @param attributePointer to the attribute array
     * @param uri to look for
     * @param localName to look for
     * @returns value of attribute with the given uri and local name or NULL if not found
     */
    def getValueForQName(attributePointer: Ptr[CString], qName: String): String

    */

    /**
     * Clones an array of strings. Uses one contiguous block of memory so as to
     * maximize performance.
     *
     * @param source char** to clone
     * @param count number of attributes
     */
    def cloneAttributes(source: Ptr[CString], count: Int): Ptr[CString] = {
      val len = count * 2

      // Figure out how big the buffer needs to be.
      val arraySize = (len + 1) * sizeof[CString].toInt
      var totalSize = arraySize
      val stringLengths = stackalloc[CSize](len.toUInt)

      var i = 0
      while (i < len) {
        val length = strlen(source(i))
        stringLengths(i) = length
        totalSize += length.toInt + 1

        i += 1
      }

      // FIXME: why buffer was allocated on the stack?
      //val buffer = stackalloc[CChar](totalSize.toUInt)
      val buffer = stdlib.malloc(totalSize.toUInt)
      assert(buffer != null, "can't allocate memory")

      // Array is at the beginning of the buffer.
      // FIXME: DBO => are we adding the NULL_CHAR properly?
      buffer(len) = NULL_CHAR // null terminate

      val clonedArray = buffer.asInstanceOf[Ptr[CString]]
      //clonedArray(len) = NULL_CHAR // null terminate

      // String data follows immediately after.
      var destinationString = buffer + arraySize

      i = 0
      while (i < len) {
        val sourceString = source(i)
        val stringLength = stringLengths(i).toInt + 1
        memcpy(destinationString, sourceString, stringLength.toUInt)
        clonedArray(i) = destinationString
        destinationString += stringLength
        i += 1
      }

      clonedArray
    }

    /**
     * Frees cloned attributes.
     */
    def freeAttributes(pointer: Ptr[CString]): Unit = {
      stdlib.free(pointer.asInstanceOf[Ptr[Byte]])
      //delete[] reinterpret_cast<char*>(static_cast<uintptr_t>(pointer));
    }

  }

}
