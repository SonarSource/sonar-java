package checks;

import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class S4349 extends OutputStream { // Noncompliant {{Provide an override of "write(byte[],int,int)" for this class.}}
//    ^^^^^
  private FileOutputStream fout;

  public S4349(File file) throws IOException {
    fout = new FileOutputStream(file);
  }

  @Override
  public void write(int b) throws IOException {
    fout.write(b);
  }

  @Override
  public void close() throws IOException {
    fout.write("\n\n".getBytes());
    fout.close();
    super.close();
  }
}

class S4349_2 extends OutputStream {
  private FileOutputStream fout;

  public S4349_2(File file) throws IOException {
    fout = new FileOutputStream(file);
  }

  @Override
  public void write(int b) throws IOException {
    fout.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    fout.write(b, off, len);
  }

  @Override
  public void close() throws IOException {
    fout.write("\n\n".getBytes());
    fout.close();
    super.close();
  }
}

class S4349_3 extends FilterOutputStream { // Noncompliant
  private FileOutputStream fout;

  public S4349_3(File file) throws IOException {
    super(null);
    fout = new FileOutputStream(file);
  }

  @Override
  public void write(int b) throws IOException {
    fout.write(b);
  }

  @Override
  public void close() throws IOException {
    fout.write("\n\n".getBytes());
    fout.close();
    super.close();
  }
}

class S4349_4 {
  private OutputStream fout = new OutputStream() {
    @Override public void write(int b) throws IOException { }
  };
}

abstract class S4349_5 extends OutputStream { // compliant : abstract class.
  private FileOutputStream fout;

  public S4349_5(File file) throws IOException {
    fout = new FileOutputStream(file);
  }

  @Override
  public void write(int b) throws IOException {
    fout.write(b);
  }

  @Override
  public void close() throws IOException {
    fout.write("\n\n".getBytes());
    fout.close();
    super.close();
  }
}

class S4349_6 extends OutputStream { // Noncompliant {{Provide an empty override of "write(byte[],int,int)" for this class as well.}}
  @Override
  public void write(int b) throws IOException {
  }
}

abstract class S4349_7 extends OutputStream {
  public abstract void write(int b) throws IOException;
}
