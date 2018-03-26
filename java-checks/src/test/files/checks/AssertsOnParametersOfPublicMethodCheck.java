class A {

  public void setPrice(int price1) {
    assert price1 >= 0 && price1 <= 10000; // Noncompliant [[sc=5;ec=43]] {{Replace this assert with a proper check.}}
  }

  public int getPrice(int a) {
    int price2 = 10;
    assert price2 >= 0 && price2 <= 10000; // Compliant
    return 0;
  }

  public void setPrice1(int price3) {
    if (true) {
      assert price3 > 1000; // Noncompliant
    }
  }

  private void setPrice2(int price4) {
    assert price4 >= 0 && price4 <= 10000; // Compliant
  }

  public void foo(Object a) {
    new B() {
      void Bar() {
        assert a != null; // Noncompliant
      }
    };
    if (a != null) {
      // ...
    }

    assert getPrice(5) > 1; // Compliant
    assert getPrice(5) > 1 || a != null; // Noncompliant

  }
}

class B {

  public void foo(int a) {
    new Thread() {
      public void run() {
        assert a > 0; // Noncompliant
        System.out.println("blah");
      }
    }.start();
  }
}

class C {

  public C(int c) {
    assert c > 0; // Noncompliant
    // ...
  }

  public synchronized void put(String a) {
    assert ((a != null) && (b != null)); // Noncompliant
    assert (find1(a) == null); // Noncompliant
  }

  public synchronized Integer find1(String a) {
    assert (a != null); // Noncompliant
    return 0;
  }

  private static class D {

    public D(int a) {
      assert a > 0; // Noncompliant

    }
  }
}
