class A {
  public int foo() {
    int i = 0;
    if (i <= 1) // Noncompliant [[sc=5;ec=16;secondary=5,6]] {{Use curly braces or indentation to denote the code conditionally executed by this "if".}}
    i = 1;
    i++;

    if(i > 0)   // Noncompliant [[sc=5;ec=14;secondary=10,12]]
    // ......
    doTheOtherThing();
    //.....
    doTheThing();

    if (i <= 1) {  // Compliant
      i = 1;
      i = 2;
      i++;
    }

    if (i <= 1)   // Compliant
      i = 1;

    if (i == 0)
          return 1;

    if(i <= 11) // Noncompliant [[sc=5;ec=16;secondary=27,28,29]]
    doTheThing();
    doTheOtherThing();
    somethingElseEntirely();

    if (i != 0)
        i++;

    if(i <= 1)  // Noncompliant

    i=1;
    i++;

    if(i==10)   // Noncompliant


    i=10;

    if (i == 5) // Noncompliant
    doTheThing();
    else if(i == 6)   // Noncompliant
    somethingElseEntirely();
    else         // Noncompliant
    doTheThing();
    doTheOtherThing();

      if(i == 0)  // Noncompliant
    doTheThing();
      else          // Noncompliant   [sc=7;ec=11;secondary=55] {{Use curly braces or indentation to denote the code conditionally executed by this "else".}}
    doTheOtherThing();

    if(i > 10)
      i =100;
    i++;

    for (; i <= 1;) // Noncompliant [[sc=5;ec=20;secondary=62,63]] {{Use curly braces or indentation to denote the code conditionally executed by this "for".}}
    i = 1;
    i++;

    for (; i <= 1;) { // Compliant
     i = 1;
     i++;
    }

    for (; i <= 1;) // Compliant
      i = 1;

    for (; i <= 1;) // Compliant
      i = 1;
    i=2;

    while (i <= 10)  // Compliant
      i++;

    while (i <= 10) {  // Compliant
      i++;
      i += 10;
    }

    while (i <= 10)  // Compliant
      i++;
    i += 10;

    while(i<=11)     // Noncompliant [[sc=5;ec=17;secondary=90,91]] {{Use curly braces or indentation to denote the code conditionally executed by this "while".}}
    i++;
    i+=10;

    while(i<=11)     // Noncompliant [[sc=5;ec=17;secondary=94]] {{Use curly braces or indentation to denote the code conditionally executed by this "while".}}
  i++;

    List<Integer> l;
    for(Integer ii : l) // Compliant
    {}

    for(Integer ii : l) // Compliant
      return i;

    for(Integer ii : l) // Noncompliant [[sc=5;ec=24;secondary=104,105,106]] {{Use curly braces or indentation to denote the code conditionally executed by this "for".}}
    i=1;
    i=2;
    return i;

    for(Integer ii : l) // Noncompliant [[sc=5;ec=24;secondary=109]] {{Use curly braces or indentation to denote the code conditionally executed by this "for".}}
   if(ii == 1)  // Noncompliant [[sc=4;ec=15;secondary=110]] {{Use curly braces or indentation to denote the code conditionally executed by this "if".}}
   i++;
   i=2;

    for(Integer ii : l) // Noncompliant [[sc=5;ec=24;secondary=114]] {{Use curly braces or indentation to denote the code conditionally executed by this "for".}}
 i=1;

    if(i ==1)   // Noncompliant
  i++;
    else
      i++;

    for(int i =0;i<10;i++)  // Noncompliant [[sc=5;ec=27;secondary=122]]  {{Use curly braces or indentation to denote the code conditionally executed by this "for".}}
  if(i==1)
    i++;

    return i;  
  }

  public void doTheThing() {};
  public void doTheOtherThing() {};
  public void somethingElseEntirely() {};
}
