package checks;

import java.util.HashSet; // keep it for testing before/after insertions
import java.util.Map;

class ReturnEmptyArrayNotNullCheckWithQuickFixesAndImports {

  java.util.List<Object> list() {
    return null; // Noncompliant [[quickfixes=list]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@list {{Replace "null" with an empty List}}
    // edit@list [[sc=12;ec=16]] {{Collections.emptyList()}}
    // edit@list [[sl=3;sc=1;el=3;ec=1]] {{import java.util.Collections;\n}}
  }

  java.util.LinkedList<Object> linkedList() {
    return null; // Noncompliant [[quickfixes=linkedList]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@linkedList {{Replace "null" with an empty LinkedList}}
    // edit@linkedList [[sc=12;ec=16]] {{new LinkedList<>()}}
    // edit@linkedList [[sl=3;sc=26;el=3;ec=26]] {{\nimport java.util.LinkedList;}}
  }

  HashSet<Object> hashset() {
    return null; // Noncompliant [[quickfixes=HashSet]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@HashSet {{Replace "null" with an empty HashSet}}
    // edit@HashSet [[sc=12;ec=16]] {{new HashSet<>()}}
  }

  Map<String, String> map() {
    return null; // Noncompliant [[quickfixes=map]] {{Return an empty map instead of null.}}
//         ^^^^
    // fix@map {{Replace "null" with an empty Map}}
    // edit@map [[sc=12;ec=16]] {{Collections.emptyMap()}}
    // edit@map [[sl=3;sc=1;el=3;ec=1]] {{import java.util.Collections;\n}}
  }

}
