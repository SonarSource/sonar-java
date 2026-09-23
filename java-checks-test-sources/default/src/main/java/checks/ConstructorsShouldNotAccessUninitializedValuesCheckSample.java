package checks;

import java.util.List;
import java.util.Locale;

public class ConstructorsShouldNotAccessUninitializedValuesCheckSample {

  record SimpleUser(String name) {
    SimpleUser {
      if (name().isBlank()) { // Noncompliant {{Replace this call to "name()" with the "name" parameter; the field is not assigned yet.}}
//        ^^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record ThisAccess(String name) {
    ThisAccess {
      if (this.name().isBlank()) { // Noncompliant {{Replace this call to "name()" with the "name" parameter; the field is not assigned yet.}}
//             ^^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record QualifiedThisAccess(String name) {
    QualifiedThisAccess {
      if (QualifiedThisAccess.this.name().isBlank()) { // Noncompliant {{Replace this call to "name()" with the "name" parameter; the field is not assigned yet.}}
//                                 ^^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record MultipleComponents(String first, String last, int age) {
    MultipleComponents {
      if (first().isEmpty()) { // Noncompliant {{Replace this call to "first()" with the "first" parameter; the field is not assigned yet.}}
//        ^^^^^
        throw new IllegalArgumentException();
      }
      if (this.last().isEmpty()) { // Noncompliant {{Replace this call to "last()" with the "last" parameter; the field is not assigned yet.}}
//             ^^^^
        throw new IllegalArgumentException();
      }
      if (age() < 0) { // Noncompliant {{Replace this call to "age()" with the "age" parameter; the field is not assigned yet.}}
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

  record ExplicitAccessor(List<String> items) {
    ExplicitAccessor {
      if (items().isEmpty()) { // Noncompliant {{Replace this call to "items()" with the "items" parameter; the field is not assigned yet.}}
//        ^^^^^
        throw new IllegalArgumentException();
      }
    }

    @Override
    public List<String> items() {
      return List.copyOf(items);
    }
  }

  record CanonicalConstructor(String name) {
    CanonicalConstructor(String name) {
      if (name().isBlank()) { // Noncompliant {{Replace this call to "name()" with the "name" parameter; the field is not assigned yet.}}
//        ^^^^
        throw new IllegalArgumentException();
      }
      this.name = name;
      if (name().isBlank()) { // Compliant, the field is assigned
        throw new IllegalArgumentException();
      }
    }
  }

  record CanonicalMultipleComponents(String name, int age) {
    CanonicalMultipleComponents(String name, int age) {
      this.name = name.toLowerCase(Locale.ROOT);
      if (name().isBlank() || age() < 0) { // Noncompliant {{Replace this call to "age()" with the "age" parameter; the field is not assigned yet.}}
//                            ^^^
        throw new IllegalArgumentException();
      }
      this.age = age;
      if (age() > 150) { // Compliant
        throw new IllegalArgumentException();
      }
    }
  }

  record CanonicalAccessorInAssignment(String name) {
    CanonicalAccessorInAssignment(String name) {
      this.name = name().trim(); // Noncompliant {{Replace this call to "name()" with the "name" parameter; the field is not assigned yet.}}
//                ^^^^
    }
  }

  record CanonicalParenthesizedAssignment(String name) {
    CanonicalParenthesizedAssignment(String name) {
      (this).name = name;
      if (name().isBlank()) { // Compliant
        throw new IllegalArgumentException();
      }
    }
  }

  record CanonicalBranches(String name) {
    CanonicalBranches(String name) {
      if (name == null) {
        this.name = "";
      } else {
        this.name = name.trim();
      }
      if (name().isEmpty()) { // Compliant
        throw new IllegalArgumentException();
      }
    }
  }

  static class Holder {
    String value;
  }

  record CanonicalOtherAssignments(String name) {
    CanonicalOtherAssignments(String name) {
      String trimmed;
      trimmed = name.trim();
      Holder holder = new Holder();
      holder.value = trimmed;
      if (name().isEmpty()) { // Noncompliant {{Replace this call to "name()" with the "name" parameter; the field is not assigned yet.}}
//        ^^^^
        throw new IllegalArgumentException();
      }
      this.name = trimmed;
    }
  }

  record DelegatingConstructor(String name) {
    DelegatingConstructor(String name, boolean flag) {
      this(name);
      if (name().isBlank()) { // Compliant, the canonical constructor already ran
        throw new IllegalArgumentException();
      }
    }
  }

  record OtherMethodCall(String name) {
    OtherMethodCall {
      helper(); // Compliant
      validate(name); // Compliant
      name("suffix"); // Compliant, not the accessor
    }

    void helper() {
    }

    String name(String suffix) {
      return name + suffix;
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
      if (outerName().isBlank()) { // Noncompliant {{Replace this call to "outerName()" with the "outerName" parameter; the field is not assigned yet.}}
//        ^^^^^^^^^
        throw new IllegalArgumentException();
      }
    }

    record InnerRecord(String innerName) {
      InnerRecord {
        if (innerName().isBlank()) { // Noncompliant {{Replace this call to "innerName()" with the "innerName" parameter; the field is not assigned yet.}}
//          ^^^^^^^^^
          throw new IllegalArgumentException();
        }
      }
    }
  }

  record DeferredCalls(String name) {
    DeferredCalls {
      class LocalClass {
        void localMethod() {
          name(); // Compliant
        }
      }
      Runnable lambda = () -> name(); // Compliant
      Object anonymous = new Object() {
        String value = name(); // Compliant
      };
    }
  }

  record WithCastAndParens(String name) {
    WithCastAndParens {
      if (((WithCastAndParens) (this)).name().isBlank()) { // Noncompliant {{Replace this call to "name()" with the "name" parameter; the field is not assigned yet.}}
//                                     ^^^^
        throw new IllegalArgumentException();
      }
    }
  }

  record InNewClassArgument(String name) {
    InNewClassArgument {
      throw new IllegalArgumentException("bad name: " + name()); // Noncompliant {{Replace this call to "name()" with the "name" parameter; the field is not assigned yet.}}
//                                                      ^^^^
    }
  }

  record NoComponents() {
    NoComponents {
      // Compliant
    }
  }
}
