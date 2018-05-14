import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;

class S2255 {

  void aServiceMethodSettingCookie(HttpServletRequest request, HttpServletResponse response){
    Cookie cookie = new Cookie("userAccountID", "1234"); // Noncompliant
    response.addCookie(cookie);
  }
}
