/*
 * Copyright (C) 2010 The Android Open Source Project
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

package libcore.util

/**
 * A pool of string instances.
 * Unlike the {@link String#intern() VM's interned strings},
 * this pool provides no guarantee of reference equality.
 * It is intended only to save allocations. This class is not thread safe.
 */
object StringPool {
  private def contentEquals(s: String, chars: Array[Char], start: Int, length: Int): Boolean = {
    if (s.length != length) return false
    for (i <- 0 until length) {
      if (chars(start + i) != s.charAt(i)) return false
    }
    true
  }
}

final class StringPool() {

  final private val pool = new Array[String](512)

  /**
   * Returns a string equal to {@code new String(array, start, length)}.
   */
  def get(array: Array[Char], start: Int, length: Int): String = { // Compute an arbitrary hash of the content
    var hashCode = 0
    for (i <- start until start + length) {
      hashCode = (hashCode * 31) + array(i)
    }
    // Pick a bucket using Doug Lea's supplemental secondaryHash function (from HashMap)
    hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12)
    hashCode ^= (hashCode >>> 7) ^ (hashCode >>> 4)
    val index = hashCode & (pool.length - 1)
    val pooled = pool(index)
    if (pooled != null && StringPool.contentEquals(pooled, array, start, length)) return pooled
    val result = new String(array, start, length)
    pool(index) = result
    result
  }
}