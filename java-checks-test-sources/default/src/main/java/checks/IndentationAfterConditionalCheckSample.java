package checks;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

class IndentationAfterConditionalCheckSample {
  public int foo() throws IOException {
    int i = 0;
    if (i <= 1) // Noncompliant {{Use indentation to denote the code conditionally executed by this "if".}}
//  ^^^^^^^^^^^
    i = 1;
//  ^^^<
    i++;

    if(i > 0) // Noncompliant
//  ^^^^^^^^^
    // ......
    doTheOtherThing();
//  ^^^<
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

    if(i <= 11) // Noncompliant
//  ^^^^^^^^^^^
    doTheThing();
//  ^^^<
    doTheOtherThing();
    somethingElseEntirely();

    if (i != 0)
        i++;

    if(i <= 1) // Noncompliant

    i=1;
    i++;

    if(i==10) // Noncompliant


    i=10;

    if (i == 5) // Noncompliant
    doTheThing();
    else if(i == 6) // Noncompliant
    somethingElseEntirely();
    else // Noncompliant
    doTheThing();
    doTheOtherThing();

      if(i == 0) // Noncompliant
    doTheThing();
      else // Noncompliant {{Use indentation to denote the code conditionally executed by this "else".}}
    doTheOtherThing();

    if(i > 10)
      i =100;
    i++;

    for (; i <= 1;) // Noncompliant {{Use indentation to denote the code conditionally executed by this "for".}}
//  ^^^^^^^^^^^^^^^
    i = 1;
//  ^^^<
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

    while(i<=11) // Noncompliant {{Use indentation to denote the code conditionally executed by this "while".}}
//  ^^^^^^^^^^^^
    i++;
//  ^^^<
    i+=10;

    List<Integer> l=null;
    for(Integer ii : l) // Compliant
      return i;

    for(Integer ii : l) // Noncompliant {{Use indentation to denote the code conditionally executed by this "for".}}
//  ^^^^^^^^^^^^^^^^^^^
    i=1;
//  ^^^<
    i=2;

    for(Integer ii : l) // Noncompliant {{Use indentation to denote the code conditionally executed by this "for".}}
//  ^^^^^^^^^^^^^^^^^^^
   if(ii == 1) // Noncompliant {{Use indentation to denote the code conditionally executed by this "if".}}
//  ^^^<
   i++;
   i=2;

    if(i ==1) // Noncompliant
  i++;
    else
      i++;

    for(i =0;i<10;i++) // Noncompliant {{Use indentation to denote the code conditionally executed by this "for".}}
//  ^^^^^^^^^^^^^^^^^^
  if(i==1)
//  ^^^<
    i++;
    if(i == 1)   // Compliant
      if(i == 2) // Noncompliant {{Use indentation to denote the code conditionally executed by this "if".}}
//    ^^^^^^^^^^
      i++;
//  ^^^<
      i+=2;
      
    if(i == 0)
      i++;
    else if(i == 1)
      i = 1;
    else    if(i ==3)
      return i;
    else // Noncompliant {{Use indentation to denote the code conditionally executed by this "else".}}
//  ^^^^
    i=1;
//  ^^^<
    
    while(i<=0)
      for(;i<10;) // Noncompliant {{Use indentation to denote the code conditionally executed by this "for".}}
//    ^^^^^^^^^^^
     if(i<1) // Noncompliant {{Use indentation to denote the code conditionally executed by this "if".}}
//   ^^^^^^^
//  ^^^<
    return 2;   
//  ^^^<
    
    if(i == 1)
      return i;
    else
    {
      i++;
    }
    
    while(i>1)
      if(i==1) // Noncompliant
    return i;
   
    if (i < 2) {
      if ((i > 0) && (i > 3)) // Noncompliant {{Use indentation to denote the code conditionally executed by this "if".}}
//    ^^^^^^^^^^^^^^^^^^^^^^^
    return i;
//  ^^^<
      return 1;
    }

    int n = 0;
    int len = 123;
    int off = 1;
    byte[] b = new byte[1];
    
    while (n < len) {
      int count = System.in.read(b, off + n, len - n);
      if (count < 0) // Noncompliant
    throw new EOFException();
      n += count;
  }
    
    if(true) {
      if(i ==1) // Noncompliant
  return 1;
    }
    if(i==0)
      i++;
    else if(i==1)
      if(i==2)
        i++;
      else 
        i++;
    else if(i ==2) // Noncompliant {{Use indentation to denote the code conditionally executed by this "if".}}
//  ^^^^^^^^^^^^^^
    i+=2;  
//  ^^^<
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
