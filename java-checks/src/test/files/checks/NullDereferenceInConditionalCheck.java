class A{
  void method(){
    if(str == null && str.length() == 0){ //Non compliant
    }else if(str != null || str.length() > 0){ //Non compliant
    }
    if(str == null || str.length() == 0){
    }else if(str != null && str.length() > 0){
    }

    if(str == null && (str!=null && str.length()>0)){} //Non-Compliant
    if(a == null && (b != null || b.length()>0)){} //Non-Compliant
    if((str) == null && str.length() == 0){} //Non compliant
    if((str == null) && str.length() == 0){} //Non compliant
    if((str == null) && prop.str == 0){} //Compliant
    if(str == null && (str = a) == null){} //Compliant
    if(str == null && str == a){} //Compliant
    if(str == null && a == str){} //Compliant
    if(str == null && a != str){} //Compliant
    if(str == null && str != a){} //Compliant
    if(str == null && str != a){} //Compliant
    if(str == null && str != str){} //Compliant
    if(str == null && str.a.b > 0){} //Compliant
  }
  String str;
  String a;
  String b;
}
