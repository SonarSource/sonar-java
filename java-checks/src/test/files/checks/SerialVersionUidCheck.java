import java.io.Serializable;

class A implements Cloneable {}
class B implements Serializable {
  private static final long serialVersionUID = 1L;
}
class C implements Serializable {} // Noncompliant
class D extends C {} // Noncompliant
class E implements Serializable {
  private final long serialVersionUID = 1L; // Noncompliant
}
class F implements Serializable {
  private static long serialVersionUID = 1L; // Noncompliant
}
class G implements Serializable {
  private static int serialVersionUID = 1; // Noncompliant
}
class H implements Serializable {
  void serialVersionUID() {}
  private static final long serialVersionUID = 1L;
}
abstract class I implements Serializable {}
class MyThrowable extends Throwable {}
class MyJPanel extends javax.swing.JPanel {}
class MyAwtButton extends java.awt.Button {
  class InnerClass implements Serializable {}
}

class Outer {
  class Inner implements Serializable {} // Noncompliant
}

@SuppressWarnings("serial")
class J implements Serializable {}

@SuppressWarnings("deprecation")
class K implements Serializable {} // Noncompliant

@SuppressWarnings(UNKNOWN)
class L implements Serializable {} // Noncompliant
