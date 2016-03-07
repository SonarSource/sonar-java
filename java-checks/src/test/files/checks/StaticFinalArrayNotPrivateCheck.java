class A {
  public static final String[] HEIRS = new String[]{ // Noncompliant [[sc=32;ec=37]] {{Make this array "private".}}
      "Betty", "Suzy"};
  public final String[] HEIRS2 = new String[]{
      "Betty", "Suzy"};
  public static String[] HEIRS3 = new String[]{
      "Betty", "Suzy"};
  private static final String[] HEIRS4 = new String[]{
      "Betty", "Suzy"};
  public static final String CONSTANT = "";
  static final String[] HEIRS5 = new String[]{ // Noncompliant [[sc=25;ec=31]] {{Make this array "private".}}
    "Betty", "Suzy"};
  @com.google.common.annotations.VisibleForTesting
  static final String[] HEIRS6 = new String[]{
    "Betty", "Suzy"};
  @VisibleForTesting
  static final String[] HEIRS7 = new String[]{
    "Betty", "Suzy"};
}
