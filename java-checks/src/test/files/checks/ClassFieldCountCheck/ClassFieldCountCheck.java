class TooManyFields1 { // Noncompliant
  public int field1;
  public int field2;
  public int field3;
  public int field4;
  public int field5;
  public int field6;
  public int field7;
  public int field8;
  public int field9;
  public int field10;
  public int field11;
  public int field12;
  public int field13;
  public int field14;
  public int field15;
  public int field16;
  public int field17;
  public int field18;
  public int field19;
  public int field20;
  public static int field21;
}


class Constants { // Compliant - static final are not counted
  public static final int field1;
  public static final int field2;
  public static final int field3;
  public static final int field4;
  public static final int field5;
  public static final int field6;
  public static final int field7;
  public static final int field8;
  public static final int field9;
  public static final int field10;
  private static final int field11;
  private static final int field12;
  private static final int field13;
  private static final int field14;
  private static final int field15;
  private static final int field16;
  static final int field17;
  static final int field18;
  static final int field19;
  static final int field20;
  static final int field21;

  private boolean youCanCountOnMe;
  static boolean youCanCountOnMeStatically;
}

