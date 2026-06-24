package checks;

import java.io.IOException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

class TryWithResourcesCheck_java_26 {

  void processFromProcessBuilderIsCloseableAsOfJava26() throws IOException {
    Process p = null;
    try { // Noncompliant {{Change this "try" to a try-with-resources.}}
      p = new ProcessBuilder("ls").start();
      p.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (p != null) {
        p.close();
      }
    }
  }

  void processFromRuntimeExec() throws IOException {
    Process p = null;
    try { // Noncompliant
      p = Runtime.getRuntime().exec(new String[] {"ls"});
      p.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      p.close();
    }
  }

  void blobFromConnectionFactory(Connection connection) throws SQLException {
    Blob blob = null;
    try { // Noncompliant
      blob = connection.createBlob();
      blob.setBytes(1L, new byte[] {0});
    } finally {
      if (blob != null) {
        blob.close();
      }
    }
  }

  void clobFromResultSetGetter(ResultSet rs) throws SQLException {
    Clob clob = null;
    try { // Noncompliant
      clob = rs.getClob("payload");
      clob.length();
    } finally {
      if (clob != null) {
        clob.close();
      }
    }
  }

  void arrayFromResultSetGetter(ResultSet rs) throws SQLException {
    Array array = null;
    try { // Noncompliant
      array = rs.getArray(1);
      array.getBaseType();
    } finally {
      if (array != null) {
        array.close();
      }
    }
  }

  void sqlxmlFromConnectionFactory(Connection connection) throws SQLException {
    SQLXML xml = null;
    try { // Noncompliant
      xml = connection.createSQLXML();
      xml.setString("<root/>");
    } finally {
      if (xml != null) {
        xml.close();
      }
    }
  }

  void blobFromCallableStatement(CallableStatement cs) throws SQLException {
    Blob blob = null;
    try { // Noncompliant
      blob = cs.getBlob(1);
      blob.length();
    } finally {
      if (blob != null) {
        blob.close();
      }
    }
  }

  void compliantTryWithResources(Connection connection) throws IOException, SQLException {
    try (Process p = new ProcessBuilder("ls").start();
         Blob blob = connection.createBlob()) {
      p.waitFor();
      blob.setBytes(1L, new byte[] {0});
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

}
