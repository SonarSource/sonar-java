package checks.serialization;

import java.io.Serial;
import java.io.Serializable;

public class SerialVersionUidInRecordCheck {
  record Person(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = 0L; // Noncompliant {{Remove this redundant "serialVersionUID" field}}
  }

  record NotSerializable(String name, int age) {
    @Serial private static final long serialVersionUID = 0L; // Compliant as the Record is not serializable
  }

  record NoExplicitField(String name, int age) implements Serializable {
    void print() {
      System.out.println("{name:" + name + ", age:" + age + "}");
    }
  } // Compliant

  record FieldSetToAcceptableValue(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = 42L; // Compliant
  }

  record NonFinal(String name, int age) implements Serializable {
    @Serial private static long serialVersionUID = 0L; // Compliant
  }

  record UnexpectedFieldName(String name, int age) implements Serializable {
    @Serial private static long somethingElseUID = 0L; // Compliant
  }

  record UnexpectedFieldType(String name, int age) implements Serializable {
    @Serial private static int serialVersionUID = 0; // Compliant
  }

  record FieldWithValueFromStaticMethod(String name, int age) implements Serializable {
    @Serial private static long serialVersionUID = getConstantValue(); // Compliant as this is derived from a method call

    private static long getConstantValue() {
      return 0L;
    }
  }

  record FieldWithValueFromStaticVariable(String name, int age) implements Serializable {
    static long DEFAULT_VALUE = 0L;
    @Serial private static long serialVersionUID = DEFAULT_VALUE; // Compliant as this is derived from a static variable
  }
}
