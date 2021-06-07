## XML4S: Scala library enabling JAXP support (Java API for XML Processing), for Scala.js and Scala Native

The Scala language, has 3 main backends nowadays: 
* the JVM Scala Compiler 
* the Scala.js Compiler
* the Scala Native Compiler (based on LLVM)

While the Scala.js and Scala Native platforms provide support for a good subset of the Java APIs, XML support is not yet implemented.

XML4S is an attempt to fill this gap, and thus to provide XML related Java APIs (a.k.a. JAXP) to the JS and Native platforms.

*Warning: while we plan to target Scala.js, this has not been tested yet. We will focus on Scala Native first.*

### XML4S components and licensing

XML4S components were ported from two open source projects:
* Android (https://source.android.com/)
* Apache Geronimo (http://geronimo.apache.org/), for the Stax API (work in progress, not yet committed)

Here is a table describing the licenses applying to each individual component:

|     Component     | License(s)    | Copyrights | Original source code |
| ----------------- | ------------- |------------- |------------- |
| xml4s-common-api  | Apache2, W3C(r) Software License (org.w3c.dom) | Copyright (C) 2008 The Android Open Source Project | https://android.googlesource.com/platform/libcore/+/refs/heads/master/luni/src/main/java/javax/xml |
| xml4s-sax-api     | Apache2 (expat, javax.xml.parsers), Public (org.xml.sax) | Copyright (C) 2008 The Android Open Source Project, Copyright (c) 2004 World Wide Web Consortium (org.w3c.dom) | https://android.googlesource.com/platform/libcore/+/refs/heads/master/luni/src/main/java |
| xml4s-stax-api    | Apache2 | Copyright 2003-2011 The Apache Software Foundation | https://github.com/apache/geronimo-specs/tree/trunk/geronimo-stax-api_1.2_spec |
| xml4s-xmlpull-api | FREE (XMLPULL API), MIT License (kXML2)  | Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany| https://android.googlesource.com/platform/libcore/+/refs/heads/master/xml/src/main/java https://github.com/stefanhaustein/kxml2 |
| tests             | Apache2 | Copyright (C) 2008 The Android Open Source Project, Copyright 2003-2011 The Apache Software Foundation | https://android.googlesource.com/platform/libcore/+/refs/heads/master/luni/src/test/java/libcore/xml/ https://github.com/apache/geronimo/blob/trunk/testsuite/webservices-testsuite/stax-tests/stax-war/src/main/java/org/apache/geronimo/test/StaxTest.java |

### XML4S implementations

XML4S provide two implementation:
* a pure Scala fork of Kxml2 (https://github.com/stefanhaustein/kxml2 http://kxml.sourceforge.net/about.shtml)
    
    This implementation can be used in any Scala Backend.


* a Scala Native specific implementation, with bindings to the C Expat library (https://libexpat.github.io/).

    This work (WIP) is based on the Android Expat wrapping code (which is itself of fork from Apache Harmony):
  *  https://android.googlesource.com/platform/libcore/+/cff1616/luni/src/main/java/org/apache/harmony/xml/
  *  https://android.googlesource.com/platform/libcore/+/cff1616/luni/src/main/native/org_apache_harmony_xml_ExpatParser.cpp
    
