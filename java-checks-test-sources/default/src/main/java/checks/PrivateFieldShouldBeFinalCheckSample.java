package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

class PrivateFieldShouldBeFinalCheckSample {

  private String assignedInConstructor; // Noncompliant {{Make this private field "final", it is never reassigned after construction.}} [[secondary=39]]
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  private List<String> assignedAtDeclaration = new ArrayList<>(); // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  private int assignedInInitializerBlock; // Noncompliant [[secondary=35]]
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

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
  private volatile String volatileField;
  private final String alreadyFinal = "final";
  private static String staticField;
  String packagePrivate;
  protected String protectedField;
  public String publicField;

  {
    assignedInInitializerBlock = 1;
  }

  PrivateFieldShouldBeFinalCheckSample(String value) {
    this.assignedInConstructor = value;
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
    this.volatileField = value;
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
      + packagePrivate + protectedField + publicField + reassignedInMethod + incrementedInMethod
      + volatileField;
  }
}

class AssignedInEveryConstructor {

  private String value; // Noncompliant [[secondary=94,99]]
//^^^^^^^^^^^^^^^^^^^^^
  private String assignedInOnlyOneConstructor;

  AssignedInEveryConstructor() {
    this.value = "default";
    this.assignedInOnlyOneConstructor = "default";
  }

  AssignedInEveryConstructor(String value) {
    this.value = value;
  }

  String read() {
    return value + assignedInOnlyOneConstructor;
  }
}

class AssignedInDelegatingConstructor {

  private String assignedInBothConstructors;
  private String assignedWithoutBraces;
  private int compoundAssigned;

  AssignedInDelegatingConstructor(String value) {
    this.assignedInBothConstructors = value;
    this.compoundAssigned = 1;
  }

  AssignedInDelegatingConstructor() {
    this("default");
    this.assignedInBothConstructors = "other";
  }

  AssignedInDelegatingConstructor(int value) {
    if (value > 0) this.assignedWithoutBraces = "positive";
    this.assignedInBothConstructors = "int";
  }

  void compound() {
    compoundAssigned += 2;
  }

  String read() {
    return assignedInBothConstructors + assignedWithoutBraces + compoundAssigned;
  }
}

class ConstructorWithEarlyReturn {

  private String guarded;

  ConstructorWithEarlyReturn(String value) {
    if (value == null) {
      return;
    }
    this.guarded = value;
  }

  String read() {
    return guarded;
  }
}

class InitializerBlockOfAnonymousClass {

  private int writtenByAnonymousInitializer;

  Runnable anonymous() {
    return new Runnable() {
      {
        writtenByAnonymousInitializer = 1;
      }

      @Override
      public void run() {
        // nothing
      }
    };
  }

  int read() {
    return writtenByAnonymousInitializer;
  }
}

class ReturnInAnonymousClass {

  private Runnable task; // Noncompliant [[secondary=180]]

  ReturnInAnonymousClass(boolean enabled) {
    this.task = new Runnable() {
      @Override
      public void run() {
        if (enabled) {
          return;
        }
      }
    };
  }

  Runnable read() {
    return task;
  }
}

class EmptyConstructor {

  private String assignedInOneConstructorOnly;

  EmptyConstructor() {
  }

  EmptyConstructor(String value) {
    this.assignedInOneConstructorOnly = value;
  }

  String read() {
    return assignedInOneConstructorOnly;
  }
}

class MultiVariableDeclaration {

  private int shared, alsoShared = 1; // Noncompliant [[secondary=216]]

  MultiVariableDeclaration(int value) {
    this.shared = value;
  }

  void reassign() {
    alsoShared = 2;
  }

  int read() {
    return shared + alsoShared;
  }
}

class EmptyAndNonExpressionConstructors {

  private String value; // Noncompliant [[secondary=233,238]]

  EmptyAndNonExpressionConstructors() {
    this.value = "empty";
  }

  EmptyAndNonExpressionConstructors(int ignored) {
    int local = ignored;
    this.value = "local" + local;
  }

  String read() {
    return value;
  }
}

@javax.persistence.Entity
class PersistenceEntity {

  private String name;

  PersistenceEntity(String name) {
    this.name = name;
  }

  String read() {
    return name;
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

  private String label; // Noncompliant [[secondary=282]]
//^^^^^^^^^^^^^^^^^^^^^
  private int counter;

  EnumWithPrivateField(String label) {
    this.label = label;
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

class UnaryReads {

  private boolean enabled; // Noncompliant
  private int offset; // Noncompliant
  private int negated; // Noncompliant

  UnaryReads(boolean enabled, int offset, int negated) {
    this.enabled = enabled;
    this.offset = offset;
    this.negated = negated;
  }

  boolean read() {
    return !enabled && -offset < ~negated;
  }
}

class ReadBeforeAssignment {

  private int readByAnotherFieldInitializer;
  private int readEarlierInConstructor;
  private int readLaterInConstructor; // Noncompliant
  private final int copy = readByAnotherFieldInitializer;

  ReadBeforeAssignment() {
    log(readEarlierInConstructor);
    this.readEarlierInConstructor = 1;
    this.readByAnotherFieldInitializer = 2;
    this.readLaterInConstructor = 3;
    log(readLaterInConstructor);
  }

  private static void log(int value) {
    // nothing
  }
}

class ReadInInitializerBlockBeforeWrite {

  private int readBeforeInitializerBlockWrite;
  private int alsoRead;

  {
    log(readBeforeInitializerBlockWrite);
    readBeforeInitializerBlockWrite = 1;
  }

  ReadInInitializerBlockBeforeWrite() {
    log(alsoRead);
    this.alsoRead = 2;
  }

  private static void log(int value) {
    // nothing
  }
}

class CopyConstructor {

  private int coordinate; // Noncompliant

  CopyConstructor(CopyConstructor other) {
    this.coordinate = other.coordinate;
  }

  int read() {
    return coordinate;
  }
}

class SelfReferencingAssignment {

  private int incrementedInConstructor;
  private int incrementedInInitializerBlock;

  {
    incrementedInInitializerBlock = incrementedInInitializerBlock + 1;
  }

  SelfReferencingAssignment() {
    this.incrementedInConstructor = this.incrementedInConstructor + 1;
  }

  int read() {
    return incrementedInConstructor + incrementedInInitializerBlock;
  }
}

class ReadInDelegatingConstructor {

  private String value; // Noncompliant

  ReadInDelegatingConstructor(String value) {
    this.value = value;
  }

  ReadInDelegatingConstructor() {
    this("default");
    log(value);
  }

  private static void log(String value) {
    // nothing
  }
}

class CStyleArrayDeclaration {

  private int sharedWithArrayFragment[], alsoSharedWithArrayFragment; // Noncompliant

  CStyleArrayDeclaration(int value) {
    this.sharedWithArrayFragment = new int[value];
  }

  void reassign() {
    alsoSharedWithArrayFragment = 2;
  }

  int read() {
    return sharedWithArrayFragment.length + alsoSharedWithArrayFragment;
  }
}
