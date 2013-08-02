class A {

  {
    return;
  }

  public A() {
    return null;        // Compliant
  }

  public void f() {     // Compliant
    return;
  }

  public int[] f() {
    return null;        // Non-Compliant
    return a;           // Compliant
  }

  public Object f() {
    return null;        // Compliant
  }

  public Object f()[] {
    return null;        // Non-Compliant
  }

  public int[] f() {
    new B() {
      public Object g() {
        return null;    // Compliant
      }

      public int[] g() {
        return null;    // Non-Compliant
      }
    };

    return new int[0];  // Compliant
    return null;        // Non-Compliant
  }

  public List f() {
    return null;        // Non-Compliant
  }

  public ArrayList f() {
    return null;        // Non-Compliant
  }

  public Set<Integer> f() {
    return Collections.EMPTY_SET;
    return null;        // Non-Compliant
  }

  public <T> List<Integer>[] f() {
    return null;        // Non-Compliant
  }

  public java.util.Collection f() {
    return null;        // Non-Compliant
  }

  public int f() {
    return null;        // Compliant
  }

}
