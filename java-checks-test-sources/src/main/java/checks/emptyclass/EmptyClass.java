package checks.emptyclass;

class A {
  int field;
}

class B { // Noncompliant [[sc=7;ec=8]] {{Remove this empty class, write its code or make it an "interface".}}
}
class C {
  I i = new I() {};
}
interface I {}
@interface annotation {}
enum E {}
class J extends C {}
class K implements I {}


class L { // Noncompliant {{Remove this empty class, write its code or make it an "interface".}}
  ;
}

@annotation
class markerType { // compliant because of annotation

}

record R() { // Noncompliant

}

record R2(int x) { // Compliant because there's a component

}

record R3() { // Compliant because there's a method
  int f() {
    return 42;
  }
}
