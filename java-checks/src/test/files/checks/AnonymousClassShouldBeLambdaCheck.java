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
    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda}}
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

    new Handler(){
      @Override
      public String handle() {
        return this.toString();
      }
    }.handle();

    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda}}
      @Override
      public String handle() {
        class C{
          String meth(){
            return "";
          }
          String fun(){
            return this.meth();
          }
        }
        return new C().fun();
      }
    };
    
    new Handler(){ // Compliant
      int myVar;
      
      @Override
      public String handle() {
        return "";
      }
    };
    
    new Handler(){ // Noncompliant {{Make this anonymous inner class a lambda}}
      @Override
      public String handle() {
        return "";
      }; // this empty statement should not be counted!
    };

    new
    Handler // Noncompliant {{Make this anonymous inner class a lambda}}
    (){
      @Override
      public String handle() {
        return A.this.toStr();
      }
    };
  }

  String toStr(){
    return "";
  }

  interface MyHandler extends Handler{}
  public abstract class Main {

    public abstract void myMethod();

    public static void main(String[] args) {
      Main main = new Main() {

        @Override
        public void myMethod() {
        }
      };
      main.myMethod();
      Object o1 = new Object() {
        public String toString(){
          return null;
        }
      };
      Object o12 = new MyHandler() { // Noncompliant
        @Override
        public String handle() {
          return null;
        }
      };
    }
  }




}
