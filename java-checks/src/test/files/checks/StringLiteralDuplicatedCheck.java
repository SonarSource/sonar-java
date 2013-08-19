class A {
  @SupressWarnings("all") // Compliant
  @SupressWarnings("all")
  @SupressWarnings("all")
  @SupressWarnings("a")
  public void f() {
    System.out.println("a"); // Compliant
    System.out.println("b"); // Non-Compliant
    System.out.println("b");
    System.out.println("c"); // Non-Compliant
    System.out.println("c");
    System.out.println("c");
  }
}
