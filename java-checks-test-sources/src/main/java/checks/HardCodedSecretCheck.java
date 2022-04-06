package checks;

import java.sql.SQLException;

/**
 * This check detect hardcoded secrets in multiples cases:
 * - 1. String literal
 * - 2. Variable declaration
 * - 3. Assignment
 * - 4. Method invocations
 * - 4.1 Equals
 * - 4.2 Setting secrets
 */
class HardCodedSecretCheck {

  String fieldNameWithSecretInIt = retrieveSecret();

  private static final String PASSED = "abcdefghijklmnopqrs"; // compliant nothing to do with secrets
  private static final String EMPTY = "";

  private void a(char[] secret, String var) throws SQLException {
    // ========== 1. String literal ==========
    // The variable name does not influence the issue, only the string is considered.
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
    String variableB = "secret=&login=abcdefghijklmnopqrs"; // Noncompliant, false-positive the secret value should stop at &
    String variableC = "Okapi-key=42, Okapia Johnstoni, Forest/Zebra Giraffe"; // Noncompliant, false-positive because "Okapi" ends with "api"
    String variableD = "gran-papi-key=Known by everybody in the world like PWD123456"; // Noncompliant, false-positive because "papi" ends with "api"
    String variableE = """
      login=a
      secret=abcdefghijklmnopqrs
      """; // false-negative, we should support text block lines, report precise location inside
    String variableF = """
      <form action="/delete?secret=abcdefghijklmnopqrs">
        <input type="text" id="item" value="42"><br><br>
        <input type="submit" value="Delete">
      </form>
      <form action="/update?api-key=abcdefghijklmnopqrs">
        <input type="text" id="item" value="42"><br><br>
        <input type="submit" value="Update">
      </form>
      """; // false-negative, we should support text block lines and several issues inside

    // Secrets starting with "?", ":", "\"", containing "%s" or with less than 2 characters are ignored
    String query1 = "secret=?abcdefghijklmnopqrs"; // Compliant
    String query1_1 = "secret=???"; // Compliant
    String query1_2 = "secret=X"; // Compliant
    String query1_3 = "secret=anonymous"; // Compliant
    String query4 = "secret='" + secret + "'"; // Compliant
    String query2 = "secret=:password"; // Compliant
    String query3 = "secret=:param"; // Compliant
    String query5 = "secret=%s"; // Compliant
    String query6 = "secret=\"%s\""; // Compliant
    String query7 = "\"secret=\""; // Compliant

    // We handle strings in a naive way, resulting in possible FP
    String params1 = "user=admin&secret=Secretabcdefghijklmnopqrs"; // Noncompliant
    String params2 = "secret=no\nuser=admin0123456789"; // Noncompliant, false-positive new line should end the secret value
    String sqlserver1= "pgsql:host=localhost port=5432 dbname=test user=postgres secret=abcdefghijklmnopqrs"; // Noncompliant
    String sqlserver2 = "pgsql:host=localhost port=5432 dbname=test secret=no user=abcdefghijklmnopqrs"; // Noncompliant, false-positive

    // URLs are reported by S2068 only.
    String[] urls = {
      "http://user:123456@server.com/path",     // Compliant
    };

    // ========== 2. Variable declaration ==========
    // The variable name should contain a secret word
    final String MY_SECRET = "abcdefghijklmnopqrs"; // Noncompliant
    String variableNameWithSecretInIt = "abcdefghijklmnopqrs"; // Noncompliant [[sc=12;ec=38]]
    String variableNameWithSecretaryInIt = "abcdefghijklmnopqrs"; // Noncompliant, false-positive Secretary is not Secret
    String variableNameWithAuthorshipInIt = "abcdefghijklmnopqrs"; // Noncompliant, false-positive Authorship is not Auth
    String variableNameWithTokenInIt = "abcdefghijklmnopqrs"; // Noncompliant
    String variableNameWithApiKeyInIt = "abcdefghijklmnopqrs"; // Noncompliant [[sc=12;ec=38]]
    String variableNameWithCredentialInIt = "abcdefghijklmnopqrs"; // Noncompliant [[sc=12;ec=42]]
    String variableNameWithAuthInIt = "abcdefghijklmnopqrs"; // Noncompliant [[sc=12;ec=36]]
    // Secrets with less than 2 characters, explicitly "anonymous", are ignored
    String variableNameWithSecretInItEmpty = "";
    String variableNameWithSecretInItOneChar = "X";
    String variableNameWithSecretInItAnonymous = "anonymous";
    String otherVariableNameWithAuthInIt;

    // Constant used to avoid duplicated string are ignored
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

    // False positives...
    String OkapiKeyboard = "what a strange QWERTY keyboard for animals"; // Noncompliant, false-positive "Okapi" and "Keyboard" is not "api" and "key"
    String OKAPI_KEYBOARD = "what a strange QWERTY keyboard for animals"; // Noncompliant, false-positive "Okapi" and "Keyboard" is not "api" and "key"
    String okApiKeyValue = "Spaces are UNEXPECTED 012 345 678"; // Noncompliant, false-positive a secret with spaces does not look like a secret
    String tokenism = "(Queen's Partner's Stored Knowledge is a Minimal Sham)"; // Noncompliant, false-positive tokenism is not token

    // ========== 3. Assignment ==========
    fieldNameWithSecretInIt = "abcdefghijklmnopqrs"; // Noncompliant
    this.fieldNameWithSecretInIt = "abcdefghijklmnopqrs"; // Noncompliant
    // Secrets with less than 2 chars are explicitly ignored
    fieldNameWithSecretInIt = "X";
    // "anonymous" is explicitly ignored
    fieldNameWithSecretInIt = "anonymous";
    // Not hardcoded
    fieldNameWithSecretInIt = retrieveSecret();
    this.fieldNameWithSecretInIt = retrieveSecret();
    variable1 = "abcdefghijklmnopqrs";

    // Same constraints apply to "toCharArray" called on a String
    char[] credential = "abcdefghijklmnopqrs".toCharArray(); // Noncompliant
    credential = "abcdefghijklmnopqrs".toCharArray(); // Noncompliant
    credential = PASSED.toCharArray(); // Noncompliant
    credential = "".toCharArray();
    credential = "X".toCharArray();
    credential = "anonymous".toCharArray();

    // ========== 4. Method invocations ==========
    // ========== 4.1 Equals ==========
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

    // ========== 4.2 Setting secrets ==========
    // When a method call has two arguments potentially containing String, we report an issue the same way we would with a variable declaration
    A myA = new A();
    myA.setProperty("secret", "abcdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("secretary", "abcdefghijklmnopqrs"); // Compliant
    myA.setProperty("token", "abcdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("tokenization", "abcdefghijklmnopqrs"); // Compliant
    myA.setProperty("api-key", "abcdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("okapi-keyboard", "abcdefghijklmnopqrs"); // Compliant
    myA.setProperty("secret", "X");
    myA.setProperty("secret", "anonymous");
    myA.setProperty("secret", new Object());
    myA.setProperty("abcdefghijklmnopqrs", "secret");
    myA.setProperty(12, "abcdefghijklmnopqrs");
    myA.setProperty(new Object(), new Object());
    myA.setProperty("secret", "secret"); // Compliant
    myA.setProperty("secret", "auth"); // Compliant
    myA.setProperty("something", "else").setProperty("secret", "abcdefghijklmnopqrs"); // Noncompliant [[sc=42;ec=53]]
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
