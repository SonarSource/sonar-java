import java.io.*;
import java.util.*;
import java.sql.*;

public class A {

  void ARM() {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output1.txt"), "UTF-8"))) {  // FP
      writer.write("This is my content.");
    }

    try (Writer writer = new OutputStreamWriter(new FileOutputStream("output1.txt"), "UTF-8")) {  // Compliant
      writer.write("This is my content.");
    }

    try (Writer writer1 = new OutputStreamWriter(new FileOutputStream("output1.txt"), "UTF-8");
      Writer writer2 = new OutputStreamWriter(new FileOutputStream("output1.txt"), "UTF-8");
    ) {  // Compliant
      writer1.write("This is my content.");
      writer2.write("This is my content.");
    }

    try (FileWriter fw = new FileWriter("")) { // Compliant - JLS8 - 14.20.3 : try-with-resources
      fw.write("hello");
    } catch (Exception e) {
      // ...
    }
  }

  void falseNegative() {
    class Foo implements AutoCloseable {

      Foo() {
        System.out.println("opening resource in foo");
      }

      @Override
      public void close() {
        System.out.println("foo is closed");
      }
    }

    class Bar implements AutoCloseable {
      private final Foo foo;

      public Bar(Foo foo) {
        this.foo = foo;
        throw new RuntimeException("Huho");
      }

      @Override
      public void close() {
        System.out.println("Bar closed (no more beers)");
        foo.close();
      }
    }

    // Foo will not be closed due to exception in Bar constructor, this is not detected because we assume all resources within resource block will be closed
    try (Bar bar = new Bar(new Foo())) {  // FN
      System.out.println("inside try");
    }
  }

  void java9() {
    final FileWriter fw = new FileWriter("");
    try (fw) {
      fw.write("hello");
    } catch (Exception e) {
      // ...
    }
  }

  private void doSelectByProjects(Connection conn, String sql) {
    try {
      try (PreparedStatement stmt = newStatement(conn, sql);
        ResultSet rs = stmt.executeQuery()) {
        doSomething();
      }
    } catch (SQLException e) {
      throw new IllegalStateException();
    }
  }

  private PreparedStatement newStatement(Connection conn, String sql) throws SQLException {
    return conn.prepareStatement(sql);
  }

  private void doSomething() {}
}
