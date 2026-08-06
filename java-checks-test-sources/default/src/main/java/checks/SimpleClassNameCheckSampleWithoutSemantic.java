package checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class SimpleClassWS {
 
  void notWildcardImport() {

    ImmutableList.builder();
    ImmutableList anotherList;
  }

  void wildcardImport() {
    java.util.List<String> myList = // Noncompliant
      new java.util.ArrayList<String>(); // Noncompliant

    List<String> myList2 =      // Compliant
      new ArrayList<String>();

    ImmutableMap.builder();

    java.awt.image.ImageProducer x; // OK
    java.nio.charset.Charset.defaultCharset().name(); // Noncompliant
    Charset.defaultCharset().name(); // Compliant
  }
}
;
