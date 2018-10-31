class A {
  A(com.google.common.base.Predicate p) {} // Noncompliant [[sc=5;ec=37]] {{Use "java.util.function.Predicate" instead. (sonar.java.source not set. Assuming 8 or greater.)}}
}
