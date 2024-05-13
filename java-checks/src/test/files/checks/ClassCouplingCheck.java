class Foo { // Noncompliant {{Split this class into smaller and more specialized ones to reduce its dependencies on other classes from 21 to the maximum authorized 20 or less.}}
//    ^^^
  T1 a1;    // Foo is coupled to T1
  T2 a2;    // Foo is coupled to T2
  T3 a3;    // Foo is coupled to T3
  T4 a4;    // etc.
  T5 a5;
  T6 a6;
  T7 a7;
  T8 a8;
  T9 a9;
  T10 a10;
  T11 a11;
  T12 a12;
  T13 a13;
  T14 a14;
  T15 a15;
  T16 a16;
  T17 a17;
  T18 a18;
  T19 a19;
  T20 a20;
  T21 a21;   // Counted just once
  T21 a22;   // Counted just once
  T21 a23;   // Counted just once
  T21 a24;   // Counted just once
}

class Bar {  // Compliant
  T1 a1;
  T2 a2;
}

class Baz { // Noncompliant
  T1 a1;
  T2 a2;
  T3 a3;
  T4 a4;
  T5 a5;
  T6 a6;
  T7 a7;
  T8 a8;
  T9 a9;
  T10 a10;
  T11 a11;
  T12 a12;
  T13 a13;
  T14 a14;
  T15 a15;
  T16 a16;
  T17 a17;
  T18 a18;
  T19 a19;
  T20 a20;
  T21 a21;
  T22 a22;
  T23 a23;
}

class Qix { // Compliant
  class Bar { // Noncompliant
    T1 a1;
    T2 a2;
    T3 a3;
    T4 a4;
    T5 a5;
    T6 a6;
    T7 a7;
    T8 a8;
    T9 a9;
    T10 a10;
    T11 a11;
    T12 a12;
    T13 a13;
    T14 a14;
    T15 a15;
    T16 a16;
    T17 a17;
    T18 a18;
    T19 a19;
    T20 a20;
    T21 a21;
  }
}

class Qux { // Noncompliant
  T1 a1;
  T2 a2;
  T3 a3;
  T4 a4;
  T5 a5;
  T6 a6;
  T7 a7;
  T8 a8;
  T9 a9;
  T10 a10;

  class Bar {
  }

  T11 a11;
  T12 a12;
  T13 a13;
  T14 a14;
  T15 a15;
  T16 a16;
  T17 a17;
  T18 a18;
  T19 a19;
  T20 a20;
  T21 a21;
}

enum Qax { // Compliant - should not fail
  ;

  T1 foo() {
  }
}

class Qex {  // Compliant
  T1 a1;
  T2 a2;
  T3 a3;
  T4 a4;
  T5 a5;
  T6 a6;
  T7 a7;
  T8 a8;
  T9 a9;
  T10 a10;
  T11 a11;
  T12 a12;
  T13 a13;
  T14 a14;
  T15 a15;
  T16 a16;
  T17 a17;
  short a18;
  float a19;
  double a20;
  int a21;
}

class Plop { // Noncompliant {{Split this class into smaller and more specialized ones to reduce its dependencies on other classes from 21 to the maximum authorized 20 or less.}}
  List<T1> a1;
  List<T2> a2;
  List<T3> a3;
  List<T4> a4;
  List<T5> a5;
  List<T6> a6;
  List<T7> a7;
  List<T8> a8;
  List<T9> a9;
  List<T10> a10;
  List<T11> a11;
  List<T12> a12;
  List<T13> a13;
  List<T14> a14;
  List<T15> a15;
  List<T16> a16;
  List<T17> a17;
  List<T18> a18;
  List<T19> a19;
  List<T20> a20;
}

class Tmp<T1, T2> { // Noncompliant
  void m() {
    try {} catch (Exception1 | Exception2 e) {} // not covered...
    
    Object o;
    org.foo.T1 t1 = (org.foo.T1) o;
    T2 t2 = new T2(o);
    T3 t3 = new <Integer> T3(o);
    T4 t4 = new T4<Integer>(o);
    T5<? extends T4> t5 = new T5<T4>(o);
    T6 t6 = (T6) o;
    T7 t7 = (T7) o;
    T8 t8 = (T8) o;
    T9 t9 = (T9) o;
    T10 t10 = (T10) o;
    T11 t11 = (T11) o;
    T12 t12 = (T12) o;
    T13 t13 = (T13) o;
    T14 t14 = (T14) o;
    T15 t15 = (T15) o;
    T16 t16 = new T16() {
      void foo() {}
    };
    T17[] t17 = new T17[3];
    boolean t18 = 0 instanceof T18;
  }
}

public class FullyQualifiedName1 extends sonar.source.support.a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.T4 {
  sonar.source.support.a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.T6 t6;
  another.pack.T6 othert6;
}

public class FullyQualifiedName2 { // Noncompliant
  a.T1 ta;
  b.T1 tb;
  c.T1 tc;
  d.T1 td;
  e.T1 te;
  f.T1 tf;
  g.T1 tg;
  h.T1 th;
  i.T1 ti;
  j.T1 tj;
  k.T1 tk;
  l.T1 tl;
  m.T1 tm;
  n.T1 tn;
  o.T1 to;
  p.T1 tp;
  q.T1 tq;
  r.T1 tr;
  s.T1 ts;
  t.T1 tt;
  u.T1 tu;
}
