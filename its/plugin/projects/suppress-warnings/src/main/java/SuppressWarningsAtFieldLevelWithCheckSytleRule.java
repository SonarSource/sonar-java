public class SuppressWarningsAtFieldLevelWithCheckSytleRule {
  @SuppressWarnings("checkstyle:com.puppycrawl.tools.checkstyle.checks.coding.ArrayTrailingCommaCheck")
  int[] var1 = new int[]{
      1,
      2,
      3,
      4,
      5,
    };

  int[] var2 = new int[]{
      1,
      2,
      3,
      4,
      5
    };
}
