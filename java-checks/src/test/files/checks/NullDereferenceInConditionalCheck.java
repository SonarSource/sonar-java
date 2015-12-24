class A{
  void method(){
    if(str == null && str.length() == 0){ // Noncompliant [[sc=8;ec=40]] {{Either reverse the equality operator in the "str" null test, or reverse the logical operator that follows it.}}
    }else if(str != null || str.length() > 0){ // Noncompliant
    }
    if(str == null || str.length() == 0){
    }else if(str != null && str.length() > 0){
    }

    if(str == null && (str!=null && str.length()>0)){} // Noncompliant
    if(a == null && (b != null || b.length()>0)){} // Noncompliant {{Either reverse the equality operator in the "b" null test, or reverse the logical operator that follows it.}}
    if((str) == null && str.length() == 0){} // Noncompliant
    if((str == null) && str.length() == 0){} // Noncompliant
    if((str == null) && prop.str == 0){}
    if(str == null && (str = a) == null){}
    if(str == null && str == a){}
    if(str == null && a == str){}
    if(str == null && a != str){}
    if(str == null && str != a){}
    if(str == null && str != a){}
    if(str == null && str != str){}
    if(str == null && str.a.b > 0){} // Noncompliant
  }
  String str;
  String a;
  String b;
}
