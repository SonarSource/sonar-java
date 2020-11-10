public class SuppressWarningsAtMethodLevelWithJavacRule {

  public final int value;

  @SuppressWarnings({"divzero"})
  public SuppressWarningsAtMethodLevelWithJavacRule(int arg) {
    // java:S3518 alias divzero
    this.value = arg / 0;
  }

}
