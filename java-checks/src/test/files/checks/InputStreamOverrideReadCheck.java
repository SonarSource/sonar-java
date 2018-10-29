import java.io.*;

public class MyStream extends InputStream { // Noncompliant [[sc=14;ec=22]]{{Provide an override of "read(byte[],int,int)" for this class.}}
  private FileInputStream fin;

  public MyStream(File file) throws IOException {
    fin = new FileInputStream(file);
  }

  @Override
  public int read(int b) throws IOException {
    return fin.read(b);
  }

  @Override
  public void close() throws IOException {
    fin.close();
    super.close();
  }
}

public class MyStream2 extends InputStream {
  private FileInputStream fin;

  public MyStream(File file) throws IOException {
    fin = new FileInputStream(file);
  }

  @Override
  public int read(int b) throws IOException {
    return fin.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return fin.read(b, off, len);
  }

  @Override
  public void close() throws IOException {
    fin.close();
    super.close();
  }
}

public class MyStream3 extends FilterInputStream { // Noncompliant
  private FileInputStream fin;

  public MyStream(File file) throws IOException {
    fin = new FileInputStream(file);
  }

  @Override
  public int read(int b) throws IOException {
    return fin.read(b);
  }

  @Override
  public void close() throws IOException {
    fin.close();
    super.close();
  }
}
public class MyStream4 {
  private InputStream fin = new InputStream() {};
}

public abstract class MyStream5 extends InputStream { // compliant : abstract class.
  private FileInputStream fin;

  public MyStream(File file) throws IOException {
    fin = new FileInputStream(file);
  }

  @Override
  public int read(int b) throws IOException {
    return fin.read(b);
  }

  @Override
  public void close() throws IOException {
    fin.close();
    super.close();
  }
}

public class MyStream6 extends InputStream { // Noncompliant {{Provide an empty override of "read(byte[],int,int)" for this class as well.}}
  @Override
  public int read(int b) throws IOException {
  }
}

public abstract class MyStream7 extends InputStream {
  public abstract byte read(int b) throws IOException;
}
