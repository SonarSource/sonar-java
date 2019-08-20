interface Nok { // Noncompliant {{Annotate the "Nok" interface with the @FunctionalInterface annotation}}
  void m();
  private void privateMethod() {}
}

interface Ok {
  private void privateMethod() {}
}
