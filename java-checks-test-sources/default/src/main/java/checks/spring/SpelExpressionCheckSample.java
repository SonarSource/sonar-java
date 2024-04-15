package checks.spring;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

public class SpelExpressionCheckSample {

  private static final String UNCLOSED = "${1 + 2 + 3";
  private static final String INVALID_SPEL = "#{1 * * 2}";
  private static final String INVALID_PROPERTY_PLACEHOLDER = "${foo.bar[}";
  private static final String VALID_PROPERTY_PLACEHOLDER = "${foo.bar}";

  @Value(UNCLOSED) // Noncompliant [[sc=10;ec=18]] {{Add missing '}' for this property placeholder or SpEL expression.}}
  private String complexArgument1;

  @Value(INVALID_SPEL) // Noncompliant [[sc=10;ec=22]] {{Correct this malformed SpEL expression.}}
  private String complexArgument2;

  @Value(INVALID_PROPERTY_PLACEHOLDER) // Noncompliant [[sc=10;ec=38]] {{Correct this malformed property placeholder.}}
  private String complexArgument3;

  @Value(value = UNCLOSED) // Noncompliant [[sc=18;ec=26]] {{Add missing '}' for this property placeholder or SpEL expression.}}
  private String complexArgument4;

  @Value(value = INVALID_SPEL) // Noncompliant [[sc=18;ec=30]] {{Correct this malformed SpEL expression.}}
  private String complexArgument5;

  @Value(value = INVALID_PROPERTY_PLACEHOLDER) // Noncompliant [[sc=18;ec=46]] {{Correct this malformed property placeholder.}}
  private String complexArgument6;

  @Value(value = "${1 + 2 + 3") // Noncompliant [[sc=19;ec=30]] {{Add missing '}' for this property placeholder or SpEL expression.}}
  private String complexArgument7;

  @Value(value = "#{1 * * 2}") // Noncompliant [[sc=19;ec=29]] {{Correct this malformed SpEL expression.}}
  private String complexArgument8;

  @Value(value = "${foo.bar[}") // Noncompliant [[sc=19;ec=30]] {{Correct this malformed property placeholder.}}
  private String complexArgument9;

  @Value(value = "#{1 + 2 + 3}") // Compliant
  private String complexArgument10;

  @Value(value = VALID_PROPERTY_PLACEHOLDER) // Compliant
  private String complexArgument11;

  @Value("#{systemProperties['user.region'}") // Noncompliant [[sc=11;ec=44]] {{Correct this malformed SpEL expression.}}
  private String region1;

  @Value("#{systemProperties['user.region']}") // Compliant
  private String region2;

  @Value("${user.region}") // Compliant
  private String region3;

  @Value("${user.2region}") // Compliant
  private String region4;

  @Value("${user.region:defaultRegion}") // Compliant
  private String default1;

  @Value("${user.region::defaultRegion}") // Noncompliant {{Correct this malformed property placeholder.}}
  private String default2;

  @Value("${:user.region:defaultRegion}") // Noncompliant {{Correct this malformed property placeholder.}}
  private String default3;

  @Value("${user.region:defaultRegion:}") // Noncompliant
  private String default4;

  @Value("${  user.region  : defaultRegion  }") // Compliant
  private String default5;

  @Value("${user.region:#{null}}") // Compliant
  private String default6;

  @Value("${user.region:#{  null  }}") // Compliant
  private String default7;

  @Value("${user.region:#{  null + 3 }}") // Compliant
  private String default8;

  @Value("${user.region:#{  null + * 3 }}") // Noncompliant [[sc=25;ec=41]] {{Correct this malformed SpEL expression.}}
  private String default9;

  @Value("${user.region:#{'D'+'E'}}") // Compliant
  private String default10;

  @Value("${user.region:#{null}:#{null}:foo.bar}") // Noncompliant
  private String default11;

  @Value("${user.region:#{null}:#{4**4}:foo.bar}") // Noncompliant [[sc=11;ec=49]] {{Correct this malformed property placeholder.}}
  private String default12;

  @Value("${user.region:#{4**4}:#{null}:foo.bar}") // Noncompliant [[sc=25;ec=32]] {{Correct this malformed SpEL expression.}}
  private String default13;

  @Value("${user.2region:default-region}") // Compliant
  private String default14;

  @Value("${:defaultRegion}") // Noncompliant
  private String default15;

  @Value("${user.region:}") // Compliant
  private String emptyDefaultValue;

  @Value("${foo.bar:0 0 * 8 b c}") // Compliant
  private String defaultValueStringContents;

  @Value("${server.error.path:${error.path}}") // Compliant
  private String nestedPropertyValue1;

  @Value("${server.error.path:${error.path}    }") // Compliant
  private String checkTrimEMptyEnd;

  @Value("${server.error.path:${error.path:defaultErrorValue}}") // Compliant
  private String nestedPropertyValue2;

  @Value("#{'${listOfValues}' split(',')}") // Noncompliant [[sc=11;ec=42]] {{Correct this malformed SpEL expression.}}
  private List<String> valuesListNc;

  @Value("#{'${listOfValues}'.split(',')}") // Compliant
  private List<String> valuesListC;

  @Value("#{T(java.lang.Math).random() * 64h}") // Noncompliant [[sc=11;ec=46]] {{Correct this malformed SpEL expression.}}
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

    @Query("select u from User u where u.age = ?#{[0}") // Noncompliant [[sc=49;ec=54]]
    List<User> findUsersByAge2(int age);

    @Query("select u from User u where u.age = ?#{[0]") // Noncompliant [[sc=49;ec=54]] {{Add missing '}' for this property placeholder or SpEL expression.}}
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
  @RequestMapping("#{1+2+}") // Noncompliant [[sc=20;ec=27]]
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

  @Value("${}") // Noncompliant [[sc=11;ec=14]]
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

  @Value("${") // Noncompliant [[sc=11;ec=13]]
  String delimiters23;

  @Value("${ ") // Noncompliant [[sc=11;ec=14]]
  String delimiters24;

  @Value("#{ ") // Noncompliant [[sc=11;ec=14]]
  String delimiters25;

  @Value("#{ " + "") // Noncompliant
  String delimiters25_2;

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

  @Value("${3foo}") // Compliant
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

  @Value("${foo-bar}") // Compliant
  String cPlaceholder6;

  @Value("${foo[10].bar.bar-baz}") // Compliant
  String cPlaceholder7;

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
