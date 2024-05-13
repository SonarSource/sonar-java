package checks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class BatchSQLStatementsCheckSample {

  void requestsInLoop(Statement statement, List<String> queries) throws SQLException {
    for (int i = 0; i < queries.size(); i++) {
      statement.execute(queries.get(i)); // Noncompliant {{Use "addBatch" and "executeBatch" to execute multiple SQL statements in a single call.}}
    }

    for (String query : queries) {
      statement.executeQuery(query); // Noncompliant
    }

    while (queries.iterator().hasNext()) {
      statement.executeUpdate(queries.iterator().next()); // Noncompliant
    }

    int j = 0;
    do {
      statement.execute(queries.get(j)); // Noncompliant
    } while (j < queries.size());

    for (int i = 0; i < 10; i++) {
      if (i % 2 == 0) {
        for (String query : queries) {
          if (i % 3 == 0) {
            statement.execute(query); // Compliant
          }
        }
      }
    }

    for (int i = 0; i < 10; i++) {
      try {
        statement.execute(queries.get(i)); // Compliant
        // do something
      } catch (SQLException e) {
        // do nothing
      }
    }

    queries.forEach(query -> {
      try {
        statement.execute(query); // Noncompliant
      } catch (SQLException e) {
        // do nothing
      }
    });

    queries.stream()
      .forEach(query -> {
        try {
          statement.execute(query); // Noncompliant
        } catch (SQLException e) {
          // do nothing
        }
      });

    for (int i = 0; i < queries.size(); i++) {
      statement.addBatch(queries.get(i)); // Compliant
    }

    for (String query : queries) {
      statement.addBatch(query); // Compliant
    }

    while (queries.iterator().hasNext()) {
      statement.addBatch(queries.iterator().next()); // Compliant
    }

    queries.forEach(query -> {
      try {
        statement.addBatch(query); // Compliant
      } catch (SQLException e) {
        // do nothing
      }
    });

    do {
      statement.addBatch(queries.get(j)); // Compliant
    } while (j < queries.size());

    statement.executeBatch();

    queries.stream().map(query -> {
      try {
        return statement.execute(query); // Compliant
      } catch (SQLException e) {
        return false;
      }
    })
      .filter(Boolean::booleanValue)
      .forEach(b -> {
        // do nothing
      });
  }

  void nonCompliantIterableForEach(PreparedStatement statement, List<String> queries) {
    queries.forEach(query -> {
      try {
        statement.execute(query); // Noncompliant
      } catch (SQLException e) {
        // do nothing
      }
    });
  }

  void nonCompliantStreamForEach(Statement statement, Stream<String> queries) {
    queries.forEach(query -> {
      try {
        statement.execute(query); // Noncompliant
      } catch (SQLException e) {
        // do nothing
      }
    });
  }

  void nonCompliantMapForEach(Statement statement, Map<Integer, String> entries) {
    entries.forEach((k, query) -> {
      try {
        statement.execute(query); // Noncompliant
      } catch (SQLException e) {
        // do nothing
      }
    });
  }

  void compliantSingleStatement(PreparedStatement statement, String query) throws SQLException {
    statement.execute(query); // Compliant
  }

  void compliantMultipleStatements(Statement statement, String query) throws SQLException {
    statement.executeQuery(query); // Compliant
    statement.execute("SELECT id, orderId FROM Users"); // Compliant
    statement.executeUpdate("SELECT id, price FROM Orders"); // Compliant
  }

  void complaintInOptional(Statement statement, String query) {
    Optional.ofNullable(query)
      .map(q -> {
        try {
          return statement.executeQuery(q); // Compliant
        } catch (SQLException e) {
          return false;
        }
      });
  }

  void complaintMultipleOptionals(Statement statement, String query) {
    Optional.ofNullable(query)
      .map(q -> {
        try {
          return statement.executeQuery(q); // Compliant
        } catch (SQLException e) {
          return false;
        }
      });
    Optional.ofNullable(query)
      .map(q -> {
        try {
          return statement.execute(q); // Compliant
        } catch (SQLException e) {
          return false;
        }
      });
  }

  private BiFunction<Statement, String, ResultSet> lambda = (statement, query) -> {
    try {
      return statement.executeQuery(query); // Compliant
    } catch (SQLException e) {
      return null;
    }
  };

  private void retryLoop(Statement statement, String query) {
    int numOfTires = 3;

    while (numOfTires > 0) {
      try (ResultSet rs = statement.executeQuery(query)) { // Compliant
        if (rs.next()) {
          // do something
        }
        numOfTires = 0;
      } catch (SQLException e) {
        // do nothing
      }
      numOfTires--;
    }

    do {
      try {
        if (statement.execute(query)) { // Compliant
          // do something
          numOfTires = 0;
        }
      } catch (SQLException e) {
        // do nothing
      }
      numOfTires--;
    } while (numOfTires > 0);
  }

  void executeQuery(Statement statement, String query) throws SQLException {
    statement.execute(query); // Compliant
  }

  void executeMultipleQueries(Statement statement, List<String> queries) throws SQLException {
    for (String query : queries) {
      executeQuery(statement, query); // Compliant
    }
  }

}
