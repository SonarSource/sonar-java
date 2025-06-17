package checks;

import java.nio.CharBuffer;

public class CharSequenceIsEmptyCheckSample {
  


  public boolean testStringBuilder(StringBuilder sb1, StringBuilder sb2) {
    boolean b;

    b = sb1.length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "AbstractStringBuilder" is empty or not.}}
    //  ^^^^^^^^^^^^^^^^^
    b = sb2.length() <= 0; // Noncompliant [[quickfixes=qf2]]
    // fix@qf2 {{Replace with "isEmpty()"}}
    // edit@qf2 [[sc=13;ec=26]]{{isEmpty()}}
    

    b = 0 >= sb1.length(); // Noncompliant
    b = 1 > sb2.length();  // Noncompliant
    
    return b;
  }
  

  public boolean testStringBuffer(StringBuffer buf1, StringBuffer buf2) {
    boolean b;

    b = buf1.length() == 0;  // Noncompliant {{Use "isEmpty()" to check whether a "StringBuffer" is empty or not.}}
    //  ^^^^^^^^^^^^^^^^^^
    b = buf2.length() < 1; // Noncompliant [[quickfixes=qf3]]
    // fix@qf3 {{Replace with "isEmpty()"}}
    // edit@qf3 [[sc=14;ec=26]]{{isEmpty()}}


    b = 0 != buf1.length(); // Noncompliant
    b = 1 <= buf2.length(); // Noncompliant
    
    return b;
  }

  public boolean testCharBuffer(CharBuffer cb1, CharBuffer cb2) {
    boolean b;
    

    b = cb1.length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "CharBuffer" is empty or not.}}
    //  ^^^^^^^^^^^^^^^^^
    b = cb2.length() > 0;  // Noncompliant [[quickfixes=qf4]]
    // fix@qf4 {{Replace with "isEmpty()"}}
    // edit@qf4 [[sc=9;ec=9]]{{!}}
    // edit@qf4 [[sc=13;ec=25]]{{isEmpty()}}
    

    b = 0 == cb1.length(); // Noncompliant
    b = 0 < cb2.length();  // Noncompliant
    
    return b;
  }

  

  public boolean testCharSequence(CharSequence cs1, CharSequence cs2) {
    boolean b;

    b = cs1.length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "CharSequence" is empty or not.}}
    //  ^^^^^^^^^^^^^^^^^
    b = cs2.length() >= 1; // Noncompliant [[quickfixes=qf5]]
    // fix@qf5 {{Replace with "isEmpty()"}}
    // edit@qf5 [[sc=9;ec=9]]{{!}}
    // edit@qf5 [[sc=13;ec=26]]{{isEmpty()}}
    

    b = 0 == cs1.length(); // Noncompliant
    b = 1 <= cs2.length(); // Noncompliant
    
    // Proper way using isEmpty
    b = cs1.isEmpty();
    b = !cs2.isEmpty();
    
    return b;
  }
}
