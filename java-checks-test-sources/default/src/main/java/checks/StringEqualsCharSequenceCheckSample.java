package checks;

import java.nio.CharBuffer;

class StringEqualsCharSequenceCheckSample {

  boolean test(String str, StringBuilder sb, StringBuffer sbuf, CharSequence cs, CharBuffer cb, String otherStr, Object obj) {
    boolean b1 = str.equals(sb); // Noncompliant {{Use "contentEquals()" instead of "equals()" to compare a "String" with a "CharSequence".}} [[quickfixes=qf1]]
//                   ^^^^^^
    // fix@qf1 {{Replace with "contentEquals()"}}
    // edit@qf1 [[sc=22;ec=28]] {{contentEquals}}

    boolean b2 = str.equals(sbuf); // Noncompliant {{Use "contentEquals()" instead of "equals()" to compare a "String" with a "CharSequence".}} [[quickfixes=qf2]]
//                   ^^^^^^
    // fix@qf2 {{Replace with "contentEquals()"}}
    // edit@qf2 [[sc=22;ec=28]] {{contentEquals}}

    boolean b3 = str.equals(cs); // Noncompliant {{Use "contentEquals()" instead of "equals()" to compare a "String" with a "CharSequence".}} [[quickfixes=qf3]]
//                   ^^^^^^
    // fix@qf3 {{Replace with "contentEquals()"}}
    // edit@qf3 [[sc=22;ec=28]] {{contentEquals}}

    boolean b4 = str.equals(cb); // Noncompliant {{Use "contentEquals()" instead of "equals()" to compare a "String" with a "CharSequence".}} [[quickfixes=qf4]]
//                   ^^^^^^
    // fix@qf4 {{Replace with "contentEquals()"}}
    // edit@qf4 [[sc=22;ec=28]] {{contentEquals}}

    boolean b5 = "literal".equals(sb); // Noncompliant {{Use "contentEquals()" instead of "equals()" to compare a "String" with a "CharSequence".}} [[quickfixes=qf5]]
//                         ^^^^^^
    // fix@qf5 {{Replace with "contentEquals()"}}
    // edit@qf5 [[sc=28;ec=34]] {{contentEquals}}

    boolean c1 = str.equals(otherStr);
    boolean c2 = str.contentEquals(sb);
    boolean c3 = str.contentEquals(sbuf);
    boolean c4 = str.contentEquals(cs);
    boolean c5 = str.contentEquals(cb);
    boolean c6 = str.equals(obj);
    boolean c7 = str.equals(null);
    boolean c8 = cs.equals(str);
    boolean c9 = sb.equals(str);
    boolean c10 = obj.equals(sb);

    return b1 && b2 && b3 && b4 && b5 && c1 && c2 && c3 && c4 && c5 && c6 && c7 && c8 && c9 && c10;
  }
}
