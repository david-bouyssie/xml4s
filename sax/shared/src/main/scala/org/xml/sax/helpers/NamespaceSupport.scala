// NamespaceSupport.java - generic Namespace support for SAX.
// http://www.saxproject.org
// Written by David Megginson
// This class is in the Public Domain.  NO WARRANTY!
// $Id: NamespaceSupport.java,v 1.15 2004/04/26 17:34:35 dmegginson Exp $

package org.xml.sax.helpers

import java.util
import java.util.Collections
import java.util.EmptyStackException

/**
 * Encapsulate Namespace logic for use by applications using SAX,
 * or internally by SAX drivers.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>This class encapsulates the logic of Namespace processing: it
 * tracks the declarations currently in force for each context and
 * automatically processes qualified XML names into their Namespace
 * parts; it can also be used in reverse for generating XML qnames
 * from Namespaces.</p>
 *
 * <p>Namespace support objects are reusable, but the reset method
 * must be invoked between each session.</p>
 *
 * <p>Here is a simple session:</p>
 *
 * <pre>
 * String parts[] = new String[3];
 * NamespaceSupport support = new NamespaceSupport();
 *
 * support.pushContext();
 * support.declarePrefix("", "http://www.w3.org/1999/xhtml");
 * support.declarePrefix("dc", "http://www.purl.org/dc#");
 *
 * parts = support.processName("p", parts, false);
 * System.out.println("Namespace URI: " + parts[0]);
 * System.out.println("Local name: " + parts[1]);
 * System.out.println("Raw name: " + parts[2]);
 *
 * parts = support.processName("dc:title", parts, false);
 * System.out.println("Namespace URI: " + parts[0]);
 * System.out.println("Local name: " + parts[1]);
 * System.out.println("Raw name: " + parts[2]);
 *
 * support.popContext();
 * </pre>
 *
 * <p>Note that this class is optimized for the use case where most
 * elements do not contain Namespace declarations: if the same
 * prefix/URI mapping is repeated for each context (for example), this
 * class will be somewhat less efficient.</p>
 *
 * <p>Although SAX drivers (parsers) may choose to use this class to
 * implement namespace handling, they are not required to do so.
 * Applications must track namespace information themselves if they
 * want to use namespace information.
 *
 * @since SAX 2.0
 * @author David Megginson
 * @version 2.0.1 (sax2r2)
 */
object NamespaceSupport {

  /**
   * The XML Namespace URI as a constant.
   * The value is <code>http://www.w3.org/XML/1998/namespace</code>
   * as defined in the "Namespaces in XML" * recommendation.
   *
   * <p>This is the Namespace URI that is automatically mapped
   * to the "xml" prefix.</p>
   */
  val XMLNS: String = "http://www.w3.org/XML/1998/namespace"

  /**
   * The namespace declaration URI as a constant.
   * The value is <code>http://www.w3.org/xmlns/2000/</code>, as defined
   * in a backwards-incompatible erratum to the "Namespaces in XML"
   * recommendation.  Because that erratum postdated SAX2, SAX2 defaults
   * to the original recommendation, and does not normally use this URI.
   *
   *
   * <p>This is the Namespace URI that is optionally applied to
   * <em>xmlns</em> and <em>xmlns:*</em> attributes, which are used to
   * declare namespaces.  </p>
   *
   * @since SAX 2.1alpha
   * @see #setNamespaceDeclUris
   * @see #isNamespaceDeclUris
   */
  val NSDECL: String = "http://www.w3.org/xmlns/2000/"

  /**
   * An empty enumeration.
   */
  private val EMPTY_ENUMERATION: util.Enumeration[_ <: Object] = {
    Collections.enumeration(Collections.emptyList[Object]())
  }
}

/**
 * Create a new Namespace support object.
 */
class NamespaceSupport() {

  private var contexts: Array[Context] = _
  private var currentContext: Context = _
  private var contextPos: Int = 0
  private var namespaceDeclUris: Boolean = false

  reset()

  /**
   * Reset this Namespace support object for reuse.
   *
   * <p>It is necessary to invoke this method before reusing the
   * Namespace support object for a new session.  If namespace
   * declaration URIs are to be supported, that flag must also
   * be set to a non-default value.
   * </p>
   *
   * @see #setNamespaceDeclUris
   */
  def reset(): Unit = {
    contexts = new Array[Context](32)
    namespaceDeclUris = false
    contextPos = 0
    currentContext = new Context()
    contexts(contextPos) = currentContext
    currentContext.declarePrefix("xml", NamespaceSupport.XMLNS)
  }

