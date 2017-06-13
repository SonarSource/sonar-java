import java.util.Date;
import java.text.SimpleDateFormat;


class A {
  private static final String CONSTANT = "yyyy/MM/dd";
  void foo() {
    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf = new SimpleDateFormat(CONSTANT);
    Date date = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
    String result = new SimpleDateFormat("YYYY/MM/dd").format(date);   // Noncompliant {{Make sure that Week Year "YYYY" is expected here instead of Year "yyyy".}}
    result = new SimpleDateFormat("YYYY").format(date);   // Noncompliant {{Make sure that Week Year "YYYY" is expected here instead of Year "yyyy".}}
    result = new SimpleDateFormat("  Y/MM/dd").format(date);   // Noncompliant {{Make sure that Week Year "Y" is expected here instead of Year "y".}}
    result = new SimpleDateFormat("yyyy/MM/dd").format(date);   //Yields '2015/12/31' as expected
    result = new SimpleDateFormat("YYYY-ww").format(date); //compliant, 'Week year' is used along with 'Week of year'. result = '2016-01'
    result = new SimpleDateFormat("ww-YYYY").format(date); //compliant, 'Week year' is used along with 'Week of year'. result = '2016-01'
  }
}
