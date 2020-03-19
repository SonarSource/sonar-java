import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

class SerializableObjectInSessionCheckUnresolved {

  void foo(HttpServletRequest request) {
    HttpSession session = request.getSession();
    session.setAttribute("name", UnknownObject.unknownValue()); // Compliant
  }
}
