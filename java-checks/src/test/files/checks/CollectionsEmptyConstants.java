import java.util.Collections;

class A{


  List<String> collection1 = Collections.EMPTY_LIST; // Noncompliant {{Replace "Collections.EMPTY_LIST" by "Collections.emptyList()".}}
  Map<String, String> collection2 = Collections.EMPTY_MAP; // Noncompliant {{Replace "Collections.EMPTY_MAP" by "Collections.emptyMap()".}}
  Set<String> collection3 = Collections.EMPTY_SET; // Noncompliant {{Replace "Collections.EMPTY_SET" by "Collections.emptySet()".}}

  Iterator iter = Collections.emptyIterator();
  String toto =  A.TOTO;

  List<String> collection1 = Collections.emptyList(); //Compliant
  Map<String, String> collection2 = Collections.emptyMap(); //Compliant
  Set<String> collection3 = Collections.emptySet(); //Compliant
}
