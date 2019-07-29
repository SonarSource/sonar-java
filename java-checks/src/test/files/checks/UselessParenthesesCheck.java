class Foo {
  void foo() {
    return 3;             // Compliant
    return (x);           // Compliant
    return ((x));         // Noncompliant [[sc=13;ec=14;secondary=5]] {{Remove these useless parentheses.}}
    return (x + 1);       // Compliant
    return ((x + 1));     // Noncompliant [[sc=13;ec=14;secondary=7]] {{Remove these useless parentheses.}}
    int x = (y / 2 + 1);  // Compliant
    int x2 = ((y / 2 + 1));  // Noncompliant [[sc=15;ec=16;secondary=9]] {{Remove these useless parentheses.}}
    int y = (4+X) * y;    // Compliant
    int y2 = 4+(X * y);    // Compliant

    if (0) {              // Compliant
    }

    System.out.println(false ? (true ? 1 : 2) : 2); // compliant
    System.out.println(false ? 0 : (true ? 1 : 2)); // compliant
    System.out.println(false ? 0 : (1)); // Compliant
    System.out.println(false ? 0 : ((1))); // Noncompliant
    //Do not check for useless parenthesis on condition
    System.out.println((false) ? 0 : 1); // Compliant
    System.out.println(((false)) ? 0 : 1); // Noncompliant
    System.out.println(((true == false)) ? 0 : 1); // Noncompliant
    System.out.println((false ? false:true) ? 0 : 1); // compliant
    System.out.println((foo()) ? 0 : 1); // Compliant
    System.out.println(((foo())) ? 0 : 1); // Noncompliant
    System.out.println((foo) ? 0 : 1); // Compliant
    System.out.println(((foo)) ? 0 : 1); // Noncompliant
    invoke(((Cast)plop)); // Compliant
    invoke((((Cast)plop))); // Noncompliant
    int[] tab;
    tab[(1+2)] = 0; // Compliant
    tab[((1+2))] = 0; // Noncompliant
    tab[(1+2)/2] = 0;
    A a = new A((1/3)); // Compliant
    A a2 = new A(((1/3))); // Noncompliant
    return (( // Compliant
      (x & 0x0000FFFF)) | y); // Noncompliant
    getContentSpec(((int[])contentSpec.value)[0], contentSpec);

    this.a = b; // Compliant
    this.a = (b); // Compliant
    this.a = ((b)); // Noncompliant
    this.a = (true ?  1 : 2); // Compliant
    this.a = ((true ?  1 : 2)); // Noncompliant
    this.a = false ? (true ? 1 : 2) : 2; // Compliant
    this.a = (1+2)/2; // Compliant
    this.a = ((int[])contentSpec.value)[0]; // Compliant
    Object[] foo = {(true?1:2)}; // Compliant
    Object[] foo2 = {((true?1:2))}; // Noncompliant
  }
    public static final short value = (short)(0);
}
