import java.lang.Override;
import java.lang.String;

enum foo  {
  FOO{
    @Override
    public String method() {
      return "foo";
    }
  },
  BAR{
    @Override
    public String method() {
      return "bar";
    }
  };


  public String method(){
    return "";
  }
}

interface Handler {
  String handle();
}

class A {
  void toto() {
    new Handler(){
      @Override
      public String handle() {
        return "handled";
      }
    }.handle();

    new Handler(){

      private String myMethod(){
        return "plop";
      }

      @Override
      public String handle() {
        return myMethod();
      }
    }.handle();
  }

}