package checks;

public class ConstructorsShouldNotAccessUninitializedValuesCheckSample {

  record SimpleUser(String name) {
    SimpleUser {
      if (name().isBlank()) { // Noncompliant {{Remove this use of the uninitialized value "name()".}}
//        ^^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record ThisAccess(String name) {
    ThisAccess {
      if (this.name().isBlank()) { // Noncompliant {{Remove this use of the uninitialized value "name()".}}
//             ^^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record QualifiedThisAccess(String name) {
    QualifiedThisAccess {
      // Noncompliant@+1 {{Remove this use of the uninitialized value "name()".}}
      if (QualifiedThisAccess.this.name().isBlank()) {
//                                 ^^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record MultipleComponents(String first, String last, int age) {
    MultipleComponents {
      if (first().isEmpty()) { // Noncompliant {{Remove this use of the uninitialized value "first()".}}
//        ^^^^^
        throw new IllegalArgumentException();
      }
      if (this.last().isEmpty()) { // Noncompliant {{Remove this use of the uninitialized value "last()".}}
//             ^^^^
        throw new IllegalArgumentException();
      }
      if (age() < 0) { // Noncompliant {{Remove this use of the uninitialized value "age()".}}
//        ^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record CompliantDirectAccess(String name, int age) {
    CompliantDirectAccess {
      if (name.isBlank() || age < 0) { // Compliant
        throw new IllegalArgumentException();
      }
    }
  }

  record CanonicalConstructor(String name) {
    CanonicalConstructor(String name) {
      if (name().isBlank()) { // Compliant, not a compact constructor
        throw new IllegalArgumentException();
      }
      this.name = name;
    }
  }

  record CustomConstructor(String name) {
    CustomConstructor(String name, boolean flag) {
      this(name);
      if (name().isBlank()) { // Compliant, canonical constructor already ran
        throw new IllegalArgumentException();
      }
    }
  }

  record ExplicitAccessor(String name) {
    ExplicitAccessor {
      if (name().isBlank()) { // Compliant, accessor is explicitly declared
        throw new IllegalArgumentException();
      }
    }

    @Override
    public String name() {
      return name;
    }
  }

  record OtherMethodCall(String name) {
    OtherMethodCall {
      helper(); // Compliant
      validate(name); // Compliant
    }

    void helper() {
    }

    static void validate(String s) {
    }
  }

  record AccessOnOtherInstance(String name) {
    AccessOnOtherInstance {
      AccessOnOtherInstance other = new AccessOnOtherInstance("other");
      if (other.name().isBlank()) { // Compliant
        throw new IllegalArgumentException();
      }
    }
  }

  record OuterRecord(String outerName) {
    OuterRecord {
      if (outerName().isBlank()) { // Noncompliant {{Remove this use of the uninitialized value "outerName()".}}
//        ^^^^^^^^^
        throw new IllegalArgumentException();
      }
    }

    record InnerRecord(String innerName) {
      InnerRecord {
        if (innerName().isBlank()) { // Noncompliant {{Remove this use of the uninitialized value "innerName()".}}
//          ^^^^^^^^^
          throw new IllegalArgumentException();
        }
      }
    }
  }

  record WithInnerClass(String name) {
    WithInnerClass {
      class LocalClass {
        void localMethod() {
          name(); // Compliant
        }
      }
    }
  }

  record WithLambda(String name) {
    WithLambda {
      Runnable r = () -> name(); // Compliant
    }
  }

  record WithCastAndParens(String name) {
    WithCastAndParens {
      // Noncompliant@+1 {{Remove this use of the uninitialized value "name()".}}
      if (((WithCastAndParens) (this)).name().isBlank()) {
//                                     ^^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record OtherMethodWithParams(String name) {
    OtherMethodWithParams {
      compute(1); // Compliant
    }

    void compute(int x) {
    }
  }

  record InNewClassArgument(String name) {
    InNewClassArgument {
      // Noncompliant@+1 {{Remove this use of the uninitialized value "name()".}}
      throw new IllegalArgumentException("bad name: " + name());
//                                                      ^^^^
    }
  }

  record NoComponents() {
    NoComponents {
      // Compliant
    }
  }
}
