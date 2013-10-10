class Foo { // Noncompliant - depends on too many other classes
  T1 a1;    // Foo is coupled to T1
  T2 a2;    // Foo is coupled to T2
  T3 a3;    // Foo is coupled to T3
  T4 a4;    // etc.
  T5 a6;
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
  T21 a21;   // Counted just once
  T21 a21;   // Counted just once
  T21 a21;   // Counted just once
}

class Bar {  // Compliant
  T1 a1;
  T2 a2;
}

class Baz {  // Noncompliant
  T1 a1;
  T2 a2;
  T3 a3;
  T4 a4;
  T5 a6;
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

class Foo { // Compliant
  class Bar { // Noncompliant
    T1 a1;
    T2 a2;
    T3 a3;
    T4 a4;
    T5 a6;
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

class Foo { // Noncompliant
  T1 a1;
  T2 a2;
  T3 a3;
  T4 a4;
  T5 a6;
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

enum Foo { // Compliant - should not fail
  ;

  T1 foo() {
  }
}

class Foo {  // Compliant
  T1 a1;
  T2 a2;
  T3 a3;
  T4 a4;
  T5 a6;
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

class Foo { // Noncompliant
  List<T1> a1;
  List<T2> a2;
  List<T3> a3;
  List<T4> a4;
  List<T5> a6;
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
