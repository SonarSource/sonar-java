class A {

  public void foo() {
    foo.equals(null); // Noncompliant [[sc=5;ec=21]] {{Use "object == null" instead of "object.equals(null)" to test for nullity to prevent null pointer exceptions.}}
    foo.equals(0);
    equals(null);
    foo.equals(null, 0);
  }

}
