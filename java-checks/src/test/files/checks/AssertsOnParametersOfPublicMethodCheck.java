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
    if (price3 < 0 || price3 > 10000) {
      throw new IllegalArgumentException("Invalid price: " + price3); // Compliant
    }
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
        assert a > 0;   // Noncompliant
        System.out.println("blah");
      }
    }.start();
  }
}

class C {

  public synchronized void put(String a, Integer b) {
    assert ((a != null) && (b != null)); // Noncompliant
    assert (find2(b) == null);
    assert (find1(a) == null);
  }

  public synchronized Integer find1(String a) {
    assert (a != null); // Noncompliant
    return 0;
  }

  public synchronized String find2(Integer b) {
    assert (b != null); // Noncompliant
    return "";
  }
}
