package checks.serialization;

import java.io.Serial;
import java.io.Serializable;

class SerialVersionUidCheckSampleA implements Cloneable {}
class SerialVersionUidCheckSampleB implements Serializable {
  private static final long serialVersionUID = 1L;
}
class SerialVersionUidCheckSampleC implements Serializable {} // Noncompliant [[sc=7;ec=35]] {{Add a "static final long serialVersionUID" field to this class.}}
class SerialVersionUidCheckSampleD extends SerialVersionUidCheckSampleC {} // Noncompliant {{Add a "static final long serialVersionUID" field to this class.}}
class SerialVersionUidCheckSampleE implements Serializable {
  private final long serialVersionUID = 1L; // Noncompliant {{Make this "serialVersionUID" field "static".}}
}
class SerialVersionUidCheckSampleF implements Serializable {
  private static long serialVersionUID = 1L; // Noncompliant [[sc=23;ec=39]] {{Make this "serialVersionUID" field "final".}}
}
class SerialVersionUidCheckSampleG implements Serializable {
  private static int serialVersionUID = 1; // Noncompliant {{Make this "serialVersionUID" field "final long".}}
}
class SerialVersionUidCheckSampleH implements Serializable {
  void serialVersionUID() {}
  private static final long serialVersionUID = 1L;
}
abstract class SerialVersionUidCheckSampleI implements Serializable {}
class MyThrowable extends Throwable {}
class MyJPanel extends javax.swing.JPanel {}
class MyAwtButton extends java.awt.Button {
  class InnerClass implements Serializable {}
}

class SerialVersionUidCheckSampleOuter {
  class SerialVersionUidCheckSampleInner implements Serializable {} // Noncompliant
}

@SuppressWarnings("serial")
class SerialVersionUidCheckSampleJ implements Serializable {} // Noncompliant, this issue will be filtered by the supress warning filter

enum SerialVersionUidCheckSampleMyEnum {
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
