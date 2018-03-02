class test1 {
  int a = 1;
  boolean flag = true;
 
  boolean flag2 = !!flag;  // Noncompliant  {{Remove multiple operator prefixes.}}
  int a1 = ~~~a; // Noncompliant {{Remove multiple operator prefixes.}}

  boolean flag3 = !!!flag; // Noncompliant {{Remove multiple operator prefixes.}}
  int a2 = ~~a; // Noncompliant {{Remove multiple operator prefixes.}}

  boolean flag4 = !!!flag2; // Noncompliant [[sc=19;ec=21]] {{Remove multiple operator prefixes.}}

  boolean flag5 = !(!flag4); // Noncompliant [[sc=19;ec=22]] {{Remove multiple operator prefixes.}}

  int c = ~(~(~a3));    // Noncompliant [[sc=11;ec=14]] {{Remove multiple operator prefixes.}}
  
  boolean flag6 = !(!(!flag4)); // Noncompliant [[sc=19;ec=22]] {{Remove multiple operator prefixes.}}
  
  int a3 =  - - -a2;  // Noncompliant [[sc=13;ec=16]] {{Remove multiple operator prefixes.}} 
  
  int a4 =  - -a2;  // Noncompliant [[sc=13;ec=16]] {{Remove multiple operator prefixes.}}
  
  int a5 =  + + +a2;  // Noncompliant [[sc=13;ec=16]] {{Remove multiple operator prefixes.}}
  
  int a6 =  + +a2;  // Noncompliant [[sc=13;ec=16]] {{Remove multiple operator prefixes.}}
  
  int a7 = --a2;  //Compliant
  int a8 = ++a2;  //Compliant
  boolean flag1 = !flag4;  // Compliant
  int b = ~a1;  //Compliant
 }