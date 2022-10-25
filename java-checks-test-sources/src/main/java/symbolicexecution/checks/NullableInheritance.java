package symbolicexecution.checks;

import java.io.File;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

public class NullableInheritance {
  public Long jdbcTemplate() {
    // Method "org.springframework.jdbc.core.JdbcTemplate.queryForObject(String, Class<Long>, Object...)"
    // In spring-jdbc < 5.0.0: Not annotated as Nullable but described as nullable in javadoc
    // In spring-jdbc >= 5.0.0: super-implementation (defined in interface org.springframework.jdbc.core.JdbcOperations), annotated with spring's @Nullable
    final Long a = new JdbcTemplate().queryForObject("a_query", Long.class, new Object[1]);
    return a == null ? Long.MIN_VALUE : a.longValue(); // Compliant - no issue; null-check is required
  }
}
