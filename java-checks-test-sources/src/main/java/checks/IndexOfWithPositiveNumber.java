package checks;

import java.util.List;

class IndexOfWithPositiveNumber {
  void method(int length, List<String> strings) {

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
}
