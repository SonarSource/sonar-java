class A {

  static class Inner {
    public String s1;
  }

  void method() {
    String s = "";
    Inner inner = new Inner();
    int i = 0;
    for(;i++;i<10){
      s = i + " : " + s; //Non-Compliant
      s += i;//Non-Compliant
      inner.s1 = i + " : "+inner.s1; //Non-Compliant
    }
    while(i<20){
      s = i + " : " + s; //Non-Compliant
      inner.s1 = i + inner.s1 + " : "; //Non-Compliant
      s = ((s + " : ")); //Non-Compliant
      inner = new Inner();
      i = i + 1;
      s = i + ":"; //Compliant
      inner.s1 = i + ":";
    }
    s += "a" + "b";
    s = s + "a" + "b";
    for(int i=0;i<10;i++){
      MyObject myObject = new MyObject();
      myObject.stringProperty = "a" + "b";  //Compliant, var is local in the loop
      myObject.stringProperty += "a";//Compliant, var is local in the loop
      MyObject.newInstance().stringProperty = "a"+"b"; //Compliant
      MyObject.newInstance().stringProperty += "b"; //Non-Compliant
      MyObject.newInstance().stringProperty = "b" + MyObject.newInstance().stringProperty;
    }
  }

  class MyObject {
    String stringProperty;
    static MyObject newInstance(){
      return new MyObject();
    }
  }

}