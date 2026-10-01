package checks;

import java.sql.Connection;
import java.util.List;

class PreparedStatementInsideLoopCheckSampleNonCompiling {
  void test(Connection conn, List<Integer> ids) {
    for (int id : ids) {
      conn.prepareStatement(unknownVar); // Compliant - unresolved symbol is not a variable symbol
    }
  }
}
