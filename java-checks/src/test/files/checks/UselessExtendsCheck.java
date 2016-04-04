interface I1 {}
class A implements I1 {}
class B extends A implements I1 {} // Noncompliant {{"I1" is implemented by a super class; there is no need to implement it here.}}
class C extends B implements I1 {} // Noncompliant  {{"I1" is implemented by a super class; there is no need to implement it here.}}

interface I2 extends UnknownInterface {}
class D extends A {}
class E extends D implements I1, I2 {} // Noncompliant {{"I1" is implemented by a super class; there is no need to implement it here.}}

interface I3<T> {}
class C1 {}
class C2 implements I3<C1> {}
class C3 extends C2 implements I3<C1> {} // Noncompliant {{"I3" is implemented by a super class; there is no need to implement it here.}}

public class C5 implements I1, I1 {} // Noncompliant [[sc=32;ec=34]] {{"I1" is listed multiple times.}}

interface I4 extends I1 {}
interface I5 extends I4, I2 {}
interface I6 extends I4, I5 {} // Noncompliant {{"I4" is already extended by "I5"; there is no need to implement it here.}}
class F implements I1, I5 {} // Noncompliant {{"I1" is implemented by a super class; there is no need to implement it here.}}

class G1 implements I4, I1 {} // Noncompliant {{"I1" is already extended by "I4"; there is no need to implement it here.}}
class G2 implements I1, I4 {} // Noncompliant {{"I1" is already extended by "I4"; there is no need to implement it here.}}

interface I7 {}
interface I8 extends I7 {}
interface I9 extends I7 {}
class H1 implements I8, I9 {} // Compliant
class H2 implements I8, I9, I7 {} // Noncompliant {{"I7" is already extended by "I8"; there is no need to implement it here.}}
class H3 implements I9, I8, I7 {} // Noncompliant {{"I7" is already extended by "I9"; there is no need to implement it here.}}

enum E1 implements I7, I8 {} // Noncompliant {{"I7" is already extended by "I8"; there is no need to implement it here.}}

class L extends Object {} // Noncompliant [[sc=17;ec=23]] {{"Object" should not be explicitly extended.}}
class M extends java.lang.Object {} // Noncompliant [[sc=17;ec=33]] {{"Object" should not be explicitly extended.}}
class N extends UnknownClass1 implements UnknownInterface1, UnknownInterface2 {} // Compliant

class Class5 extends UnknownClass1 implements 
  UnknownInterface,
  UnknownInterface, // Noncompliant {{"UnknownInterface" is listed multiple times.}}
  java.io.UnknownInterface,
  java.io.UnknownInterface, // Noncompliant {{"java.io.UnknownInterface" is listed multiple times.}}
  UnknownParametrized<Unknown>,
  UnknownParametrized<Unknown> {} // Noncompliant {{"UnknownParametrized" is listed multiple times.}}
