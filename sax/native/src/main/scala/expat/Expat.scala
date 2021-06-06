/*
                            __  __            _
                         ___\ \/ /_ __   __ _| |_
                        / _ \\  /| '_ \ / _` | __|
                       |  __//  \| |_) | (_| | |_
                        \___/_/\_\ .__/ \__,_|\__|
                                 |_| XML parser
   Copyright (c) 1997-2000 Thai Open Source Software Center Ltd
   Copyright (c) 2000      Clark Cooper <coopercc@users.sourceforge.net>
   Copyright (c) 2000-2005 Fred L. Drake, Jr. <fdrake@users.sourceforge.net>
   Copyright (c) 2001-2002 Greg Stein <gstein@users.sourceforge.net>
   Copyright (c) 2002-2016 Karl Waclawek <karl@waclawek.net>
   Copyright (c) 2016-2021 Sebastian Pipping <sebastian@pipping.org>
   Copyright (c) 2016      Cristian Rodríguez <crrodriguez@opensuse.org>
   Copyright (c) 2016      Thomas Beutlich <tc@tbeu.de>
   Copyright (c) 2017      Rhodri James <rhodri@wildebeest.org.uk>
   Licensed under the MIT license:
   Permission is  hereby granted,  free of charge,  to any  person obtaining
   a  copy  of  this  software   and  associated  documentation  files  (the
   "Software"),  to  deal in  the  Software  without restriction,  including
   without  limitation the  rights  to use,  copy,  modify, merge,  publish,
   distribute, sublicense, and/or sell copies of the Software, and to permit
   persons  to whom  the Software  is  furnished to  do so,  subject to  the
   following conditions:
   The above copyright  notice and this permission notice  shall be included
   in all copies or substantial portions of the Software.
   THE  SOFTWARE  IS  PROVIDED  "AS  IS",  WITHOUT  WARRANTY  OF  ANY  KIND,
   EXPRESS  OR IMPLIED,  INCLUDING  BUT  NOT LIMITED  TO  THE WARRANTIES  OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
   NO EVENT SHALL THE AUTHORS OR  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
   DAMAGES OR  OTHER LIABILITY, WHETHER  IN AN  ACTION OF CONTRACT,  TORT OR
   OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
   USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package expat

import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

@link("expat")
@extern
object Expat {

  object Constants {
    // --- Manual additions (missing #define entries) --- //
    val XML_TRUE: XML_Bool = 1.toUByte
    val XML_FALSE : XML_Bool = 0.toUByte
    // --- End of manual additions --- //

    val XML_MAJOR_VERSION: CInt = 2
    val XML_MINOR_VERSION: CInt = 3
    val XML_MICRO_VERSION: CInt = 0
  }

  /**
   * The XML_Status enum gives the possible return values for several API functions.
   */
  type enum_XML_Status = CUnsignedInt
  object enum_XML_Status {
    final val XML_STATUS_ERROR: enum_XML_Status = 0.toUInt
    final val XML_STATUS_OK: enum_XML_Status = 1.toUInt
    final val XML_STATUS_SUSPENDED: enum_XML_Status = 2.toUInt
  }

  type enum_XML_Error = CUnsignedInt
  object enum_XML_Error {
    final val XML_ERROR_NONE: enum_XML_Error = 0.toUInt
    final val XML_ERROR_NO_MEMORY: enum_XML_Error = 1.toUInt
    final val XML_ERROR_SYNTAX: enum_XML_Error = 2.toUInt
    final val XML_ERROR_NO_ELEMENTS: enum_XML_Error = 3.toUInt
    final val XML_ERROR_INVALID_TOKEN: enum_XML_Error = 4.toUInt
    final val XML_ERROR_UNCLOSED_TOKEN: enum_XML_Error = 5.toUInt
    final val XML_ERROR_PARTIAL_CHAR: enum_XML_Error = 6.toUInt
    final val XML_ERROR_TAG_MISMATCH: enum_XML_Error = 7.toUInt
    final val XML_ERROR_DUPLICATE_ATTRIBUTE: enum_XML_Error = 8.toUInt
    final val XML_ERROR_JUNK_AFTER_DOC_ELEMENT: enum_XML_Error = 9.toUInt
    final val XML_ERROR_PARAM_ENTITY_REF: enum_XML_Error = 10.toUInt
    final val XML_ERROR_UNDEFINED_ENTITY: enum_XML_Error = 11.toUInt
    final val XML_ERROR_RECURSIVE_ENTITY_REF: enum_XML_Error = 12.toUInt
    final val XML_ERROR_ASYNC_ENTITY: enum_XML_Error = 13.toUInt
    final val XML_ERROR_BAD_CHAR_REF: enum_XML_Error = 14.toUInt
    final val XML_ERROR_BINARY_ENTITY_REF: enum_XML_Error = 15.toUInt
    final val XML_ERROR_ATTRIBUTE_EXTERNAL_ENTITY_REF: enum_XML_Error = 16.toUInt
    final val XML_ERROR_MISPLACED_XML_PI: enum_XML_Error = 17.toUInt
    final val XML_ERROR_UNKNOWN_ENCODING: enum_XML_Error = 18.toUInt
    final val XML_ERROR_INCORRECT_ENCODING: enum_XML_Error = 19.toUInt
    final val XML_ERROR_UNCLOSED_CDATA_SECTION: enum_XML_Error = 20.toUInt
    final val XML_ERROR_EXTERNAL_ENTITY_HANDLING: enum_XML_Error = 21.toUInt
    final val XML_ERROR_NOT_STANDALONE: enum_XML_Error = 22.toUInt
    final val XML_ERROR_UNEXPECTED_STATE: enum_XML_Error = 23.toUInt
    final val XML_ERROR_ENTITY_DECLARED_IN_PE: enum_XML_Error = 24.toUInt
    final val XML_ERROR_FEATURE_REQUIRES_XML_DTD: enum_XML_Error = 25.toUInt
    final val XML_ERROR_CANT_CHANGE_FEATURE_ONCE_PARSING: enum_XML_Error = 26.toUInt
    final val XML_ERROR_UNBOUND_PREFIX: enum_XML_Error = 27.toUInt
    final val XML_ERROR_UNDECLARING_PREFIX: enum_XML_Error = 28.toUInt
    final val XML_ERROR_INCOMPLETE_PE: enum_XML_Error = 29.toUInt
    final val XML_ERROR_XML_DECL: enum_XML_Error = 30.toUInt
    final val XML_ERROR_TEXT_DECL: enum_XML_Error = 31.toUInt
    final val XML_ERROR_PUBLICID: enum_XML_Error = 32.toUInt
    final val XML_ERROR_SUSPENDED: enum_XML_Error = 33.toUInt
    final val XML_ERROR_NOT_SUSPENDED: enum_XML_Error = 34.toUInt
    final val XML_ERROR_ABORTED: enum_XML_Error = 35.toUInt
    final val XML_ERROR_FINISHED: enum_XML_Error = 36.toUInt
    final val XML_ERROR_SUSPEND_PE: enum_XML_Error = 37.toUInt
    final val XML_ERROR_RESERVED_PREFIX_XML: enum_XML_Error = 38.toUInt
    final val XML_ERROR_RESERVED_PREFIX_XMLNS: enum_XML_Error = 39.toUInt
    final val XML_ERROR_RESERVED_NAMESPACE_URI: enum_XML_Error = 40.toUInt
    final val XML_ERROR_INVALID_ARGUMENT: enum_XML_Error = 41.toUInt
    final val XML_ERROR_NO_BUFFER: enum_XML_Error = 42.toUInt
    final val XML_ERROR_AMPLIFICATION_LIMIT_BREACH: enum_XML_Error = 43.toUInt
  }

  type enum_XML_Content_Type = CUnsignedInt
  object enum_XML_Content_Type {
    final val XML_CTYPE_EMPTY: enum_XML_Content_Type = 1.toUInt
    final val XML_CTYPE_ANY: enum_XML_Content_Type = 2.toUInt
    final val XML_CTYPE_MIXED: enum_XML_Content_Type = 3.toUInt
    final val XML_CTYPE_NAME: enum_XML_Content_Type = 4.toUInt
    final val XML_CTYPE_CHOICE: enum_XML_Content_Type = 5.toUInt
    final val XML_CTYPE_SEQ: enum_XML_Content_Type = 6.toUInt
  }

  type enum_XML_Content_Quant = CUnsignedInt
  object enum_XML_Content_Quant {
    final val XML_CQUANT_NONE: enum_XML_Content_Quant = 0.toUInt
    final val XML_CQUANT_OPT: enum_XML_Content_Quant = 1.toUInt
    final val XML_CQUANT_REP: enum_XML_Content_Quant = 2.toUInt
    final val XML_CQUANT_PLUS: enum_XML_Content_Quant = 3.toUInt
  }

  type enum_XML_Parsing = CUnsignedInt
  object enum_XML_Parsing {
    final val XML_INITIALIZED: enum_XML_Parsing = 0.toUInt
    final val XML_PARSING: enum_XML_Parsing = 1.toUInt
    final val XML_FINISHED: enum_XML_Parsing = 2.toUInt
    final val XML_SUSPENDED: enum_XML_Parsing = 3.toUInt
  }

  type enum_XML_ParamEntityParsing = CUnsignedInt
  object enum_XML_ParamEntityParsing {
    final val XML_PARAM_ENTITY_PARSING_NEVER: enum_XML_ParamEntityParsing = 0.toUInt
    final val XML_PARAM_ENTITY_PARSING_UNLESS_STANDALONE: enum_XML_ParamEntityParsing = 1.toUInt
    final val XML_PARAM_ENTITY_PARSING_ALWAYS: enum_XML_ParamEntityParsing = 2.toUInt
  }

  type enum_XML_FeatureEnum = CUnsignedInt
  object enum_XML_FeatureEnum {
    final val XML_FEATURE_END: enum_XML_FeatureEnum = 0.toUInt
    final val XML_FEATURE_UNICODE: enum_XML_FeatureEnum = 1.toUInt
    final val XML_FEATURE_UNICODE_WCHAR_T: enum_XML_FeatureEnum = 2.toUInt
    final val XML_FEATURE_DTD: enum_XML_FeatureEnum = 3.toUInt
    final val XML_FEATURE_CONTEXT_BYTES: enum_XML_FeatureEnum = 4.toUInt
    final val XML_FEATURE_MIN_SIZE: enum_XML_FeatureEnum = 5.toUInt
    final val XML_FEATURE_SIZEOF_XML_CHAR: enum_XML_FeatureEnum = 6.toUInt
    final val XML_FEATURE_SIZEOF_XML_LCHAR: enum_XML_FeatureEnum = 7.toUInt
    final val XML_FEATURE_NS: enum_XML_FeatureEnum = 8.toUInt
    final val XML_FEATURE_LARGE_SIZE: enum_XML_FeatureEnum = 9.toUInt
    final val XML_FEATURE_ATTR_INFO: enum_XML_FeatureEnum = 10.toUInt
    final val XML_FEATURE_BILLION_LAUGHS_ATTACK_PROTECTION_MAXIMUM_AMPLIFICATION_DEFAULT: enum_XML_FeatureEnum = 11.toUInt
    final val XML_FEATURE_BILLION_LAUGHS_ATTACK_PROTECTION_ACTIVATION_THRESHOLD_DEFAULT: enum_XML_FeatureEnum = 12.toUInt
  }

  type XML_Char = CChar
  type XML_Index = CLong
  type XML_Size = CUnsignedLong
  type struct_XML_ParserStruct = CStruct0 // incomplete type
  type XML_Parser = Ptr[struct_XML_ParserStruct]
  type XML_Bool = CUnsignedChar
  type struct_XML_cp = CStruct5[enum_XML_Content_Type, enum_XML_Content_Quant, CString, CUnsignedInt, Ptr[Byte]]
  type XML_Content = struct_XML_cp
  type XML_ElementDeclHandler = CFuncPtr3[Ptr[Byte], CString, Ptr[XML_Content], Unit]
  type XML_AttlistDeclHandler = CFuncPtr6[Ptr[Byte], CString, CString, CString, CString, CInt, Unit]
  type XML_XmlDeclHandler = CFuncPtr4[Ptr[Byte], CString, CString, CInt, Unit]
  type struct_XML_Memory_Handling_Suite = CStruct3[CFuncPtr1[CSize, Ptr[Byte]], CFuncPtr2[Ptr[Byte], CSize, Ptr[Byte]], CFuncPtr1[Ptr[Byte], Unit]]
  type XML_Memory_Handling_Suite = struct_XML_Memory_Handling_Suite
  type XML_StartElementHandler = CFuncPtr3[Ptr[Byte], CString, Ptr[CString], Unit]
  type XML_EndElementHandler = CFuncPtr2[Ptr[Byte], CString, Unit]
  type XML_CharacterDataHandler = CFuncPtr3[Ptr[Byte], CString, CInt, Unit]
  type XML_ProcessingInstructionHandler = CFuncPtr3[Ptr[Byte], CString, CString, Unit]
  type XML_CommentHandler = CFuncPtr2[Ptr[Byte], CString, Unit]
  type XML_StartCdataSectionHandler = CFuncPtr1[Ptr[Byte], Unit]
  type XML_EndCdataSectionHandler = CFuncPtr1[Ptr[Byte], Unit]
  type XML_DefaultHandler = CFuncPtr3[Ptr[Byte], CString, CInt, Unit]
  type XML_StartDoctypeDeclHandler = CFuncPtr5[Ptr[Byte], CString, CString, CString, CInt, Unit]
  type XML_EndDoctypeDeclHandler = CFuncPtr1[Ptr[Byte], Unit]
  type XML_EntityDeclHandler = CFuncPtr9[Ptr[Byte], CString, CInt, CString, CInt, CString, CString, CString, CString, Unit]
  type XML_UnparsedEntityDeclHandler = CFuncPtr6[Ptr[Byte], CString, CString, CString, CString, CString, Unit]
  type XML_NotationDeclHandler = CFuncPtr5[Ptr[Byte], CString, CString, CString, CString, Unit]
  type XML_StartNamespaceDeclHandler = CFuncPtr3[Ptr[Byte], CString, CString, Unit]
  type XML_EndNamespaceDeclHandler = CFuncPtr2[Ptr[Byte], CString, Unit]
  type XML_NotStandaloneHandler = CFuncPtr1[Ptr[Byte], CInt]
  type XML_ExternalEntityRefHandler = CFuncPtr5[XML_Parser, CString, CString, CString, CString, CInt]
  type XML_SkippedEntityHandler = CFuncPtr3[Ptr[Byte], CString, CInt, Unit]
  type struct_XML_Encoding = CStruct4[CArray[CInt, Nat.Digit3[Nat._2, Nat._5, Nat._6]], Ptr[Byte], CFuncPtr2[Ptr[Byte], CString, CInt], CFuncPtr1[Ptr[Byte], Unit]]
  type XML_Encoding = struct_XML_Encoding
  type XML_UnknownEncodingHandler = CFuncPtr3[Ptr[Byte], CString, Ptr[XML_Encoding], CInt]
  type struct_XML_ParsingStatus = CStruct2[enum_XML_Parsing, XML_Bool]
  type XML_ParsingStatus = struct_XML_ParsingStatus
  type struct_XML_Expat_Version = CStruct3[CInt, CInt, CInt]
  type XML_Expat_Version = struct_XML_Expat_Version
  type struct_XML_Feature = CStruct3[enum_XML_FeatureEnum, CString, CLong]
  type XML_Feature = struct_XML_Feature

  def XML_SetElementDeclHandler(parser: XML_Parser, eldecl: CFuncPtr3[Ptr[Byte], CString, Ptr[XML_Content], Unit]): Unit = extern
  def XML_SetAttlistDeclHandler(parser: XML_Parser, attdecl: CFuncPtr6[Ptr[Byte], CString, CString, CString, CString, CInt, Unit]): Unit = extern
  def XML_SetXmlDeclHandler(parser: XML_Parser, xmldecl: CFuncPtr4[Ptr[Byte], CString, CString, CInt, Unit]): Unit = extern
  def XML_ParserCreate(encoding: CString): XML_Parser = extern
  def XML_ParserCreateNS(encoding: CString, namespaceSeparator: XML_Char): XML_Parser = extern
  def XML_ParserCreate_MM(encoding: CString, memsuite: Ptr[XML_Memory_Handling_Suite], namespaceSeparator: CString): XML_Parser = extern
  def XML_ParserReset(parser: XML_Parser, encoding: CString): XML_Bool = extern
  def XML_SetEntityDeclHandler(parser: XML_Parser, handler: CFuncPtr9[Ptr[Byte], CString, CInt, CString, CInt, CString, CString, CString, CString, Unit]): Unit = extern
  def XML_SetElementHandler(parser: XML_Parser, start: CFuncPtr3[Ptr[Byte], CString, Ptr[CString], Unit], end: CFuncPtr2[Ptr[Byte], CString, Unit]): Unit = extern
  def XML_SetStartElementHandler(parser: XML_Parser, handler: CFuncPtr3[Ptr[Byte], CString, Ptr[CString], Unit]): Unit = extern
  def XML_SetEndElementHandler(parser: XML_Parser, handler: CFuncPtr2[Ptr[Byte], CString, Unit]): Unit = extern
  def XML_SetCharacterDataHandler(parser: XML_Parser, handler: CFuncPtr3[Ptr[Byte], CString, CInt, Unit]): Unit = extern
  def XML_SetProcessingInstructionHandler(parser: XML_Parser, handler: CFuncPtr3[Ptr[Byte], CString, CString, Unit]): Unit = extern
  def XML_SetCommentHandler(parser: XML_Parser, handler: CFuncPtr2[Ptr[Byte], CString, Unit]): Unit = extern
  def XML_SetCdataSectionHandler(parser: XML_Parser, start: CFuncPtr1[Ptr[Byte], Unit], end: CFuncPtr1[Ptr[Byte], Unit]): Unit = extern
  def XML_SetStartCdataSectionHandler(parser: XML_Parser, start: CFuncPtr1[Ptr[Byte], Unit]): Unit = extern
  def XML_SetEndCdataSectionHandler(parser: XML_Parser, end: CFuncPtr1[Ptr[Byte], Unit]): Unit = extern
  def XML_SetDefaultHandler(parser: XML_Parser, handler: CFuncPtr3[Ptr[Byte], CString, CInt, Unit]): Unit = extern
  def XML_SetDefaultHandlerExpand(parser: XML_Parser, handler: CFuncPtr3[Ptr[Byte], CString, CInt, Unit]): Unit = extern
  def XML_SetDoctypeDeclHandler(parser: XML_Parser, start: CFuncPtr5[Ptr[Byte], CString, CString, CString, CInt, Unit], end: CFuncPtr1[Ptr[Byte], Unit]): Unit = extern
  def XML_SetStartDoctypeDeclHandler(parser: XML_Parser, start: CFuncPtr5[Ptr[Byte], CString, CString, CString, CInt, Unit]): Unit = extern
  def XML_SetEndDoctypeDeclHandler(parser: XML_Parser, end: CFuncPtr1[Ptr[Byte], Unit]): Unit = extern
  def XML_SetUnparsedEntityDeclHandler(parser: XML_Parser, handler: CFuncPtr6[Ptr[Byte], CString, CString, CString, CString, CString, Unit]): Unit = extern
  def XML_SetNotationDeclHandler(parser: XML_Parser, handler: CFuncPtr5[Ptr[Byte], CString, CString, CString, CString, Unit]): Unit = extern
  def XML_SetNamespaceDeclHandler(parser: XML_Parser, start: CFuncPtr3[Ptr[Byte], CString, CString, Unit], end: CFuncPtr2[Ptr[Byte], CString, Unit]): Unit = extern
  def XML_SetStartNamespaceDeclHandler(parser: XML_Parser, start: CFuncPtr3[Ptr[Byte], CString, CString, Unit]): Unit = extern
  def XML_SetEndNamespaceDeclHandler(parser: XML_Parser, end: CFuncPtr2[Ptr[Byte], CString, Unit]): Unit = extern
  def XML_SetNotStandaloneHandler(parser: XML_Parser, handler: CFuncPtr1[Ptr[Byte], CInt]): Unit = extern
  def XML_SetExternalEntityRefHandler(parser: XML_Parser, handler: CFuncPtr5[XML_Parser, CString, CString, CString, CString, CInt]): Unit = extern
  def XML_SetExternalEntityRefHandlerArg(parser: XML_Parser, arg: Ptr[Byte]): Unit = extern
  def XML_SetSkippedEntityHandler(parser: XML_Parser, handler: CFuncPtr3[Ptr[Byte], CString, CInt, Unit]): Unit = extern
  def XML_SetUnknownEncodingHandler(parser: XML_Parser, handler: CFuncPtr3[Ptr[Byte], CString, Ptr[XML_Encoding], CInt], encodingHandlerData: Ptr[Byte]): Unit = extern
  def XML_DefaultCurrent(parser: XML_Parser): Unit = extern
  def XML_SetReturnNSTriplet(parser: XML_Parser, do_nst: CInt): Unit = extern
  def XML_SetUserData(parser: XML_Parser, userData: Ptr[Byte]): Unit = extern
  def XML_SetEncoding(parser: XML_Parser, encoding: CString): enum_XML_Status = extern
  def XML_UseParserAsHandlerArg(parser: XML_Parser): Unit = extern
  def XML_UseForeignDTD(parser: XML_Parser, useDTD: XML_Bool): enum_XML_Error = extern
  def XML_SetBase(parser: XML_Parser, base: CString): enum_XML_Status = extern
  def XML_GetBase(parser: XML_Parser): CString = extern
  def XML_GetSpecifiedAttributeCount(parser: XML_Parser): CInt = extern
  def XML_GetIdAttributeIndex(parser: XML_Parser): CInt = extern
  def XML_Parse(parser: XML_Parser, s: CString, len: CInt, isFinal: CInt): enum_XML_Status = extern
  def XML_GetBuffer(parser: XML_Parser, len: CInt): Ptr[Byte] = extern
  def XML_ParseBuffer(parser: XML_Parser, len: CInt, isFinal: CInt): enum_XML_Status = extern
  def XML_StopParser(parser: XML_Parser, resumable: XML_Bool): enum_XML_Status = extern
  def XML_ResumeParser(parser: XML_Parser): enum_XML_Status = extern
  def XML_GetParsingStatus(parser: XML_Parser, status: Ptr[XML_ParsingStatus]): Unit = extern
  def XML_ExternalEntityParserCreate(parser: XML_Parser, context: CString, encoding: CString): XML_Parser = extern
  def XML_SetParamEntityParsing(parser: XML_Parser, parsing: enum_XML_ParamEntityParsing): CInt = extern
  def XML_SetHashSalt(parser: XML_Parser, hash_salt: CUnsignedLong): CInt = extern
  def XML_GetErrorCode(parser: XML_Parser): enum_XML_Error = extern
  def XML_GetCurrentLineNumber(parser: XML_Parser): XML_Size = extern
  def XML_GetCurrentColumnNumber(parser: XML_Parser): XML_Size = extern
  def XML_GetCurrentByteIndex(parser: XML_Parser): XML_Index = extern
  def XML_GetCurrentByteCount(parser: XML_Parser): CInt = extern
  def XML_GetInputContext(parser: XML_Parser, offset: Ptr[CInt], size: Ptr[CInt]): CString = extern
  def XML_FreeContentModel(parser: XML_Parser, model: Ptr[XML_Content]): Unit = extern
  def XML_MemMalloc(parser: XML_Parser, size: CSize): Ptr[Byte] = extern
  def XML_MemRealloc(parser: XML_Parser, ptr: Ptr[Byte], size: CSize): Ptr[Byte] = extern
  def XML_MemFree(parser: XML_Parser, ptr: Ptr[Byte]): Unit = extern
  def XML_ParserFree(parser: XML_Parser): Unit = extern
  def XML_ErrorString(code: enum_XML_Error): CString = extern
  def XML_ExpatVersion(): CString = extern
  def XML_GetFeatureList(): Ptr[XML_Feature] = extern

  object implicits {
    implicit class struct_XML_cp_ops(val p: Ptr[struct_XML_cp]) extends AnyVal {
      def `type`: enum_XML_Content_Type = p._1
      def `type_=`(value: enum_XML_Content_Type): Unit = p._1 = value
      def quant: enum_XML_Content_Quant = p._2
      def quant_=(value: enum_XML_Content_Quant): Unit = p._2 = value
      def name: CString = p._3
      def name_=(value: CString): Unit = p._3 = value
      def numchildren: CUnsignedInt = p._4
      def numchildren_=(value: CUnsignedInt): Unit = p._4 = value
      def children: Ptr[XML_Content] = p._5.asInstanceOf[Ptr[XML_Content]]
      def children_=(value: Ptr[XML_Content]): Unit = p._5 = value.asInstanceOf[Ptr[Byte]]
    }

    implicit class struct_XML_Memory_Handling_Suite_ops(val p: Ptr[struct_XML_Memory_Handling_Suite]) extends AnyVal {
      def malloc_fcn: CFuncPtr1[CSize, Ptr[Byte]] = p._1
      def malloc_fcn_=(value: CFuncPtr1[CSize, Ptr[Byte]]): Unit = p._1 = value
      def realloc_fcn: CFuncPtr2[Ptr[Byte], CSize, Ptr[Byte]] = p._2
      def realloc_fcn_=(value: CFuncPtr2[Ptr[Byte], CSize, Ptr[Byte]]): Unit = p._2 = value
      def free_fcn: CFuncPtr1[Ptr[Byte], Unit] = p._3
      def free_fcn_=(value: CFuncPtr1[Ptr[Byte], Unit]): Unit = p._3 = value
    }

    implicit class struct_XML_Encoding_ops(val p: Ptr[struct_XML_Encoding]) extends AnyVal {
      def map: CArray[CInt, Nat.Digit3[Nat._2, Nat._5, Nat._6]] = p._1
      def map_=(value: Ptr[CArray[CInt, Nat.Digit3[Nat._2, Nat._5, Nat._6]]]): Unit = p._1 = !value
      def data: Ptr[Byte] = p._2
      def data_=(value: Ptr[Byte]): Unit = p._2 = value
      def convert: CFuncPtr2[Ptr[Byte], CString, CInt] = p._3
      def convert_=(value: CFuncPtr2[Ptr[Byte], CString, CInt]): Unit = p._3 = value
      def release: CFuncPtr1[Ptr[Byte], Unit] = p._4
      def release_=(value: CFuncPtr1[Ptr[Byte], Unit]): Unit = p._4 = value
    }

    implicit class struct_XML_ParsingStatus_ops(val p: Ptr[struct_XML_ParsingStatus]) extends AnyVal {
      def parsing: enum_XML_Parsing = p._1
      def parsing_=(value: enum_XML_Parsing): Unit = p._1 = value
      def finalBuffer: XML_Bool = p._2
      def finalBuffer_=(value: XML_Bool): Unit = p._2 = value
    }

    implicit class struct_XML_Expat_Version_ops(val p: Ptr[struct_XML_Expat_Version]) extends AnyVal {
      def major: CInt = p._1
      def major_=(value: CInt): Unit = p._1 = value
      def minor: CInt = p._2
      def minor_=(value: CInt): Unit = p._2 = value
      def micro: CInt = p._3
      def micro_=(value: CInt): Unit = p._3 = value
    }

    implicit class struct_XML_Feature_ops(val p: Ptr[struct_XML_Feature]) extends AnyVal {
      def feature: enum_XML_FeatureEnum = p._1
      def feature_=(value: enum_XML_FeatureEnum): Unit = p._1 = value
      def name: CString = p._2
      def name_=(value: CString): Unit = p._2 = value
      def value: CLong = p._3
      def value_=(value: CLong): Unit = p._3 = value
    }
  }

  object struct_XML_cp {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_XML_cp] = alloc[struct_XML_cp]
    def apply(`type`: enum_XML_Content_Type, quant: enum_XML_Content_Quant, name: CString, numchildren: CUnsignedInt, children: Ptr[XML_Content])(implicit z: Zone): Ptr[struct_XML_cp] = {
      val ptr = alloc[struct_XML_cp]
      ptr.`type` = `type`
      ptr.quant = quant
      ptr.name = name
      ptr.numchildren = numchildren
      ptr.children = children
      ptr
    }
  }

  object struct_XML_Memory_Handling_Suite {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_XML_Memory_Handling_Suite] = alloc[struct_XML_Memory_Handling_Suite]
    def apply(malloc_fcn: CFuncPtr1[CSize, Ptr[Byte]], realloc_fcn: CFuncPtr2[Ptr[Byte], CSize, Ptr[Byte]], free_fcn: CFuncPtr1[Ptr[Byte], Unit])(implicit z: Zone): Ptr[struct_XML_Memory_Handling_Suite] = {
      val ptr = alloc[struct_XML_Memory_Handling_Suite]
      ptr.malloc_fcn = malloc_fcn
      ptr.realloc_fcn = realloc_fcn
      ptr.free_fcn = free_fcn
      ptr
    }
  }

  object struct_XML_Encoding {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_XML_Encoding] = alloc[struct_XML_Encoding]

    // FIXME: implement me
    /*def apply(
      map: Ptr[CArray[CInt, Nat.Digit3[Nat._2, Nat._5, Nat._6]]],
      data: Ptr[Byte],
      convert: CFuncPtr2[Ptr[Byte], CString, CInt],
      release: CFuncPtr1[Ptr[Byte], Unit]
    )(implicit z: Zone): Ptr[struct_XML_Encoding] = {
      val ptr = alloc[struct_XML_Encoding]
      ptr.map = !map
      ptr.data = data
      ptr.convert = convert
      ptr.release = release
      ptr
    }*/
  }

  object struct_XML_ParsingStatus {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_XML_ParsingStatus] = alloc[struct_XML_ParsingStatus]
    def apply(parsing: enum_XML_Parsing, finalBuffer: XML_Bool)(implicit z: Zone): Ptr[struct_XML_ParsingStatus] = {
      val ptr = alloc[struct_XML_ParsingStatus]
      ptr.parsing = parsing
      ptr.finalBuffer = finalBuffer
      ptr
    }
  }

  object struct_XML_Expat_Version {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_XML_Expat_Version] = alloc[struct_XML_Expat_Version]
    def apply(major: CInt, minor: CInt, micro: CInt)(implicit z: Zone): Ptr[struct_XML_Expat_Version] = {
      val ptr = alloc[struct_XML_Expat_Version]
      ptr.major = major
      ptr.minor = minor
      ptr.micro = micro
      ptr
    }
  }

  object struct_XML_Feature {
    import implicits._
    def apply()(implicit z: Zone): Ptr[struct_XML_Feature] = alloc[struct_XML_Feature]
    def apply(feature: enum_XML_FeatureEnum, name: CString, value: CLong)(implicit z: Zone): Ptr[struct_XML_Feature] = {
      val ptr = alloc[struct_XML_Feature]
      ptr.feature = feature
      ptr.name = name
      ptr.value = value
      ptr
    }
  }
}

