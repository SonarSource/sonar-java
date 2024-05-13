package checks.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

public class AndroidNonAuthenticatedUsersCheckSample {

  KeyGenParameterSpec.Builder builderField;

  void f(boolean cond) {
    new KeyGenParameterSpec.Builder("test_secret_key_noncompliant", // Noncompliant {{Make sure authorizing non-authenticated users to use this key is safe here.}}
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^
      KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .build();

    new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT) // Noncompliant
      .setUserAuthenticationRequired(false)
      .build();

    new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT) // Compliant
      .setUserAuthenticationRequired(true)
      .build();

    // Compliant
    KeyGenParameterSpec.Builder builderInProgress = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    builderInProgress.setUserAuthenticationRequired(true);
    builderInProgress.build();

    // Compliant
    KeyGenParameterSpec.Builder builderInProgress2 = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    builderInProgress2
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setUserAuthenticationRequired(true);
    builderInProgress2.build();

 // Noncompliant@+1
    KeyGenParameterSpec.Builder builderInProgress3 = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    builderInProgress3
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setUserAuthenticationRequired(false);
    builderInProgress3.build();

    // Compliant
    KeyGenParameterSpec.Builder builderInProgress4 = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT)
      .setUserAuthenticationRequired(true);
    builderInProgress4.setBlockModes(KeyProperties.BLOCK_MODE_GCM);
    builderInProgress4.build();

 // Noncompliant@+1
    KeyGenParameterSpec.Builder builderInProgress5 = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT)
      .setUserAuthenticationRequired(false);
    builderInProgress5.setBlockModes(KeyProperties.BLOCK_MODE_GCM);
    builderInProgress5.build();

    // Compliant, consider it as authenticated to avoid FP.
    KeyGenParameterSpec.Builder builderConditionallyAuthenticated = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    if (cond) {
      builderConditionallyAuthenticated.setUserAuthenticationRequired(true);
    }
    builderConditionallyAuthenticated.build();

 // Noncompliant@+1
    KeyGenParameterSpec.Builder builderNotAuthenticated = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    builderNotAuthenticated.build();

    // Compliant, authenticated in another method
    KeyGenParameterSpec.Builder builderAuthenticatedInOtherMethod = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    authenticating(builderAuthenticatedInOtherMethod);
    builderAuthenticatedInOtherMethod.build();

    // Compliant, FN, consider as authenticated to avoid FP
    KeyGenParameterSpec.Builder builderNotAuthenticatedInOtherMethod = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    setBlockModes(builderNotAuthenticatedInOtherMethod);
    builderNotAuthenticatedInOtherMethod.build();

    // Compliant, fields be authenticated anywhere
    builderField.build();

    // Compliant, can be secured somewhere else.
    getBuilder().build();

    // Compliant when not initialized directly, to avoid FP.
    KeyGenParameterSpec.Builder builderAssignedAfter;
    builderAssignedAfter = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    builderAssignedAfter.build();

    KeyGenParameterSpec.Builder builderReAssigned = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    builderReAssigned.setUserAuthenticationRequired(true);
    builderReAssigned.build();
    // Compliant when re-assigned, corner case
    builderReAssigned = new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    builderReAssigned.build();
  }

  void valueAsArgument(boolean arg) {
 // Noncompliant@+1
    new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT)
      // Corner case: can not guarantee that the value is always true, we still report an issue.
      .setUserAuthenticationRequired(arg)
      .build();
  }

  void comingFromArgument(KeyGenParameterSpec.Builder builder) {
    builder.build(); // Compliant, can be signed somewhere else
  }

  void authenticating(KeyGenParameterSpec.Builder builder) {
    builder.setUserAuthenticationRequired(true);
  }

  void setBlockModes(KeyGenParameterSpec.Builder builder) {
    builder.setBlockModes(KeyProperties.BLOCK_MODE_GCM);
  }

  KeyGenParameterSpec.Builder getBuilder() {
    // Compliant, can be secured later
    return new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
  }

  // For coverage
  void throughWrapper() {
    // Compliant, could be authenticated somewhere else
    new Wrapper()
      .getBuilder()
      .build();
  }

  static class Wrapper {
    KeyGenParameterSpec.Builder getBuilder() {
      return new KeyGenParameterSpec.Builder("test_secret_key", KeyProperties.PURPOSE_ENCRYPT);
    }
  }
}
