package checks;

class CopyConstructorMissesFieldCheckSample {

  static class Basic {
    private static int instances;
    private transient Object cache;
    private Object initialized = new Object();
    private final String name;
    private String note;
    private int retries;
    private boolean enabled;

    Basic(Basic other) { // Noncompliant {{This copy constructor leaves eligible fields uninitialized; initialize them explicitly to distinguish omissions from intentional resets.}} [[secondary=10,11,12]]
      name = other.name;
    }
  }

  static class ExplicitDefaults {
    private String text;
    private int number;
    private boolean flag;

    ExplicitDefaults(ExplicitDefaults other) {
      this.text = null;
      number = 0;
      this.flag = false;
    }
  }

  static class ConstructorsThatAreNotCopies {
    private String value;

    ConstructorsThatAreNotCopies() {
    }

    ConstructorsThatAreNotCopies(String value) {
    }

    ConstructorsThatAreNotCopies(ConstructorsThatAreNotCopies other, int ignored) {
    }
  }

  static class Generic<T> {
    private T first;
    private T second;

    Generic(Generic<T> other) { // Noncompliant [[secondary=46]]
      first = other.first;
    }
  }

  static class ForeignAssignments {
    private int value;

    ForeignAssignments(ForeignAssignments other) { // Noncompliant [[secondary=54]]
      other.value = 1;
    }
  }

  static class ShadowedField {
    private int value;

    ShadowedField(ShadowedField other) { // Noncompliant [[secondary=62]]
      int value = other.value;
      value = 1;
    }
  }

  static class CompoundAssignment {
    private int value;

    CompoundAssignment(CompoundAssignment other) {
      value += other.value;
    }
  }

  static class IncrementIsNotAssignment {
    private int value;

    IncrementIsNotAssignment(IncrementIsNotAssignment other) { // Noncompliant [[secondary=79]]
      value++;
    }
  }

  static class Delegation {
    private String name;
    private int count;

    Delegation(Delegation other) {
      this(other.name, other.count);
    }

    Delegation(String name, int count) {
      this.name = name;
      this.count = count;
    }
  }

  static class IncompleteDelegation {
    private String name;
    private int count;

    IncompleteDelegation(IncompleteDelegation other) { // Noncompliant [[secondary=102]]
      this(other.name);
    }

    IncompleteDelegation(String name) {
      this.name = name;
    }
  }

  static class Helpers {
    private String name;
    private int count;

    Helpers(Helpers other) {
      copyName(other);
      this.copyCount(other);
    }

    private void copyName(Helpers other) {
      name = other.name;
    }

    private void copyCount(Helpers other) {
      finishCount(other);
    }

    private void finishCount(Helpers other) {
      count = other.count;
    }
  }

  static class IncompleteHelper {
    private String name;
    private int count;

    IncompleteHelper(IncompleteHelper other) { // Noncompliant [[secondary=137]]
      setName(other);
    }

    private void setName(IncompleteHelper other) {
      name = other.name;
    }
  }

  static class CallsOnOtherDoNotCount {
    private int value;

    CallsOnOtherDoNotCount(CallsOnOtherDoNotCount other) { // Noncompliant [[secondary=149]]
      other.initialize();
    }

    private void initialize() {
      value = 1;
    }
  }

  static class StaticHelperDoesNotCount {
    private int value;

    StaticHelperDoesNotCount(StaticHelperDoesNotCount other) { // Noncompliant [[secondary=161]]
      initialize(other);
    }

    private static void initialize(StaticHelperDoesNotCount target) {
      target.value = 1;
    }
  }

  static class ConditionalAssignment {
    private int value;

    ConditionalAssignment(ConditionalAssignment other) {
      if (other.value > 0) {
        value = other.value;
      }
    }
  }

  static class DeferredAssignments {
    private int lambdaValue;
    private int localClassValue;
    private int anonymousClassValue;

    DeferredAssignments(DeferredAssignments other) { // Noncompliant [[secondary=183,184,185]]
      Runnable lambda = () -> lambdaValue = other.lambdaValue;
      class Local {
        void set() {
          localClassValue = other.localClassValue;
        }
      }
      Runnable anonymous = new Runnable() {
        @Override
        public void run() {
          anonymousClassValue = other.anonymousClassValue;
        }
      };
    }
  }

  static class SelfTypedField {
    private SelfTypedField parent;
    private String label;

    SelfTypedField(SelfTypedField other) { // Noncompliant [[secondary=205]]
      parent = other.parent;
    }
  }

  record Point(int x, int y) {
    Point(Point other) {
      this(other.x, other.y);
    }
  }

  static class MultipleCopyConstructors {
    private int first;
    private int second;

    MultipleCopyConstructors(MultipleCopyConstructors other) { // Noncompliant [[secondary=220]]
      first = other.first;
    }

    MultipleCopyConstructors(MultipleCopyConstructors other, boolean marker) {
      second = other.second;
    }
  }

  static class ParenthesizedThis {
    private int value;

    ParenthesizedThis(ParenthesizedThis other) {
      (this).value = other.value;
    }
  }

  static class EligibilityExclusions {
    private static int staticValue;
    private transient int transientValue;
    private int initializedValue = 1;
    private final int initializedFinal = 2;

    EligibilityExclusions(EligibilityExclusions other) {
    }
  }

  static class DifferentTypeParameter {
    private int value;

    DifferentTypeParameter(Basic other) {
    }
  }

  static class ParentType {
  }

  static class ChildType extends ParentType {
    private int value;

    ChildType(ParentType other) {
    }
  }

  static class RecursiveHelper {
    private int value;

    RecursiveHelper(RecursiveHelper other) {
      initialize(other);
    }

    private void initialize(RecursiveHelper other) {
      initialize(other);
    }
  }

  static class DelegationChain {
    private String name;
    private int value;

    DelegationChain(DelegationChain other) {
      this(other.name);
    }

    DelegationChain(String name) {
      this(name, 0);
    }

    DelegationChain(String name, int value) {
      this.name = name;
      this.value = value;
    }
  }
}
