class test1 {
  int a = 1;
  boolean flag = true;
  int hash = 1;
  int hashCode = 2;
  
  boolean flag2 = !!flag;  // Noncompliant  
  int a1 = ~~~a; // Noncompliant [[sc=12;ec=14]]{{Remove multiple operator prefixes.}}

  boolean flag3 = !!!flag; // Noncompliant

  boolean flag4 = !!!foo(); // Noncompliant [[sc=19;ec=21]]

  boolean flag5 = !(!flag4); // Noncompliant [[sc=19;ec=22]] {{Remove multiple operator prefixes.}}

  int c = ~(~(~a3));    // Noncompliant [[sc=11;ec=14]] {{Remove multiple operator prefixes.}}
  
  boolean flag6 = !(!(!flag4)); // Noncompliant [[sc=19;ec=22]] {{Remove multiple operator prefixes.}}
  
  int a3 =  - - -a2;  // Noncompliant [[sc=13;ec=16]] 
  
  int a4 =  - -a2;  // Noncompliant [[sc=13;ec=16]]
  
  int a5 =  + + +a2;  // Noncompliant [[sc=13;ec=16]]
  
  int a6 =  + +a2;  // Noncompliant [[sc=13;ec=16]]
  
  int a2 = ~~a; // Compliant
  
  int a9 = ~(~a); // Compliant
  
  int a10 = ~(~(a9 - 9));  // Compliant
  
  int a7 = --a2;  // Compliant
  int a8 = ++a2;  // Compliant
  boolean flag1 = !flag4;  // Compliant
  int b = ~a1;  // Compliant
  boolean flag7 = !foo(); // Compliant
  int h1 = ~~hash; // Compliant
  int h2 = ~~hashCodep; // Compliant
}