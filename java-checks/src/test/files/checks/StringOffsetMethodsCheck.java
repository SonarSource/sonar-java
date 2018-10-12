class A {
  String field;
  void test(String str, char char1, int beginIndex, String str2) {
    str.substring(beginIndex).indexOf(char1); // Noncompliant [[sc=9;ec=45]] {{Replace "indexOf" with the overload that accepts an offset parameter.}}
    str.indexOf(char1, beginIndex);
    this.field.indexOf(char1);
    this.fun().indexOf(char1);

    str.substring(beginIndex).indexOf(str2); // Noncompliant [[sc=9;ec=44]]
    str.indexOf(str2, beginIndex);

    str.substring(beginIndex).lastIndexOf(char1); // Noncompliant [[sc=9;ec=49]]
    str.lastIndexOf(char1, beginIndex);

    str.substring(beginIndex).lastIndexOf(str2); // Noncompliant [[sc=9;ec=48]] {{Replace "lastIndexOf" with the overload that accepts an offset parameter.}}
    str.lastIndexOf(str2, beginIndex);

    str.substring(beginIndex).startsWith(str2); // Noncompliant [[sc=9;ec=47]] {{Replace "startsWith" with the overload that accepts an offset parameter.}}
    str.startsWith(str2, beginIndex);
  }
  String fun()  {return "";}
}
