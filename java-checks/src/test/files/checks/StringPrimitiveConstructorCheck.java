class A {

  void bar(char[] chars) {

    String empty = new String(); // Noncompliant [[sc=24;ec=30]] {{Remove this "String" constructor}}
    String nonempty = new String("Hello world"); // Noncompliant
    nonempty = new String(chars);
    Double myDouble = new Double(1.1); // Noncompliant [[sc=27;ec=33]] {{Remove this "Double" constructor}}
    Integer integer = new Integer(1); // Noncompliant [[sc=27;ec=34]] {{Remove this "Integer" constructor}}
    Boolean bool = new Boolean(true); // Noncompliant [[sc=24;ec=31]] {{Remove this "Boolean" constructor}}
    Character myChar = new Character('c'); // Noncompliant [[sc=28;ec=37]] {{Remove this "Character" constructor}}
    Long myLong = new Long(1L); // Noncompliant [[sc=23;ec=27]] {{Remove this "Long" constructor}}
    Byte myByte = new Byte(b); // Noncompliant [[sc=23;ec=27]] {{Remove this "Byte" constructor}}
    Short myShort = new Short((short) 0); // Noncompliant [[sc=25;ec=30]] {{Remove this "Short" constructor}}
    Float myFloat = new Float(1.0f); // Noncompliant [[sc=25;ec=30]] {{Remove this "Float" constructor}}
    byte b = 0;
  }

  void foo() {
    String empty = "";
    String nonempty = "Hello world";
    Double myDouble = Double.valueOf(1.1);
    Integer integer = Integer.valueOf(1);
    Boolean bool = Boolean.valueOf(true);
  }
}