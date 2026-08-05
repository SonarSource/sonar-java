package checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class SimpleClass {
 
  void notWildcardImport() {
    com.google.common.collect.ImmutableList list; // FN
    com.google.common.collect.ImmutableList.Builder<Object> builder = // FN
      com.google.common.collect.ImmutableList.builder(); // FN
    System.out.println(com.google.common.collect.ImmutableList.class); // FN

    ImmutableList.builder();
    ImmutableList anotherList;
  }

  void wildcardImport() {
    java.util.List<String> myList = // Noncompliant
      new java.util.ArrayList<String>(); // Noncompliant

    List<String> myList2 =      // Compliant
      new ArrayList<String>();

    com.google.common.collect.ImmutableMap map; // FN

    ImmutableMap.builder();

    java.awt.image.ImageProducer x; // OK
    java.nio.charset.Charset.defaultCharset().name(); // Noncompliant
    Charset.defaultCharset().name(); // Compliant
  }
}
;
