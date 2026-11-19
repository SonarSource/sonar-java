package checks;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hibernate.Session;
import javax.persistence.EntityManager;

import javax.jdo.PersistenceManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

class SQLInjection {
  private static final String CONSTANT = "SELECT * FROM TABLE";
  public void method(String param, String param2, EntityManager entityManager, jakarta.persistence.EntityManager entityManager2) {
    try {
      Connection conn = DriverManager.getConnection("url", "user1", "password");
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT Lname FROM Customers WHERE Snum = 2001");
      rs = stmt.executeQuery("SELECT Lname FROM Customers WHERE Snum = "+param); // Noncompliant {{Make sure using a dynamically formatted SQL query is safe here.}} [[sc=30;ec=79]]
      String query = "SELECT Lname FROM Customers WHERE Snum = "+param;
      rs = stmt.executeQuery(query); // Noncompliant

      boolean bool = false;
      String query2 = "Select Lname ";
      if(bool) {
        query2 += "FROM Customers";
      }else {
        query2 += "FROM Providers";
      }
      query2 = query2 + " WHERE Snum =2001";
      rs = stmt.executeQuery(query2);

      //Prepared statement
      PreparedStatement ps = conn.prepareStatement("SELECT Lname FROM Customers"+" WHERE Snum = 2001");
      ps.executeQuery("SELECT Lname FROM Customers WHERE Snum = "+param); // Noncompliant
      ps  = conn.prepareStatement("SELECT Lname FROM Customers WHERE Snum = "+param); // Noncompliant
      ps = conn.prepareStatement(query2);

      final String queryNoConcatenation = bool ? param : param2;
      conn.prepareStatement(queryNoConcatenation);

      //Callable Statement
      CallableStatement cs = conn.prepareCall("SELECT Lname FROM Customers WHERE Snum = 2001");
      cs.executeQuery(query); // Noncompliant
      cs  = conn.prepareCall("SELECT Lname FROM Customers WHERE Snum = "+param2); // Noncompliant
      cs = conn.prepareCall(query2);
      cs = conn.prepareCall(CONSTANT);
      cs = conn.prepareCall(foo());
      String query3 = "SELECT * from table";
      cs = conn.prepareCall(query3);

      String s;
      String tableName = "TableName";
      String column = " column ";
      String FROM = " FROM ";
      if(true) {
        s = "SELECT" +column+FROM +tableName;
      } else {
        s = "SELECT" +column+"FROM" +tableName;
      }
      cs = conn.prepareCall(s);
      String request = foo() + " FROM table";
      cs = conn.prepareCall(request); // Noncompliant
      new A().prepareStatement(query);
      A a = new A();
      a.prepareStatement(query);
      ps.executeQuery();


      Session session = null;
      session.createQuery("From Customer where id > ?");
      session.createQuery(query); // Noncompliant
      conn.prepareStatement(param);
      conn.prepareStatement(sqlQuery + "plop"); // Noncompliant

      String sql = "SELECT lastname, firstname FROM employee where uid = '" + param + "'";
      entityManager.createNativeQuery(sql); // Noncompliant
      entityManager2.createNativeQuery(sql); // Noncompliant

      String concatenatedQuery0 = "SELECT * ";
      concatenatedQuery0 += "FROM " + param;
      rs = stmt.executeQuery(concatenatedQuery0); // Noncompliant

      String concatenatedQuery = "SELECT * FROM ";
      concatenatedQuery += param;
      rs = stmt.executeQuery(concatenatedQuery); // Noncompliant

      String concatenatedQuery2 = "SELECT * FROM ";
      concatenatedQuery2 += param;
      concatenatedQuery2 += "WHERE Snum =2";
      rs = stmt.executeQuery(concatenatedQuery2); // Noncompliant

      String concatenatedQuery3 = "SELECT * FROM ";
      concatenatedQuery3 += "MYTABLE ";
      concatenatedQuery3 += "WHERE Snum = 2";
      rs = stmt.executeQuery(concatenatedQuery3); // Compliant

    } catch (Exception e) {
    }
  }

