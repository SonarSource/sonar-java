class A {
  private String foo; // Noncompliant {{Rename the "foo" member.}}
  private String bAr; // Noncompliant
  private String qix;
  public String foo() {
    //doSomething
  }
  public String Bar() {
  }
  public String getQix() {
  }
  private plop(){ }
  private plop(int a){ }
}
