package checks.ClassFieldCountCheck;

class TooManyFields { // Noncompliant [[sc=7;ec=20]] {{Refactor this class so it has no more than 20 fields, rather than the 26 it currently has.}}
  int field1;
  int field2;
  int field3;
  int field4;
  int field5;
  int field6;
  int field7;
  int field8;

  public void method() {

  }

  int field9;
  int field10;
  int field11;
  int field12;
  int field13;
  int field14;
  int field15;
  int field16;
  int field17;
  int field18;
  int field19;
  int field20;
  int field21;
  int field22;
  int field23;
  int field24;
  int field25;
  int field26;
}


class B extends TooManyFields {  // Compliant , inherited fields do not count

}
