package checks;

class CompareToResultTestCheckSample {
  class MyComparable implements Comparable<MyComparable> {
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int MINUS_ONE = -1;

    int compareToField = compareTo(new MyComparable());

    public int compareTo(MyComparable other) {
      return 0;
    }

    int field;

    public void aMethod(MyComparable other, NotComparable notComparable) {
      if (compareTo(other) == -1) {} // Noncompliant [[sc=28;ec=30]] {{Only the sign of the result should be examined.}}
      if (compareTo(other) == (-1)) {} // Noncompliant
      if (compareTo(other) == -5) {} // Noncompliant
      if (compareTo(other) == 0) {}
      if (compareTo(other) == 1) {} // Noncompliant
      if (compareTo(other) != 1) {} // Noncompliant
      if (compareTo(other) == 1L) {} // Noncompliant
      if (compareTo(other) == 01) {} // Noncompliant
      if (compareTo(other) == (1 + 2 + 3) * 0) {}
      if (compareTo(other) == ZERO) {}
      if (compareTo(other) == ONE) {} // Noncompliant
      if (compareTo(other) == 0x0) {}
      if (compareTo(other) == 0b0) {}
      if (compareTo(other) == 0_0) {}
      if (compareTo(other) == -0) {}

      if (other.compareTo(this) == 1) {} // Noncompliant
      if (-1 == compareTo(other)) {} // Noncompliant
      if ((-1) == compareTo(other)) {} // Noncompliant
      boolean result = compareTo(other) == -1; // Noncompliant
      if (notComparable.compareTo(other) == 1) {}
      if (compareTo(other) == hashCode()) {}
      if (compareTo(other) == - hashCode()) {}
      if (compareTo(other, other) == 1) {}
      //false positive:
      if (1 == compareTo(notComparable)) {} // Noncompliant
      if (0 == compareTo(other)) {}

      int c1 = compareTo(other);
      if (c1 == 1) {} // Noncompliant

      int c2 = compareTo(other);
      c2 = compareTo(other, other);
      if (c2 == 1) {}

      int c3 = compareTo(other);
      c3++;
      if (c3 == 1) {}

      int c5 = 1;
      if (c5 == 1) {}

      int c6 = compareTo(other, other);
      if (c6 == 1) {}

      int c7 = compareTo(other);
      (c7)++;
      if (c7 == 1) {}

      int c8 = compareTo(other);
      (c8) = 2;
      if (c8 == 1) {}

      if (compareTo(other) + 1 == 1) {}
      if (compareToField == 1) {}

      this.field = compareTo(other);
      this.field++;
      if (this.field == 1) {}
    }

    public int compareTo(NotComparable o2) {
      return 0;
    }

    public int compareTo(MyComparable other1, MyComparable other2) {
      return 0;
    }

    public void quickFixes(MyComparable other, NotComparable notComparable) {
      if (compareTo(other) == 1) {} // Noncompliant [[sc=28;ec=30;quickfixes=qf1]]
      // fix@qf1 {{Replace with "> 0"}}
      // edit@qf1 [[sc=28;ec=32]] {{> 0}}

      if (compareTo(other) == -1) {} // Noncompliant [[sc=28;ec=30;quickfixes=qf2]]
      // fix@qf2 {{Replace with "< 0"}}
      // edit@qf2 [[sc=28;ec=33]] {{< 0}}

      if (1 == compareTo(notComparable)) {} // Noncompliant [[sc=13;ec=15;quickfixes=qf3]]
      // fix@qf3 {{Replace with "0 <"}}
      // edit@qf3 [[sc=11;ec=15]] {{0 <}}
      if (-1 == compareTo(notComparable)) {} // Noncompliant [[sc=14;ec=16;quickfixes=qf4]]
      // fix@qf4 {{Replace with "0 >"}}
      // edit@qf4 [[sc=11;ec=16]] {{0 >}}

      if (compareTo(other) == (-1)) {} // Noncompliant [[sc=28;ec=30;quickfixes=qf5]]
      // fix@qf5 {{Replace with "< 0"}}
      // edit@qf5 [[sc=28;ec=35]] {{< 0}}

      if (compareTo(other) == MINUS_ONE) {} // Noncompliant [[sc=28;ec=30;quickfixes=qf6]]
      // fix@qf6 {{Replace with "< 0"}}
      // edit@qf6 [[sc=28;ec=40]] {{< 0}}

      if (compareTo(other) == -MINUS_ONE) {} // Noncompliant [[sc=28;ec=30;quickfixes=qf7]]
      // fix@qf7 {{Replace with "> 0"}}
      // edit@qf7 [[sc=28;ec=41]] {{> 0}}

      if (compareTo(other) == -(-1)) {} // Noncompliant [[sc=28;ec=30;quickfixes=qf8]]
      // fix@qf8 {{Replace with "> 0"}}
      // edit@qf8 [[sc=28;ec=36]] {{> 0}}

      // For !=, even if we could in theory replace by <=/>= 0, we do not suggest quick fixes and let the user figure out what was his intent
      if (1 != compareTo(notComparable)) {} // Noncompliant [[sc=13;ec=15;quickfixes=!]]
      if (-1 != compareTo(notComparable)) {} // Noncompliant [[sc=14;ec=16;quickfixes=!]]
      if (compareTo(other) != 1) {} // Noncompliant [[sc=28;ec=30;quickfixes=!]]
      if (compareTo(other) != -1) {} // Noncompliant [[sc=28;ec=30;quickfixes=!]]
    }

  }

  class NotComparable {

    public int compareTo(MyComparable other) {
      return 0;
    }

    public void aMethod(MyComparable other) {
      if (compareTo(other) == -1) {}
    }

  }
}
