package checks.spring;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

public class SpelExpressionCheckSample {

  @Value("#{systemProperties['user.region'}") // Noncompliant {{Correct this malformed SpEL expression.}}
  private String regionNc;

  @Value("#{systemProperties['user.region']}") // Compliant
  private String regionC;

  @Value("#{'${listOfValues}' split(',')}") // Noncompliant {{Correct this malformed SpEL expression.}}
  private List<String> valuesListNc;

  @Value("#{'${listOfValues}'.split(',')}") // Compliant
  private List<String> valuesListC;

  @Value("#{T(java.lang.Math).random() * 64h}") // Noncompliant {{Correct this malformed SpEL expression.}}
  private Double randPercentNc;

  @Value("#{T(java.lang.Math).random() * 100.0}") // Compliant
  private Double randPercentC;

  static class User {
    String status;
  }

  static class Customer {
    String firstname;
    String name;
  }

  interface Repo {

    @Query("select u from User u where u.age = ?#{[0]}") // Compliant
    List<User> findUsersByAge1(int age);

    @Query("select u from User u where u.age = ?#{[0}") // Noncompliant
    List<User> findUsersByAge2(int age);

    @Query("select u from User u where u.age = ?#{[0]") // Noncompliant {{Add missing '}' for this property placeholder or SpEL expression.}}
    List<User> findUsersByAge3(int age);

    @Query("select u from User u where u.age = ?#{[0*]}") // Noncompliant
    List<User> findUsersByAge4(int age);

    @Query("select u from User u where u.firstname = :#{#customer.firstname}") // Compliant
    List<User> findUsersByCustomersFirstname1(@Param("customer") Customer customer);

    @Query("select u from User u where u.firstname = :#{#customer firstname}") // Noncompliant
    List<User> findUsersByCustomersFirstname2(@Param("customer") Customer customer);

    @Query("select u from User u where u.name = :#{#customer.name} and u.firstname = :#{#customer.firstname}")  // Compliant
    List<User> findUsersByCustomersFullName1(@Param("customer") Customer customer);

    @Query("select u from User u where u.name = :#{#customer.name} and u.firstname = :#{#customer.firstname")  // Noncompliant {{Add missing '}' for this property placeholder or SpEL expression.}}
    List<User> findUsersByCustomersFullName2(@Param("customer") Customer customer);

    @Query("select u from User u where u.name = :#{#customer.name and u.firstname = :#{#customer.firstname}")  // Noncompliant {{Add missing '}' for this property placeholder or SpEL expression.}}
    List<User> findUsersByCustomersFullName3(@Param("customer") Customer customer);

    @Query("select u from User u where u.name = :#{#customer.name and u.firstname} = :#{#*customer.firstname}")  // Noncompliant {{Correct this malformed SpEL expression.}}
    List<User> findUsersByCustomersFullName4(@Param("customer") Customer customer);

    @Query("select u from User u where u.firstname = :#{#customer.firstname} and u.role=${admin}")  // Compliant
    List<User> findAdminUsersByFirstname1(@Param("customer") Customer customer);

    @Query("select u from User u where u.firstname = :#{#customer.firstname} and u.role=${admin")  // Noncompliant
    List<User> findAdminUsersByFirstname2(@Param("customer") Customer customer);
  }

  @Controller
  @RequestMapping("#{1+2+3}") // Compliant
  public static class RequestController1 { }

  @Controller
  @RequestMapping("#{1+2+}") // Noncompliant
  public static class RequestController2 { }

  @Value("foo") // Compliant
  String noTemplate0;

  @Value("foo.bar") // Compliant
  String noTemplate1;

  @Value("foo[10]") // Compliant
  String noTemplate2;

  @Value("foo[10][20]") // Compliant
  String noTemplate3;

  @Value("foo[10].bar") // Compliant
  String noTemplate4;

  @Value("foo.bar[10][20].baz") // Compliant
  String noTemplate5;

  @Value("{}") // Compliant
  String delimiters0;

  @Value("{123") // Compliant
  String delimiters1;

  @Value("123}") // Compliant
  String delimiters2;

  @Value("$") // Compliant
  String delimiters3;

  @Value("$ ") // Compliant
  String delimiters4;

  @Value("#") // Compliant
  String delimiters5;

  @Value("# ") // Compliant
  String delimiters6;

  @Value("$123") // Compliant
  String delimiters7;

