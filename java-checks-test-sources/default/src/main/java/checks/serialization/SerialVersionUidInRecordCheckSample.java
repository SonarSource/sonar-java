package checks.serialization;

import java.io.Serial;
import java.io.Serializable;

public class SerialVersionUidInRecordCheckSample {
  record Person(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = 0L; // Noncompliant {{Remove this redundant "serialVersionUID" field}}
  }

  record FieldWithValueFromConstant(String name, int age) implements Serializable {
    static final long DEFAULT_VALUE = 0L;
    @Serial private static final long serialVersionUID = DEFAULT_VALUE; // Noncompliant {{Remove this redundant "serialVersionUID" field}}
  }

  record FieldWithValueFromIntegerLiteral(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = 0; // Noncompliant {{Remove this redundant "serialVersionUID" field}}
  }

  record FieldWithValueFromIntegerConstant(String name, int age) implements Serializable {
    private static final int DEFAULT_VALUE = 0;
    @Serial private static final long serialVersionUID = DEFAULT_VALUE; // Noncompliant {{Remove this redundant "serialVersionUID" field}}
  }

  record FieldWithValueFromStaticVariable(String name, int age) implements Serializable {
    static long DEFAULT_VALUE = 0L;
    @Serial private static final long serialVersionUID = DEFAULT_VALUE; // Compliant as the variable is not final its value could change at runtime
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

  record FieldSetToAcceptableValueThroughConstant(String name, int age) implements Serializable {
    static final long DEFAULT_VALUE = 42L;
    @Serial private static final long serialVersionUID = DEFAULT_VALUE; // Compliant
  }

  record FieldSetToAcceptableValueThroughIntegerConstant(String name, int age) implements Serializable {
    static final int DEFAULT_VALUE = 42;
    @Serial private static final long serialVersionUID = DEFAULT_VALUE; // Compliant
  }

  record NonFinal(String name, int age) implements Serializable {
    @Serial private static long serialVersionUID = 0L; // Compliant
  }

  record UnexpectedFieldName(String name, int age) implements Serializable {
    @Serial private static final long somethingElseUID = 0L; // Compliant
  }

  record UnexpectedFieldType(String name, int age) implements Serializable {
    @Serial private static final int serialVersionUID = 0; // Compliant
  }

  record FieldWithValueFromStaticMethod(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = getConstantValue(); // Compliant FN as this is derived from a method call

    private static long getConstantValue() {
      return 0L;
    }
  }
}
