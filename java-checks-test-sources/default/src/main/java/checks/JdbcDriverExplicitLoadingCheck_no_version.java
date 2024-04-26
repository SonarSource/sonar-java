package checks;

class JdbcDriverExplicitLoadingCheck_no_version {
  void doStuff() throws ClassNotFoundException {
    Class.forName("org.h2.Driver"); // Noncompliant {{Remove this "Class.forName()", it is useless. (sonar.java.source not set. Assuming 6 or greater.)}}
//        ^^^^^^^
    Class.forName("java.lang.String");
  }
}
