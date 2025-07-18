package checks.serialization;

import javax.servlet.http.HttpSession;

class SerializableObjectInSessionCheckMissingImportSample {
  public static record R(String foo, Boolean bar) implements Serializable {}

  private HttpSession session = null;

  public void usage() {
    R r = new R("foo", true);
    session.setAttribute("foo", r);
  }
}