  void falseNegative(String param) throws SQLException {
    Connection conn = DriverManager.getConnection("url", "user1", "password");
    String s;
    s = "SELECT FROM " + param;
    conn.prepareStatement(s); // Compliant, false negative

    String s2 = "SELECT";
    s2 = s2 + param;
    conn.prepareStatement(s2); // Compliant, true negative

    String s3;
    s3 = "SELECT";
    s3 += param;
    conn.prepareStatement(s3); // Noncompliant
  }

  void testSecondaryLocations(String param) throws SQLException {
    Connection conn = DriverManager.getConnection("url", "user1", "password");

    String query1 = "SELECT"; // secondary location
  //                ^^^^^^^^>
    query1 += param; // secondary location
  //          ^^^^^>
    conn.prepareStatement(query1); // Noncompliant
  //                      ^^^^^^


    boolean bool = false;
    String query2 = "Select Lname ";
    if(bool) {
      query2 += "FROM Customers";
    } else {
      query2 += param;
    }
    query2 = query2 + " WHERE Snum =2001";
    conn.prepareStatement(query2); // Noncompliant [[secondary=133,135,137,139]]
  }

  void foo(Connection conn, String param) throws SQLException {
    Statement stmt = conn.createStatement();
    String localVar = param;
    String aliasParam;
    aliasParam = param;
    stmt.execute(aliasParam); // OK
    stmt.execute(localVar); // OK
    stmt.execute(param); // OK
    String[] queries = {"test"};
    stmt.execute(queries[0]); // OK
  }

  String foo() {
    return "SELECT * ";
  }

  private String sqlQuery;
  class A {
    void prepareStatement(String s) {

    }
  }

  private static void makeQuery(Connection p_con) {
    try {
      String query = null;
      StringBuffer qryBuffer = new StringBuffer();

      qryBuffer = new StringBuffer();
      qryBuffer.append(" select abc from xyz ");
      qryBuffer.append(" where bulubulu=?");
      query = qryBuffer.toString();

      p_con.prepareStatement(query); // Compliant
    } catch (Exception e) {
      System.out.println("makeQuery");
    }
  }

  PersistenceManager pm;

  void jdo(int id, String name) {
    javax.jdo.Query q = pm.newQuery(Test.class, id + " > query_id "); // Compliant
    q.setFilter("name == " + name); // Noncompliant
  }

  private final String classNameField = getClass().getName();

  void classNameDirectCalls(Statement statement) throws SQLException {
    statement.execute("SELECT " + SQLInjection.class.getName()); // Compliant
    statement.execute("SELECT " + SQLInjection.class.getSimpleName()); // Compliant
    statement.execute("SELECT " + SQLInjection.class.getCanonicalName()); // Compliant
    statement.execute("SELECT " + SQLInjection.class.getTypeName()); // Compliant
    statement.execute("SELECT " + (SQLInjection.class.getName())); // Compliant
  }

  void classNameLocalAliases(Statement statement) throws SQLException {
    String className = getClass().getName();
    statement.execute("CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"" + className + ".init\""); // Compliant

    String alias = className;
    statement.execute("SELECT " + alias); // Compliant
    statement.execute("SELECT " + alias + "." + alias); // Compliant

    String assignedOnce;
    assignedOnce = SQLInjection.class.getSimpleName();
    statement.execute("SELECT " + assignedOnce); // Compliant

    String query = "SELECT " + className;
    statement.execute(query); // Compliant

    statement.execute(String.format("SELECT %s", className)); // Compliant
    statement.execute("SELECT %s".formatted(SQLInjection.class.getTypeName())); // Compliant
    classNameParameterFalsePositive(statement, className);
  }

  void classNameDynamicOperands(Statement statement, String input) throws SQLException {
    statement.execute("SELECT " + SQLInjection.class.getName() + input); // Noncompliant
    statement.execute(String.format("SELECT %s %s", SQLInjection.class.getName(), input)); // Noncompliant

    String reassigned = getClass().getName();
    reassigned = input;
    statement.execute("SELECT " + reassigned); // Noncompliant

    statement.execute("SELECT " + input); // Noncompliant
  }

