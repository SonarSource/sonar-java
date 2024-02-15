package checks;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.sql.Date;
import java.util.List;

public class PreparedStatementLoopInvariantCheckSample {

  public void basicCase1(PreparedStatement preparedStatement, List<Order> orders) throws SQLException {
    Date today = java.sql.Date.valueOf(LocalDate.now());
    for(Order order: orders) {
      preparedStatement.setDate(0, today); // Noncompliant [[sc=7;ec=42;secondary=-1]] {{Move this loop-invariant setter invocation out of this loop.}}
      preparedStatement.executeUpdate();
    }
  }

  public void basicCase2(PreparedStatement preparedStatement, List<Order> orders) throws SQLException {
    Date today = java.sql.Date.valueOf(LocalDate.now());
    for(Order order: orders) {
      preparedStatement.setDate(0, today); // Compliant
      preparedStatement.executeUpdate();
      today = java.sql.Date.valueOf(LocalDate.now());
    }
  }

  public void basicCase3(PreparedStatement preparedStatement, List<Order> orders) throws SQLException {
    for(Order order: orders) {
      Date today = java.sql.Date.valueOf(LocalDate.now());
      preparedStatement.setDate(0, today); // Compliant
      preparedStatement.executeUpdate();
    }
  }

  public void basicCase4(NoPreparedStatement noPreparedStatement, List<Order> orders) throws SQLException {
    Date today = java.sql.Date.valueOf(LocalDate.now());
    for(Order order: orders) {
      noPreparedStatement.setDate(0, today); // Compliant
      noPreparedStatement.executeUpdate();
    }
  }

  public void someOtherFunctions(PreparedStatement preparedStatement, List<Order> orders, Blob blob) throws SQLException {
    preparedStatement.setBlob(1, blob); // Compliant
    preparedStatement.setInt(2, 23); // Compliant
    preparedStatement.setString(3, "Text"); // Compliant
    preparedStatement.setCursorName("Name"); // Compliant
    preparedStatement.setInt(4, constant); // Compliant
    preparedStatement.setInt(5, noConstant); // Compliant
    for(Order order: orders) {
      preparedStatement.setBlob(1, blob); // Noncompliant
      preparedStatement.setInt(2, 23); // Noncompliant
      preparedStatement.setString(3, "Text"); // Noncompliant
      preparedStatement.setCursorName("Name"); // Noncompliant
      preparedStatement.setInt(4, constant); // Noncompliant
      preparedStatement.setInt(5, noConstant); // Noncompliant
    }
  }

  public void checkNonVarOrFieldAssignment(
    PreparedStatement preparedStatement,
    List<Order> orders,
    String[] strings,
    String[] moreStrings
  ) throws SQLException {
    preparedStatement.setString(0, strings[1]); // Compliant
    preparedStatement.setString(1, moreStrings[2]); // Compliant
    for(Order order: orders) {
      preparedStatement.setString(0, strings[1]); // Compliant
      preparedStatement.setString(1, moreStrings[2]); // Compliant
      strings[1] = "foo";
      strings[2] = "bar";
    }
  }

  private static final int constant = 23;

  private int noConstant = getLength();

  private int getLength() {
    return 42;
  }

  public void nestedSample(
    PreparedStatement preparedStatement,
    List<Order> orders,
    boolean condition1,
    boolean condition2,
    Date yesterday,
    Date today,
    Date tomorrow,
    Date dayAfterTomorrow
  ) throws SQLException {
    while(true) {
      preparedStatement.setDate(0, yesterday); // Noncompliant [[secondary=-1]]
      preparedStatement.setDate(0, today); // Compliant
      preparedStatement.setDate(0, tomorrow); // Compliant
      preparedStatement.setDate(0, dayAfterTomorrow); // Compliant
      today = java.sql.Date.valueOf(LocalDate.now());
      if (condition1) do {
        preparedStatement.setDate(0, yesterday); // Noncompliant [[secondary=-1]]
        preparedStatement.setDate(0, today); // Noncompliant [[secondary=-2]]
        preparedStatement.setDate(0, tomorrow); // Compliant
        preparedStatement.setDate(0, dayAfterTomorrow); // Compliant
        tomorrow = java.sql.Date.valueOf(LocalDate.now());
        if (condition2) {
          for(Order order: orders) {
            preparedStatement.setDate(0, yesterday); // Noncompliant [[secondary=-1]]
            preparedStatement.setDate(0, today); // Noncompliant [[secondary=-2]]
            preparedStatement.setDate(0, tomorrow); // Noncompliant [[secondary=-3]]
            preparedStatement.setDate(0, dayAfterTomorrow); // Compliant
            dayAfterTomorrow = java.sql.Date.valueOf(LocalDate.now());
          }
        }
        for(int i = 0; i < 10; i++) {
          preparedStatement.setDate(0, yesterday); // Noncompliant [[secondary=-1]]
          preparedStatement.setDate(0, today); // Noncompliant [[secondary=-2]]
          preparedStatement.setDate(0, tomorrow); // Noncompliant [[secondary=-3]]
          preparedStatement.setDate(0, dayAfterTomorrow); // Compliant
          if (condition2) {
            dayAfterTomorrow = java.sql.Date.valueOf(LocalDate.now());
          }
        }
      } while (true);
    }
  }

  public record Order(String id, String price) {}

  private static class NoPreparedStatement {
    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException {
    }

    public void executeUpdate() {
    }
  }
}
