package checks;

import java.math.BigDecimal;
import java.util.Objects;

class BigDecimalEqualsCheckSample {

  private static final int SCALE = 2;

  void method(BigDecimal a, BigDecimal b, Object o, String s, int runtimeScaleA, int runtimeScaleB) {
    boolean res;

    res = a.equals(b); // Noncompliant [["BigDecimal.equals()" compares scale as well as value; use "compareTo() == 0" for numerical comparison.]]
//          ^^^^^^
    res = !a.equals(b); // Noncompliant
//           ^^^^^^
    res = a.equals(o); // Noncompliant

    res = Objects.equals(a, b); // Noncompliant
//                ^^^^^^
    res = Objects.equals(a, o); // Noncompliant
    res = Objects.equals(o, a); // Noncompliant
    res = a.setScale(2).equals(b); // Noncompliant
    res = a.equals(b.setScale(2)); // Noncompliant
    res = a.setScale(2).equals(b.setScale(3)); // Noncompliant
    res = a.setScale(runtimeScaleA).equals(b.setScale(runtimeScaleB)); // Noncompliant
    res = Objects.equals(a.setScale(2), b); // Noncompliant

    // Compliant
    res = a.compareTo(b) == 0;
    res = a.compareTo(b) != 0;
    res = o.equals(a);
    res = s.equals(a);
    res = s.equals("hello");
    res = Objects.equals(s, "hello");
    res = Objects.equals(null, s);
    res = Objects.equals(s, null);
    res = Objects.equals(null, a);
    res = Objects.equals(a, null);
    res = Objects.equals((null), s);
    res = a.setScale(2).equals(b.setScale(2));
    res = a.setScale(2, java.math.RoundingMode.UP).equals(b.setScale(2, java.math.RoundingMode.DOWN));
    res = a.setScale(SCALE, java.math.RoundingMode.HALF_UP).equals(b.setScale(SCALE, java.math.RoundingMode.HALF_UP));
    res = (a.setScale(2)).equals((b.setScale(2)));
    res = Objects.equals(a.setScale(2), b.setScale(2));
  }

  static class Account {
    private BigDecimal balance;

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Account other)) return false;
      return balance != null && balance.equals(other.balance); // Compliant: inside equals method override
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(balance);
    }
  }

  static class AccountWithStaticEquals {
    private BigDecimal balance;

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof AccountWithStaticEquals other)) return false;
      return Objects.equals(balance, other.balance); // Compliant: inside equals method override
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(balance);
    }
  }

  static class MyBigDecimal extends BigDecimal {
    public MyBigDecimal(String val) {
      super(val);
    }

    void testCustom(MyBigDecimal other) {
      boolean r = this.equals(other); // Noncompliant
    }
  }
}
