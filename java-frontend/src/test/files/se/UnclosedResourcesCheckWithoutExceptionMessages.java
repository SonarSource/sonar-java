package org.foo;

import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

abstract class A {
  void foo(List<A> elements) {
    for (A element : elements) {
      try {
        String packageName = substring(element.name(), ".");

        Writer writer = new FileWriter(""); // Noncompliant

        put("packageName", packageName);

        writer.close();
      } catch (Exception e) {
      }
    }
  }

  abstract String substring(String str, String separator);
  abstract String name();
  abstract void put(String s, Object o);
}
