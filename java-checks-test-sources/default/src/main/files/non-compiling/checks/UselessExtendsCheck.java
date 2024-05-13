import checks.UselessExtendsCheck.I1;

interface I1 {}
interface I2 extends UnknownInterface {}

class A implements I1 {}
class D extends A {}
class E extends D implements I1, I2 {} // Noncompliant {{"I1" is implemented by a super class; there is no need to implement it here.}}

class N extends UnknownClass1 implements UnknownInterface1, UnknownInterface2 {} // Compliant

class C5 implements I1, I1 {} // Noncompliant {{"I1" is listed multiple times.}}
//                      ^^

class Class5 extends UnknownClass1 implements 
  UnknownInterface,
  UnknownInterface, // Noncompliant {{"UnknownInterface" is listed multiple times.}}
  java.io.UnknownInterface,
  java.io.UnknownInterface, // Noncompliant {{"java.io.UnknownInterface" is listed multiple times.}}
  UnknownParametrized<Unknown>,
  UnknownParametrized<Unknown> {} // Noncompliant {{"UnknownParametrized" is listed multiple times.}}
