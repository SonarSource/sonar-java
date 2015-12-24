import java.util.ArrayList;

class A {
  void myMethod(String str) {
    Object x = new StringBuffer();
    new StringBuffer(); // Noncompliant [[sc=9;ec=21]] {{Either remove this useless object instantiation of class "StringBuffer" or use it}}
    new ArrayList<String>(); // Noncompliant
    new java.util.LinkedList(); // Noncompliant
    new java.io.File() {}; // Noncompliant
    new StringBuffer().append(".");
    str.contains(new StringBuffer());
  }
}
