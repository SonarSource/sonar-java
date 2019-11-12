
import java.util.stream.Stream;

class Foo {

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

    unknown++;
    this.unknown++;
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

}
