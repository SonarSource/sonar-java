package checks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SQLInjectionCheckSample {

  private final JdbcTemplate jdbcTemplate;
  private final JdbcOperations jdbcOperations;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final JdbcClient jdbcClient;

  @Autowired
  public SQLInjectionCheckSample(JdbcTemplate jdbcTemplate,
    JdbcOperations jdbcOperations,
    NamedParameterJdbcTemplate namedParameterJdbcTemplate,
    JdbcClient jdbcClient) {
    this.jdbcTemplate = jdbcTemplate;
    this.jdbcOperations = jdbcOperations;
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    this.jdbcClient = jdbcClient;
  }


  @GetMapping("/S2077")
  public String s2077(@RequestParam String input) {
    RowMapper<String> rowMapper = (rs, rowNum) -> null;
    PreparedStatementCallback<String> preparedStatementCallback = ps -> null;

    jdbcTemplate.execute("SELECT " + input); // Noncompliant
    jdbcTemplate.queryForStream("SELECT " + input, rowMapper); // Noncompliant

    jdbcOperations.execute("SELECT " + input); // Noncompliant
    jdbcOperations.queryForStream("SELECT " + input, rowMapper); // Noncompliant

    namedParameterJdbcTemplate.execute("SELECT " + input, preparedStatementCallback); // Noncompliant

    JdbcDaoImpl jdbcDao = new JdbcDaoImpl();
    jdbcDao.setAuthoritiesByUsernameQuery("SELECT " + input); // Noncompliant

    JdbcUserDetailsManager jdbcUserDetailsManager = new JdbcUserDetailsManager();
    jdbcUserDetailsManager.setFindGroupIdSql("SELECT " + input); // Noncompliant

    jdbcClient.sql("SELECT " + input).query(); // Noncompliant

    return "Done";
  }

}
