package checks;

interface notAnnotated { // Compliant - the @FunctionalInterface annotation does not exists before java 8
  public int transform(int a);
}
