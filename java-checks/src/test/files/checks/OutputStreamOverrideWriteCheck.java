import java.io.*;

public class MyStream extends OutputStream { // Noncompliant [[sc=14;ec=22]]{{Provide an override of "write(byte[],int,int)" for this class.}}
  private FileOutputStream fout;

  public MyStream(File file) throws IOException {
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

public class MyStream2 extends OutputStream {
  private FileOutputStream fout;

  public MyStream(File file) throws IOException {
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

public class MyStream3 extends FilterOutputStream { // Noncompliant
  private FileOutputStream fout;

  public MyStream(File file) throws IOException {
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
public class MyStream4 {
  private OutputStream fout = new OutputStream() {
  };
}

public abstract class MyStream5 extends OutputStream { // compliant : abstract class.
  private FileOutputStream fout;

  public MyStream(File file) throws IOException {
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

public class MyStream6 extends OutputStream { // Noncompliant {{Provide an empty override of "write(byte[],int,int)" for this class as well.}}
  @Override
  public void write(int b) throws IOException {
  }
}

public abstract class MyStream7 extends OutputStream {
  public abstract void write(int b) throws IOException;
}
