class A {
  int field;
}

class B { // Noncompliant {{Remove this empty class, write its code or make it an "interface".}}
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