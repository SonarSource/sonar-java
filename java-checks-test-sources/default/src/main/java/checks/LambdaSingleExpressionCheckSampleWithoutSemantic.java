package checks;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class LambdaSingleExpressionCheckSampleWithoutSemantic {

  @FunctionalInterface
  interface ThrowingRunnable {
    void run() throws Throwable;
  }

  void process(ThrowingRunnable r) throws Throwable {
    r.run();
  }

  void springJdbcQuery(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
    Map<Long, Long> countByRecipient = new HashMap<>();
    jdbc.query("SELECT recipient_id, cnt FROM t", rs -> { countByRecipient.merge(rs.getLong("recipient_id"), rs.getLong("cnt"), Long::sum); }); // Noncompliant
    namedJdbc.query("SELECT recipient_id, cnt FROM t", Collections.emptyMap(),
      rs -> { countByRecipient.merge(rs.getLong("recipient_id"), rs.getLong("cnt"), Long::sum); }); // Noncompliant
  }

  void methodHandleInvocations(MethodHandle handle) throws Throwable {
    process(() -> { handle.invokeExact(); });
    process(() -> { handle.invoke(); });
  }

}
