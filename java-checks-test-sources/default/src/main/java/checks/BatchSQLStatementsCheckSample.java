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

  void nonCompliantRequestsInLoop(Statement statement, List<String> queries) throws SQLException {
    for (int i = 0; i < queries.size(); i++) {
      statement.execute(queries.get(i)); // Noncompliant [sc=7,ec=40] {{Use "addBatch" and "executeBatch" to execute multiple SQL statements in a single call.}}
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
            statement.execute(query); // Noncompliant
          }
        }
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

  void compliantRequestsInLoop(Statement statement, List<String> queries) throws SQLException {
    for (int i = 0; i < queries.size(); i++) {
      statement.addBatch(queries.get(i));
    }

    for (String query : queries) {
      statement.addBatch(query);
    }

    while (queries.iterator().hasNext()) {
      statement.addBatch(queries.iterator().next());
    }

    queries.forEach(query -> {
      try {
        statement.addBatch(query);
      } catch (SQLException e) {
        // do nothing
      }
    });

    int j = 0;
    do {
      statement.addBatch(queries.get(j));
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
}
