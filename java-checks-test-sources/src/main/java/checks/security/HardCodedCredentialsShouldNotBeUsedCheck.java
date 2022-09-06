package checks.security;


import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.h2.security.SHA256;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import static java.lang.System.getProperty;

public class HardCodedCredentialsShouldNotBeUsedCheck {
  static final String FINAL_SECRET_STRING = "hunter2";
  static final byte[] FINAL_SECRET_BYTE_ARRAY = FINAL_SECRET_STRING.getBytes(StandardCharsets.UTF_8);
  private static String secretStringField = "hunter2";
  private static byte[] secretByteArrayField = new byte[]{0xC, 0xA, 0xF, 0xE};
  private static char[] secretCharArrayField = new char[]{0xC, 0xA, 0xF, 0xE};
  private static CharSequence secretCharSequenceField = "Hello, World!".subSequence(0, 12);

  public static void nonCompliant(byte[] message) throws ServletException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
    String effectivelyConstantString = "s3cr37";
    byte[] key = effectivelyConstantString.getBytes();

    // byte array based
    SHA256.getHMAC(FINAL_SECRET_BYTE_ARRAY, message); // Noncompliant [[sc=20;ec=43;secondary=29]] {{Revoke and change this password, as it is compromised.}}
    SHA256.getHMAC(key, message);  // Noncompliant [[sc=20;ec=23;secondary=-4]]
    SHA256.getHMAC(effectivelyConstantString.getBytes(), message); // Noncompliant
    SHA256.getHMAC("anotherS3cr37".getBytes(), message); // Noncompliant
    SHA256.getHMAC(FINAL_SECRET_STRING.getBytes(), message); // Noncompliant
    SHA256.getHMAC(FINAL_SECRET_STRING.getBytes(StandardCharsets.UTF_8), message); // Noncompliant
    SHA256.getHMAC(FINAL_SECRET_STRING.getBytes("UTF-8"), message); // Noncompliant
    SHA256.getHMAC((FINAL_SECRET_STRING).getBytes("UTF-8"), message); // Noncompliant
    SHA256.getHMAC(new byte[1], message); // Noncompliant

    // String based
    HttpServletRequest request = new HttpServletRequestWrapper(null);
    request.login("user", "password"); // Noncompliant
    request.login("user", effectivelyConstantString); // Noncompliant [[sc=27;ec=52;secondary=36]]
    request.login("user", FINAL_SECRET_STRING); // Noncompliant [[sc=27;ec=46;secondary=28]]
    String plainTextSecret = new String("BOOM");
    request.login("user", plainTextSecret); // Noncompliant
    request.login("user", new String("secret")); // Noncompliant

    KeyStore store = KeyStore.getInstance(null);

    store.getKey("", new char[]{0xC, 0xA, 0xF, 0xE}); // Noncompliant

    char[] password = new char[]{0xC, 0xA, 0xF, 0xE};
    store.getKey("", password); // Noncompliant [[sc=22;ec=30;secondary=-1]]

    String passwordAsString = "hunter2";
    store.getKey("", passwordAsString.toCharArray()); // Noncompliant [[sc=22;ec=52]]

    Encryptors.delux(effectivelyConstantString.subSequence(0, effectivelyConstantString.length()), effectivelyConstantString); // Noncompliant [[sc=22;ec=98]]
    Encryptors.delux("password".subSequence(0, 0), "salt"); // Noncompliant

