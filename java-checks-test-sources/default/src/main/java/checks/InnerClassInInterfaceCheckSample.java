package checks;

import java.util.List;
import java.util.Map;
import java.util.Optional;

interface InnerClassInInterfaceCheckSample {

  void doSomething();

  class DefaultImpl implements InnerClassInInterfaceCheckSample { // Noncompliant {{Move this class out of this interface.}}
//      ^^^^^^^^^^^
    @Override
    public void doSomething() {
    }
  }

  class Empty {} // Noncompliant
//      ^^^^^

  static class ExplicitlyStatic {} // Noncompliant
//             ^^^^^^^^^^^^^^^^

  abstract class AbstractHelper {} // Compliant - abstract classes are contract-like

  interface NestedContract { // Compliant - nested interface
    void process();
  }

  enum Status { // Compliant - enums for contract constants
    ACTIVE, INACTIVE
  }

  record Payload(String data, int size) {} // Compliant - record as data carrier

  @interface Marker {} // Compliant - annotation type

  interface InnerInterface {
    class InnerInNested {} // Noncompliant
//        ^^^^^^^^^^^^^
  }
}

interface ExtendedInterface extends Comparable<ExtendedInterface> {
  class Helper { // Noncompliant
//      ^^^^^^
    public int compare(ExtendedInterface a, ExtendedInterface b) {
      return 0;
    }
  }

  class First {} // Noncompliant
//      ^^^^^
  class Second {} // Noncompliant
//      ^^^^^^
}

interface WithHelperTypes {

  Result compute(Request request) throws ComputationException;

  Optional<Wrapped> wrapped();

  void consume(List<Map<String, Element>> elements);

  void consumeQualified(WithHelperTypes.Qualified qualified);

  default Defaulted defaulted() {
    return new Defaulted();
  }

  static Created create() {
    return new Created();
  }

  class Result {} // Compliant - return type of an interface method

  class Request {} // Compliant - parameter type of an interface method

  class ComputationException extends Exception {} // Compliant - exception thrown by an interface method

  class Wrapped {} // Compliant - type argument of a return type

  class Element {} // Compliant - nested type argument of a parameter type

  class Qualified {} // Compliant - qualified parameter type

  class Defaulted {} // Compliant - return type of a default method

  class Created {} // Compliant - return type of a static method

  class Unused {} // Noncompliant
//      ^^^^^^

  class UsedInBodyOnly {} // Noncompliant
//      ^^^^^^^^^^^^^^
  default void useInBody() {
    UsedInBodyOnly local = new UsedInBodyOnly();
  }

  class ParameterName {} // Noncompliant
//      ^^^^^^^^^^^^^
  void named(String ParameterName);
}

interface WithNameCollisionsInSignatures {

  java.util.Optional<String> qualifiedForeignType();

  <Element> Element typeParameter();

  class Optional {} // Noncompliant
//      ^^^^^^^^

  class Element {} // Noncompliant
//      ^^^^^^^
}

interface WithImplementationsUsedInSignatures {

  WithImplementationsUsedInSignatures copy(Nested other);

  Generic<String> generic();

  Qualified qualified();

  class Nested implements WithImplementationsUsedInSignatures { // Noncompliant
//      ^^^^^^
    @Override
    public WithImplementationsUsedInSignatures copy(Nested other) {
      return this;
    }
    @Override
    public Generic<String> generic() {
      return null;
    }
    @Override
    public Qualified qualified() {
      return null;
    }
  }

  class Generic<T> implements WithImplementationsUsedInSignatures, Comparable<Generic<T>> { // Noncompliant
//      ^^^^^^^
    @Override
    public WithImplementationsUsedInSignatures copy(Nested other) {
      return this;
    }
    @Override
    public Generic<String> generic() {
      return null;
    }
    @Override
    public Qualified qualified() {
      return null;
    }
    @Override
    public int compareTo(Generic<T> o) {
      return 0;
    }
  }

  class Qualified implements checks.WithImplementationsUsedInSignatures { // Noncompliant
//      ^^^^^^^^^
    @Override
    public WithImplementationsUsedInSignatures copy(Nested other) {
      return this;
    }
    @Override
    public Generic<String> generic() {
      return null;
    }
    @Override
    public Qualified qualified() {
      return null;
    }
  }
}

interface ComparableUser {

  Helper helper();

  class Helper implements Comparable<ComparableUser> { // Compliant - helper type, does not implement the enclosing interface
    @Override
    public int compareTo(ComparableUser o) {
      return 0;
    }
  }
}

interface WithBuilder {

  String name();

  class Builder { // Compliant - builder pattern
    private String name;

    public Builder name(String name) {
      this.name = name;
      return this;
    }
  }

  class WithBuilderBuilder {} // Compliant - builder pattern
}

class TopLevelClass { // Compliant - not inside an interface
  class InnerClass {} // Compliant - class inside class

  interface InnerInterface {} // Compliant - interface inside class
}

enum TopLevelEnum {
  VALUE;
  class Helper {} // Compliant - class inside enum
}

interface WithDefaultMethod {
  default Runnable createAction() {
    return new Runnable() { // Compliant - anonymous class
      @Override
      public void run() {
      }
    };
  }
}

@interface AnnotationWithClass {
  class Helper {} // Compliant - annotation types are not interfaces
}

interface WithNestedClassHierarchy {
  class Outer { // Noncompliant
//      ^^^^^
    class Inner {} // Compliant - parent is a class, not an interface
  }
}

interface WithGenericHelperTypes {

  GenericResult<String> compute(GenericRequest<Integer> request);

  void consume(List<GenericElement<String>> elements);

  class GenericResult<T> {} // Compliant - generic return type of an interface method

  class GenericRequest<T> {} // Compliant - generic parameter type of an interface method

  class GenericElement<T> {} // Compliant - generic type argument of a parameter type

  class GenericUnused<T> {} // Noncompliant
//      ^^^^^^^^^^^^^
}