  @Value("$foo") // Compliant
  String delimiters8;

  @Value("${}") // Noncompliant
  String delimiters9;

  @Value("${123") // Noncompliant
  String delimiters10;

  @Value("$123}") // Compliant
  String delimiters11;

  @Value("#{}") // Noncompliant
  String delimiters12;

  @Value("#{123") // Noncompliant
  String delimiters13;

  @Value("#123}") // Compliant
  String delimiters14;

  @Value("#{123}}") // Compliant
  String delimiters15;

  @Value("#{{123}") // Noncompliant
  String delimiters16;

  @Value("#{12{}3}") // Noncompliant, open
  String delimiters17;

  @Value("#{{12}3{}") // Noncompliant, open
  String delimiters18;

  @Value("#{{12}3{4{5}6}") // Noncompliant
  String delimiters19;

  @Value("{ }") // Compliant
  String delimiters20;

  @Value("${ }") // Noncompliant {{Correct this malformed property placeholder.}}
  String delimiters21;

  @Value("#{ }") // Noncompliant
  String delimiters22;

  @Value("${") // Noncompliant
  String delimiters23;

  @Value("${ ") // Noncompliant
  String delimiters24;

  @Value("#{ ") // Noncompliant
  String delimiters25;

  @Value("$ {") // Compliant
  String delimiters26;

  @Value("# {") // Compliant
  String delimiters27;

  @Value("$}") // Compliant
  String delimiters28;

  @Value("$ }") // Compliant
  String delimiters29;

  @Value("# }") // Compliant
  String delimiters30;

  @Value("${3foo}") // Noncompliant
  String ncPlaceholder0;

  @Value("${foo bar}") // Noncompliant
  String ncPlaceholder1;

  @Value("${foo .bar}") // Noncompliant
  String ncPlaceholder2;

  @Value("${foo,bar}") // Noncompliant
  String ncPlaceholder3;

  @Value("${foo..bar}") // Noncompliant {{Correct this malformed property placeholder.}}
  String ncPlaceholder4;

  @Value("${foo.}") // Noncompliant {{Correct this malformed property placeholder.}}
  String ncPlaceholder5;

  @Value("${.bar}") // Noncompliant
  String ncPlaceholder6;

  @Value("${foo[10}") // Noncompliant
  String ncPlaceholder7;

  @Value("${foo 10]}") // Noncompliant
  String ncPlaceholder8;

  @Value("${foo[10 20]}") // Noncompliant
  String ncPlaceholder9;

  @Value("${foo[10.bar]}") // Noncompliant
  String ncPlaceholder10;

  @Value("${foo.bar[][20].baz}") // Noncompliant
  String ncPlaceholder11;

  @Value("${foo[]}") // Noncompliant
  String ncPlaceholder12;

  @Value("${[10]}") // Noncompliant
  String ncPlaceholder13;

  @Value("${[[10]}") // Noncompliant
  String ncPlaceholder14;

  @Value("${foo + bar}") // Noncompliant
  String ncPlaceholder15;

  @Value("${foo}") // Compliant
  String cPlaceholder0;

  @Value("${foo.bar}") // Compliant
  String cPlaceholder1;

  @Value("${foo[10]}") // Compliant
  String cPlaceholder2;

  @Value("${foo[10][20]}") // Compliant
  String cPlaceholder3;

  @Value("${foo[10].bar}") // Compliant
  String cPlaceholder4;

  @Value("${foo.bar[10][20].baz}") // Compliant
  String cPlaceholder5;

  @Value("#{foo bar}") // Noncompliant
  String spel0;

  @Value("#{foo .bar}") // Compliant
  String spel1;

  @Value("#{foo,bar}") // Noncompliant
  String spel2;

  @Value("#{foo}") // Compliant
  String spel3;

  @Value("#{foo.bar}") // Compliant
  String spel4;

  @Value("#{10 * (20 + foo)}") // Compliant
  String spel5;

  @Value("#{foo[10].bar}") // Compliant
  String spel6;

  @Value("#{foo.bar[10][20].baz}") // Compliant
  String spel7;

  @Value("#(123))") // Compliant
  String spel8;

  @Value("#())") // Compliant
  String spel9;

  @Value("#{()})") // Noncompliant
  String spel10;

  @Value("#{(42)})") // Compliant
  String spel11;
}
