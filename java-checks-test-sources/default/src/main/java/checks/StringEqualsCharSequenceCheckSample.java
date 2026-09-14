package checks;

import java.nio.CharBuffer;
import java.util.List;

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

  <T extends String> boolean testGenericString(String str, T genericStr) {
    return str.equals(genericStr);
  }

  <T extends CharSequence> boolean testGenericCharSequence(String str, T genericCs) {
    return str.equals(genericCs); // Noncompliant {{Use "contentEquals()" instead of "equals()" to compare a "String" with a "CharSequence".}} [[quickfixes=qfGeneric]]
//             ^^^^^^
    // fix@qfGeneric {{Replace with "contentEquals()"}}
    // edit@qfGeneric [[sc=16;ec=22]] {{contentEquals}}
  }

  boolean testWildcard(String str, List<? extends String> stringList, List<? extends CharSequence> csList) {
    boolean w1 = str.equals(stringList.get(0));
    boolean w2 = str.equals(csList.get(0)); // Noncompliant {{Use "contentEquals()" instead of "equals()" to compare a "String" with a "CharSequence".}} [[quickfixes=qfWildcard]]
//                   ^^^^^^
    // fix@qfWildcard {{Replace with "contentEquals()"}}
    // edit@qfWildcard [[sc=22;ec=28]] {{contentEquals}}
    return w1 && w2;
  }

  static class CustomCharSequence implements CharSequence {
    @Override public int length() { return 0; }
    @Override public char charAt(int index) { return 'a'; }
    @Override public CharSequence subSequence(int start, int end) { return ""; }
  }

  boolean testCustom(String str, CustomCharSequence custom) {
    return str.equals(custom); // Noncompliant {{Use "contentEquals()" instead of "equals()" to compare a "String" with a "CharSequence".}} [[quickfixes=qfCustom]]
//             ^^^^^^
    // fix@qfCustom {{Replace with "contentEquals()"}}
    // edit@qfCustom [[sc=16;ec=22]] {{contentEquals}}
  }
}
