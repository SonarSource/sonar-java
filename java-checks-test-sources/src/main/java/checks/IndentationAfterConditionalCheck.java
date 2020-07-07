package checks;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

class IndentationAfterConditionalCheck {
  public int foo() throws IOException {
    int i = 0;
    if (i <= 1) // Noncompliant [[sc=5;ec=16;secondary=11]] {{Use indentation to denote the code conditionally executed by this "if".}}
    i = 1;
    i++;

    if(i > 0)   // Noncompliant [[sc=5;ec=14;secondary=16]]
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

    if(i <= 11) // Noncompliant [[sc=5;ec=16;secondary=33]]
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
      else          // Noncompliant   [sc=7;ec=11;secondary=59] {{Use indentation to denote the code conditionally executed by this "else".}}
    doTheOtherThing();

    if(i > 10)
      i =100;
    i++;

    for (; i <= 1;) // Noncompliant [[sc=5;ec=20;secondary=68]] {{Use indentation to denote the code conditionally executed by this "for".}}
    i = 1;
    i++;

    for (; i <= 1;) { // Compliant
     i = 1;
     i++;
    }

    for (; i <= 1;) // Compliant
      i = 1;

    while (i <= 10)  // Compliant
      i++;

    while (i <= 10) {  // Compliant
      i++;
      i += 10;
    }

    while(i<=11)     // Noncompliant [[sc=5;ec=17;secondary=88]] {{Use indentation to denote the code conditionally executed by this "while".}}
    i++;
    i+=10;

    List<Integer> l=null;
    for(Integer ii : l) // Compliant
      return i;

    for(Integer ii : l) // Noncompliant [[sc=5;ec=24;secondary=96]] {{Use indentation to denote the code conditionally executed by this "for".}}
    i=1;
    i=2;

    for(Integer ii : l) // Noncompliant [[sc=5;ec=24;secondary=100]] {{Use indentation to denote the code conditionally executed by this "for".}}
   if(ii == 1)  // Noncompliant [[sc=4;ec=15;secondary=101] {{Use indentation to denote the code conditionally executed by this "if".}}
   i++;
   i=2;

    if(i ==1)   // Noncompliant
  i++;
    else
      i++;

    for(i =0;i<10;i++)  // Noncompliant [[sc=5;ec=23;secondary=110]]  {{Use indentation to denote the code conditionally executed by this "for".}}
  if(i==1)
    i++;
    if(i == 1)   // Compliant
      if(i == 2)  // Noncompliant [[sc=7;ec=17;secondary=114]] {{Use indentation to denote the code conditionally executed by this "if".}}
      i++;
      i+=2;
      
    if(i == 0)
      i++;
    else if(i == 1)
      i = 1;
    else    if(i ==3)
      return i;
    else       // Noncompliant [[sc=5;ec=9;secondary=124]] {{Use indentation to denote the code conditionally executed by this "else".}}
    i=1;
    
    while(i<=0)
      for(;i<10;)  // Noncompliant [[sc=7;ec=18;secondary=128]] {{Use indentation to denote the code conditionally executed by this "for".}}
     if(i<1)  // Noncompliant [[sc=6;ec=13;secondary=129]] {{Use indentation to denote the code conditionally executed by this "if".}}
    return 2;   
    
    if(i == 1)
      return i;
    else
    {
      i++;
    }
    
    while(i>1)
      if(i==1)    // Noncompliant
    return i;
   
    if (i < 2) {
      if ((i > 0) && (i > 3))  // Noncompliant [[sc=7;ec=30;secondary=144]] {{Use indentation to denote the code conditionally executed by this "if".}}
    return i;
      return 1;
    }

    int n = 0;
    int len = 123;
    int off = 1;
    byte[] b = new byte[1];
    
    while (n < len) {
      int count = System.in.read(b, off + n, len - n);
      if (count < 0)        // Noncompliant
    throw new EOFException();
      n += count;
  }
    
    if(true) {
      if(i ==1)  // Noncompliant
  return 1;
    }
    if(i==0)
      i++;
    else if(i==1)
      if(i==2)
        i++;
      else 
        i++;
    else if(i ==2) // Noncompliant [[sc=5;ec=19;secondary=172]] {{Use indentation to denote the code conditionally executed by this "if".}}
    i+=2;  
    else 
      if(i==1)
        return 1;
    if (i > 0)
      if (i >1) // Noncompliant
      doTheThing();
      else
        doTheOtherThing();
    else
      somethingElseEntirely();

    if (i > 0) {
      doTheThing();
    } else
      return i;

    if (i > 0) {
      doTheOtherThing();
    } else // Noncompliant
    return i;

    if (i > 0) {
      doTheOtherThing();
    } else if (i < 0)
      return i;

    if (i > 0) {
      doTheOtherThing();
    } else if (i < 0) // Noncompliant
    return i;

    if (i > 0) {
      doTheOtherThing();
    }
      else if (i < 0) // Noncompliant
      return i;

    return i;
  }

  public void doTheThing() {};
  public void doTheOtherThing() {};
  public void somethingElseEntirely() {};
}
