package checks;

import java.io.Serializable;
import java.util.function.Supplier;

class ArrayCovarianceCheckSample {

  abstract static class Fruit {}
  static class Apple extends Fruit {}
  static class Orange extends Fruit {}

  abstract static class Shape {}
  static class Circle extends Shape {}

  static class A {}
  static class B extends A {}
  static class C extends B {}

  // --- Noncompliant: variable declarations with new array creation ---

  void variableDeclarations() {
    Fruit[] fruits1 = new Apple[10]; // Noncompliant {{Use the type of the actual array element here; array covariance can lead to ArrayStoreException at runtime.}}
//                    ^^^^^^^^^^^^^
    Object[] objects = new String[5]; // Noncompliant
    Number[] numbers = new Integer[4]; // Noncompliant
    CharSequence[] seqs = new String[3]; // Noncompliant
    Serializable[] items = new String[2]; // Noncompliant
    A[] deepHierarchy = new C[3]; // Noncompliant
  }

  // --- Noncompliant: variable initialized from existing subtype array reference ---

  void variableFromReference() {
    Apple[] apples = new Apple[5];
    Fruit[] fruits = apples; // Noncompliant
  }

  // --- Noncompliant: field-level covariance ---

  Shape[] shapeField = new Circle[2]; // Noncompliant

  // --- Noncompliant: assignments ---

  void assignments() {
    Fruit[] fruits;
    fruits = new Apple[5]; // Noncompliant
    Object[] objects;
    objects = new Integer[3]; // Noncompliant
  }

  // --- Noncompliant: return statements ---

  Fruit[] returnCovariant() {
    return new Apple[1]; // Noncompliant
  }

  Object[] returnCovariantJdk() {
    return new String[2]; // Noncompliant
  }

  // --- Noncompliant: method arguments ---

  void acceptFruits(Fruit[] fruits) {}
  void acceptObjects(Object[] objects) {}

  void methodArguments() {
    acceptFruits(new Apple[1]); // Noncompliant
    acceptObjects(new String[1]); // Noncompliant
  }

  // --- Noncompliant: constructor arguments ---

  static class Container {
    Container(Fruit[] fruits) {}
    Container(int x, Object[] objects) {}
  }

  void constructorArguments() {
    new Container(new Apple[1]); // Noncompliant
    new Container(1, new String[2]); // Noncompliant
  }

  // --- Noncompliant: lambda return ---

  void lambdaReturn() {
    Supplier<Fruit[]> s = () -> {
      return new Apple[1]; // Noncompliant
    };
  }

  // --- Noncompliant: switch expression ---

  void switchExpression(int code) {
    Apple[] apples = new Apple[1];
    Fruit[] result = switch (code) {
      case 0 -> apples; // Noncompliant
      default -> null;
    };
  }

  // --- Noncompliant: yield statement ---

  void yieldStatement(int code) {
    Apple[] apples = new Apple[1];
    Fruit[] result = switch (code) {
      case 0 -> null;
      default -> {
        yield apples; // Noncompliant
      }
    };
  }

  // --- Compliant: same-type arrays ---

  void sameType() {
    Apple[] apples = new Apple[10]; // Compliant
    String[] strings = new String[5]; // Compliant
    Fruit[] fruits = new Fruit[3]; // Compliant
  }

  // --- Compliant: primitive arrays ---

  void primitiveArrays() {
    int[] numbers = new int[4]; // Compliant
    double[] doubles = new double[3]; // Compliant
  }

  // --- Compliant: null assignment ---

  void nullAssignment() {
    Object[] objects = null; // Compliant
  }

  // --- Compliant: method return matching declared type ---

  Apple[] returnSameType() {
    return new Apple[1]; // Compliant
  }

  // --- Compliant: method argument matching parameter type ---

  void acceptApples(Apple[] apples) {}

  void methodArgumentsSameType() {
    acceptApples(new Apple[1]); // Compliant
  }

  // --- Compliant: multi-dimensional same-type ---

  void multiDimensional() {
    String[][] matrix = new String[3][3]; // Compliant
  }

  // --- Compliant: base type array creation with initializer ---

  void baseTypeInitializer() {
    Number[] numbers = new Number[] { 1, 2.0 }; // Compliant
  }

  // --- Compliant: void return ---

  void voidReturn() {
    return; // Compliant
  }

  // --- Compliant: non-array types ---

  void nonArrayTypes() {
    Object o = "hello"; // Compliant
    Number n = 42; // Compliant
  }

  // --- Compliant: lambda returning matching type ---

  void lambdaCompliant() {
    Supplier<Apple[]> s = () -> {
      return new Apple[1]; // Compliant
    };
  }

  // --- Compliant: switch expression with matching types ---

  void switchCompliant(int code, Fruit[] fruits) {
    Fruit[] result = switch (code) {
      case 0 -> fruits; // Compliant
      default -> null;
    };
  }

  // --- Compliant: switch statement yield (not expression) ---

  void switchStatement(int code) {
    switch (code) {
      default -> doNothing();
    };
  }

  private void doNothing() {}
}
