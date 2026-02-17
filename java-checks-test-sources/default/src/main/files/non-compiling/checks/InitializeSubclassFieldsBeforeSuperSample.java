package checks;

import java.io.IO;

public class InitializeSubclassFieldsBeforeSuperSample {

  abstract static class ParentAbstract {
    protected ParentAbstract() {
      IO.println("name is " + getName());
    }

    abstract String getName();
  }

  static class ParentConcreteHelper {
    protected ParentConcreteHelper() {
      doInit();
    }

    void doInit() {
      IO.println("default init");
    }
  }

  abstract static class ParentTransitive {
    protected ParentTransitive() {
      helper();
    }

    private void helper() {
      IO.println(getValue());
    }

    abstract String getValue();
  }

  static class ParentDirectFieldAccess {
    protected String usedField;
    protected String unusedField;

    protected ParentDirectFieldAccess() {
      IO.println("Value: " + usedField);
    }
  }

  // Field used via abstract method in super constructor
  public static class NonCompliant extends ParentAbstract {
    private final String name;

    NonCompliant(String name) {
      super();
      this.name = name; // Noncompliant
    }

    @Override
    String getName() {
      return name;
    }
  }

  // Assignment in nested if/else block after super
  public static class NonCompliantNestedBlock extends ParentAbstract {
    private final String name;

    NonCompliantNestedBlock(String name, boolean condition) {
      super();
      if (condition) {
        this.name = name; // Noncompliant
      } else {
        this.name = "default"; // Noncompliant
      }
    }

    @Override
    String getName() {
      return name;
    }
  }

  // Field used transitively: super() → helper() → abstract getValue()
  public static class NonCompliantTransitive extends ParentTransitive {
    private final String data;

    NonCompliantTransitive(String data) {
      super();
      this.data = data; // Noncompliant
    }

    @Override
    String getValue() {
      return data;
    }
  }

  // Mixed: only the inherited field actually used in parent constructor is flagged
  public static class NonCompliantMixedInheritedFields extends ParentDirectFieldAccess {
    NonCompliantMixedInheritedFields(String used, String unused) {
      super();
      this.usedField = used; // Noncompliant
      this.unusedField = unused; // Compliant - not read in parent constructor
    }
  }

  // Abstract method override: multiple fields, only one used in override
  public static class NonCompliantMixedAbstractOverride extends ParentAbstract {
    private String name;
    private int age;

    NonCompliantMixedAbstractOverride(String name, int age) {
      super();
      this.name = name; // Noncompliant
      this.age = age; // Compliant - getName() override doesn't use age
    }

    @Override
    String getName() {
      return name;
    }
  }

  // Field assigned before super()
  public static class Compliant extends ParentAbstract {
    private final String name;

    Compliant(String name) {
      this.name = name;
      super();
    }

    @Override
    String getName() {
      return name;
    }
  }

  // Super calls concrete method that doesn't use child's field
  public static class CompliantConcreteMethod extends ParentConcreteHelper {
    private String extra;

    CompliantConcreteMethod(String extra) {
      super();
      this.extra = extra; // Compliant - doInit() doesn't use this field
    }
  }

  // Local variable assignment after super (not a field)
  public static class CompliantLocalVar extends ParentAbstract {
    private final String name;

    CompliantLocalVar(String name) {
      this.name = name;
      String localVar1;
      super();
      localVar1 = "test"; // Compliant - not a field assignment
      String localVar2 = "test"; // Compliant - not a field assignment
    }

    @Override
    String getName() {
      return name;
    }
  }

  // Override resolved in grandchild, not the direct child that assigns the field
  static class GrandchildOverride {
    abstract class Base {
      Base() {
        describe();
      }

      abstract void describe();
    }

    abstract class Middle extends Base {
      protected final String name;

      Middle() {
        super();
        this.name = "name"; // Noncompliant
      }

      abstract void describe();
    }

    abstract class Leaf extends Middle {
      Leaf() {
        super();
      }

      @Override
      void describe() {
        IO.println(name);
      }
    }
  }


  // Parameter shadowing: method parameter has same name as field
  static class ParameterShadowing {
    abstract class Base {
      String someText;

      Base() {
        doWork();
      }

      abstract void doWork();
    }

    // Compliant: isValid's parameter 'someText' shadows the field, so bare 'someText' references are the parameter
    class CompliantParameterShadowsField extends Base {
      CompliantParameterShadowsField(String someText) {
        super();
        this.someText = someText; // Compliant
      }

      @Override
      void doWork() {
        isValid("parameter value");
      }

      private boolean isValid(String someText) {
        return !someText.isEmpty();
      }
    }

    // Field accessed via this.someText in isValid - should raise issue
    class NonCompliantFieldAccessedViaThis extends Base {
      NonCompliantFieldAccessedViaThis(String someText) {
        super();
        this.someText = someText; // Noncompliant
      }

      @Override
      void doWork() {
        isValid("parameter value");
      }

      private boolean isValid(String someText) {
        return !this.someText.isEmpty(); // accessing field via this.
      }
    }
  }

  // Transitive: super() → process() override → abstract formatOutput() → grandchild override uses field
  static class TransitiveCallChain {
    abstract class Base {
      Base() {
        process();
      }

      abstract void process();
    }

    abstract class Middle extends Base {
      protected final String name;

      Middle() {
        super();
        this.name = "name"; // Noncompliant
        process(); // just to have a statement after super that is not an assignment
      }

      @Override
      void process() {
        formatOutput();
      }

      abstract void formatOutput();
    }

    class Leaf extends Middle {
      @Override
      void formatOutput() {
        IO.println(name);
      }
    }
  }

  // Parent only writes to the field (never reads it) → child assignment is compliant
  class OnlyUsageIsReassignment {
    class Parent {
      String name;

      Parent() {
        this.name = "hello";
        name = "world";
      }
    }

    class Child extends Parent {
      Child() {
        super();
        this.name = "name"; // Compliant - only usage of name is assignment, not read in parent constructor
        name = "name"; // Compliant - only usage of name is assignment, not read in parent constructor
      }
    }
  }

  // Tests implicit this (bare field name), explicit this, and outer class field exclusion
  class ImplicitThis {
    Integer outerField;

    class Parent {
      String name;
      Integer age;

      Parent() {
        this.name = name.toLowerCase();
        name = this.name.toUpperCase();
        this.age = 2 * (age + 1);
        postInit();
      }

      void postInit() {
      }
    }

    class Child extends Parent {
      String message;

      Child() {
        super();
        this.name = "name"; // Noncompliant
        name = "name"; // Noncompliant
        this.age = 5; // Noncompliant
        this.message = "message"; // Noncompliant
        message = "message"; // Noncompliant
        outerField = 1; // Compliant - not field of Child
      }

      @Override
      void postInit() {
        IO.println(message);
        IO.println(outerField);
      }
    }
  }
}
