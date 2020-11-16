package foo.bar;
import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import java.awt.ActiveEvent;
import java.nio.charset.Charset;
import B;
import java.util.List1;
;
class A {
  foo.bar.A field; // Noncompliant
  ;
  void notWildcardImport() {
    var r = new java.util.ArrayList<String>(); // Noncompliant
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

    com.google.common.collect.ImmutableMap map; // Noncompliant

    java.awt.image.ImageProducer x; // OK
    java.nio.charset.Charset.defaultCharset().name(); // Noncompliant
  }
}
;
