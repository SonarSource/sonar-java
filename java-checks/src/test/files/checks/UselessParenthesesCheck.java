class Foo {
  void foo() {
    return 3;             // Compliant
    return (x);           // Non-Compliant
    return (x + 1);       // Non-Compliant
    int x = (y / 2 + 1);  // Non-Compliant
    int y = (4+X) * y;    // Compliant

    if (0) {              // Compliant
    }

    System.out.println(false ? (true ? 1 : 2) : 2); // Was previously noncompliant
    System.out.println(false ? 0 : (true ? 1 : 2)); // Was previously compliant
    invoke(((Cast)plop));
    System.out.println((false) ? 0 : 1); // Compliant : Do not check for useless parenthesis on condition
    int[] tab;
    tab[(1+2)];
    tab[(1+2)/2];
    A a = new A((1/3));
    return (((x & 0x0000FFFF)) | y);
    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
  }
    public static final short value = (short)(0);
}
