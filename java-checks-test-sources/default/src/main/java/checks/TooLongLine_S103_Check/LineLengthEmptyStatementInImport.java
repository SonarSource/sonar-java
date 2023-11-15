package checks.TooLongLine_S103_Check;

import checks.TooLongLine_S103_Check.
                        very_very_very.big.VeryBig;

class LineLength2 {
  void method() {
    // Noncompliant {{Split this 97 characters long line (which is greater than 40 authorized).}}
    // Noncompliant {{Split this 97 characters long line (which is greater than 40 authorized).}}
  }
}