  /**
   * Start a new Namespace context.
   * The new context will automatically inherit
   * the declarations of its parent context, but it will also keep
   * track of which declarations were made within this context.
   *
   * <p>Event callback code should start a new context once per element.
   * This means being ready to call this in either of two places.
   * For elements that don't include namespace declarations, the
   * <em>ContentHandler.startElement()</em> callback is the right place.
   * For elements with such a declaration, it'd done in the first
   * <em>ContentHandler.startPrefixMapping()</em> callback.
   * A boolean flag can be used to
   * track whether a context has been started yet.  When either of
   * those methods is called, it checks the flag to see if a new context
   * needs to be started.  If so, it starts the context and sets the
   * flag.  After <em>ContentHandler.startElement()</em>
   * does that, it always clears the flag.
   *
   * <p>Normally, SAX drivers would push a new context at the beginning
   * of each XML element.  Then they perform a first pass over the
   * attributes to process all namespace declarations, making
   * <em>ContentHandler.startPrefixMapping()</em> callbacks.
   * Then a second pass is made, to determine the namespace-qualified
   * names for all attributes and for the element name.
   * Finally all the information for the
   * <em>ContentHandler.startElement()</em> callback is available,
   * so it can then be made.
   *
   * <p>The Namespace support object always starts with a base context
   * already in force: in this context, only the "xml" prefix is
   * declared.</p>
   *
   * @see ContentHandler
   * @see #popContext
   */
  def pushContext(): Unit = {
    var max: Int = contexts.length
    contexts(contextPos).declsOK = false
    contextPos += 1

    // Extend the array if necessary
    if (contextPos >= max) {
      val newContexts = new Array[Context](max * 2)
      System.arraycopy(contexts, 0, newContexts, 0, max)
      max *= 2
      contexts = newContexts
    }

    // Allocate the context if necessary.
    currentContext = contexts(contextPos)

    if (currentContext == null) {
      currentContext = new Context
      contexts(contextPos) = currentContext
    }

    // Set the parent, if any.
    if (contextPos > 0)
      currentContext.setParent(contexts(contextPos - 1))
  }

  /**
   * Revert to the previous Namespace context.
   *
   * <p>Normally, you should pop the context at the end of each
   * XML element.  After popping the context, all Namespace prefix
   * mappings that were previously in force are restored.</p>
   *
   * <p>You must not attempt to declare additional Namespace
   * prefixes after popping a context, unless you push another
   * context first.</p>
   *
   * @see #pushContext
   */
  def popContext(): Unit = {
    contexts(contextPos).clear()
    contextPos -= 1
    if (contextPos < 0) throw new EmptyStackException()
    currentContext = contexts(contextPos)
  }

  ////////////////////////////////////////////////////////////////////
  // Operations within a context.

  /**
   * Declare a Namespace prefix.  All prefixes must be declared
   * before they are referenced.  For example, a SAX driver (parser)
   * would scan an element's attributes
   * in two passes:  first for namespace declarations,
   * then a second pass using {@link #processName processName()} to
   * interpret prefixes against (potentially redefined) prefixes.
   *
   * <p>This method declares a prefix in the current Namespace
   * context; the prefix will remain in force until this context
   * is popped, unless it is shadowed in a descendant context.</p>
   *
   * <p>To declare the default element Namespace, use the empty string as
   * the prefix.</p>
   *
   * <p>Note that you must <em>not</em> declare a prefix after
   * you've pushed and popped another Namespace context, or
   * treated the declarations phase as complete by processing
   * a prefixed name.</p>
   *
   * <p>Note that there is an asymmetry in this library: {@link
 * #getPrefix getPrefix} will not return the "" prefix,
   * even if you have declared a default element namespace.
   * To check for a default namespace,
   * you have to look it up explicitly using {@link #getURI getURI}.
   * This asymmetry exists to make it easier to look up prefixes
   * for attribute names, where the default prefix is not allowed.</p>
   *
   * @param prefix The prefix to declare, or the empty string to
   *               indicate the default element namespace.  This may never have
   *               the value "xml" or "xmlns".
   * @param uri    The Namespace URI to associate with the prefix.
   * @return true if the prefix was legal, false otherwise
   * @see #processName
   * @see #getURI
   * @see #getPrefix
   */
  def declarePrefix(prefix: String, uri: String): Boolean = {
    if (prefix == "xml" || prefix == "xmlns") return false
    else {
      currentContext.declarePrefix(prefix, uri)
      return true
    }
  }

