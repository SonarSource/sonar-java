package checks.security;


import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.h2.security.SHA256;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class HardCodedCredentialsShouldNotBeUsedCheck {
  private static String secretString = "hunter2";
  private static String secretReassignedField = "hunter2";
  private static byte[] secretByteArrayReassignedField = new byte[]{0xC, 0xA, 0xF, 0xE};
  private static char[] secretCharArrayReassignedField = new char[]{0xC, 0xA, 0xF, 0xE};
  static {
    secretReassignedField = "*******";
    secretByteArrayReassignedField = new byte[]{};
    secretCharArrayReassignedField = new char[]{'c', 'a', 'f', 'e'};
  }
  private static byte[] secretByteArray = new byte[]{0xC, 0xA, 0xF, 0xE};

  public static void nonCompliant(byte[] message) throws ServletException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
    String effectivelyConstantString = "s3cr37";
    byte[] key = effectivelyConstantString.getBytes();

    // byte array based
    SHA256.getHMAC(secretByteArray, message); // Noncompliant [[sc=20;ec=35;secondary=-7]] {{Revoke and change this password, as it is compromised.}}
    SHA256.getHMAC(key, message);  // Noncompliant [[sc=20;ec=23;secondary=-4]]
    SHA256.getHMAC(effectivelyConstantString.getBytes(), message); // Noncompliant
    SHA256.getHMAC("anotherS3cr37".getBytes(), message); // Noncompliant
    SHA256.getHMAC(secretString.getBytes(), message); // Noncompliant
    SHA256.getHMAC(secretString.getBytes(StandardCharsets.UTF_8), message); // Noncompliant
    SHA256.getHMAC(secretString.getBytes("UTF-8"), message); // Noncompliant

    // String based
    HttpServletRequest request = new HttpServletRequestWrapper(null);
    request.login("user", "password"); // Noncompliant
    request.login("user", effectivelyConstantString); // Noncompliant [[sc=27;ec=52;secondary=-15]]
    request.login("user", secretString); // Noncompliant [[sc=27;ec=39;secondary=-28]]

    KeyStore store = KeyStore.getInstance(null);

    store.getKey("", new char[]{0xC, 0xA, 0xF, 0xE}); // Noncompliant

    char[] password = new char[]{0xC, 0xA, 0xF, 0xE};
    store.getKey("", password); // Noncompliant [[sc=22;ec=30;secondary=-1]]

    String passwordAsString = "hunter2";
    store.getKey("", passwordAsString.toCharArray()); // Noncompliant [[sc=22;ec=52]]
  }

  public static void compliant(String message, String secretParameter, byte[] secretByteArrayParameter, char[] secretCharArrayParameter) throws ServletException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
    final byte[] messageAsBytes = message.getBytes(StandardCharsets.UTF_8);
    String secretReassginedVariable = "s3cr37";
    secretReassginedVariable = "very" + secretReassginedVariable;
    byte[] secretReassignedAsBytesVariable = secretReassginedVariable.getBytes(StandardCharsets.UTF_8);
    secretReassignedAsBytesVariable = message.getBytes(StandardCharsets.UTF_8);
    char[] secretReassignedAsCharsVariable = secretReassginedVariable.toCharArray();
    secretReassignedAsCharsVariable = "".toCharArray();

    SHA256.getHMAC(secretByteArrayParameter, messageAsBytes); // compliant because we do not check parameters
    SHA256.getHMAC(secretReassignedAsBytesVariable, messageAsBytes); // compliant because we do not check reassigned variables
    SHA256.getHMAC(secretByteArrayReassignedField, messageAsBytes); // compliant because we do not check reassigned variables
    SHA256.getHMAC(convertToByteArray(secretParameter), messageAsBytes); // compliant because we do not check calls to methods defined out of String

    HttpServletRequest request = new HttpServletRequestWrapper(null);
    request.login("user", secretParameter); // compliant because we do not check parameters
    request.login("user", secretReassginedVariable); // compliant because we do not check reassigned variables
    request.login("user", secretReassignedField); // compliant because we do not check reassigned fields


    KeyStore store = KeyStore.getInstance(null);
    store.getKey("", secretCharArrayParameter); // compliant because we do not check parameters
    store.getKey("", secretReassignedAsCharsVariable); // compliant because we do not check reassigned variables
    store.getKey("", secretCharArrayReassignedField); // compliant because we do not check reassigned fields
    store.getKey("", convertToCharArray(secretParameter)); // compliant because we do not check calls to methods defined out of String

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

  private static byte[] convertToByteArray(final String string) {
    return string.getBytes(StandardCharsets.UTF_8);
  }

  private static char[] convertToCharArray(final String string) {
    return string.toCharArray();
  }

  private static CharSequence convertToCharSequence(final String string) {
    return string;
  }
}
