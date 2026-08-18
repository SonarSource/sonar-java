package checks;

import java.math.BigDecimal;
import java.util.Objects;

class BigDecimalEqualsCheckSample {

  void method(BigDecimal a, BigDecimal b, Object o, String s) {
    boolean res;

    res = a.equals(b); // Noncompliant [["BigDecimal.equals()" compares scale as well as value; use "compareTo() == 0" for numerical comparison.]]
//          ^^^^^^
    res = !a.equals(b); // Noncompliant
//           ^^^^^^
    res = a.equals(o); // Noncompliant
    res = o.equals(a); // Noncompliant

    res = Objects.equals(a, b); // Noncompliant
//                ^^^^^^
    res = Objects.equals(a, o); // Noncompliant
    res = Objects.equals(o, a); // Noncompliant
    res = com.google.common.base.Objects.equal(a, b); // Noncompliant
//                                       ^^^^^
    res = com.google.common.base.Objects.equal(a, o); // Noncompliant

    // Compliant
    res = a.compareTo(b) == 0;
    res = a.compareTo(b) != 0;
    res = s.equals("hello");
    res = Objects.equals(s, "hello");
    res = com.google.common.base.Objects.equal(s, "hello");
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
