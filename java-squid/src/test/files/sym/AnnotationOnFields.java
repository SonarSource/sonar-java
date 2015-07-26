public class App {

  @Deprecated
  private static String test2,test1;
  @Deprecated
  private static String test3;

  public static void main( String[] args ) {
    test1 = "test1";
    test2 = "test2";
    test3 = "test3";
    System.out.println( "Hello World! " + test1 + test2 + test3 );
  }
}