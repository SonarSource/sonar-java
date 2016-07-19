import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class JdbcSample {
  
  public void directClose(String url) {
    DriverManager.getConnection(url).close();
  }
  
  public void unclosedConnection(String url) {
    Connection connection = DriverManager.getConnection(url); // Noncompliant {{Close this "Connection".}}
    Statement statement = connection.createStatement(); // Noncompliant {{Close this "Statement".}}
  }
  
  public void unclosedResultSet(String url, String query) {
    try (Connection connection = DriverManager.getConnection(url);) {
      Statement statement = connection.createStatement(); // Noncompliant {{Close this "Statement".}}
      ResultSet result = statement.executeQuery(query); // Noncompliant {{Close this "ResultSet".}}
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
          ResultSet result = statement.getResultSet(); // Noncompliant {{Close this "ResultSet".}}
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
