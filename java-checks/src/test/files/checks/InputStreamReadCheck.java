package org.foo;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

class A extends InputStream {

  public int pos;
  public byte currentByte;
  public String buffer;

  @Override
  public int read() throws IOException {
    Supplier<Byte> mySupplier1 = () -> { return currentByte; };
    Supplier<Byte> mySupplier2 = new Supplier<Byte>() {
      @Override
      public Byte get() {
        return currentByte;
      }
    };
    if (pos == buffer.length()) {
      return -1;
    }
    if (buffer.isEmpty()) {
      return currentByte; // Noncompliant {{Convert this signed byte into an unsigned byte.}}
    }
    return buffer.getBytes()[pos++]; // Noncompliant {{Convert this signed byte into an unsigned byte.}}
  }

  public int read(boolean b) throws IOException {
    return currentByte; // Compliant - not the read() method
  }
}

class B extends A {
  @Override
  public int read() throws IOException {
    return currentByte & 0xFF; // Compliant - return an int
  }

  @Override
  public int read(boolean b) throws IOException {
    return currentByte; // Compliant - not the read() method
  }
}

abstract class C extends InputStream {
  @Override
  public abstract int read();
}
