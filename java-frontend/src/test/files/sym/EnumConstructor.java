enum EnumConstructor {
  EXAMPLE("String", 0);

  private String string;
  private Integer i;

  EnumConstructor(String string, Integer i) { // unused private constructor because the constructor from the superclass was picked (strict invocation over loose invocation).
    this.string = string;
    this.i = i;
  }
}
