/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.xml.parsers

object FilePathToURI {

  private val gHexChs = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
  private val escChs = Array(' ', '<', '>', '#', '%', '"', '{', '}', '|', '\\', '^', '~', '[', ']', '`')

  // which ASCII characters need to be escaped
  private val gNeedEscaping = new Array[Boolean](128)
  // the first hex character if a character needs to be escaped
  private val gAfterEscaping1 = new Array[Char](128)
  // the second hex character if a character needs to be escaped
  private val gAfterEscaping2 = new Array[Char](128)

  // initialize the above 3 arrays
  for (i <- 0 to 0x1f) {
    gNeedEscaping(i) = true
    gAfterEscaping1(i) = gHexChs(i >> 4)
    gAfterEscaping2(i) = gHexChs(i & 0xf)
  }
  gNeedEscaping(0x7f) = true
  gAfterEscaping1(0x7f) = '7'
  gAfterEscaping2(0x7f) = 'F'

  for (ch <- escChs) {
    gNeedEscaping(ch) = true
    gAfterEscaping1(ch) = gHexChs(ch >> 4)
    gAfterEscaping2(ch) = gHexChs(ch & 0xf)
  }

  // To escape a file path to a URI, by using %HH to represent
  // special ASCII characters: 0x00~0x1F, 0x7F, ' ', '<', '>', '#', '%'
  // and '"' and non-ASCII characters (whose value >= 128).
  def filepath2URI(path: String): String = {
    // return null if path is null.
    if (path == null) return null

    val separator = java.io.File.separatorChar
    val unixPath = path.replace(separator, '/')

    var len = unixPath.length
    var ch = 0

    val buffer = new StringBuilder(len * 3)
    buffer.append("file://")
    // change C:/blah to /C:/blah
    if (len >= 2 && unixPath.charAt(1) == ':') {
      ch = Character.toUpperCase(unixPath.charAt(0))
      if (ch >= 'A' && ch <= 'Z') buffer.append('/')
    }

    // for each character in the path
    var i = 0
    while (i < len) {
      ch = unixPath.charAt(i)
      // if it's not an ASCII character, break here, and use UTF-8 encoding
      if (ch >= 128) i = len
      else {
        if (gNeedEscaping(ch)) {
          buffer.append('%')
          buffer.append(gAfterEscaping1(ch))
          buffer.append(gAfterEscaping2(ch))
          // record the fact that it's escaped
        }
        else buffer.append(ch.toChar)
        i += 1
      }
    }
    // we saw some non-ascii character
    if (i < len) { // get UTF-8 bytes for the remaining sub-string

      val bytes = try unixPath.substring(i).getBytes("UTF-8")
      catch {
        case e: java.io.UnsupportedEncodingException =>
          // should never happen
          return unixPath
      }

      var b = 0
      len = bytes.length
      // for each byte
      i = 0
      while (i < len) {
        b = bytes(i)
        // for non-ascii character: make it positive, then escape
        if (b < 0) {
          ch = b + 256
          buffer.append('%')
          buffer.append(gHexChs(ch >> 4))
          buffer.append(gHexChs(ch & 0xf))
        }
        else if (gNeedEscaping(b)) {
          buffer.append('%')
          buffer.append(gAfterEscaping1(b))
          buffer.append(gAfterEscaping2(b))
        }
        else buffer.append(b.toChar)
        i += 1
      }
    }

    buffer.toString
  }

}