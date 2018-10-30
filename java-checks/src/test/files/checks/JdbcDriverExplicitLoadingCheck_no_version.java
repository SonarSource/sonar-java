class A {
  void doStuff() {
    Class.forName("org.h2.Driver"); // Noncompliant [[sc=11;ec=18]] {{Remove this "Class.forName()", it is useless. (sonar.java.source not set. Assuming 6 or greater.)}}
    Class.forName("java.lang.String");
    Class.forName(javax.print.ServiceUIFactory.DIALOG_UI);
  }
}
