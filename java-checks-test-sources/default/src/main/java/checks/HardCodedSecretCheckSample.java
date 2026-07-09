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
class HardCodedSecretCheckSample {

  String fieldNameWithSecretInIt = retrieveSecret();

  private static final String PASSED = "bacdefghijklmnopqrs"; // compliant nothing to do with secrets
  private static final String EMPTY = "";

  private void a(char[] secret, String var) throws SQLException {
    // ========== 1. String literal ==========
    // The variable name does not influence the issue, only the string is considered.
    String variable1 = "blabla";
    String variable2 = "login=a&secret=abcdefghijklmnopqrs"; // Compliant, fake secret filter (contains "abcd")
    String variable3 = "login=a&token=bacdefghijklmnopqrs"; // Noncompliant
//                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variable4 = "login=a&api_key=bacdefghijklmnopqrs"; // Noncompliant
    String variable5 = "login=a&api.key=bacdefghijklmnopqrs"; // Noncompliant
    String variable6 = "login=a&api-key=bacdefghijklmnopqrs"; // Noncompliant
    String variable7 = "login=a&credential=bacdefghijklmnopqrs"; // Noncompliant
    String variable8 = "login=a&auth=bacdefghijklmnopqrs"; // Noncompliant
    String variable9 = "login=a&secret=";
    String variableA = "login=a&secret= ";
    String variableB = "secret=&login=bacdefghijklmnopqrs"; // Compliant
    String variableC = "Okapi-key=42, Okapia Johnstoni, Forest/Zebra Giraffe"; // Compliant
    String variableD = "gran-papi-key=Known by everybody in the world like PWD213456"; // Compliant
    String variableE = """
      login=a
      secret=bacdefghijklmnopqrs
      """; // false-negative, we should support text block lines, report precise location inside
    String variableF = """
      <form action="/delete?secret=bacdefghijklmnopqrs">
        <input type="text" id="item" value="42"><br><br>
        <input type="submit" value="Delete">
      </form>
      <form action="/update?api-key=bacdefghijklmnopqrs">
        <input type="text" id="item" value="42"><br><br>
        <input type="submit" value="Update">
      </form>
      """; // false-negative, we should support text block lines and several issues inside

    // Secrets starting with "?", ":", "\"", containing "%s" or with short lenght
    String query1 = "secret=?bacdefghijklmnopqrs"; // Noncompliant
    String query1_1 = "secret=???"; // Compliant
    String query1_2 = "secret=X"; // Compliant
    String query1_3 = "secret=anonymous"; // Compliant, bellow the enthropy threshold
    String query4 = "secret='" + secret + "'"; // Compliant
    String query2 = "secret=:password"; // Compliant
    String query3 = "secret=:param"; // Compliant
    String query5 = "secret=%s"; // Compliant
    String query6 = "secret=\"%s\""; // Compliant
    String query7 = "\"secret=\""; // Compliant

    String params1 = "user=admin&secret=Secret1023456789021345678"; // Noncompliant
    String params2 = "secret=no\nuser=admin0213456789"; // Compliant
    String sqlserver1= "pgsql:host=localhost port=5432 dbname=test user=postgres secret=bacdefghijklmnopqrs"; // Noncompliant
    String sqlserver2 = "pgsql:host=localhost port=5432 dbname=test secret=no user=bacdefghijklmnopqrs"; // Compliant

    // Spaces and & are not included into the token, it shows us the end of the token.
    String params3 = "token=bacdefghijklmnopqrs user=admin"; // Noncompliant
    String params4 = "token=bacdefghijklmnopqrs&user=admin"; // Noncompliant

    String params5 = "token=213456&bacdefghijklmnopqrs"; // Compliant, FN, even if "&" is accepted in a password, it also indicates a cut in a string literal
    String params6 = "token=213456:bacdefghijklmnopqrs"; // Noncompliant

    // URLs are reported by S2068 only.
    String[] urls = {
      "http://user:213456@server.com/path",     // Compliant
    };

    // ========== 2. Variable declaration ==========
    // The variable name should contain a secret word
    final String MY_SECRET = "bacdefghijklmnopqrs"; // Noncompliant
    String variableNameWithSecretInIt = "bacdefghijklmnopqrs"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variableNameWithSecretaryInIt = "bacdefghijklmnopqrs"; // Noncompliant
    String variableNameWithAuthorshipInIt = "bacdefghijklmnopqrs"; // Noncompliant
    String variableNameWithTokenInIt = "bacdefghijklmnopqrs"; // Noncompliant
    String variableNameWithApiKeyInIt = "bacdefghijklmnopqrs"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variableNameWithCredentialInIt = "bacdefghijklmnopqrs"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variableNameWithAuthInIt = "bacdefghijklmnopqrs"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^
    // Shtrong strings are ignored
    String variableNameWithSecretInItEmpty = "";
    String variableNameWithSecretInItOneChar = "X";
    String otherVariableNameWithAuthInIt;

    // Secret containing words and random characters should be filtered
    String secret001 = "sk_live_xf2fh0Hu3LqXlqqUg2DEWhEz"; // Noncompliant
    String secret002 = "examples/commit/16ad89c4172c259f15bce56e";
    String secret003 = "examples/commit/8e1d746900f5411e9700fea0"; // Compliant, excluded by the classifier (matches the "example" substring), not by the human-language check
    String secret004 = "examples/commit/revision/469001e9700fea0";
    String secret005 = "xml/src/main/java/org/xwiki/xml/html/file";
    String secret006 = "bacdefghijklmnop"; // Compliant, under the enthropy threshold 
    String secret007 = "bacdefghijklmnopq"; // Noncompliant
    String secret008 = "0213456789bacdef0"; // Noncompliant
    String secret009 = "021345678902134567890213456789"; // Noncompliant
    String secret010 = "bacdefghijklmnopbacdefghijkl"; // Noncompliant
    String secret011 = "021345670213456702134567021345";
    String secret012 = "021345678021345678021345678012"; // Noncompliant
    String secret013 = "234.167.076";
    String ip_secret1 = "bfee:e3e1:9a92:6617:02d5:256a:b87a:fbcc"; // Compliant: ipv6 format
    String ip_secret2 = "2001:db8:1::ab9:C0A8:102"; // Compliant: ipv6 format
    String ip_secret3 = "::ab9:C0A8:102"; // Compliant: ipv6 format
    String secret015 = "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH";
    // Example of Telegram bot token
    String secret016 = "bot213456:BAC-DEF2134ghIkl-zyx57W2v1u213ew11"; // Noncompliant
    // Secret with "&"
    String secret017 = "012&345678021345678021345&678012"; // Noncompliant
    String secret018 = "&12&345678021345678021345&67801&"; // Noncompliant


    // Don't filter when the secret is containing any of the secret word.
    String secretConst = "Secret_0213456789021345678"; // Noncompliant
    String secrets = "secret_0213456789021345678"; // Noncompliant
    final String SECRET = "Secret_0213456789021345678"; // Noncompliant
    // Simple constants will be filtered thanks to the entropy check
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

    // Backslashed, \n, \t, \r, \" not excluded
    String secretWithBackSlashes = "bacdefghij\nklmnopqrs"; // Noncompliant
    String secretWithBackSlashes2 = "bacdefghij\tklmnopqrs"; // Noncompliant
    String secretWithBackSlashes3 = "bacdefghij\rklmnopqrs"; // Noncompliant
    String secretWithBackSlashes4 = "bacdefghij\"klmnopqrs"; // Noncompliant
    String secretWithBackSlashes5 = "\\bacdefghijklmnopqrs"; // Noncompliant
    String secretWithBackSlashes6 = "bacdefghijklmnopqrs\\"; // Noncompliant
    // Starting or containing =
    String secretWithBackSlashes7 = "=bacdefghijklmnopqrs"; // Noncompliant
    String secretWithBackSlashes8 = "bacdefghijklmnopqrs="; // Noncompliant
    String secretWithBackSlashes9 = "bacdefghijklmnopqrs=="; // Noncompliant
    String secretWithBackSlashes10 = "bacdefghij=klmnopqrs"; // Noncompliant

    // Long strings but low entropy
    String OkapiKeyboard = "what a strange keyboard for animals"; // Compliant
    String OKAPI_KEYBOARD = "what a strange keyboard for animals"; // Compliant
    String okApiKeyValue = "Spaces are UNEXPECTED 012 345 678"; // Compliant
    String tokenism = "(Queen's Partner's Stored Knowledge is a Minimal Sham)"; // Compliant
    String tokenWithExcludedCharacters2 = "abcdefghij|klmnopqrs"; // Compliant, contain fake secret pattern "abcd"

    // ========== 3. Assignment ==========
    fieldNameWithSecretInIt = "bacdefghijklmnopqrs"; // Noncompliant
    this.fieldNameWithSecretInIt = "bacdefghijklmnopqrs"; // Noncompliant
    // Short lenght strings are explicitly ignored
    fieldNameWithSecretInIt = "X";
    // Not hardcoded
    fieldNameWithSecretInIt = retrieveSecret();
    this.fieldNameWithSecretInIt = retrieveSecret();
    variable1 = "bacdefghijklmnopqrs";

    // Same constraints apply to "toCharArray" called on a String
    char[] credential = "bacdefghijklmnopqrs".toCharArray(); // Noncompliant
    credential = "bacdefghijklmnopqrs".toCharArray(); // Noncompliant
    credential = PASSED.toCharArray(); // Noncompliant
    credential = "".toCharArray();
    credential = "X".toCharArray();
    credential = "anonymous".toCharArray();

    // ========== 4. Method invocations ==========
    // ========== 4.1 Equals ==========
    String auth = "bacdefghijklmnopqrs"; // Noncompliant
    if(auth.equals("bacdefghijklmnopqrs")) { // Noncompliant
    }
    if("bacdefghijklmnopqrs".equals(auth)) { // Noncompliant
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
    myA.setProperty("secret", "bacdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("secretary", "bacdefghijklmnopqrs"); // Compliant
    myA.setProperty("token", "bacdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("tokenization", "bacdefghijklmnopqrs"); // Compliant
    myA.setProperty("api-key", "bacdefghijklmnopqrs"); // Noncompliant
    myA.setProperty("okapi-keyboard", "bacdefghijklmnopqrs"); // Compliant
    myA.setProperty("secret", "X");
    myA.setProperty("secret", "anonymous");
    myA.setProperty("secret", new Object());
    myA.setProperty("bacdefghijklmnopqrs", "secret");
    myA.setProperty(12, "bacdefghijklmnopqrs");
    myA.setProperty(new Object(), new Object());
    myA.setProperty("secret", "secret"); // Compliant
    myA.setProperty("secret", "auth"); // Compliant
    myA.setProperty("something", "else").setProperty("secret", "bacdefghijklmnopqrs"); // Noncompliant
//                                       ^^^^^^^^^^^
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
