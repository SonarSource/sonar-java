package checks.unused;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

class UnusedLocalVariableCheck {

  int unusedField;

  public void f(int unusedParameter) {
    int unusedLocalVariable; // Noncompliant [[sc=9;=ec=28]] {{Remove this unused "unusedLocalVariable" local variable.}}

    int usedLocalVariable = 42;
    System.out.println(usedLocalVariable);

    try {
    } catch (Exception e) {
    }

    try (Stream foo = Stream.of()) { // Compliant
    }

    for (int a : new int[]{0, 1, 2}) { // Noncompliant
    }

    for (int i = 0; condition(); i++) { // Noncompliant
    }

    for (int j = 0; j < 10; j++) {
    }

    try (Stream foo2 = Stream.of()) {
      foo2.findFirst();
    }

    int notReadLocalVariable = 0; // Noncompliant
    notReadLocalVariable = 1;
    notReadLocalVariable += 1;
    notReadLocalVariable++;

    int readLocalVariable = 0;
    notReadLocalVariable = readLocalVariable++;

    java.util.stream.Stream<Object> s = Stream.of();
    s.map(v -> "");

    try (Stream foo3 = Stream.of()) {
      foo3.findFirst();
    }
    try (Stream foo3 = Stream.of()) {
      foo3.findFirst();
    }
  }

  private boolean condition() {
    return false;
  }

  public BinaryOperator<UnaryOperator<Object>> foo() {
    return (a, b) -> input -> {
      Object o = a.apply(input); // Compliant, lambda expression correctly handled
      o.toString();
      return o;
    };
  }
}