  void classNameFieldFalsePositive(Statement statement) throws SQLException {
    // Known FP: the field is initialized from Class#getName(), but local-variable backtracking does not resolve fields.
    statement.execute("SELECT " + classNameField); // Noncompliant
  }

  void classNameParameterFalsePositive(Statement statement, String className) throws SQLException {
    // Known FP: the caller passes Class#getName(), but local-variable backtracking does not resolve parameters.
    statement.execute("SELECT " + className); // Noncompliant
  }

  void quotedIdentifiers(Statement statement, String input, String table) throws SQLException {
    statement.execute("DROP TABLE IF EXISTS " + statement.enquoteIdentifier(input, true)); // Compliant
    statement.execute("DROP TABLE IF EXISTS " + (statement.enquoteIdentifier(input, false))); // Compliant

    String quotedName = statement.enquoteIdentifier(input, true);
    statement.execute("DROP TABLE IF EXISTS " + quotedName); // Compliant

    String schema = statement.enquoteIdentifier(input, false);
    statement.execute("VACUUM " + schema + "." + statement.enquoteIdentifier(table, false)); // Compliant

    String alias = quotedName;
    statement.execute("SELECT " + alias + "." + alias); // Compliant

    String assignedOnce;
    assignedOnce = statement.enquoteIdentifier(input, false);
    statement.execute("DROP TABLE IF EXISTS " + assignedOnce); // Compliant

    String query = "DROP TABLE IF EXISTS " + quotedName;
    statement.execute(query); // Compliant

    String appendedQuery = "DROP TABLE IF EXISTS ";
    appendedQuery += quotedName;
    statement.execute(appendedQuery); // Compliant
  }

  void quotedIdentifierSubtype(PreparedStatement statement, String input) throws SQLException {
    statement.execute("DROP TABLE IF EXISTS " + statement.enquoteIdentifier(input, true)); // Compliant
  }

  void unsafeQuotedIdentifiers(Statement statement, String input) throws SQLException {
    statement.execute("SELECT " + statement.enquoteIdentifier(input, true) + input); // Noncompliant
    //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    statement.execute("SELECT " + input + statement.enquoteIdentifier(input, false)); // Noncompliant
    //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    String reassigned = statement.enquoteIdentifier(input, true);
    reassigned = input;
    statement.execute("SELECT " + reassigned); // Noncompliant
    //                ^^^^^^^^^^^^^^^^^^^^^^

    statement.execute("SELECT " + enquoteIdentifier(input, true)); // Noncompliant
    //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  void quotedIdentifierFormatting(Statement statement, String input) throws SQLException {
    String quotedName = statement.enquoteIdentifier(input, true);
    statement.execute(String.format("SELECT %s", quotedName)); // Noncompliant
    //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    statement.execute("SELECT %s".formatted(statement.enquoteIdentifier(input, false))); // Noncompliant
    //                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  String enquoteIdentifier(String input, boolean alwaysQuote) {
    return input;
  }
}


class Spring {

  private JdbcTemplate jdbcTemplate;
  private JdbcOperations jdbcOperations;
  private PreparedStatementCreatorFactory preparedStatementCreatorFactory;

  void test(String parameter) {
    java.lang.String sqlInjection = "select count(*) from t_actor where column =  " + parameter;
    jdbcTemplate.queryForObject(sqlInjection, Integer.class); // Noncompliant
    jdbcOperations.queryForObject(sqlInjection, Integer.class);  // Noncompliant

    new PreparedStatementCreatorFactory(sqlInjection);  // Noncompliant
    preparedStatementCreatorFactory.newPreparedStatementCreator(sqlInjection, new Object[] {});  // Noncompliant
  }
}


class Test {
  public void foo(String page, String projectUuid) {
    String from = "from ResourceDBO r, ProjectDBO p where p.id = r.entityId and r.type = :entityType and r.mimeType in :mimeTypes";
    if (projectUuid != null) {
      from += " and p.uuid = :projectUuid";
    }
    String sortField = "lastUpdateTime";
    boolean asc = false;
    if (page != null) {
      String countJql = "select count(*) " + from;
      Session session = null;
      session.createQuery(countJql); // Noncompliant

    }
  }
}

