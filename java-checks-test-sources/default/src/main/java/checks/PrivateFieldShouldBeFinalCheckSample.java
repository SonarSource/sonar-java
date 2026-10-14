package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

class PrivateFieldShouldBeFinalCheckSample {

  private String assignedInConstructor; // Noncompliant {{Make this private field "final", it is never reassigned after construction.}}
//               ^^^^^^^^^^^^^^^^^^^^^
  private List<String> assignedAtDeclaration = new ArrayList<>(); // Noncompliant
//                     ^^^^^^^^^^^^^^^^^^^^^
  private int assignedInInitializerBlock; // Noncompliant
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^

  private int reassignedInMethod;
  private int incrementedInMethod;
  private String neverAssigned;
  private String assignedTwiceInConstructor;
  private String assignedConditionally;
  private String assignedInTry;
  private String assignedInLambda;
  private String lazilyInitialized;
  private String assignedInHelperMethod;
  private String assignedOnAnotherInstance;
  private final String alreadyFinal = "final";
  private static String staticField;
  String packagePrivate;
  protected String protectedField;
  public String publicField;

  {
    assignedInInitializerBlock = 1;
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^<
  }

  PrivateFieldShouldBeFinalCheckSample(String value) {
    this.assignedInConstructor = value;
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^<
    this.assignedTwiceInConstructor = value;
    this.assignedTwiceInConstructor = value + value;
    if (value != null) {
      this.assignedConditionally = value;
    }
    try {
      this.assignedInTry = value;
    } catch (RuntimeException e) {
      this.assignedInTry = "";
    }
    Runnable r = () -> this.assignedInLambda = value;
    r.run();
    initialize(value);
    PrivateFieldShouldBeFinalCheckSample other = new PrivateFieldShouldBeFinalCheckSample();
    other.assignedOnAnotherInstance = value;
  }

  PrivateFieldShouldBeFinalCheckSample() {
    this("default");
  }

  private void initialize(String value) {
    this.assignedInHelperMethod = value;
  }

  void reassign() {
    reassignedInMethod = 1;
    incrementedInMethod++;
  }

  String lazilyInitialized() {
    if (lazilyInitialized == null) {
      lazilyInitialized = "computed";
    }
    return lazilyInitialized;
  }

  String readAll() {
    return assignedInConstructor + assignedAtDeclaration + assignedInInitializerBlock + neverAssigned
      + assignedTwiceInConstructor + assignedConditionally + assignedInTry + assignedInLambda
      + assignedInHelperMethod + assignedOnAnotherInstance + alreadyFinal + staticField
      + packagePrivate + protectedField + publicField + reassignedInMethod + incrementedInMethod;
  }
}

class AssignedInEveryConstructor {

  private String value; // Noncompliant
//               ^^^^^
  private String assignedInOnlyOneConstructor;

  AssignedInEveryConstructor() {
    this.value = "default";
//  ^^^^^^^^^^^^^^^^^^^^^^<
    this.assignedInOnlyOneConstructor = "default";
  }

  AssignedInEveryConstructor(String value) {
    this.value = value;
//  ^^^^^^^^^^^^^^^^^^<
  }

  String read() {
    return value + assignedInOnlyOneConstructor;
  }
}

class AnnotatedFields {

  @Nullable
  private String annotated;

  AnnotatedFields(String value) {
    this.annotated = value;
  }

  String read() {
    return annotated;
  }
}

enum EnumWithPrivateField {
  INSTANCE("name");

  private String label; // Noncompliant
//               ^^^^^
  private int counter;

  EnumWithPrivateField(String label) {
    this.label = label;
//  ^^^^^^^^^^^^^^^^^^<
  }

  void increment() {
    counter++;
  }

  String read() {
    return label + counter;
  }
}

class OuterWithNestedWriter {

  private String writtenByNestedClass;
  private String writtenByAnonymousClass;

  OuterWithNestedWriter(String value) {
    this.writtenByNestedClass = value;
    this.writtenByAnonymousClass = value;
  }

  class Inner {
    void write() {
      writtenByNestedClass = "changed";
    }
  }

  Supplier<String> anonymous() {
    return new Supplier<>() {
      @Override
      public String get() {
        writtenByAnonymousClass = "changed";
        return writtenByAnonymousClass;
      }
    };
  }

  String read() {
    return writtenByNestedClass + writtenByAnonymousClass;
  }
}

interface InterfaceWithField {
  String CONSTANT = "constant";
}

record RecordSample(String component) {
}
