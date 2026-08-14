package checks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

class CompilationOrPreparationInLoopCheckSample {

  private static final String CONSTANT_PATTERN = "[a-z]+";
  private String mutablePattern = "[a-z]+";

  void patternCompileNoncompliant(List<String> inputs) {
    for (String input : inputs) {
      Pattern.compile("[a-z]+").matcher(input).find(); // Noncompliant {{Move this "compile" call outside the loop.}}
    //^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    int i = 0;
    while (i++ < inputs.size()) {
      Pattern.compile("[a-z]+"); // Noncompliant
    }

    for (String input : inputs) {
      Pattern.compile(CONSTANT_PATTERN).matcher(input).find(); // Noncompliant
    }

    String invariantPattern = "[a-z]+";
    for (String input : inputs) {
      Pattern.compile(invariantPattern).matcher(input).find(); // Noncompliant
    }

    for (String input : inputs) {
      Pattern.compile("[a-z]+", Pattern.CASE_INSENSITIVE); // Noncompliant
      int flags = input.isEmpty() ? 0 : Pattern.CASE_INSENSITIVE;
      Pattern.compile("[a-z]+", flags); // Compliant: flags vary
    }
  }

  void stringMethodsNoncompliant(List<String> inputs) {
    for (String input : inputs) {
      input.matches("[a-z]+"); // Noncompliant {{Extract this regular expression to a Pattern compiled outside the loop.}}
      input.replaceAll("[a-z]+", "X"); // Noncompliant
      input.replaceFirst("[a-z]+", "X"); // Noncompliant
      input.split("[,;]"); // Noncompliant
      input.split(","); // Compliant: split fast path
      input.split("."); // Noncompliant
    }
  }

  void prepareStatementNoncompliant(Connection conn, List<Integer> ids) throws SQLException {
    for (int id : ids) {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM t WHERE id = ?"); // Noncompliant
      ps.setInt(1, id);
      ps.execute();
      ps.close();
    }
  }

  void forInitializer(String s) {
    for (Pattern p = Pattern.compile("[a-z]+"); p.matcher(s).find(); ) { // Compliant: initializer runs once
      break;
    }
    for (; Pattern.compile("[a-z]+").matcher(s).find(); ) { // Noncompliant
      break;
    }
  }

  void compliant(List<String> inputs, Connection conn, List<Integer> ids) throws SQLException {
    Pattern p = Pattern.compile("[a-z]+");
    for (String input : inputs) {
      p.matcher(input).find();
    }

    for (String input : inputs) {
      input.toLowerCase(); // not a regex method
    }

    PreparedStatement ps = conn.prepareStatement("SELECT * FROM t WHERE id = ?");
    for (int id : ids) {
      ps.setInt(1, id);
      ps.execute();
    }
  }

  void patternVariesPerIteration(List<String> patterns, List<String> inputs) {
    for (int i = 0; i < inputs.size(); i++) {
      String pattern = patterns.get(i);
      Pattern.compile(pattern).matcher(inputs.get(i)).find(); // Compliant - pattern changes per iteration
    }
  }

  void mutableFieldPattern(List<String> inputs) {
    for (String input : inputs) {
      Pattern.compile(mutablePattern).matcher(input).find(); // Compliant - non-final field may be mutated via member select or method call
    }
  }
}