class SQLInjectionB {

  private String user;
  private JdbcTemplate tmpl = new JdbcTemplate();

  private void foo() {
    // field accessed with "this."
    tmpl.batchUpdate(this.user); // Compliant
    tmpl.queryForObject(this.user, String.class); // Compliant

    // field accessed without "this."
    tmpl.batchUpdate(user); // compliant
    tmpl.queryForObject(user, String.class); // compliant
  }
}

class SQLFormat {
  private final Statement stmt;

  public SQLFormat(Statement stmt) {
    this.stmt = stmt;
  }

  public void formatInline(String input) throws SQLException {
    this.stmt.execute(String.format("SELECT %s", input)); // Noncompliant
  }

  public void formatInlineConst() throws SQLException {
    // Allow strings built with `format` if the arguments are constants.
    this.stmt.execute(String.format("SELECT %s", "1"));
  }

  public void formatVar(String input) throws SQLException {
    String query = String.format("SELECT %s", input);
    this.stmt.execute(query); // Noncompliant
  }

  public void formatVarObject(Object input) throws SQLException {
    String query = String.format("SELECT %s", input);
    this.stmt.execute(query); // Noncompliant
  }

  public void formatVarConst() throws SQLException {
    String query = String.format("SELECT %s", "1");
    this.stmt.execute(query);
  }

  public void formatLocale(Locale locale, String input) throws SQLException {
    String query = String.format(locale, "SELECT %s", input);
    this.stmt.execute(query); // Noncompliant
  }

  public void formatLocaleConst(Locale locale) throws SQLException {
    String query = String.format(locale, "SELECT %s", "1");
    this.stmt.execute(query);
  }

  public void formatPrimitiveVar(int input) throws SQLException {
    String query = String.format("SELECT %s", input);
    this.stmt.execute(query);
  }

  public void formatPrimitiveConst() throws SQLException {
    String query = String.format("SELECT %s", 1);
    this.stmt.execute(query);
  }

  public void formatPrimitiveWrapperVar(Long input) throws SQLException {
    String query = String.format("SELECT %s", input);
    this.stmt.execute(query);
  }

  public void formatPrimitiveWrapperConst() throws SQLException {
    String query = String.format("SELECT %s", Long.valueOf(4));
    this.stmt.execute(query);
  }

  public void formatted(String input) throws SQLException {
    String query = "SELECT %s".formatted(input);
    this.stmt.execute(query); // Noncompliant
  }

  public void formattedConst() throws SQLException {
    String query = "SELECT %s".formatted("1");
    this.stmt.execute(query);
  }

  public void plusAssignment(String input) throws SQLException {
    String query = "SELECT";
    query += String.format("WHERE col = %c", input);
    this.stmt.execute(query); // Noncompliant
  }

  public void plusAssignmentConst() throws SQLException {
    String query = "SELECT";
    query += String.format("WHERE col = \"%c\"", "value");
    this.stmt.execute(query); // Compliant
  }

  public void plusAssignmentClassName(String input) throws SQLException {
    String query = "SELECT ";
    query += getClass().getName();
    this.stmt.execute(query); // Compliant

    String query2 = "SELECT ";
    query2 += "." + SQLInjection.class.getSimpleName();
    this.stmt.execute(query2); // Compliant

    String query3 = "SELECT ";
    query3 += SQLInjection.class.getName() + input;
    this.stmt.execute(query3); // Noncompliant
  }

