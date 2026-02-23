package checks;

import java.io.IO;

public class InitializeSubclassFieldsBeforeSuperSample {

  // =================== Parent classes ===================

  // Constructor calls abstract method → child overrides are invoked during super()
  abstract static class ParentCallsAbstract {
    protected ParentCallsAbstract() {
      IO.println("name is " + getName());
    }

    abstract String getName();
  }

  // Constructor calls a concrete method that the child may or may not override
  static class ParentCallsConcrete {
    protected ParentCallsConcrete() {
      doInit();
    }

    void doInit() {
      IO.println("default init");
    }
  }

  // Constructor calls private method → which calls abstract method (transitive chain)
  abstract static class ParentCallsTransitive {
    protected ParentCallsTransitive() {
      helper();
    }

    private void helper() {
      IO.println(getValue());
    }

    abstract String getValue();
  }

  // Constructor directly reads an inherited field (no method call involved)
  static class ParentReadsField {
    protected String readField;
    protected String ignoredField;

    protected ParentReadsField() {
      IO.println("Value: " + readField);
    }
  }

  // =================== Noncompliant ===================

  // Basic: field returned by abstract override, assigned after super()
  public static class FieldUsedViaOverride extends ParentCallsAbstract {
    private final String name;

    FieldUsedViaOverride(String name) {
      super();
      this.name = name; // Noncompliant {{Initialize subclass fields before calling super constructor.}}
//    ^^^^^^^^^^^^^^^^
    }

    @Override
    String getName() {
      return name;
    }
  }

  // Assignment inside if/else block after super() — visitor must recurse into nested blocks
  public static class AssignmentInNestedBlock extends ParentCallsAbstract {
    private final String name;

    AssignmentInNestedBlock(String name, boolean condition) {
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

  // Transitive: super() → private helper() → abstract getValue() → child override
  public static class FieldUsedTransitively extends ParentCallsTransitive {
    private final String data;

    FieldUsedTransitively(String data) {
      super();
      this.data = data; // Noncompliant
    }

    @Override
    String getValue() {
      return data;
    }
  }

  // Inherited field directly read by parent constructor; only the read field is flagged
  public static class InheritedFieldReadByParent extends ParentReadsField {
    InheritedFieldReadByParent(String read, String ignored) {
      super();
      this.readField = read; // Noncompliant
      this.ignoredField = ignored; // Compliant - not read in parent constructor
    }
  }

  // =================== Compliant ===================

  // Assignment BEFORE super() — not included in the post-super statement list
  public static class AssignedBeforeSuper extends ParentCallsAbstract {
    private final String name;

    AssignedBeforeSuper(String name) {
      this.name = name;
      super();
    }

    @Override
    String getName() {
      return name;
    }
  }

  // Parent calls concrete method, child doesn't override it → field not reachable
  public static class ConcreteMethodNotOverridden extends ParentCallsConcrete {
    private String extra;

    ConcreteMethodNotOverridden(String extra) {
      super();
      this.extra = extra; // Compliant - doInit() doesn't use this field
    }
  }

  // Local variable assignment after super() — not a field, should not be flagged
  public static class LocalVariableAfterSuper extends ParentCallsAbstract {
    private final String name;

    LocalVariableAfterSuper(String name) {
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

  // =================== Edge cases ===================

  // Override is in a grandchild, not the direct child that assigns the field
  // → findChildOverride returns the abstract re-declaration → conservative flag
  static class GrandchildOverride {
    abstract class Base {
      Base() {
        describe();
      }

      abstract void describe();
    }

    abstract class Middle extends Base {
      protected final String label;

      Middle() {
        super();
        this.label = "value"; // Noncompliant
      }

      abstract void describe();
    }

    abstract class Leaf extends Middle {
      Leaf() {
        super();
      }

      @Override
      void describe() {
        IO.println(label);
      }
    }
  }

  // Parameter with same name as field: bare name resolves to parameter, this.field resolves to field
  static class ParameterShadowing {
    abstract class Base {
      String someText;

      Base() {
        doWork();
      }

      abstract void doWork();
    }

    // Compliant: bare 'someText' in isValid refers to the parameter, not the field
    class ShadowedByParameter extends Base {
      ShadowedByParameter(String someText) {
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

    // this.someText explicitly accesses the field despite parameter shadowing
    class ExplicitThisBypassesShadowing extends Base {
      ExplicitThisBypassesShadowing(String someText) {
        super();
        this.someText = someText; // Noncompliant
      }

      @Override
      void doWork() {
        isValid("parameter value");
      }

      private boolean isValid(String someText) {
        return !this.someText.isEmpty();
      }
    }
  }

  // Parent only writes to the field (never reads) → assignment-only usage is not flagged
  class FieldOnlyWrittenByParent {
    class Parent {
      String label;

      Parent() {
        this.label = "hello";
        label = "world";
      }
    }

    class Child extends Parent {
      Child() {
        super();
        this.label = "value"; // Compliant - parent only assigns, never reads
        label = "value"; // Compliant
      }
    }
  }

  // Tests bare field name (implicit this) assignment detection and outer-class field exclusion
  class ImplicitThisAndOuterField {
    Integer outerField;

    class Parent {
      String label;

      Parent() {
        IO.println(label);
        postInit();
      }

      void postInit() {
      }
    }

    class Child extends Parent {
      String message;

      Child() {
        super();
        this.label = "value"; // Noncompliant
        label = "value"; // Noncompliant
        this.message = "msg"; // Noncompliant
        message = "msg"; // Noncompliant
        outerField = 1; // Compliant - field belongs to enclosing class, not Child
      }

      @Override
      void postInit() {
        IO.println(message);
        IO.println(outerField);
      }
    }
  }
}
