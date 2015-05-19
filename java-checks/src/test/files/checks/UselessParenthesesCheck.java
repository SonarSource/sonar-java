class Foo {
  void foo() {
    return 3;             // Compliant
    return (x);           // Noncompliant {{Remove those useless parentheses.}}
    return (x + 1);       // Noncompliant
    int x = (y / 2 + 1);  // Noncompliant
    int y = (4+X) * y;    // Compliant

    if (0) {              // Compliant
    }
    // Was previously noncompliant
    System.out.println(false ? (true ? 1 : 2) : 2); // Noncompliant
    // Was previously compliant
    System.out.println(false ? 0 : (true ? 1 : 2)); // compliant
    invoke(((Cast)plop)); // Noncompliant
    //Do not check for useless parenthesis on condition
    System.out.println((false) ? 0 : 1); // compliant
    int[] tab;
    tab[(1+2)]; // Noncompliant
    tab[(1+2)/2];
    A a = new A((1/3)); // Noncompliant
    return (((x & 0x0000FFFF)) | y); // Noncompliant 2
    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
  }
    public static final short value = (short)(0);
}
