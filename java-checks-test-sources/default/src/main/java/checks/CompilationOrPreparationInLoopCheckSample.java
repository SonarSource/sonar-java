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
      input.split("."); // Noncompliant
      input.split("\\a"); // Noncompliant
      input.split(","); // Compliant: single non-metacharacter fast path
      input.split("\\."); // Compliant: escaped non-alphanumeric fast path
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

  void localReassignedInLoop(List<String> inputs) {
    String pattern = "[a-z]+";
    for (String input : inputs) {
      pattern = input; // reassigned each iteration
      Pattern.compile(pattern).matcher(input).find(); // Compliant - pattern changes per iteration
    }
  }

  void doWhileLoop(List<String> inputs) {
    int i = 0;
    do {
      Pattern.compile("[a-z]+").matcher(inputs.get(i)).find(); // Noncompliant
    } while (i++ < inputs.size());
  }

  void nonConstantArg(List<String> inputs) {
    for (String input : inputs) {
      Pattern.compile(input.trim()).matcher(input).find(); // Compliant - method call result is not a constant
    }
  }

  void nonIncrementUnaryInLoop(List<String> inputs) {
    boolean inverse = false;
    for (String input : inputs) {
      if (!inverse) { // non-increment unary expression
        Pattern.compile("[a-z]+").matcher(input).find(); // Noncompliant
      }
    }
  }

  void nonIdentifierMutationsInLoop(List<String> inputs) {
    int[] counters = new int[2];
    for (String input : inputs) {
      counters[0] = input.length(); // assignment to non-identifier target
      counters[1]++;                // increment on non-identifier target
      Pattern.compile("[a-z]+").matcher(input).find(); // Noncompliant
    }
  }

  void expressionInEnhancedForLoopWithSplit(String text) {
    for (String s : text.split(";")) { // Compliant; text.split(";") evaluated only once
      if (s.isEmpty()) {
        return;
      }
    }
  }

  void expressionInEnhancedForLoopWithCompile(String text) {
    for (String s : Pattern.compile(";").split(text)) { // Compliant; Pattern.compile(";").split(text) evaluated only once
      if (s.isEmpty()) {
        return;
      }
    }
  }
}
