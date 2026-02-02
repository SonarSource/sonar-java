// SONARJAVA-6028: FPs ahead. Only the line with "Too much." should be noncompliant.

void main() { // Noncompliant
  System.out.println("Just right."); // Noncompliant
  if (true) {
        System.out.println("Too much."); // Noncompliant
  }
}

class MyClass { // Noncompliant
}
