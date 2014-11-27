import java.util.ArrayList;

class A {
  void myMethod(String str) {
    Object x = new StringBuffer();
    new StringBuffer(); // Noncompliant
    new ArrayList<String>(); // Noncompliant
    new java.util.LinkedList(); // Noncompliant
    new java.io.File() {}; // Noncompliant
    new StringBuffer().append(".");
    str.contains(new StringBuffer());
  }
}
