package foo;

class Utilities {
  private static String magicWord = "magic";
  private static String string = magicWord; // coverage
  private String otherWord = "other";

  public Utilities() {
  }

  private String getMagicWord() { // Noncompliant {{Make "getMagicWord" a "static" method.}}
    return magicWord;
  }
  private static String getMagicWordOK() {
    return magicWord;
  }

  private void setMagicWord(String value) { // Noncompliant {{Make "setMagicWord" a "static" method.}}
    magicWord = value;
  }
  private static void setMagicWordOK(String value) {
    magicWord = value;
  }

  private String getOtherWord() {
    return otherWord;
  }
  private String getOtherWord2() {
    return this.otherWord;
  }
  private String getOtherWord3() {
    return super.toString();
  }

  private void setOtherWord(String value) {
    otherWord = value;
    // coverage
    otherWord = value;
  }

  class Inner {
    public Inner(String a, String b) {
    }

    public final String getMagicWord() {
      return "a";
    }
  }

  static class Nested {
    private String getAWord() { // Noncompliant {{Make "getAWord" a "static" method.}}
      return "a";
    }
  }

  public void publicMethod() {
  }

  private Utilities.Inner test() {
    return new Utilities.Inner("", "");
  }

  private Unknown unknown() {
    return new Unknown("", "");
  }

}

class UtilitiesExtension extends Utilities {
  public UtilitiesExtension() {
  }
  private void method() { // Compliant
    publicMethod();
  }
}
