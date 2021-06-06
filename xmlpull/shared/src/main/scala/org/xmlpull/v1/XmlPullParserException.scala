/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)
package org.xmlpull.v1

/**
 * This exception is thrown to signal XML Pull Parser related faults.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
class XmlPullParserException (private val s: String) extends Exception(s) {

  protected var detail: Throwable = _
  protected var row: Int = -1
  protected var column: Int = -1

  def this() = {
    this(null)
  }

  def this(msg: String, parser: XmlPullParser, chain: Throwable) = {
    this (
      (if (msg == null) "" else msg + " ") +
      (if (parser == null) "" else "(position:" + parser.getPositionDescription() + ") ") +
      (if (chain == null) "" else "caused by: " + chain)
    )
    if (parser != null) {
      this.row = parser.getLineNumber()
      this.column = parser.getColumnNumber()
    }
    this.detail = chain
  }

  def getDetail(): Throwable = detail
  //def setDetail(cause: Throwable): Unit = { this.detail = cause }

  def getLineNumber(): Int = row

  def getColumnNumber(): Int = column

  //NOTE: code that prints this and detail is difficult in J2ME
  override def printStackTrace(): Unit = {
    if (detail == null) super.printStackTrace()
    else {
      System.err synchronized System.err.println(super.getMessage + "; nested exception is:")
      detail.printStackTrace()
    }
  }
}