package checks.serialization;

import java.io.Serial;
import java.io.Serializable;

class SerialVersionUidCheckA implements Cloneable {}
class SerialVersionUidCheckB implements Serializable {
  private static final long serialVersionUID = 1L;
}
class SerialVersionUidCheckC implements Serializable {} // Noncompliant [[sc=7;ec=29]] {{Add a "static final long serialVersionUID" field to this class.}}
class SerialVersionUidCheckD extends SerialVersionUidCheckC {} // Noncompliant {{Add a "static final long serialVersionUID" field to this class.}}
class SerialVersionUidCheckE implements Serializable {
  private final long serialVersionUID = 1L; // Noncompliant {{Make this "serialVersionUID" field "static".}}
}
class SerialVersionUidCheckF implements Serializable {
  private static long serialVersionUID = 1L; // Noncompliant [[sc=23;ec=39]] {{Make this "serialVersionUID" field "final".}}
}
class SerialVersionUidCheckG implements Serializable {
  private static int serialVersionUID = 1; // Noncompliant {{Make this "serialVersionUID" field "final long".}}
}
class SerialVersionUidCheckH implements Serializable {
  void serialVersionUID() {}
  private static final long serialVersionUID = 1L;
}
abstract class SerialVersionUidCheckI implements Serializable {}
class MyThrowable extends Throwable {}
class MyJPanel extends javax.swing.JPanel {}
class MyAwtButton extends java.awt.Button {
  class InnerClass implements Serializable {}
}

class SerialVersionUidCheckOuter {
  class SerialVersionUidCheckInner implements Serializable {} // Noncompliant
}

@SuppressWarnings("serial")
class SerialVersionUidCheckJ implements Serializable {} // Noncompliant, this issue will be filtered by the supress warning filter

enum SerialVersionUidCheckMyEnum {
  FOO {
    void fun() {}
  },
  BAR;
  void fun(){}
}

record RecordWithoutSerializationField(Object unused) implements Serializable { // Compliant
}

record RecordWithSerializationField(Object unused) implements Serializable { // Compliant
  static final long serialVersionUID = 2L;
}

record RecordWithSerializationFieldAndAnnotation(Object unused) implements Serializable { // Compliant
  @Serial
  static final long serialVersionUID = 2L;
}
