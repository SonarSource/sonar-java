package java.foo.com;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable; // Compliant
import java.util.List;
import java.util.Map;
import java.util.Stack; // Compliant
import java.util.Vector; // Compliant
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Foo {
  StringBuffer sb = new StringBuffer();

  class APIrequirement {
    APIrequirement() {
      this("foo");
    }
    APIrequirement(String s){}
    void plop() {
      String input = "a=123,b=456,c=789";
      Pattern p = Pattern.compile("\\b[-\\w]+=");
      Matcher m = p.matcher(input);

      while (m.find()) {
        m.appendReplacement(sb, m.group().toUpperCase());
      }
      m.appendTail(sb);
      Foo.flabburst();
    }
  }
}

class Request {
  void foo(javax.servlet.http.HttpServletRequest request) {
    StringBuffer requestURLBuffer = request.getRequestURL(); // compliant because of return type of getRequestURL
  }
}

class InvokeDefinedMethod {
  Vector v; // Noncompliant
  void addToVector(Vector v, Object item) { // Noncompliant
  }

  void fun(Object o){
    this.addToVector(v, o);
  }

}
