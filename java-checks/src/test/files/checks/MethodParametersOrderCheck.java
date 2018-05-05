class A {
  int a;

  public double divide(int divisor, int dividend) {
    return divisor / dividend;
  }

  public void alpha(int a) {
  }
  public void beta(Unknown e, String b) {
  }
  
  public void doTheThing() {
    Unknown u, A;
    alpha(u);
    alpha(A);
    int divisor = 15;
    int dividend = 5;
    double result = divide(dividend, divisor); // Noncompliant [[sc=27;ec=46;secondary=4,4]] {{Parameters to divide have the same names but not the same order as the method arguments.}}
    double result1 = divide(new B().alpha, divisor); // Compliant
    double result2 = divide(new B().dividend, divisor); // Noncompliant [[sc=28;ec=55;secondary=4,4]] {{Parameters to divide have the same names but not the same order as the method arguments.}}
    Unknown e;
    String b ="";
    beta(e, b);   // Compliant
    beta(b, e);    // Compliant
    beta(a,b); // Compliant
    // coverage

    alpha(divisor); // Compliant
    alpha(e);   // Compliant
    int a = 2;
    alpha(a); // Compliant
  }

  public void doTheThing2() {
    int divisor = 15;
    int dividend = 5;
    double result = divide(divisor, dividend); // Compliant
    foo(dividend); // Compliant
  }

  int fun() {
    return 0;
  }

  void test(int parameter1, int parameter2) {
    int divisOr = 15;
    int dividend = 5;
    String a = "1", b = "2";
    double result = divide(dividend, divisOr); // Noncompliant {{Parameters to divide have the same names but not the same order as the method arguments.}}

    result = divide(divisOr, dividend); // Compliant
    result = divide(parameter1, parameter2); // Compliant
    result = divide(fun(), dividend); // Compliant

    // coverage
    fun();

    // in the cases below we expect them not to raise an issue, because java.util.Objects.equals is declared in a different file
    // even though equals is defined: java.util.Objects.equals(a, b);
    java.util.Objects.equals(divisOr, dividend); // Compliant
    java.util.Objects.equals(b, a); // Compliant
  }

  private void insert(Integer entry, Integer oldEntryForKey) {
  }

  private void foo(int a) {
  }

  public void test1() {
    int a = 0, b = 1, c=2;
    int newEntry = 0;
    int oldEntryForKey = 0;
    int entry = 0;
    insert(newEntry, oldEntryForKey); // Compliant
    insert(entry, entry); // Compliant
    new B().test2(newEntry, entry); // Compliant
    new B().test2(a, b); // Compliant
    new B().test2(b, a); // Noncompliant
    new B().test2(a, b, entry); // Compliant
    new B().test2(c, b, a); // Noncompliant [[secondary=101,103]]
  }

  public void foo2() {
    int b = 0;
    String a = "";
    new B().test2(b, a); // Compliant
  }
}

class B {
  int alpha = 0, dividend = 1;
  String b = "";
  Integer upperBound = 0;

  public void test2(int a, int b) {
  };

  public void test2(
    int a,
    int b,
    int c) {
  };

  public void test2(int a, String b) {
  };

  public void test2(int a, Object b, Object c, int d) {
  };

  public void test2(int a, Object b, String c, int d) {
  }

  public void foo(String a, int b) {
    test2(b, a); // Compliant
    test2(new A().a, new B().b); // Compliant
  }

  private void strange(Integer lowerBound, Integer upperBound, String value) {
  }

  void foo2(int a, int d) {
    test2(d, toString(), toString(), a); // Noncompliant
    test2(d, toString(), a, 6); // Noncompliant
    test2(d, toString(), a, Integer.parseInt(toString())); // Noncompliant
    test2(d, 0, a, 1); // Noncompliant
    int c = 1;
    String A = ((Integer) a).toString();
    test2(d, null, A, Integer.parseInt(new B().b)); // Compliant -- because b is int not Object
    strange(new B().upperBound, Integer.valueOf(0), A); // Compliant
    strange(new B().upperBound, Integer.valueOf(0), null); // Compliant if only 1 argument matches a parameter in a different position
  }
}
