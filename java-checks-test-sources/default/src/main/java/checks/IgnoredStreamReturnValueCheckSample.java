package checks;

import java.io.FileInputStream;
import java.io.InputStream;

class IgnoredStreamReturnValueCheckSample {

  class A {
    public void foo() throws Exception {
      FileInputStream is = new FileInputStream("");
      byte[] buffer = new byte[1000];
      long skipValue = 12L;

      is.read(buffer); // Noncompliant [[sc=10;ec=14]] {{Check the return value of the "read" call to see how many bytes were read.}}
      is.skip(skipValue); // Noncompliant
      getInputStream().read(buffer); // Noncompliant
      getInputStream().skip(skipValue); // Noncompliant

      // return values are used
      int i;
      long l;
      i = is.read(buffer); // Compliant
      l = is.skip(skipValue); // Compliant

      // methods not overriding the skip/read from InputStream
      MyInputStream mis = new MyInputStream();
      mis.skip(); // compliant
      mis.skip(true); // compliant
      mis.skip('c'); // compliant
      mis.skip(0L, 0L); // Compliant
      mis.read(0L); // Compliant
      mis.read(buffer, null); // Compliant
      mis.read(buffer, 0, 0); // Compliant

      // same signature but no subtype of InputStream
      MyClass mc = new MyClass();
      mc.read(buffer); // Compliant
      mc.skip(skipValue); // Compliant
      read(buffer); // compliant
      skip(skipValue); // compliant
    }

    private InputStream getInputStream() throws Exception {
      return new FileInputStream("");
    }

    private int read(byte[] bytes) {
      return 0;
    }

    private long skip(long value) {
      return 0L;
    }
  }

  class MyClass {
    public int read(byte[] bytes) {
      return 0;
    }

    public long skip(long value) {
      return 0L;
    }
  }

  class MyInputStream extends InputStream {

    @Override
    public int read() {
      return 0;
    }

    public long read(byte[] bytes, Object o) {
      return 0L;
    }

    public int read(long value) {
      return 0;
    }

    public int skip() {
      return 0;
    }

    public boolean skip(boolean value) {
      return value;
    }

    public long skip(char value) {
      return 0L;
    }

    public long skip(long v1, long v2) {
      return 0L;
    }
  }
}
