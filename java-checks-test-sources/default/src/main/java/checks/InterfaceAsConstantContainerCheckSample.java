package checks;

interface InterfaceAsConstantContainerCheckSample {
}

interface InterfaceAsConstantContainerCheckSampleA { // Noncompliant [[sc=11;ec=51;secondary=7,8]] {{Move constants defined in this interfaces to another class or enum.}}
  int a = 0;
  int b = 0;
}

interface InterfaceAsConstantContainerCheckSampleB {
  void f();
}

interface InterfaceAsConstantContainerCheckSampleC { // Compliant
  int a = 0;
  void f();
}

interface InterfaceAsConstantContainerCheckSampleD { // Compliant
  void f();
  int a = 0;
}

interface InterfaceAsConstantContainerCheckSampleE {
  int f();
  void g();
}

interface InterfaceAsConstantContainerCheckSampleF {
  int f();

  interface InterfaceAsConstantContainerCheckSampleG { // Compliant
    void f();
    int a = 0;
  }

  interface InterfaceAsConstantContainerCheckSampleH { // Noncompliant
    int a = 0;
    ;
    int b = 0;
  }
}

interface InterfaceAsConstantContainerCheckSampleI {
  ;
}

interface InterfaceAsConstantContainerCheckSampleWithParent extends InterfaceAsConstantContainerCheckSampleB { // Compliant
  // If the interface has a parent, you will be forced to implement methods, the interface represents more than a set of constants, it is therefore a legitimate use case.
  int a = 0;
}
