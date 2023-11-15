package checks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

abstract class PreparedStatementAndResultSetCheck extends UnknownClassFromSamePackage {
  abstract PreparedStatement getPreparedStatement(String s);
  void unknownQuery() throws SQLException {
    PreparedStatement ps = getPreparedStatement(UNKNOWN_QUERY_FROM_UNKNOWN_PARENT); // Compliant
    ps.setDouble(2, 0.0);
  }

  public class Example {

    private final String REQUETE_SELECT_RESA_RESEAU = "SELECT COLUMN1 FROM TABLE";

    private final String CLAUSE_ETAT = " WHERE COLUMN2 = ?";

    public Example() {
    }

    public synchronized void method1() {
      String req= REQUETE_SELECT_RESA_RESEAU;
      req = req + CLAUSE_ETAT; //StackOverflowError
      PreparedStatement pstmt= m_con.prepareStatement(req);
      pstmt.setInt(1,10);
      ResultSet rs=pstmt.executeQuery();

    }
  }
}
