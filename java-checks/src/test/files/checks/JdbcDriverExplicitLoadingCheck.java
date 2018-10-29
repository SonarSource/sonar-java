
class A {
  void doStuff() {
    Class.forName("org.h2.Driver"); // Noncompliant [[sc=11;ec=18]]
  }
}

class B {
  void doStuff() {
    Class.forName("java.lang.String");
  }
}

class C {
  void doStuff() {
    Class.forName(javax.print.ServiceUIFactory.DIALOG_UI);
  }
}
