abstract class A {
  int x; Point myPoint;

  void foo(Object... objects) {
    foo(bar("42"), bar("42")); // Noncompliant [[sc=20;ec=29]] {{Verify that this is the intended value; it is the same as the 1st argument.}}
    foo(1, bar(), 42, bar());  // Noncompliant [[sc=23;ec=28]] {{Verify that this is the intended value; it is the same as the 2nd argument.}}
    foo(-bar(), -bar());       // Noncompliant [[sc=17;ec=23]] {{Verify that this is the intended value; it is the same as the 1st argument.}}

    foo(x, x, x, x); // Compliant

    foo(myPoint.x,
        myPoint.x,  // Noncompliant [[secondary=11]] {{Verify that this is the intended value; it is the same as the 1st argument.}}
        myPoint.x,  // Noncompliant [[secondary=11]] {{Verify that this is the intended value; it is the same as the 1st argument.}}
        myPoint.x); // Noncompliant [[secondary=11]] {{Verify that this is the intended value; it is the same as the 1st argument.}}

    foo(0,1,myPoint.x,myPoint.x);   // Noncompliant [[sc=23;ec=32]] {{Verify that this is the intended value; it is the same as the 3rd argument.}}
    foo(0,1,2,myPoint.x,myPoint.x); // Noncompliant [[sc=25;ec=34]] {{Verify that this is the intended value; it is the same as the 4th argument.}}

    foo(myPoint.x, (myPoint.x)); // Noncompliant

    foo(bar("42"), bar("0"));
    foo(new A(), new A());
    foo((byte) 0, (byte) 0);

    A a = new A();
    qix(myPoint.x, myPoint.x); // Noncompliant

    // literals
    foo(true, true);
    foo(1, 1);
    foo("bar", "bar");

    unknown(x, x);
    gul(new A(), new A());
  }

  abstract int bar(Object... objects);
  abstract int gul(Object o, A a);
  abstract int qix(Object o, Integer ... a);
}

class Point { int x, y; }
