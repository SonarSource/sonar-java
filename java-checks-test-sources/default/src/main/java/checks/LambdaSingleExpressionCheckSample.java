package checks;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class LambdaSingleExpressionCheckSample {
  public void method() {
    IntStream.range(1, 5).map(x -> x * x - 1).forEach(x -> System.out.println(x));
    IntStream.range(1, 5).map(x -> {return x * x - 1;}) // Noncompliant {{Remove useless curly braces around statement and then remove useless return keyword}}
        .forEach(x -> { // Compliant - lambda body spans multiple lines, block form is kept for readability
          System.out.println(x + 11);
        });
    IntStream.range(1, 5).map(x -> { // Compliant - non-expression statement
      if (x % 2 == 0) return 0;
      else return 1;
    });
    IntStream.range(1, 5).forEach(x -> {
      try {
        x = x/0;
      } catch (Exception e) {
        System.out.println(x);
      }
    });
    IntStream.range(1, 5).forEach(x -> {
      while(true) {
      }
    });
    // Nested block
    IntStream.range(1, 5).map(x -> { { { return x + 1; } } }); // Noncompliant {{Remove useless curly braces around statement}}
  }

  // Block lambda binds to RowCallbackHandler (void); simplifying to expression lambda
  // would be ambiguous with ResultSetExtractor since merge() returns a value
  void springJdbcQuery(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
    Map<Long, Long> countByRecipient = new HashMap<>();
    jdbc.query("SELECT recipient_id, cnt FROM t", rs -> { countByRecipient.merge(rs.getLong("recipient_id"), rs.getLong("cnt"), Long::sum); }); // Compliant
    namedJdbc.query("SELECT recipient_id, cnt FROM t", Collections.emptyMap(),
      rs -> { countByRecipient.merge(rs.getLong("recipient_id"), rs.getLong("cnt"), Long::sum); }); // Compliant
    jdbc.query("SELECT name FROM t",
      (rs, rowNum) -> { return rs.getString("name"); }); // Noncompliant {{Remove useless curly braces around statement and then remove useless return keyword}}
  }

  @FunctionalInterface
  interface ThrowingRunnable {
    void run() throws Throwable;
  }

  void process(ThrowingRunnable r) throws Throwable {
    r.run();
  }

  // MethodHandle.invokeExact() and MethodHandle.invoke() are signature-polymorphic
  void methodHandleInvocations(MethodHandle handle) throws Throwable {
    process(() -> { handle.invokeExact(); });
    process(() -> { handle.invoke(); });
  }

}
