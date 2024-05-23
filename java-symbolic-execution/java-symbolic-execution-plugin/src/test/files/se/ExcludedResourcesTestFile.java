import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.FileInputStream;

public class A {
  
  public void unclosedConnectionExcluded(String url) {
    Connection connection = DriverManager.getConnection(url); // Noncompliant {{Use try-with-resources or close this "Connection" in a "finally" clause.}}
    Statement statement = connection.createStatement(); // Compliant - java.sql.Statement is excluded
  }
  
  public void wrongHandlingExcluded() {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant - java.io.FileInputStream is excluded
    stream.read();
  }
  
}
