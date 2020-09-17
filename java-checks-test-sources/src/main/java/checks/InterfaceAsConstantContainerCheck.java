package checks;

interface InterfaceAsConstantContainerCheck {
}

interface InterfaceAsConstantContainerCheckA { // Noncompliant [[sc=11;ec=45;secondary=7,8]] {{Move constants defined in this interfaces to another class or enum.}}
  int a = 0;
  int b = 0;
}

interface InterfaceAsConstantContainerCheckB {
  void f();
}

interface InterfaceAsConstantContainerCheckC { // Compliant
  int a = 0;
  void f();
}

interface InterfaceAsConstantContainerCheckD { // Compliant
  void f();
  int a = 0;
}

interface InterfaceAsConstantContainerCheckE {
  int f();
  void g();
}

interface InterfaceAsConstantContainerCheckF {
  int f();

  interface InterfaceAsConstantContainerCheckG { // Compliant
    void f();
    int a = 0;
  }

  interface InterfaceAsConstantContainerCheckH { // Noncompliant
    int a = 0;
    ;
    int b = 0;
  }
}

interface InterfaceAsConstantContainerCheckI {
  ;
}
