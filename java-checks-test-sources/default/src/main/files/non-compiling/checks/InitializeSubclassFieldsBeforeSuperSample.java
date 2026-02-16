package checks;

import java.io.IO;

public class InitializeSubclassFieldsBeforeSuperSample {

  // =================== Parent classes ===================

  abstract static class ParentAbstract {
    protected ParentAbstract() {
      IO.println("name is " + getName());
    }

    abstract String getName();
  }

  static class ParentSimple {
    protected ParentSimple() {
      IO.println("Hello from parent");
    }
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

  abstract static class ParentAbstractInit {
    protected ParentAbstractInit() {
      init();
    }

    abstract void init();
  }

  static class ParentDirectFieldAccess {
    protected String usedField;
    protected String unusedField;

    protected ParentDirectFieldAccess() {
      IO.println("Value: " + usedField);
    }
  }

  // =================== Noncompliant cases ===================

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

  // Parent directly accesses inherited field in its constructor
  public static class NonCompliantInheritedField extends ParentDirectFieldAccess {
    NonCompliantInheritedField(String value) {
      super();
      this.usedField = value; // Noncompliant
    }
  }

  // Multiple fields, all used in the child's getName() override
  public static class NonCompliantMultipleFieldsAbstract extends ParentAbstract {
    private String first;
    private String last;

    NonCompliantMultipleFieldsAbstract(String first, String last) {
      super();
      this.first = first; // Noncompliant
      this.last = last; // Noncompliant
    }

    @Override
    String getName() {
      return first + " " + last;
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

  // Abstract method override uses the field
  public static class NonCompliantAbstractOverrideUsesField extends ParentAbstractInit {
    private String value;

    NonCompliantAbstractOverrideUsesField(String value) {
      super();
      this.value = value; // Noncompliant
    }

    @Override
    void init() {
      IO.println(value);
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

  // =================== Compliant cases ===================

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

  // Super constructor doesn't use child's fields or overridable methods
  public static class CompliantFieldNotUsed extends ParentSimple {
    private String data;

    CompliantFieldNotUsed(String data) {
      super();
      this.data = data; // Compliant - ParentSimple doesn't use this field
    }
  }

  // No explicit super() call (implicit super)
  public static class CompliantImplicitSuper extends ParentSimple {
    private String value;

    CompliantImplicitSuper(String value) {
      this.value = value; // Compliant - no explicit super()
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
      super();
      String localVar = "test"; // Compliant - not a field assignment
    }

    @Override
    String getName() {
      return name;
    }
  }

  // Abstract method override does NOT use the field
  public static class CompliantAbstractOverrideNotUsingField extends ParentAbstractInit {
    private String unused;

    CompliantAbstractOverrideNotUsingField(String unused) {
      super();
      this.unused = unused; // Compliant - init() override doesn't use this field
    }

    @Override
    void init() {
      IO.println("init without field");
    }
  }

  // Only the unused inherited field is assigned - not the one read in parent constructor
  public static class CompliantUnusedInheritedField extends ParentDirectFieldAccess {
    CompliantUnusedInheritedField(String value) {
      super();
      this.unusedField = value; // Compliant - only usedField is read in parent constructor
    }
  }

  static class TrickyOverridesChain {
    abstract class A {
      A() {
        hello();
      }

      abstract void hello();

    }

    abstract class B extends A {
      protected final String name;

      B() {
        super();
        this.name = "name"; // Noncompliant
      }

      abstract void hello();
    }

    abstract class C extends B {
      C() {
        super();
      }

      @Override
      void hello() {
        IO.println(name);
      }
    }
  }


  static class NonAbstractOverrides {
    class A {
      A() {
        hello();
      }

      void hello() {
        IO.println("hello");
      }

    }

    class B extends A {
      protected final String name;

      B() {
        super();
        this.name = "name"; // Noncompliant
      }

      @Override
      void hello() {
        IO.println(name);
      }
    }
  }
}
