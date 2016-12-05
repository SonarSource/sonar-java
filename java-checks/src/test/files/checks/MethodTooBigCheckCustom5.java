class A {
  public A() { // Noncompliant {{This method has 6 lines, which is greater than the 5 lines authorized. Split it into smaller methods.}}
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
  }

  public void f() { // 1
    System.out.println("");
    System.out.println("");
    System.out.println("");
  }
}
