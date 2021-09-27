package android.hardware.biometrics;

import android.os.CancellationSignal;

import java.util.concurrent.Executor;

public class BiometricPrompt {
  public static class AuthenticationCallback {

  }

  public static final class CryptoObject {

  }

  public void authenticate(CancellationSignal cancel, Executor executor, AuthenticationCallback callback) {

  }

  public void authenticate(CryptoObject crypto, CancellationSignal cancel, Executor executor, AuthenticationCallback callback) {

  }
}
