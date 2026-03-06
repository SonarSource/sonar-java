package checks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import java.util.Random;

public class RedundantRecordMethodsCheckSample {

  record RedundantConstructorAndGetters(String name, int age) {

    static Object variable = null;
    static Object someOtherVariable = null;

    RedundantConstructorAndGetters(String name, int age) { // Noncompliant {{Remove this redundant constructor which is the same as a default one.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      this.name = name;
      this.age = age;
    }

    public String name() { // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//                ^^^^
      return this.name;
    }

    public int age() { // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//             ^^^
      return age;
    }
  }

  record ConstructorAssignsWithRedundantCast(String name, int age) {
    ConstructorAssignsWithRedundantCast(String name, int age) { // Compliant FN as the unnecessary cast should be flagged by other rules
      this.name = (String) name;
      this.age = age;
    }

    public String name() { // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//                ^^^^
      return this.name;
    }

    public int age() { // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//             ^^^
      return age;
    }
  }

  record ConstructorWithSideEffects(String name, int age) {
    ConstructorWithSideEffects(String name, int age) { // Noncompliant {{Consider using a compact constructor here.}}
      sideEffect();
      this.name = name;
      this.age = age;
    }

    public void sideEffect() {
      System.out.println("Here's a side effect!");
    }
  }

  record ThrowingConstructor(String name, int age) {
    ThrowingConstructor(String name, int age) { // Noncompliant {{Consider using a compact constructor here.}}
      if (age < 0) {
        throw new IllegalArgumentException("Negative age");
      }
      this.name = name;
      this.age = age;
    }
  }

  record ParameterMismatch(String name, String address) {
    ParameterMismatch(String name, String address) { // Compliant
      this.name = address;
      this.address = name;
    }
  }

  record CompliantConstructorIgnoringParameter(String name, int age) {
    CompliantConstructorIgnoringParameter(String name, int age) { // Compliant
      this.name = name;
      this.age = 42;
    }
  }

  record CompliantConstructorNotAssigningToComponent(String name, int age) {
    CompliantConstructorNotAssigningToComponent(String name, int age) { // Compliant
      this.name = "A";
      this.age = age;
    }

    CompliantConstructorNotAssigningToComponent(int age) {
      this("ignored", 42);
    }
  }

  record ConstructorAssignsStaticValue(String name, int age) {
    static final String THE_ONLY_ACCEPTABLE_NAME = "A";

    ConstructorAssignsStaticValue(String name, int age) { // Compliant
      this.name = THE_ONLY_ACCEPTABLE_NAME;
      this.age = 42;
    }
  }

  record EmptyConstructorAndRedundantGetter(String name, int age) {
    EmptyConstructorAndRedundantGetter { // Noncompliant {{Remove this useless empty compact constructor.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    public String name() { // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//                ^^^^
      return name;
    }
  }

  record Compliant(String name, int age) { // Compliant
  }

  record CompliantConstructorWithAddedValue(String name, int age) {
    CompliantConstructorWithAddedValue(String name, int age) { // Compliant
      this.name = name.toLowerCase(Locale.ROOT);
      this.age = age;
    }
  }

  record CompliantConstructorWithSideEffect(String name, int age) {
    CompliantConstructorWithSideEffect(String name, int age) { // Compliant: the constructor includes side effects after assignments
      if (age < 0) {
        throw new IllegalArgumentException("Negative age");
      }
      this.name = name.toLowerCase(Locale.ROOT);
      this.age = age;
      System.out.println("Hello");
    }
  }

  record CompliantConstructorComplementAndTransformativeGetter(String name, int age) {
    CompliantConstructorComplementAndTransformativeGetter { // Compliant
      if (age < 0) {
        throw new IllegalArgumentException("Negative age");
      }
    }

    public String name() { // Compliant
      return name.toUpperCase(Locale.ROOT);
    }
  }

  record MisleadingGetters(String name, int age) {
    static final String MESSAGE = "Hello";

    public String name() {// Compliant
      return MESSAGE;
    }

    public int age() {// Compliant
      return 42;
    }
  }

  record PoorlyNamedGetter(String name, int age) {
    public String something() { // Compliant
      return name;
    }
  }

  record GetterWithBranches(String name, int age) {
    public String name() { // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//                ^^^^
      if ((new Random()).nextBoolean()) {
        return this.name;
      } else {
        return this.name;
      }
    }
  }

  record RecordWithConstructorAnnotation(String value) {
    @JsonCreator
    RecordWithConstructorAnnotation(@JsonProperty("myName") String value) { // Compliant: annotated
      this.value = value;
    }
  }

  record RecordWithParamAnnotation(String value) {
    // The annotation can be applied to the component and the constructor can be removed.
    RecordWithParamAnnotation(@JsonProperty("myName") String value) { // Noncompliant
      this.value = value;
    }
  }

  @interface MyAnnotation {}

  record RecordWithCustomAnnotation(String value) {
    @MyAnnotation
    RecordWithCustomAnnotation(String value) { // Compliant: annotated
      this.value = value;
    }
  }

  record AssignmentInConstructor(int arg1, int arg2) {
    public AssignmentInConstructor(int arg1, int arg2) { // Compliant
      this.arg1 = arg1;
      if (arg2 == 5) {
        arg2 = 4;
      }
      this.arg2 = arg2;
    }
  }
  record BranchInConstructor(int arg1, int arg2) {
    public BranchInConstructor(int arg1, int arg2) { // Noncompliant
      this.arg1 = arg1;
      if (arg2 == 5) {
        this.arg2 = arg2;
      } else {
        arg1 = 0;
        this.arg2 = arg2;
      }
    }
  }

  record BranchInConstructor2(int arg1, int arg2) {
    public BranchInConstructor2(int arg1, int arg2) { // Compliant
      this.arg1 = arg1;
      if (arg2 == 5) {
        this.arg2 = arg2;
      } else {
        this.arg2 = 0;
      }
    }
  }

  record AccessorWithLoop(String name) {
    public String name() { // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//                ^^^^
      while (true) {
        return name;
      }
    }
  }

  interface NamedThing {

    String name();

  }

  record AnnotatedAccessor(String name) implements NamedThing {
    @Override
    public String name() { // Compliant, no issues are raised for annotated accessors
      return name;
    }
  }

  record DifferentlyNamedAccessor(String name) {
    public String getName() { // Compliant, no issues are raised for accessors with different names
      return name;
    }
  }

  record AccessorSettingField(String name) {
    private static int AGE;

    public String name() {
      AGE = 0;
      return name;
    }
  }

  record AccessorWithParameter(String name) {
    public String name(int x) { // Compliant, no issues are raised for accessors with parameters
      return name;
    }
  }

  record AccessorWithLogic(String name) {
    public String name() { // Compliant, no issues are raised for accessors with logic
      System.out.println("Side effects !");
      return name;
    }
  }

  record AccessorWithLogic2(String name) {
    public String name() {
      int x = 42;
      System.out.println(x);
      return name;
    }
  }

  record AccessorWithAssertion(String name) {
    public String name() { // Compliant, no issues are raised for accessors with assertions
      assert name != null;
      return name;
    }
  }

  record AccessorWithThrow(int age) {
    public int age() { // Compliant, no issues are raised for accessors that throw exceptions
      if (age < 0) {
        throw new IllegalStateException("Negative age");
      }
      return age;
    }
  }

  record AccessorWithValidation(int age) {
    public int age() { // Compliant, no issues are raised for accessors that validate input
      validate(age);
      return age;
    }

    void validate(int age) {
      if (this.age < 0) {
        throw new IllegalStateException("Negative age");
      }
    }
  }
}
