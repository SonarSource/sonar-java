// mimic spring package structure
package org.springframework.context;

import java.io.Closeable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreator;

public interface ConfigurableApplicationContext extends Closeable {}

abstract class SpringResource {

  public static ConfigurableApplicationContext run() {
    return null;
  }

  void foo() {
    SpringResource.run(); // Compliant
  }
}

class MyCloseable extends Closeable {

}

class MyPreparedStatementCreator implements PreparedStatementCreator {
  @Override
  public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
    PreparedStatement ps = connection.prepareStatement("insert into emp(name, surname) values (?, ?)"); // Compliant
    ps.setString(1, "bruce");
    ps.setString(2, "lee");

    MyCloseable myCloseable = new MyCloseable(); // Noncompliant

    return ps;
  }
}

class MyCallableStatementCreator implements CallableStatementCreator {
  @Override
  public CallableStatement createCallableStatement(Connection connection) throws SQLException {
    CallableStatement cs = connection.prepareCall("insert into emp(name, surname) values (?, ?)"); // Compliant
    cs.setString(1, "bruce");
    cs.setString(2, "lee");
    return cs;
  }
}

