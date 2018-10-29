import java.io.Serializable;

class A implements Cloneable {}
class B implements Serializable {
  private static final long serialVersionUID = 1L; // Noncompliant [[sc=29;ec=45]] {{Remove this "serialVersionUID".}}
}
class C implements Serializable {}
class D1 extends C {}
class D2 extends C {
  private static final long serialVersionUID = 1L; // Noncompliant
}
class E implements Serializable {
  void serialVersionUID() {}
  private static final long serialVersionUID = 1L; // Noncompliant {{Remove this "serialVersionUID".}}
}
class F {}
abstract class I implements Serializable {}
class J implements Serializable {
  private static long serialVersionUID = 1L;
}
class K implements Serializable {
  private final long serialVersionUID = 1L;
}
class L implements Serializable {
  private static final int serialVersionUID = 1;
}
class M implements Serializable {
  private static final long notSerialVersionUID = 1L;
}
class N implements Serializable {
  private static final int serialVersionUID() {
    return 1L;
  }
}
class MyThrowable extends Throwable {}
class MyJPanel extends javax.swing.JPanel {}
class MyAwtButton extends java.awt.Button {
  class InnerClass implements Serializable {}
}
class Outer {
  class Inner1 implements Serializable {
    private static final long serialVersionUID = 1L; // Noncompliant
  }
  class Inner2 implements Serializable {}
}
enum MyEnum {
  FOO {
    void fun() {}
  },
  BAR;
  private static final long serialVersionUID = 1L; // Noncompliant
  void fun(){}
}
enum OtherEnum {
  FOO, BAR;
}