    new Pbkdf2PasswordEncoder("secret"); // Noncompliant
    new Pbkdf2PasswordEncoder(("secret")); // Noncompliant
  }

  public static void compliant(String message, String secretParameter, byte[] secretByteArrayParameter, char[] secretCharArrayParameter, CharSequence charSequenceParameter)
    throws ServletException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
    final byte[] messageAsBytes = message.getBytes(StandardCharsets.UTF_8);
    String secretReassginedVariable = "s3cr37";
    secretReassginedVariable = "very" + secretReassginedVariable;
    byte[] secretReassignedAsBytesVariable = secretReassginedVariable.getBytes(StandardCharsets.UTF_8);
    secretReassignedAsBytesVariable = message.getBytes(StandardCharsets.UTF_8);
    char[] secretReassignedAsCharsVariable = secretReassginedVariable.toCharArray();
    secretReassignedAsCharsVariable = "".toCharArray();

    SHA256.getHMAC(secretByteArrayParameter, messageAsBytes); // compliant because we do not check parameters
    SHA256.getHMAC(secretReassignedAsBytesVariable, messageAsBytes); // compliant because we do not check reassigned variables
    SHA256.getHMAC(secretByteArrayField, messageAsBytes); // compliant because we do not check non-final fields
    SHA256.getHMAC(convertToByteArray(secretParameter), messageAsBytes); // compliant because we do not check calls to methods defined out of String

    HttpServletRequest request = new HttpServletRequestWrapper(null);
    request.login("user", secretParameter); // compliant because we do not check parameters
    request.login("user", secretReassginedVariable); // compliant because we do not check reassigned variables
    request.login("user", secretStringField); // compliant because we do not check non-final fields
    request.login("user", getAString()); // compliant
    request.login("user", new String()); // compliant
    request.login("user", getProperty("hope")); // compliant
    request.login("user", ""); // compliant
    final String emptyString = "";
    request.login("user", emptyString); // compliant


    KeyStore store = KeyStore.getInstance(null);
    store.getKey("", secretCharArrayParameter); // compliant because we do not check parameters
    store.getKey("", secretReassignedAsCharsVariable); // compliant because we do not check reassigned variables
    store.getKey("", secretCharArrayField); // compliant because we do not check non-final fields
    store.getKey("", convertToCharArray(secretParameter)); // compliant because we do not check calls to methods defined out of String
    store.getKey("", new char[0]); // compliant because we don't consider empty arrays


    CharSequence secretReassignedAsCharSequence = "hello".subSequence(0, 4);
    secretReassignedAsCharSequence = "world".subSequence(0, 4);

    Encryptors.delux(charSequenceParameter, "salt");  // compliant because we do not check parameters
    Encryptors.delux(secretReassignedAsCharSequence, "salt"); // compliant because we do not check reassigned variables
    Encryptors.delux(secretCharSequenceField, "salt"); // compliant because we do not check non-final fields
    Encryptors.delux(convertToCharSequence("password"), "salt"); // compliant because we do not check calls to methods defined out of String

    StringBuilder passwordFromStringBuilder = new StringBuilder();
    passwordFromStringBuilder.append("secret");
    Encryptors.delux(passwordFromStringBuilder.subSequence(0, 0), "salt"); // compliant because we do not check CharSequences that are not derived from String.subSequence

  }

  public static void compliantAzure(SecretClient secretClient, String secretName, byte[] message) {
    KeyVaultSecret retrievedSecret = secretClient.getSecret(secretName);
    String secret = retrievedSecret.getValue();

    byte[] key = secret.getBytes();
    SHA256.getHMAC(key, message);
  }

  public static void compliantAws(SecretsManagerClient secretsClient, String secretName, byte[] message) {
    GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
      .secretId(secretName)
      .build();

    GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
    String secret = valueResponse.secretString();

    byte[] key = secret.getBytes();
    SHA256.getHMAC(key, message);
  }

  public static void compliantFromEnvironment(KeyStore keyStore, InputStream in) throws CertificateException, IOException, NoSuchAlgorithmException {
    String defaultKeyStorePassword = getProperty("MY_SECRET");

    char[] passwd = defaultKeyStorePassword.toCharArray();
    keyStore.load(in, passwd);  // Compliant, we should not raise when the password is recovered from an external source

    String withDefault = getProperty("MY_SECRET", "DEFAULT");
    char[] passwdWithDefaultFallback = withDefault.toCharArray();
    keyStore.load(in, passwdWithDefaultFallback); // Compliant, we should not raise when the password is recovered from an external source

    char[] conditionalPasswd = defaultKeyStorePassword == null ? new char[0] : defaultKeyStorePassword.toCharArray();
    keyStore.load(in, conditionalPasswd); // Compliant, we should not raise when the password is recovered from a conditional
  }

  public static void nonCompliantFromNewObject(String parameterSecret) throws ServletException {
    // String based
    HttpServletRequest request = new HttpServletRequestWrapper(null);
    String secret = new String(parameterSecret);
    request.login("user", secret); // Compliant
  }

  private static byte[] convertToByteArray(final String string) {
    return string.getBytes(StandardCharsets.UTF_8);
  }

  private static char[] convertToCharArray(final String string) {
    return string.toCharArray();
  }

  private static CharSequence convertToCharSequence(final String string) {
    return string;
  }

  private static String getAString() {
    return "secret";
  }
}
