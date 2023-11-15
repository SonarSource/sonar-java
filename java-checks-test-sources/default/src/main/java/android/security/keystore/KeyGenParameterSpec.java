package android.security.keystore;

public final class KeyGenParameterSpec {

  public static final class Builder {

    public Builder(String keystoreAlias, int purposes) { }

    public Builder setBlockModes(String... blockModeGcm) {
      return this;
    }

    public Builder setEncryptionPaddings(String... paddings) {
      return this;
    }

    public Builder setUserAuthenticationRequired(boolean b) {
      return this;
    }

    public KeyGenParameterSpec build() {
      return null;
    }
  }
}
