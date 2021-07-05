package checks;

import java.util.Arrays;

public class MissingOverridesInRecordWithArrayComponentCheck {
  record IrrelevantRecord(int value) { // Compliant

  }

  record Compliant(Object[] objects) { // Compliant
    @Override
    public boolean equals(Object o) {
      return false;
    }

    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public String toString() {
      return "Nothing to see here";
    }
  }

  record MissingEverything(Object[] objects) { // Noncompliant {{Override equals, hashCode and toString to consider array's content in the method}}
    static Object defaultValue = null;
    void doNothing(){}
  }

  record MissingHashCodeAndToString(Object[] objects) { // Noncompliant {{Override hashCode and toString to consider array's content in the method}}
    @Override
    public boolean equals(Object o) {
      return false;
    }
  }

  record MissingEqualsAndToString(Object[] objects) { // Noncompliant {{Override equals and toString to consider array's content in the method}}
    @Override
    public int hashCode() {
      return 42;
    }
  }

  record MissingEqualsAndHashCode(Object[] objects) { // Noncompliant {{Override equals and hashCode to consider array's content in the method}}
    @Override
    public String toString() {
      return "Nothing to see here";
    }
  }

  record MissingEquals(Object[] objects) { // Noncompliant {{Override equals to consider array's content in the method}}
    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public String toString() {
      return "Nothing to see here";
    }
  }

  record MissingHashCode(Object[] objects) { // Noncompliant {{Override hashCode to consider array's content in the method}}
    @Override
    public boolean equals(Object o) {
      return false;
    }

    @Override
    public String toString() {
      return "Nothing to see here";
    }
  }

  record MissingToString(Object[] objects) { // Noncompliant {{Override toString to consider array's content in the method}}
    @Override
    public boolean equals(Object o) {
      return false;
    }

    @Override
    public int hashCode() {
      return 42;
    }
  }
}
