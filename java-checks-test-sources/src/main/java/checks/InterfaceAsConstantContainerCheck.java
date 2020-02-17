package checks;

interface InterfaceAsConstantContainerCheck {
}

interface InterfaceAsConstantContainerCheckB {
}

interface InterfaceAsConstantContainerCheckC { // Noncompliant [[sc=11;ec=45]] {{Move constants to a class or enum.}}
  int a = 0;
  int b = 0;
}

interface InterfaceAsConstantContainerCheckD {
  void f();
}

interface InterfaceAsConstantContainerCheckE { // Noncompliant
  int a = 0;
  void f();
}

interface InterfaceAsConstantContainerCheckF { // Noncompliant
  void f();
  int a = 0;
}

interface InterfaceAsConstantContainerCheckG { // Noncompliant
  int a = 0;
  int f();
}

interface InterfaceAsConstantContainerCheckH { // Noncompliant
  int f();
  int a = 0;
}

interface InterfaceAsConstantContainerCheckI {
  int f();
  void g();
}

interface InterfaceAsConstantContainerCheckJ {
  int f();

  interface InterfaceAsConstantContainerCheckK { // Noncompliant
    void f();
    int a = 0;
  }
}

interface InterfaceAsConstantContainerCheckL {
  ;
}