  public void constantTernaryAndPrimitives(boolean asc, int offset, Integer limit, String input, EntityManager em) {
    em.createNativeQuery("SELECT * FROM entity ORDER BY x " + (asc ? "ASC" : "DESC")); // Compliant
    em.createNativeQuery("SELECT * FROM entity OFFSET " + offset + " LIMIT " + limit); // Compliant
    String query = "SELECT * FROM entity OFFSET " + offset;
    query += " LIMIT " + limit;
    em.createNativeQuery(query); // Compliant
    em.createNativeQuery("SELECT * FROM entity ORDER BY " + (asc ? "ASC" : input)); // Noncompliant
    em.createNativeQuery("SELECT * FROM entity ORDER BY " + (asc ? input : "DESC")); // Noncompliant
    em.createNativeQuery("SELECT * FROM entity ORDER BY " + (asc ? (offset > 0 ? "A" : "B") : "C")); // Compliant
  }

  private static final String PRODUCT_OPERAND = "p.product_id";
  private static final String IN_TEMPLATE = "SELECT * FROM t WHERE id IN (%s)";
  private JdbcTemplate fixedJdbc;
  private String fieldFragment;
  @Value("${query.hint:/*+ x */}")
  private String configuredHint;

  private String literalWhere(boolean a, boolean b) {
    StringBuilder sb = new StringBuilder();
    if (a) {
      sb.append(" AND account_id = :accountId");
    }
    if (b) {
      sb.append(" AND room_id = :roomId").append(" AND x = 1");
    }
    return sb.toString();
  }

  private StringBuilder literalBuilder(boolean a) {
    StringBuilder sb = new StringBuilder("");
    if (a) {
      sb.append(" AND x = :x");
    }
    return sb;
  }

  private static String literalStatic() {
    return " AND y = :y";
  }

  private String literalConcat(boolean a) {
    if (a) {
      return " AND a = :a" + " AND b = :b";
    }
    return "";
  }

  private String dynamicWhere(String param) {
    return " AND x = " + param;
  }

  private String dynamicBuilderWhere(String param) {
    StringBuilder sb = new StringBuilder();
    sb.append(" AND x = ").append(param);
    return sb.toString();
  }

  private String dynamicBuilderAliasWhere(String param) {
    StringBuilder sb = new StringBuilder();
    StringBuilder alias = sb;
    alias.append(param);
    return sb.toString();
  }

  private String escapedBuilderWhere(String param) {
    StringBuilder sb = new StringBuilder();
    consume(sb);
    return sb.toString();
  }

  private String builderFromParam(String param) {
    StringBuilder sb = new StringBuilder(param);
    return sb.toString();
  }

  private String mixedReturns(boolean a, String param) {
    if (a) {
      return " AND a = :a";
    }
    return " AND b = " + param;
  }

  private String fieldReturn() {
    return fieldFragment;
  }

  private String recursiveHelper(int n) {
    return " AND r = :r" + recursiveHelper(n - 1);
  }

  private String callingHelper() {
    return " AND c = :c" + literalConcat(true);
  }

  private String lambdaReturn() {
    Supplier<String> supplier = () -> {
      return fieldFragment;
    };
    return " AND l = :l";
  }

  private String anonymousClassReturn() {
    Supplier<String> supplier = new Supplier<>() {
      @Override
      public String get() {
        return fieldFragment;
      }
    };
    return " AND a = :a";
  }

  String nonPrivateHelper() {
    return " AND n = :n";
  }

  private void consume(StringBuilder sb) {
  }

