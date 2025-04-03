package checks.security;

import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

class PasswordEncoderJavaxCryptoSample {
  // region Noncompliant cases

  public static class NoncompliantConstantIterations {
    private static final int PBKDF2_ITERATIONS = 120000;
    //                                           ^^^^^^>

    public void noncompliantConstantIterations(String password, byte[] salt) throws Exception {
      PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256); // Noncompliant {{Use at least 210000 PBKDF2 iterations.}}
      //                                                                ^^^^^^^^^^^^^^^^^
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
      //                                                               ^^^^^^^^^^^^^^^^^^^^^^<
      secretKeyFactory.generateSecret(keySpec);
    }
  }

  public static class NoncompliantConstantAlgorithm {
    private static final String SHA512_ALGORITHM = "PBKDF2withHmacSHA512";
    //                                             ^^^^^^^^^^^^^^^^^^^^^^>

    public void noncompliantConstantAlgorithm(String password, byte[] salt) throws Exception {
      PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // Noncompliant {{Use at least 210000 PBKDF2 iterations.}}
      //                                                                ^^^^^^
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(SHA512_ALGORITHM);
      secretKeyFactory.generateSecret(keySpec);
    }
  }

  public void noncompliantNoKeyLength(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000); // Noncompliant
    //                                                                ^^^^^^
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
    //                                                               ^^^^^^^^^^^^^^^^^^^^^^<
    secretKeyFactory.generateSecret(keySpec);
  }

  public void noncompliantIntLiteralIterationWithSha512(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // Noncompliant {{Use at least 210000 PBKDF2 iterations.}}
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void noncompliantIntLiteralIterationWithSha256(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 300000, 256); // Noncompliant {{Use at least 600000 PBKDF2 iterations.}}
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void noncompliantIntLiteralIterationWithSha1(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 1_200_000, 256); // Noncompliant {{Use at least 1300000 PBKDF2 iterations.}}
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void noncompliantLocalVariableIteration(String password, byte[] salt) throws Exception {
    int iterations = 120000;
    //               ^^^^^^>
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // Noncompliant
    //                                                                ^^^^^^^^^^
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
    //                                                               ^^^^^^^^^^^^^^^^^^^^^^<
    secretKeyFactory.generateSecret(keySpec);
  }

  public void noncompliantLocalVariableIterationAndAlgorithm(String password, byte[] salt) throws Exception {
    String algorithm = "PBKDF2withHmacSHA512";
    //                 ^^^^^^^^^^^^^^^^^^^^^^>
    int iterations = 120000;
    //               ^^^^^^>
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // Noncompliant
    //                                                                ^^^^^^^^^^
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
    secretKeyFactory.generateSecret(keySpec);
  }

  public void noncompliantComplexFlow(String password, byte[] salt) throws Exception {
    int iterations = 500_000;
    //               ^^^^^^^>
    if (salt.length > 10) {
      System.out.print("Some flow between relevant code");
    }
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // Noncompliant
    //                                                                ^^^^^^^^^^
    while (salt.length < 10) {
      if (salt.hashCode() == 42) {
        System.out.print("Some other flow between relevant code");

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256");
        //                                                               ^^^^^^^^^^^^^^^^^^^^^^<
        Runnable aLambda = () -> {
          try {
            secretKeyFactory.generateSecret(keySpec);
          } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
          }
        };
      }
    }
  }

  public void noncompliantStringConcatenation(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // Noncompliant
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmac" + "SHA512");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void noncompliantToString(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // FN
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512".toString());
    secretKeyFactory.generateSecret(keySpec);
  }

  public void noncompliantInlineSecretKeyFactoryGetInstance(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // Noncompliant
    SecretKeyFactory.getInstance("PBKDF2withHmacSHA512").generateSecret(keySpec);
  }

  public void noncompliantEverythingInline(String password, byte[] salt) throws Exception {
    SecretKeyFactory.getInstance("PBKDF2withHmacSHA512").generateSecret(
      //                         ^^^^^^^^^^^^^^^^^^^^^^>
      new PBEKeySpec(password.toCharArray(), salt, 110000, 256) // Noncompliant
      //                                           ^^^^^^
    );
  }

  public void noncompliantEverythingInlineWithLet(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 110000, 256); // Noncompliant
    //                                                                ^^^^^^
    SecretKeyFactory.getInstance("PBKDF2withHmacSHA512").generateSecret(keySpec);
    //                           ^^^^^^^^^^^^^^^^^^^^^^<
  }

  public void noncompliantWithAliases(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // Noncompliant
    SecretKeyFactory.getInstance("PBKDF2withHmacSHA512").generateSecret(keySpec);
  }

  public static class NoncompliantFinalAndNonFinalInlineFieldIterationAndAlgorithm {
    private final int iteration = 110000;
    //                            ^^^^^^>
    private String algorithm = "PBKDF2withHmacSHA512";
    //                         ^^^^^^^^^^^^^^^^^^^^^^>

    public void test(String password, byte[] salt) throws Exception {
      PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iteration, 256); // Noncompliant
      //                                                                ^^^^^^^^^
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
      secretKeyFactory.generateSecret(keySpec);
    }
  }

  public static class NoncompliantFinalNonInlineCtorHardcodedFieldIterationAndAlgorithm {
    private final int iteration;
    private final String algorithm;

    public NoncompliantFinalNonInlineCtorHardcodedFieldIterationAndAlgorithm(int iteration, String algorithm) {
      this.iteration = 110000;
      this.algorithm = "PBKDF2withHmacSHA512";
    }

    public void test(String password, byte[] salt) throws Exception {
      PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iteration, 256); // FN: requires flow from ctor
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
      secretKeyFactory.generateSecret(keySpec);
    }
  }

  public void nonCompliantIterationNotInitializedInline(String password, byte[] salt) throws Exception {
    int iterations;
    iterations = 10;
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // FN: requires flow from assignment
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
    secretKeyFactory.generateSecret(keySpec);
  }

  // endregion

  // region Compliant cases

  public void compliantIntLiteralAboveThresholdForSHA512(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 210000, 256); // Compliant: 210_000 >= 210_000
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void compliantIntLiteralAboveThresholdForSHA256(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 700000, 256); // Compliant: 700_000 >= 600_000
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void compliantIntLiteralAboveThresholdForSHA1(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 1_400_000, 256); // Compliant: 1_400_000 >= 1_300_000
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void compliantUnknownAlgorithm(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // Compliant: unknown algorithm
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("unknown");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void compliantComplexFlow(String password, byte[] salt) throws Exception {
    int iterations = salt.length > 10 ? 210000 : 60000;
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // Compliant: salt.size can be anything
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
    secretKeyFactory.generateSecret(keySpec);
  }

  public void compliantWithoutSecretKeyFactoryGenerateSecret(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // Compliant: no generateSecret
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
  }

  public void compliantWithoutSecretKeyFactoryGetInstance(String password, byte[] salt, SecretKeyFactory secretKeyFactory) throws Exception {
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256); // Compliant: no getInstance
    secretKeyFactory.generateSecret(keySpec);
  }

  public void compliantMultipleKeySpec(String password, byte[] salt) throws Exception {
    PBEKeySpec keySpec1 = new PBEKeySpec(password.toCharArray(), salt, 200000, 256);
    PBEKeySpec keySpec2 = new PBEKeySpec(password.toCharArray(), salt, 220000, 256);
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
    // This is commented out: secretKeyFactory.generateSecret(keySpec1)
    secretKeyFactory.generateSecret(keySpec2); // Compliant: keySpec2 has enough iterations
  }

  public static class CompliantFinalNonInlineExternallyDefinedFieldIterationAndAlgorithm {
    private final int iteration;
    private final String algorithm;

    public CompliantFinalNonInlineExternallyDefinedFieldIterationAndAlgorithm(int iteration, String algorithm) {
      this.iteration = iteration;
      this.algorithm = algorithm;
    }

    public void test(String password, byte[] salt) throws Exception {
      PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iteration, 256); // Compliant: unknown algorithm and iteration
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
      secretKeyFactory.generateSecret(keySpec);
    }
  }

  public void compliantIterationReassigned(String password, byte[] salt) throws Exception {
    int iterations = 10;
    iterations = 210000;
    // FP: we only look at the initialization
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // Noncompliant
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA512");
    secretKeyFactory.generateSecret(keySpec);
  }

  // endregion
}
