package checks;

import java.util.List;

class IndexOfWithPositiveNumber {

  public static final int literal1 = 0;

  void method(int length, List<String> strings, int literal3) {
    if ("".indexOf(" ") > literal3) { // Compliant
    }
    int literal4 = 2;
    literal4 = 0;
    if ("".indexOf(" ") > literal4) { // Noncompliant
    }

    if ("".indexOf(' ') > literal1) { // Noncompliant
    }
    final int literal2 = 0;
    if ("".indexOf(" ") > literal2) { // Noncompliant
    }
    if ("".indexOf(" ") > getLiteral0()) { // Compliant - do not check
    }
    if ("".indexOf(" ") > getLiteral()) { // Compliant - do not check
    }
    if (length > 0) { // Compliant
    }
    if (length < length) { // Compliant
    }
    if ("".length() > 0) {// Compliant
    }
    if ("".indexOf("") > -1) { // Compliant
    }

    if ("".indexOf(' ') > 0) { // Noncompliant [[sc=9;ec=28]] {{0 is a valid index, but is ignored by this check.}}
    }
    if ("".indexOf(" ") > 0) { // Noncompliant {{0 is a valid index, but is ignored by this check.}}
    }
    if (strings.indexOf("") > 0) { // Noncompliant {{0 is a valid index, but is ignored by this check.}}
    }

    if ("".indexOf("") >= -1) { // Compliant
    }
    if ("".indexOf("") >= 0) { // Compliant
    }
    if ("".indexOf("") > 0) { // Noncompliant {{0 is a valid index, but is ignored by this check.}}
    }
    if ("".indexOf("") >= 1) { // Compliant
    }
    if ("".indexOf("") >= 2) { // Compliant
    }

    if (-1 <= "".indexOf("")) { // Compliant
    }
    if (0 <= "".indexOf("")) { // Compliant
    }
    if (0 < "".indexOf("")) { // Noncompliant {{0 is a valid index, but is ignored by this check.}}
    }
    if (1 <= "".indexOf("")) { // Compliant
    }
    if (2 <= "".indexOf("")) { // Compliant
    }
  }

  private int getLiteral() {
    return 2;
  }

  private int getLiteral0() {
    return 0;
  }

}
