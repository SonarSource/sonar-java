class SwitchByString {

  public int example(String param) {
    switch (param) {
      case "Java 5":
        return 5;
      case "Java 6":
        return 6;
      case "Java 7":
        return 7;
      default:
        throw new IllegalArgumentException();
    }
  }

}
