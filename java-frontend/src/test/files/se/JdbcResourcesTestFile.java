import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import java.sql.SQLException;

public class JdbcSample {
  
  public void directClose(String url) {
    DriverManager.getConnection(url).close();
  }
  
  public void unclosedConnection(String url) {
    Connection connection = DriverManager.getConnection(url); // Noncompliant {{Use try-with-resources or close this "Connection" in a "finally" clause.}}
    Statement statement = connection.createStatement(); // Noncompliant {{Use try-with-resources or close this "Statement" in a "finally" clause.}}
  }
  
  public void unclosedResultSet(String url, String query) {
    try (Connection connection = DriverManager.getConnection(url);) {
      Statement statement = connection.createStatement(); // Noncompliant {{Use try-with-resources or close this "Statement" in a "finally" clause.}}
      PreparedStatement preparedStatement = connection.prepareStatement("SELECT"); // Noncompliant {{Use try-with-resources or close this "PreparedStatement" in a "finally" clause.}}
      ResultSet result = statement.executeQuery(query); // Noncompliant {{Use try-with-resources or close this "ResultSet" in a "finally" clause.}}
      ResultSet result2 = preparedStatement.executeQuery(); // Noncompliant {{Use try-with-resources or close this "ResultSet" in a "finally" clause.}}
      String name = result.getString(0);
    }
  }
  
  public void adequateHandling(String url, String query) {
    try (Connection connection = DriverManager.getConnection(url);) {
      Statement statement = connection.createStatement();
      try {
        ResultSet result = statement.executeQuery(query);
        try {
          String name = result.getString(0);
        } finally {
          result.close();
        }
      } finally {
        statement.close();
      }
    }
  }
  
  public void properHandling(String url, String query) {
    try (Connection connection = DriverManager.getConnection(url);) {
      try (Statement statement = connection.createStatement();) {
        try (ResultSet result = statement.executeQuery(query);) {
          String name = result.getString(0);
        }
      }
    }
  }
  
  public void properHandlingWithMoreResults(String url, String query) {
    try (Connection connection = DriverManager.getConnection(url);) {
      try (Statement statement = connection.createStatement();) {
        boolean hasResultSets = statement.execute(query);
        while (hasResultSets) {
          ResultSet result = statement.getResultSet(); // Noncompliant {{Use try-with-resources or close this "ResultSet" in a "finally" clause.}}
          String name = result.getString(0);
          hasResultSets = statement.getMoreResults();
        }
      }
    }
  }
  
  // The 4 methods below are testing the pattern of resources that are passed
  // to another object which could close them.
  public Connection returnedConnection_1(String url) {
    Connection connection = DriverManager.getConnection(url);
    return connection;
  }
  
  public Connection returnedConnection_2(String url) {
    return DriverManager.getConnection(url);
  }
  
  public void delegatedConnection_1(String url) {
    Connection connection = DriverManager.getConnection(url);
    processConnection(connection);
  }
  
  public void delegatedConnection_2(String url) {
    processConnection(DriverManager.getConnection(url));
  }
}

public class A {
  void foo() {
    PreparedStatement var1 = null;
    PreparedStatement var2 = null;

    Connection conn = getConnection();
    try {

      var1 = conn.prepareStatement("UPDATE ");
      var2 = conn.prepareStatement("UPDATE ");// Noncompliant can be open if var1.close throws an exception

    }finally {
      if(var1 != null) {
        var1.close();
      }
      if(var2 != null) {
        var2.close();
      }
    }
  }

  abstract Connection getConnection();
}

class DataSourceTest {

  void test(DataSource dataSource) throws SQLException {
    Connection connection = null;
    try {
      connection = dataSource.getConnection(); // Noncompliant
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  void compliant(DataSource dataSource) throws SQLException {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  Connection returnConn(DataSource dataSource) {
    Connection connection = dataSource.getConnection();
    return connection;
  }

  Connection returnConn(DataSource dataSource) {
    Connection connection = dataSource.getConnection(); // Noncompliant
    connection.setAutoCommit(false); // throws exception
    return connection;
  }
}

class JdbcNotCreatingResource {

  Statement test(Statement statement) throws SQLException {
    Statement local = statement.unwrap(Statement.class); // Compliant
    local.setCursorName("bla");
    return local;
  }
}