  /**
   * Process a raw XML qualified name, after all declarations in the
   * current context have been handled by {@link #declarePrefix
   * declarePrefix()}.
   *
   * <p>This method processes a raw XML qualified name in the
   * current context by removing the prefix and looking it up among
   * the prefixes currently declared.  The return value will be the
   * array supplied by the caller, filled in as follows:</p>
   *
   * <dl>
   * <dt>parts[0]</dt>
   * <dd>The Namespace URI, or an empty string if none is
   * in use.</dd>
   * <dt>parts[1]</dt>
   * <dd>The local name (without prefix).</dd>
   * <dt>parts[2]</dt>
   * <dd>The original raw name.</dd>
   * </dl>
   *
   * <p>All of the strings in the array will be internalized.  If
   * the raw name has a prefix that has not been declared, then
   * the return value will be null.</p>
   *
   * <p>Note that attribute names are processed differently than
   * element names: an unprefixed element name will receive the
   * default Namespace (if any), while an unprefixed attribute name
   * will not.</p>
   *
   * @param qName The XML qualified name to be processed.
   * @param parts An array supplied by the caller, capable of
   * holding at least three members.
   * @param isAttribute A flag indicating whether this is an
   * attribute name (true) or an element name (false).
   * @return The supplied array holding three internalized strings
   * representing the Namespace URI (or empty string), the
   * local name, and the XML qualified name; or null if there
   * is an undeclared prefix.
   * @see #declarePrefix
   * @see java.lang.String#intern */
  def processName(qName: String, parts: Array[String], isAttribute: Boolean): Array[String] = {
    val myParts: Array[String] = currentContext.processName(qName, isAttribute)
    if (myParts == null) {
      return null
    }
    else {
      parts(0) = myParts(0)
      parts(1) = myParts(1)
      parts(2) = myParts(2)
      return parts
    }
  }

  /**
   * Look up a prefix and get the currently-mapped Namespace URI.
   *
   * <p>This method looks up the prefix in the current context.
   * Use the empty string ("") for the default Namespace.</p>
   *
   * @param prefix The prefix to look up.
   * @return The associated Namespace URI, or null if the prefix
   *         is undeclared in this context.
   * @see #getPrefix
   * @see #getPrefixes
   */
  def getURI(prefix: String): String = currentContext.getURI(prefix)

  /**
   * Return an enumeration of all prefixes whose declarations are
   * active in the current context.
   * This includes declarations from parent contexts that have
   * not been overridden.
   *
   * <p><strong>Note:</strong> if there is a default prefix, it will not be
   * returned in this enumeration; check for the default prefix
   * using the {@link #getURI getURI} with an argument of "".</p>
   *
   * @return An enumeration of prefixes (never empty).
   * @see #getDeclaredPrefixes
   * @see #getURI
   */
  def getPrefixes(): util.Enumeration[_ <: Object] = currentContext.getPrefixes()

  /**
   * Return one of the prefixes mapped to a Namespace URI.
   *
   * <p>If more than one prefix is currently mapped to the same
   * URI, this method will make an arbitrary selection; if you
   * want all of the prefixes, use the {@link #getPrefixes}
   * method instead.</p>
   *
   * <p><strong>Note:</strong> this will never return the empty (default) prefix;
   * to check for a default prefix, use the {@link #getURI getURI}
   * method with an argument of "".</p>
   *
   * @param uri the namespace URI
   * @return one of the prefixes currently mapped to the URI supplied,
   *         or null if none is mapped or if the URI is assigned to
   *         the default namespace
   * @see #getPrefixes(java.lang.String)
   * @see #getURI
   */
  def getPrefix(uri: String): String = currentContext.getPrefix(uri)

  /**
   * Return an enumeration of all prefixes for a given URI whose
   * declarations are active in the current context.
   * This includes declarations from parent contexts that have
   * not been overridden.
   *
   * <p>This method returns prefixes mapped to a specific Namespace
   * URI.  The xml: prefix will be included.  If you want only one
   * prefix that's mapped to the Namespace URI, and you don't care
   * which one you get, use the {@link #getPrefix getPrefix}
   * method instead.</p>
   *
   * <p><strong>Note:</strong> the empty (default) prefix is <em>never</em> included
   * in this enumeration; to check for the presence of a default
   * Namespace, use the {@link #getURI getURI} method with an
   * argument of "".</p>
   *
   * @param uri The Namespace URI.
   * @return An enumeration of prefixes (never empty).
   * @see #getPrefix
   * @see #getDeclaredPrefixes
   * @see #getURI
   */
  def getPrefixes(uri: String): util.Enumeration[_ <: Object] = {
    val prefixes: util.ArrayList[String] = new util.ArrayList[String]
    val allPrefixes = getPrefixes()
    while ( {allPrefixes.hasMoreElements}) {
      val prefix: String = allPrefixes.nextElement.asInstanceOf[String]
      if (uri == getURI(prefix)) {
        prefixes.add(prefix)
      }
    }
    return Collections.enumeration(prefixes)
  }


