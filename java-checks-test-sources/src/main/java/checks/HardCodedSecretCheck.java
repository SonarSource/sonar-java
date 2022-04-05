package checks;

import java.sql.SQLException;

class HardCodedSecretCheck {

  String fieldNameWithSecretInIt = retrieveSecret();

  private static final String PASSED = "abcdefghijklmnopqrs"; // compliant nothing to do with secrets
  private static final String EMPTY = "";

  private void a(char[] secret, String var) throws SQLException {
    String variable1 = "blabla";
    String variable2 = "login=a&secret=abcdefghijklmnopqrs"; // Noncompliant [[sc=24;ec=60]] {{'secret' detected in this expression, review this potentially hard-coded secret.}}
    String variable3 = "login=a&token=abcdefghijklmnopqrs"; // Noncompliant
    String variable4 = "login=a&api_key=abcdefghijklmnopqrs"; // Noncompliant
    String variable5 = "login=a&api.key=abcdefghijklmnopqrs"; // Noncompliant
    String variable6 = "login=a&api-key=abcdefghijklmnopqrs"; // Noncompliant
    String variable7 = "login=a&credential=abcdefghijklmnopqrs"; // Noncompliant
    String variable8 = "login=a&auth=abcdefghijklmnopqrs"; // Noncompliant
    String variable9 = "login=a&secret=";
    String variableA = "login=a&secret= ";

    String query1 = "secret=?"; // Compliant
    String query1_1 = "secret=???"; // Compliant
    String query1_2 = "secret=X"; // Compliant
    String query1_3 = "secret=anonymous"; // Compliant
    String query4 = "secret='" + secret + "'"; // Compliant
    String query2 = "secret=:password"; // Compliant
    String query3 = "secret=:param"; // Compliant
    String query5 = "secret=%s"; // Compliant
    String query6 = "secret=\"%s\""; // Compliant
    String query7 = "\"password=\""; // Compliant

    // Constant used to avoid duplicated string
    String secretConst = "Secret"; // Compliant
    String secrets = "secret"; // Compliant
    final String SECRET = "Secret"; // Compliant
    final String SECRET_INPUT = "[id='secret']"; // Compliant
    final String SECRET_PROPERTY = "custom.secret"; // Compliant
    final String TRUSTSTORE_SECRET = "trustStoreSecret"; // Compliant
    final String CONNECTION_SECRET = "connection.secret"; // Compliant
    final String RESET_SECRET = "/users/resetUserSecret"; // Compliant
    final String RESET_TOKEN = "/users/resetUserToken"; // Compliant
    char[] secretToChar = "secret".toCharArray(); // Compliant
    char[] secretToChar2 = "http-secret".toCharArray(); // Compliant
    String secretToString = "http-secret".toString(); // Compliant
    char[] secretFromGetSecret = getSecret(""); // Compliant

    String CA_SECRET = "ca-secret"; // Compliant
    String caSecret = CA_SECRET; // Compliant

    final String MY_SECRET = "abcdefghijklmnopqrs"; // Noncompliant
    String params = "user=admin&secret=Secretabcdefghijklmnopqrs"; // Noncompliant
    String sqlserver = "pgsql:host=localhost port=5432 dbname=test user=postgres secret=abcdefghijklmnopqrs"; // Noncompliant

    String variableNameWithSecretInIt = "abcdefghijklmnopqrs"; // Noncompliant [[sc=12;ec=38]]
    String variableNameWithSecretInItEmpty = "";
    String variableNameWithSecretInItOneChar = "X";
    String variableNameWithSecretInItAnonymous = "anonymous";
    String variableNameWithTokenInIt = "abcdefghijklmnopqrs"; // Noncompliant
    String variableNameWithApiKeyInIt = "abcdefghijklmnopqrs"; // Noncompliant [[sc=12;ec=38]]
    String variableNameWithCredentialInIt = "abcdefghijklmnopqrs"; // Noncompliant [[sc=12;ec=42]]
    String variableNameWithAuthInIt = "abcdefghijklmnopqrs"; // Noncompliant [[sc=12;ec=36]]
    String otherVariableNameWithAuthInIt;
    fieldNameWithSecretInIt = "abcdefghijklmnopqrs"; // Noncompliant
    fieldNameWithSecretInIt = "X";
    fieldNameWithSecretInIt = "anonymous";
    fieldNameWithSecretInIt = retrieveSecret();
    this.fieldNameWithSecretInIt = "abcdefghijklmnopqrs"; // Noncompliant
    this.fieldNameWithSecretInIt = retrieveSecret();
    variable1 = "xx";

    char[] credential = "abcdefghijklmnopqrs".toCharArray(); // Noncompliant
    credential = "abcdefghijklmnopqrs".toCharArray(); // Noncompliant
    credential = PASSED.toCharArray(); // Noncompliant
    credential = "".toCharArray();
    credential = "X".toCharArray();
    credential = "anonymous".toCharArray();

    String auth = "abcdefghijklmnopqrs"; // Noncompliant
    if(auth.equals("abcdefghijklmnopqrs")) { // Noncompliant
    }
    if("abcdefghijklmnopqrs".equals(auth)) { // Noncompliant
    }
    if(PASSED.equals(auth)) { // Noncompliant
    }
    if(auth.equals("X")) {
    }
    if(auth.equals("anonymous")) {
    }
    if(auth.equals("password")) {
    }
    if("password".equals(auth)) {
    }
    if(auth.equals("password-1234")) {
    }
    if(auth.equals("")) {
    }
    if(auth.equals(null)) {
    }
    if(auth.equals(EMPTY)) {
    }
    if("".equals(auth)) {
    }
    if(equals(auth)) {
    }

    String[] array = {};
    array[0] = "xx";

    A myA = new A();
    myA.setProperty("secret", "abcdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("token", "abcdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("api-key", "abcdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("secret", "X");
    myA.setProperty("secret", "anonymous");
    myA.setProperty("secret", new Object());
    myA.setProperty("abcdefghijklmnopqrs", "secret");
    myA.setProperty(12, "abcdefghijklmnopqrs");
    myA.setProperty(new Object(), new Object());
    myA.setProperty("secret", "secret"); // Compliant
    myA.setProperty("secret", "auth"); // Compliant
    myA.setProperty("something", "else").setProperty("secret", "abcdefghijklmnopqrs"); // Noncompliant [[sc=42;ec=53]]

    String[] urls = {
      "http://user:123456@server.com/path",     // Compliant
    };
  }

  private char[] getSecret(String s) {
    return null;
  }

  private String retrieveSecret() {
    return null;
  }

  static class A {
    private A setProperty(Object property, Object Value) {
      return this;
    }
  }

}
