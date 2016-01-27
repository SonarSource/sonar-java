class A {
  public String foo() {
    //doSomething
  }
  private String foo; // Noncompliant [[sc=18;ec=21]] {{Rename the "foo" member.}}
  private String bAr; // Noncompliant
  private String qix;
  public String Bar() {
  }
  public String getQix() {
  }
  private plop(){ }
  private plop(int a){ }
}
