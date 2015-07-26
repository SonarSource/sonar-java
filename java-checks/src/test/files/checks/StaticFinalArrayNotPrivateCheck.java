class A {
  public static final String[] HEIRS = new String[]{ // Noncompliant {{Make this array "private".}}
      "Betty", "Suzy"};
  public final String[] HEIRS = new String[]{
      "Betty", "Suzy"};
  public static String[] HEIRS = new String[]{
      "Betty", "Suzy"};
  private static final String[] HEIRS = new String[]{
      "Betty", "Suzy"};
  public static final String CONSTANT = "";
}