import java.sql.ResultSet;

class A{
  void foo() {
    ResultSet rs;
    rs.isLast(); //NonCompliant
    rs.afterLast();
  }
}