package checks.security;

import android.hardware.biometrics.BiometricPrompt;
import android.os.CancellationSignal;

import java.util.concurrent.Executor;

class AndroidBiometricAuthWithoutCryptoCheckSample {
  void android_no_crypto(
    BiometricPrompt bp,
    CancellationSignal cancel,
    Executor executor,
    BiometricPrompt.AuthenticationCallback callback) {

    bp.authenticate(cancel, executor, callback); // Noncompliant [[sc=8;ec=20]] {{Make sure performing a biometric authentication without a "CryptoObject" is safe here.}}
  }

  void android_crypto(
    BiometricPrompt bp,
    CancellationSignal cancel,
    Executor executor,
    BiometricPrompt.AuthenticationCallback callback,
    BiometricPrompt.CryptoObject crypto) {

    bp.authenticate(crypto, cancel, executor, callback);
  }

  void androidx_no_crypto(
    androidx.biometric.BiometricPrompt bp, androidx.biometric.BiometricPrompt.PromptInfo info) {

    bp.authenticate(info); // Noncompliant [[sc=8;ec=20]] {{Make sure performing a biometric authentication without a "CryptoObject" is safe here.}}
  }

  void androidx_crypto(
    androidx.biometric.BiometricPrompt bp,
    androidx.biometric.BiometricPrompt.PromptInfo info,
    androidx.biometric.BiometricPrompt.CryptoObject crypto) {

    bp.authenticate(info, crypto);
  }
}
