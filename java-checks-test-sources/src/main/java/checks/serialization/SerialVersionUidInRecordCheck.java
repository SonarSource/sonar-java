package checks.serialization;

import java.io.Serial;
import java.io.Serializable;

public class SerialVersionUidInRecordCheck {
  record Person(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = 0L; // Noncompliant {{Remove this redundant "serialVersionUID" field}}
  }

  record Individual(String name, int age) implements Serializable {
    void print() {
      System.out.println("{name:" + name + ", age:" + age + "}");
    }
  } // Compliant

  record Human(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = 42L; // Compliant
  }

  record NoValue(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID; // Compliant
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
    @Serial private static long serialVersionUID = getConstantValue(); // Compliant as this is not set explicitly

    private static long getConstantValue() {
      return 0L;
    }
  }
}
