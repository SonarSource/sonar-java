package sym;

import org.sonar.java.model.JavaTree.AnnotationTreeImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import static java.util.List.*;
import static java.net.HttpURLConnection.HTTP_OK;

@SuppressWarnings("all")
class ImportResolution {
  private AnnotationTreeImpl annotationTree;

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
      return null;
    }
  }

  static class Class2 extends Class1 implements Interface1, Interface2 {
  }

  private class MyClass extends Example.Bar {
  }

}
