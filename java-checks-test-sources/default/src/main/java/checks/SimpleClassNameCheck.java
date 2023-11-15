package checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class SimpleClass {
 
  void notWildcardImport() {
    com.google.common.collect.ImmutableList list;      // Noncompliant
    com.google.common.collect.ImmutableList.Builder<Object> builder =  // Noncompliant [[startColumn=5;endColumn=44]]
      com.google.common.collect.ImmutableList.builder(); // Noncompliant {{Replace this fully qualified name with "ImmutableList"}}
    System.out.println(com.google.common.collect.ImmutableList.class); // Noncompliant [[startColumn=24;endColumn=63]]

    ImmutableList.builder();
    ImmutableList anotherList;
  }

  void wildcardImport() {
    java.util.List<String> myList =      // Noncompliant {{Replace this fully qualified name with "List"}}
      new java.util.ArrayList<String>(); // Noncompliant [[startColumn=11;endColumn=30]]

    List<String> myList2 =      // Compliant
      new ArrayList<String>();

    com.google.common.collect.ImmutableMap map; // Noncompliant

    ImmutableMap.builder();

    java.awt.image.ImageProducer x; // OK
    java.nio.charset.Charset.defaultCharset().name(); // Noncompliant
    Charset.defaultCharset().name(); // Compliant
  }
}
;
