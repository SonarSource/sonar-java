package checks;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

public class SQLInjectionCheckSample {

  public void s2077(String input, JdbcTemplate jdbcTemplate, JdbcOperations jdbcOperations,
    NamedParameterJdbcTemplate namedParameterJdbcTemplate, JdbcClient jdbcClient, RowMapper<String> rowMapper,
    PreparedStatementCallback<String> preparedStatementCallback) {

    jdbcTemplate.execute("SELECT " + input); // Noncompliant
    jdbcTemplate.queryForStream("SELECT " + input, rowMapper); // Noncompliant

    jdbcOperations.execute("SELECT " + input); // Noncompliant
    jdbcOperations.queryForStream("SELECT " + input, rowMapper); // Noncompliant

    namedParameterJdbcTemplate.batchUpdate("SELECT " + input, new SqlParameterSource[1]); // Noncompliant
    namedParameterJdbcTemplate.execute("SELECT " + input, preparedStatementCallback); // Noncompliant
    namedParameterJdbcTemplate.query("SELECT " + input, new BeanPropertySqlParameterSource(new Object()), new RowMapperResultSetExtractor(rowMapper)); // Noncompliant
    namedParameterJdbcTemplate.queryForList("SELECT " + input, EmptySqlParameterSource.INSTANCE); // Noncompliant
    namedParameterJdbcTemplate.queryForMap("SELECT " + input, EmptySqlParameterSource.INSTANCE); // Noncompliant
    namedParameterJdbcTemplate.queryForObject("SELECT " + input, EmptySqlParameterSource.INSTANCE, rowMapper); // Noncompliant
    namedParameterJdbcTemplate.queryForRowSet("SELECT " + input, EmptySqlParameterSource.INSTANCE); // Noncompliant
    namedParameterJdbcTemplate.queryForStream("SELECT " + input, EmptySqlParameterSource.INSTANCE, rowMapper); // Noncompliant
    namedParameterJdbcTemplate.update("SELECT " + input, EmptySqlParameterSource.INSTANCE); // Noncompliant


    JdbcDaoImpl jdbcDao = new JdbcDaoImpl();
    jdbcDao.setAuthoritiesByUsernameQuery("SELECT " + input); // Noncompliant
    jdbcDao.setGroupAuthoritiesByUsernameQuery("SELECT " + input); // Noncompliant
    jdbcDao.setUsersByUsernameQuery("SELECT " + input); // Noncompliant

    JdbcUserDetailsManager jdbcUserDetailsManager = new JdbcUserDetailsManager();
    jdbcUserDetailsManager.setChangePasswordSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setCreateAuthoritySql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setCreateUserSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setDeleteGroupAuthoritiesSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setDeleteGroupAuthoritySql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setDeleteGroupMemberSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setDeleteGroupMembersSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setDeleteGroupSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setDeleteUserAuthoritiesSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setDeleteUserSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setFindAllGroupsSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setFindGroupIdSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setFindUsersInGroupSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setGroupAuthoritiesSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setInsertGroupAuthoritySql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setInsertGroupMemberSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setInsertGroupSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setRenameGroupSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setUpdateUserSql("SELECT " + input); // Noncompliant
    jdbcUserDetailsManager.setUserExistsSql("SELECT " + input); // Noncompliant

    jdbcClient.sql("SELECT " + input).query(); // Noncompliant
  }

}
