package checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class SimpleClass {
 
  void notWildcardImport() {
    ImmutableList list;      // Noncompliant
    ImmutableList.Builder<Object> builder =  // Noncompliant [[startColumn=5;endColumn=44]]
      ImmutableList.builder(); // Noncompliant {{Replace this fully qualified name with "ImmutableList"}}
    System.out.println(ImmutableList.class); // Noncompliant [[startColumn=24;endColumn=63]]

    ImmutableList.builder();
    ImmutableList anotherList;
  }

  void wildcardImport() {
    List<String> myList =      // Noncompliant {{Replace this fully qualified name with "List"}}
      new ArrayList<String>(); // Noncompliant [[startColumn=11;endColumn=30]]

    ImmutableMap map; // Noncompliant

    java.awt.image.ImageProducer x; // OK
    Charset.defaultCharset().name(); // Noncompliant
  }
}
;
