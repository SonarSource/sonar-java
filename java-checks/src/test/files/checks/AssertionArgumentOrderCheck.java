import static org.junit.Assert.*;
class A {
  static final String CONSTANT = "";
  void fun() {
    assertEquals(0, new A().actual());
    assertEquals(new A().actual(), 0); // Noncompliant [[sc=36;ec=37;secondary=6]]
    assertEquals("message", new A().actual(), 0); // Noncompliant
    assertEquals("message", 0, new A().actual());
    assertEquals("message", "constantString", actualObject());
    assertEquals("message", actualObject(), "constantString"); // Noncompliant
    assertSame(0, new A().actual());
    assertSame(new A().actual(), 0); // Noncompliant
    assertNotSame(new A().actual(), 0); // Noncompliant
    assertNotSame("message", new A().actual(), 0); // Noncompliant

    //assert equals with double/float and delta
    assertEquals("message", 0d, actualDouble(), 1d);
    assertEquals(0d, actualDouble(), 1d);
    assertEquals(actualDouble(), 0.0d, 1d); // Noncompliant {{Swap these 2 arguments so they are in the correct order: expected value, actual value.}}
    assertEquals("message", actualDouble(), 0.0d, 1d); // Noncompliant
    assertEquals(actualDouble(), 0.0d); // Noncompliant

    assertEquals("message", 0f, actualFloat(), 1f);
    assertEquals(0f, actualFloat(), 1f);
    assertEquals(actualFloat(), 0.0f, 1f); // Noncompliant {{Swap these 2 arguments so they are in the correct order: expected value, actual value.}}
    assertEquals("message", actualFloat(), 0.0f, 1f); // Noncompliant
    assertEquals(actualFloat(), 0.0f); // Noncompliant


    assertEquals("message", actualObject(), CONSTANT); // Noncompliant
    assertEquals("message", actualObject(), A.CONSTANT); // Noncompliant
    assertEquals("message", A.CONSTANT, actualObject());
  }

  int actual() {
    return 0;
  }
  double actualDouble() {
    return 0;
  }
  float actualFloat() {
    return 0;
  }
  int actualObject() {
    return 0;
  }

  public void testMethod() throws Exception {
    MyBean bean = new MyBean();

    assertEquals(1, bean.getDouble(), 0);
    assertEquals("no FP here", 1, bean.getDouble(), 0);
  }

  static class MyBean {
    public double getDouble() {
      return 1;
    }
  }
}
