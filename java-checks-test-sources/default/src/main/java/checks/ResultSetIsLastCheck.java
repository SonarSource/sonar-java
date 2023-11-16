package checks;

import java.sql.ResultSet;

class ResultSetIsLastCheck {
  void foo(ResultSet rs) throws Exception {
    rs.isLast(); // Noncompliant [[sc=8;ec=14]] {{Remove this call to "isLast()".}}
    rs.afterLast();
    this.isLast(); // Compliant
  }

  void isLast() { }
}
