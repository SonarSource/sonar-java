package checks;

import java.math.BigDecimal;

class BigDecimalEqualsCheckGuavaSample {

  void method(BigDecimal a, BigDecimal b, Object o, String s) {
    boolean res;

    res = com.google.common.base.Objects.equal(a, b); // Noncompliant [["BigDecimal.equals()" compares scale as well as value; use "compareTo() == 0" for numerical comparison.]]
//                                       ^^^^^
    res = com.google.common.base.Objects.equal(a, o); // Noncompliant
    res = com.google.common.base.Objects.equal(o, a); // Noncompliant

    // Compliant
    res = com.google.common.base.Objects.equal(s, "hello");
  }

  static class AccountWithGuavaEquals {
    private BigDecimal balance;

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof AccountWithGuavaEquals other)) return false;
      return com.google.common.base.Objects.equal(balance, other.balance); // Compliant: inside equals method override
    }

    @Override
    public int hashCode() {
      return com.google.common.base.Objects.hashCode(balance);
    }
  }
}
