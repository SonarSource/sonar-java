package checks;

class JdbcDriverExplicitLoadingCheckSample {
  private static final String DRIVER = "com.mysql.jdbc.Driver";

  void doStuff() throws ClassNotFoundException {
    Class.forName("org.h2.Driver"); // Noncompliant [[sc=11;ec=18]] {{Remove this "Class.forName()", it is useless.}}
    Class.forName("com.mysql.jdbc.Driver"); // Noncompliant
    Class.forName("oracle.jdbc.driver.OracleDriver"); // Noncompliant
    Class.forName("com.ibm.db2.jdbc.app.DB2Driver"); // Noncompliant
    Class.forName("com.ibm.db2.jdbc.net.DB2Driver"); // Noncompliant
    Class.forName("com.ibm.db2.jcc.DB2Driver"); // Noncompliant
    Class.forName("com.sybase.jdbc.SybDriver"); // Noncompliant
    Class.forName("com.sybase.jdbc2.jdbc.SybDriver"); // Noncompliant
    Class.forName("com.teradata.jdbc.TeraDriver"); // Noncompliant
    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); // Noncompliant
    Class.forName("org.postgresql.Driver"); // Noncompliant
    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver"); // Noncompliant
    Class.forName("org.hsqldb.jdbc.JDBCDriver"); // Noncompliant
    Class.forName("org.h2.Driver"); // Noncompliant
    Class.forName("org.firebirdsql.jdbc.FBDriver"); // Noncompliant
    Class.forName("net.sourceforge.jtds.jdbc.Driver"); // Noncompliant
    Class.forName(DRIVER); // Noncompliant

    Class.forName("java.lang.String");
    Class.forName(javax.print.ServiceUIFactory.DIALOG_UI);
  }
}