  void fixedFragmentHelpers(boolean a, boolean b, String param, MapSqlParameterSource params) {
    fixedJdbc.query("SELECT count(*) FROM assignment" + literalWhere(a, b), (rs, i) -> 1); // Compliant
    String sql = "SELECT count(*) FROM assignment" + literalWhere(a, b);
    fixedJdbc.query(sql, (rs, i) -> 1); // Compliant
    StringBuilder where = literalBuilder(a);
    fixedJdbc.query("SELECT 1 FROM t" + where, (rs, i) -> 1); // Compliant
    fixedJdbc.query("SELECT 1 FROM t" + literalStatic(), (rs, i) -> 1); // Compliant
    fixedJdbc.query("SELECT 1 FROM t" + literalConcat(a), (rs, i) -> 1); // Compliant
    fixedJdbc.query("SELECT 1 FROM t WHERE x = %s".formatted(literalStatic()), (rs, i) -> 1); // Compliant
    fixedJdbc.query("SELECT 1 FROM t" + callingHelper(), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + lambdaReturn(), (rs, i) -> 1); // Compliant
    fixedJdbc.query("SELECT 1 FROM t" + anonymousClassReturn(), (rs, i) -> 1); // Compliant

    fixedJdbc.query("SELECT 1 FROM t" + dynamicWhere(param), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + dynamicBuilderWhere(param), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + dynamicBuilderAliasWhere(param), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + escapedBuilderWhere(param), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + builderFromParam(param), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + mixedReturns(a, param), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + fieldReturn(), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + recursiveHelper(3), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + nonPrivateHelper(), (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + fieldFragment, (rs, i) -> 1); // Noncompliant
    fixedJdbc.query("SELECT 1 FROM t" + param, (rs, i) -> 1); // Noncompliant
  }

  void repeatedPlaceholders(List<String> ids, String param) {
    String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
    fixedJdbc.query("SELECT 1 FROM t WHERE id IN (" + placeholders + ")", (rs, i) -> 1); // Compliant
    fixedJdbc.query("SELECT 1 FROM t WHERE id IN (" + String.join(", ", Collections.nCopies(ids.size(), "(?)")) + ")", (rs, i) -> 1); // Compliant
    fixedJdbc.query(IN_TEMPLATE.formatted(placeholders), (rs, i) -> 1); // Compliant
    fixedJdbc.query(String.format(IN_TEMPLATE, placeholders), (rs, i) -> 1); // Compliant

    String badElement = String.join(",", Collections.nCopies(ids.size(), param));
    fixedJdbc.query("SELECT 1 FROM t WHERE id IN (" + badElement + ")", (rs, i) -> 1); // Noncompliant
    String badDelimiter = String.join(param, Collections.nCopies(ids.size(), "?"));
    fixedJdbc.query("SELECT 1 FROM t WHERE id IN (" + badDelimiter + ")", (rs, i) -> 1); // Noncompliant
    String plainJoin = String.join(",", ids);
    fixedJdbc.query("SELECT 1 FROM t WHERE id IN (" + plainJoin + ")", (rs, i) -> 1); // Noncompliant
    String reassigned = String.join(",", Collections.nCopies(ids.size(), "?"));
    reassigned = param;
    fixedJdbc.query("SELECT 1 FROM t WHERE id IN (" + reassigned + ")", (rs, i) -> 1); // Noncompliant
  }

  void mappedPlaceholders(List<String> rows, String param) {
    String values = rows.stream().map(r -> "(?, ?)").collect(Collectors.joining(", "));
    fixedJdbc.query("INSERT INTO t VALUES %s".formatted(values), (rs, i) -> 1); // Compliant
    fixedJdbc.query("INSERT INTO t VALUES " + rows.stream().map(r -> "(?)").collect(Collectors.joining(",")), (rs, i) -> 1); // Compliant

    String fromElement = rows.stream().map(r -> "(" + r + ")").collect(Collectors.joining(", "));
    fixedJdbc.query("INSERT INTO t VALUES " + fromElement, (rs, i) -> 1); // Noncompliant
    String badDelimiter = rows.stream().map(r -> "(?)").collect(Collectors.joining(param));
    fixedJdbc.query("INSERT INTO t VALUES " + badDelimiter, (rs, i) -> 1); // Noncompliant
    String blockLambda = rows.stream().map(r -> { return "(?)"; }).collect(Collectors.joining(","));
    fixedJdbc.query("INSERT INTO t VALUES " + blockLambda, (rs, i) -> 1); // Noncompliant
  }

  void loopsAndConstants(List<String> ids, List<String> columns) {
    StringBuilder literalLoop = new StringBuilder("SELECT 1 FROM t WHERE 1 = 0");
    for (String id : ids) {
      literalLoop.append(" OR code = ?");
    }
    fixedJdbc.query(literalLoop.toString(), (rs, i) -> 1); // Compliant
    fixedJdbc.query("SELECT 1 FROM t WHERE 1 = 0" + literalLoop.toString(), (rs, i) -> 1); // Compliant

    String concatenatedLoop = "SELECT 1 FROM t WHERE 1 = 0";
    for (String id : ids) {
      concatenatedLoop += " OR code = ?";
    }
    fixedJdbc.query(concatenatedLoop, (rs, i) -> 1); // Compliant

    String elementLoop = "SELECT 1 FROM t WHERE 1 = 0";
    for (String column : columns) {
      elementLoop += " OR " + column + " = ?";
    }
    fixedJdbc.query(elementLoop, (rs, i) -> 1); // Noncompliant

    fixedJdbc.query("SELECT 1 FROM t WHERE %1$s = ?".formatted(PRODUCT_OPERAND), (rs, i) -> 1); // Compliant
    String clause = "WHERE %1$s = ?".formatted(PRODUCT_OPERAND);
    fixedJdbc.query("SELECT 1 FROM t " + clause, (rs, i) -> 1); // Compliant
  }

  void dynamicBuilderInCaller(String param) {
    StringBuilder sb = new StringBuilder("SELECT 1 FROM t WHERE x = ");
    sb.append(param);
    fixedJdbc.query("SELECT 1 FROM t" + sb.toString(), (rs, i) -> 1); // Noncompliant
    StringBuilder passed = new StringBuilder("SELECT 1 FROM t");
    consume(passed);
    fixedJdbc.query("SELECT 1 FROM t" + passed.toString(), (rs, i) -> 1); // Noncompliant
    StringBuilder fixed = new StringBuilder("SELECT 1 FROM t");
    fixed.append(" WHERE x = 1");
    fixedJdbc.query("SELECT 1 FROM t" + fixed.toString(), (rs, i) -> 1); // Compliant
  }

  void builderEdgeCases(boolean a, String param) {
    StringBuilder self = new StringBuilder("SELECT 1");
    self.append(self.toString());
    fixedJdbc.query("x" + self.toString(), (rs, i) -> 1); // Noncompliant

    StringBuilder indirect = new StringBuilder("SELECT 1");
    String snapshot = indirect.toString();
    indirect.append(snapshot);
    fixedJdbc.query("x" + indirect.toString(), (rs, i) -> 1); // Noncompliant

    StringBuilder extended = literalBuilder(a);
    extended.append(param);
    fixedJdbc.query("SELECT 1 FROM t" + extended, (rs, i) -> 1); // Noncompliant

    StringBuilder original = literalBuilder(a);
    StringBuilder alias = original;
    alias.append(param);
    fixedJdbc.query("SELECT 1 FROM t" + original, (rs, i) -> 1); // Noncompliant

    StringBuilder escaping = literalBuilder(a);
    Supplier<StringBuilder> blockLeak = () -> {
      return escaping;
    };
    blockLeak.get().append(param);
    fixedJdbc.query("SELECT 1 FROM t" + escaping, (rs, i) -> 1); // Noncompliant

    StringBuilder escapingAnonymous = literalBuilder(a);
    Supplier<StringBuilder> anonymousLeak = new Supplier<>() {
      @Override
      public StringBuilder get() {
        return escapingAnonymous;
      }
    };
    anonymousLeak.get().append(param);
    fixedJdbc.query("SELECT 1 FROM t" + escapingAnonymous, (rs, i) -> 1); // Noncompliant

    StringBuilder returned = literalBuilder(a);
    fixedJdbc.query("SELECT 1 FROM t" + returned, (rs, i) -> 1); // Compliant
    fixedJdbc.query("SELECT 1 FROM t %s".formatted(returned), (rs, i) -> 1); // Compliant
  }

  void formatWithLocale(String param, Locale locale) {
    fixedJdbc.query(String.format(locale, "SELECT %s", "1"), (rs, i) -> 1); // Compliant
    fixedJdbc.query(String.format("SELECT '%s'", new Locale(param)), (rs, i) -> 1); // Noncompliant
  }

  void configuredValue() {
    fixedJdbc.query("SELECT " + configuredHint + " 1 FROM t", (rs, i) -> 1); // Noncompliant
  }
}
