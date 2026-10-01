package checks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

class PreparedStatementInsideLoopCheckSample {

  private static final String CONSTANT_SQL = "SELECT * FROM t WHERE id = ?";
  private String mutableSql = "SELECT * FROM t WHERE id = ?";

  void prepareStatementNoncompliant(Connection conn, List<Integer> ids) throws SQLException {
    for (int id : ids) {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM t WHERE id = ?"); // Noncompliant {{Move this "prepareStatement" call outside the loop.}}
      ps.setInt(1, id);
      ps.execute();
      ps.close();
    }

    int i = 0;
    while (i++ < ids.size()) {
      conn.prepareStatement("SELECT * FROM t WHERE id = ?"); // Noncompliant
    }

    for (int id : ids) {
      conn.prepareStatement(CONSTANT_SQL); // Noncompliant
    }

    String invariantSql = "SELECT * FROM t WHERE id = ?";
    for (int id : ids) {
      conn.prepareStatement(invariantSql); // Noncompliant
    }
  }

  void forInitializer(Connection conn) throws SQLException {
    for (PreparedStatement ps = conn.prepareStatement("SELECT 1"); ps != null; ) { // Compliant: initializer runs once
      break;
    }
    for (; conn.prepareStatement("SELECT 1") != null; ) { // Noncompliant
      break;
    }
  }

  void forEachExpression(Connection conn) throws SQLException {
    for (PreparedStatement p : List.of(conn.prepareStatement("SELECT 1"))) { // Compliant: expression evaluated once
      p.close();
    }
  }

  void forEachExpressionInsideOuterLoop(Connection conn, List<Integer> ids) throws SQLException {
    for (int id : ids) {
      for (PreparedStatement p : List.of(conn.prepareStatement("SELECT 1"))) { // Noncompliant
        p.close();
      }
    }
  }

  void compliant(Connection conn, List<Integer> ids) throws SQLException {
    PreparedStatement ps = conn.prepareStatement("SELECT * FROM t WHERE id = ?");
    for (int id : ids) {
      ps.setInt(1, id);
      ps.execute();
    }

    for (int id : ids) {
      conn.toString(); // not a prepareStatement call
    }
  }

  void sqlVariesPerIteration(List<String> sqls, Connection conn) throws SQLException {
    for (int i = 0; i < sqls.size(); i++) {
      String sql = sqls.get(i);
      conn.prepareStatement(sql); // Compliant - sql changes per iteration
    }
  }

  void mutableFieldSql(Connection conn, List<Integer> ids) throws SQLException {
    for (int id : ids) {
      conn.prepareStatement(mutableSql); // Compliant - non-final field may be mutated via member select or method call
    }
  }

  void localReassignedInLoop(Connection conn, List<Integer> ids) throws SQLException {
    String sql = "SELECT * FROM t WHERE id = ?";
    for (int id : ids) {
      sql = "SELECT * FROM t WHERE id = " + id; // reassigned each iteration
      conn.prepareStatement(sql); // Compliant - sql changes per iteration
    }
  }

  void doWhileLoop(Connection conn, List<Integer> ids) throws SQLException {
    int i = 0;
    do {
      conn.prepareStatement("SELECT * FROM t WHERE id = ?"); // Noncompliant
    } while (i++ < ids.size());
  }

  void nonConstantArg(Connection conn, List<String> sqls) throws SQLException {
    for (String sql : sqls) {
      conn.prepareStatement(sql.trim()); // Compliant - method call result is not a constant
    }
  }

  void nonIncrementUnaryInLoop(Connection conn, List<Integer> ids) throws SQLException {
    boolean inverse = false;
    for (int id : ids) {
      if (!inverse) { // non-increment unary expression
        conn.prepareStatement("SELECT * FROM t WHERE id = ?"); // Noncompliant
      }
    }
  }

  void nonIdentifierMutationsInLoop(Connection conn, List<Integer> ids) throws SQLException {
    int[] counters = new int[2];
    for (int id : ids) {
      counters[0] = id; // assignment to non-identifier target
      counters[1]++;    // increment on non-identifier target
      conn.prepareStatement("SELECT * FROM t WHERE id = ?"); // Noncompliant
    }
  }
}
