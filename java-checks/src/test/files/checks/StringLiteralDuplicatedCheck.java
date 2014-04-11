class A {
  @SupressWarnings("allall") // Compliant
  @SupressWarnings("allall")
  @SupressWarnings("allall")
  @SupressWarnings("aaaaa")
  public void f() {
    System.out.println("aaaaa"); // Compliant
    System.out.println("bbbbb"); // Compliant because less than threshold
    System.out.println("bbbbb");
    System.out.println("ccccc"); // Non-Compliant
    System.out.println("ccccc");
    System.out.println("ccccc");
    System.out.println("dddd"); // Compliant - too short
    System.out.println("dddd");
  }
}
