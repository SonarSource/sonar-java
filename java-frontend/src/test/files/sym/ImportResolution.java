package sym;

import org.sonar.java.model.JavaTree.NotImplementedTreeImpl;
import static sym.A.BAR;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
; // empty statements should be ignored
import java.util.List;
import java.io.*;
import java.util.ArrayList; //Twice the same import should be ignored
import static java.util.Collections.nCopies;
import static java.util.List.*;
import static java.net.HttpURLConnection.*;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Collections.reverse;
import static java.util.Collections.sort;; // extra empty statements should be ignored
import static java.util.Collections.FOO.*;
@SuppressWarnings("all")
class ImportResolution {
  private NotImplementedTreeImpl annotationTree;

  interface Interface1 extends List{
  }

  interface Interface2 extends Interface1 {
  }

  enum Enum implements Interface1, Interface2 {
  }

  static class Class1 extends Collection {
    Interface1 field;

    Interface1 method(Interface2 param) {
      Interface1 localVariable;
      for (Interface1 forLoopVariable : Collections.<Interface1>emptyList()) {
      }
      if(HTTP_OK==200){
      }
      if(HTTP_ACCEPTED==201) {
      }
      List<String> list = new ArrayList<String>();
      reverse(list);
      sort(list);
      sort(list, new Comparator<String>() {
        @Override
        public int compare(String s, String s2) {
          return s2.compareTo(s);
        }
      });
      File file = new File("file");
      return null;
    }
  }

  Iterable iterable;

  static class Class2 extends Class1 implements Interface1, Interface2 {
  }

  private class MyClass extends Example.Bar {
  }

  void foo() {
    nCopies(2, new Object());
    nCopies(2, 2);
  }
}

public class A<T> {
  public static final String BAR = "value";
}
