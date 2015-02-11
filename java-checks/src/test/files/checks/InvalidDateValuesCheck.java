import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;



class A {
  int foo() {



    Date d = new Date();
    d.setDate(25);
    d.setDate(32);// Noncompliant;
    d.setYear(2014);
    d.setMonth(11);
    d.setMonth(12); // Noncompliant; rolls d into the next year
    d.setHours(23);
    d.setHours(24); // NonCompliant
    d.setMinutes(59);
    d.setMinutes(61); // NonCompliant
    d.setSeconds(61);
    d.setSeconds(63);// NonCompliant
    d.setSeconds(-1);// NonCompliant
    java.sql.Date d1;
    d1.setHours(23);
    d1.setHours(24); // NonCompliant
    d1.setMinutes(59);
    d1.setMinutes(61);// NonCompliant
    d1.setSeconds(61);
    d1.setSeconds(63);// NonCompliant
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MONTH, 11);
    cal.set(Calendar.MONTH, 12);// NonCompliant
    cal.set(Calendar.DAY_OF_MONTH, 11);
    cal.set(Calendar.DAY_OF_MONTH, 32);// NonCompliant
    cal.set(Calendar.HOUR_OF_DAY, 11);
    cal.set(Calendar.HOUR_OF_DAY, 24);// NonCompliant
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.MINUTE, 61);// NonCompliant
    cal.set(Calendar.SECOND, 61);
    cal.set(Calendar.SECOND, 63);// NonCompliant
    cal.set(Calendar.HOUR_OF_DAY, -2);// NonCompliant
    GregorianCalendar gc = new GregorianCalendar();
    gc = new GregorianCalendar(2015, 11, 31);
    gc = new GregorianCalendar(2015, 12, 31); //NonCompliant
    gc = new GregorianCalendar(2015, 11, 31);
    gc = new GregorianCalendar(2015, 11, 32); //NonCompliant
    gc = new GregorianCalendar(2015, 11, 31, 23, 59);
    gc = new GregorianCalendar(2015, 11, 31, 24, 60); //NonCompliant
    gc = new GregorianCalendar(2015, 11, 31, 23, 59);
    gc = new GregorianCalendar(2015, 11, 31, 23, 61); //NonCompliant
    gc = new GregorianCalendar(2015, 11, 31, 23, 59, 61);
    gc = new GregorianCalendar(2015, 11, 31, 23, 59, 63); //NonCompliant
    gc = new GregorianCalendar(2015, -1, 31, 23, 59, +63); //NonCompliant
    gc = new GregorianCalendar(2015, -foo(), 31, 23, 59, 63); //NonCompliant

    cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, 11);

    cal.get(Calendar.MONTH) == 11;
    cal.get(Calendar.MONTH) == foo();
    cal.get(Calendar.MONTH) == 12; //NonCompliant
    cal.get(Calendar.DAY_OF_MONTH) != 11;
    cal.get(Calendar.DAY_OF_MONTH) != foo();
    cal.get(Calendar.DAY_OF_MONTH) != 32; //NonCompliant
    31 == d.getDate();
    foo() == d.getDate();
    32 == d.getDate(); //NonCompliant
    d1.getSeconds() == -1;// NonCompliant
    calendar.get(Calendar.DST_OFFSET) == 0;
  }
}