import java.sql.ResultSet;

class A{
  void foo() {
    ResultSet rs;
    rs.isLast(); // Noncompliant {{Remove this call to "isLast()".}}
    rs.afterLast();
  }
}
