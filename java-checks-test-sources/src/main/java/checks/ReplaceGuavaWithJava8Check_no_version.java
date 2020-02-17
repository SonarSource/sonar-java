package checks;

class ReplaceGuavaWithJava8Check_no_version {
  ReplaceGuavaWithJava8Check_no_version(com.google.common.base.Predicate p) {} // Noncompliant [[sc=41;ec=73]] {{Use "java.util.function.Predicate" instead. (sonar.java.source not set. Assuming 8 or greater.)}}
}
