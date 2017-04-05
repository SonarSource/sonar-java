import java.util.Date;

public class OutOfBoundsTrigger {

  private static void m0(Date t1, Date t2, Date t3) {
    if (m1(t1, t2)) { // flow@flow1
      m1(t1, t2);
    } else if (m2(t1, t2)) { // flow ignored because of varargs.
      t1.getTime(); // Noncompliant [[flows=flow1]] {{NullPointerException might be thrown as 't1' is nullable here}} flow@flow1
    }
  }

  private static boolean m1(Date t1, Date t2) {
    if (t1 == null || t2 == null) { // flow@flow1
      return false;
    }
    return true;
  }

  private static boolean m2(Date... dates) {
    if (null == dates || 1 >= dates.length) {
      return true;
    }
    for (Date date : dates) {
      if (null == date) {
        return false;
      }
    }
    return true;
  }
}