  /**
   * Return an enumeration of all prefixes declared in this context.
   *
   * <p>The empty (default) prefix will be included in this
   * enumeration; note that this behaviour differs from that of
   * {@link #getPrefix} and {@link #getPrefixes}.</p>
   *
   * @return An enumeration of all prefixes declared in this
   *         context.
   * @see #getPrefixes
   * @see #getURI
   */
  def getDeclaredPrefixes(): util.Enumeration[_ <: Object] = currentContext.getDeclaredPrefixes()

  /**
   * Controls whether namespace declaration attributes are placed
   * into the {@link #NSDECL NSDECL} namespace
   * by {@link #processName processName()}.  This may only be
   * changed before any contexts have been pushed.
   *
   * @param value the namespace declaration attribute state. A value of true
   *              enables this feature, a value of false disables it.
   * @since SAX 2.1alpha
   * @exception IllegalStateException when attempting to set this
   *            after any context has been pushed.
   */
  def setNamespaceDeclUris(value: Boolean): Unit = {
    if (contextPos != 0) throw new IllegalStateException()

    if (value == namespaceDeclUris)
      return ()

    namespaceDeclUris = value

    if (value) currentContext.declarePrefix("xmlns", NamespaceSupport.NSDECL)
    else {
      currentContext = new Context()
      contexts(contextPos) = currentContext
      currentContext.declarePrefix("xml", NamespaceSupport.XMLNS)
    }

    ()
  }

  /**
   * Returns true if namespace declaration attributes are placed into
   * a namespace.  This behavior is not the default.
   *
   * @return true if namespace declaration attributes are enabled, false
   *         otherwise.
   * @since SAX 2.1alpha
   */
  def isNamespaceDeclUris(): Boolean = namespaceDeclUris

  /**
   * Internal class for a single Namespace context.
   *
   * <p>This module caches and reuses Namespace contexts,
   * so the number allocated
   * will be equal to the element depth of the document, not to the total
   * number of elements (i.e. 5-10 rather than tens of thousands).
   * Also, data structures used to represent contexts are shared when
   * possible (child contexts without declarations) to further reduce
   * the amount of memory that's consumed.
   * </p>
   *
   * Create the root-level Namespace context.
   */
  final class Context() {

    var prefixTable: util.Hashtable[String, String] = _
    var uriTable: util.Hashtable[String, String] = _
    var elementNameTable: util.Hashtable[String, AnyRef] = _
    var attributeNameTable: util.Hashtable[String, AnyRef] = _
    var defaultNS: String = _
    var declsOK: Boolean = true
    private var declarations: util.ArrayList[String] = _
    private var declSeen: Boolean = false
    private var parent: Context = _

    copyTables()

    /**
     * (Re)set the parent of this Namespace context.
     * The context must either have been freshly constructed,
     * or must have been cleared.
     *
     * @param context The parent Namespace context object.
     */
    def setParent(parent: Context): Unit = {
      this.parent = parent
      declarations = null
      prefixTable = parent.prefixTable
      uriTable = parent.uriTable
      elementNameTable = parent.elementNameTable
      attributeNameTable = parent.attributeNameTable
      defaultNS = parent.defaultNS
      declSeen = false
      declsOK = true
    }

    /**
     * Makes associated state become collectible,
     * invalidating this context.
     * {@link #setParent} must be called before
     * this context may be used again.
     */
    def clear(): Unit = {
      parent = null
      prefixTable = null
      uriTable = null
      elementNameTable = null
      attributeNameTable = null
      defaultNS = null
    }

    /**
     * Declare a Namespace prefix for this context.
     *
     * @param prefix The prefix to declare.
     * @param uri    The associated Namespace URI.
     * @see NamespaceSupport#declarePrefix
     */
    def declarePrefix(prefix: String, uri: String): Unit = {
      // Lazy processing...
      if (!declsOK) {
        throw new IllegalStateException("can't declare any more prefixes in this context")
      }

      if (!declSeen) copyTables ()
      if (declarations == null) declarations = new util.ArrayList[String]

      val cachedPrefix = prefix.intern
      val cachedUri = uri.intern
      if ("" == prefix) {
        defaultNS = if ("" == uri) null else cachedUri
      }
      else {
        prefixTable.put(prefix, uri)
        uriTable.put(uri, prefix) // may wipe out another prefix
      }

      declarations.add(prefix)

      ()
    }

