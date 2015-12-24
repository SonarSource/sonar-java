import java.sql.ResultSet;

class A{
  void foo() {
    ResultSet rs;
    rs.isLast(); // Noncompliant [[sc=8;ec=14]] {{Remove this call to "isLast()".}}
    rs.afterLast();
  }
}
