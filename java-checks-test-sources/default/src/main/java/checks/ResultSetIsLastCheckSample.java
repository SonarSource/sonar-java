package checks;

import java.sql.ResultSet;

class ResultSetIsLastCheckSample {
  void foo(ResultSet rs) throws Exception {
    rs.isLast(); // Noncompliant {{Remove this call to "isLast()".}}
//     ^^^^^^
    rs.afterLast();
    this.isLast(); // Compliant
  }

  void isLast() { }
}