    /**
     * Process an XML qualified name in this context.
     *
     * @param qName       The XML qualified name.
     * @param isAttribute true if this is an attribute name.
     * @return An array of three strings containing the
     *         URI part (or empty string), the local part,
     *         and the raw name, all internalized, or null
     *         if there is an undeclared prefix.
     * @see NamespaceSupport#processName
     */
    def processName(qName: String, isAttribute: Boolean): Array[String] = {

      // detect errors in call sequence
      declsOK = false

      // Select the appropriate table.
      val table = if (isAttribute) attributeNameTable else elementNameTable

      // Start by looking in the cache, and
      // return immediately if the name
      // is already known in this content
      var name = table.get (qName).asInstanceOf[Array[String]]
      if (name != null) return name

      // We haven't seen this name in this
      // context before.  Maybe in the parent
      // context, but we can't assume prefix
      // bindings are the same.
      name = new Array[String](3)
      name (2) = qName.intern

      val index: Int = qName.indexOf (':')

      // No prefix.
      if (index == -1 ) {
        if (isAttribute) {
          if ((qName eq "xmlns") && namespaceDeclUris) name (0) = NamespaceSupport.NSDECL
          else name (0) = ""
        } else {
          if (defaultNS == null) name (0) = ""
          else name (0) = defaultNS
        }

        name(1) = name(2)
      }
      // Prefix
      else {
        val prefix: String = qName.substring (0, index)
        val local: String = qName.substring (index + 1)

        var uri: String = if ("" == prefix) defaultNS
        else prefixTable.get (prefix).asInstanceOf[String]

        if (uri == null || (!isAttribute && "xmlns" == prefix))
          return null

        name(0) = uri
        name(1) = local.intern
      }

      // Save in the cache for future use.
      // (Could be shared with parent context...)
      table.put(name(2), name)

      name
    }

    /**
     * Look up the URI associated with a prefix in this context.
     *
     * @param prefix The prefix to look up.
     * @return The associated Namespace URI, or null if none is
     *         declared.
     * @see NamespaceSupport#getURI
     */
    def getURI(prefix: String): String = {
      if ("" == prefix)  defaultNS
      else {
        if (prefixTable == null) null
        else prefixTable.get (prefix).asInstanceOf[String]
      }
    }

    /**
     * Look up one of the prefixes associated with a URI in this context.
     *
     * <p>Since many prefixes may be mapped to the same URI,
     * the return value may be unreliable.</p>
     *
     * @param uri The URI to look up.
     * @return The associated prefix, or null if none is declared.
     * @see NamespaceSupport#getPrefix
     */
    def getPrefix(uri: String): String = {
      if (uriTable == null) null else uriTable.get(uri).asInstanceOf[String]
    }

    /**
     * Return an enumeration of prefixes declared in this context.
     *
     * @return An enumeration of prefixes (possibly empty).
     * @see NamespaceSupport#getDeclaredPrefixes
     */
    def getDeclaredPrefixes(): util.Enumeration[_ <: Object] = {
      if (declarations == null) NamespaceSupport.EMPTY_ENUMERATION
      else Collections.enumeration (declarations)
    }

    /**
     * Return an enumeration of all prefixes currently in force.
     *
     * <p>The default prefix, if in force, is <em>not</em>
     * returned, and will have to be checked for separately.</p>
     *
     * @return An enumeration of prefixes (never empty).
     * @see NamespaceSupport#getPrefixes
     */
    def getPrefixes(): util.Enumeration[_ <: Object] = {
      if (prefixTable == null) NamespaceSupport.EMPTY_ENUMERATION else prefixTable.keys
    }

    /**
     * Copy on write for the internal tables in this context.
     *
     * <p>This class is optimized for the normal case where most
     * elements do not contain Namespace declarations.</p>
     */
    private def copyTables (): Unit = {
      prefixTable = if (prefixTable == null) new util.Hashtable[String, String]
      else prefixTable.clone.asInstanceOf[util.Hashtable[String, String]]

      uriTable = if (uriTable == null) new util.Hashtable[String, String]
      else uriTable.clone.asInstanceOf[util.Hashtable[String, String]]

      elementNameTable = new util.Hashtable[String, AnyRef]
      attributeNameTable = new util.Hashtable[String, AnyRef]
      declSeen = true
    }

  }
}