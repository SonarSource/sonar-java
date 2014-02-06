import java.util.Collections;

class A{


  List<String> collection1 = Collections.EMPTY_LIST; //Non-compliant
  Map<String, String> collection2 = Collections.EMPTY_MAP; //Non-compliant
  Set<String> collection3 = Collections.EMPTY_SET; //Non-compliant

  Iterator iter = Collections.emptyIterator();
  String toto =  A.TOTO;

  List<String> collection1 = Collections.emptyList(); //Compliant
  Map<String, String> collection2 = Collections.emptyMap(); //Compliant
  Set<String> collection3 = Collections.emptySet(); //Compliant
}
