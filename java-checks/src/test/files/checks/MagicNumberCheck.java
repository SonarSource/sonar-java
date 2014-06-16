@Annotation(title= "plop", value=51)
class A {
  //All compliant
  int a = 0;
  int b = 1;
  int c = -1;

  int d = 42;
  long aLong = 12L;
  double aDouble = 12.3d;
  float aFloat = 12.3F;
  String string = "string";
  String strDouble = "123.3d";
  boolean bool = true;

  long a = 0;
  long b = 1;
  long c = -1;

  double a = 0.0d;
  double b = 1.0d;
  double c = -1.0d;

  float a = 0.0f;
  float b = 1.0f;
  float c = -1.0f;

  private static final int CONSTANT = 42;

  private static final MyType MY_TYPE = new MyType(){
    int magic = 42;
  };
}