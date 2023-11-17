package checks;

class MethodTooBigCheckCustom4 {
  public MethodTooBigCheckCustom4() { // Noncompliant [[sc=10;ec=34]] {{This method has 6 lines, which is greater than the 4 lines authorized. Split it into smaller methods.}}
    System.out.println("");
    // This does not count as a line
    // This does not count as a line
    System.out.println("");
    // This does not count as a line
    System.out.println("");
    // This does not count as a line
    System.out.println("");
  }

  public void f() { // Noncompliant {{This method has 5 lines, which is greater than the 4 lines authorized. Split it into smaller methods.}}
    System.out.println("");
    // This does not count as a line




    // This does not count as a line
    System.out.println("");
    System.out.println("");
  }
}
