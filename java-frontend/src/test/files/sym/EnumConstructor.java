enum EnumConstructor {
  EXAMPLE("String", 0);

  private String string;
  private Integer i;

  EnumConstructor(String string, Integer i) {
    this.string = string;
    this.i = i;
  }
}
