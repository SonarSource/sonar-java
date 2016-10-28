import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

abstract class A {
  public void foo(File f) throws IOException {
    InputStream fis = new FileInputStream(f); // Noncompliant
    doNotClose(fis);
  }

  private void doNotClose(InputStream is) {
    // do nothing;
  }

  public void bar(File f) throws IOException {
    InputStream fis = new FileInputStream(f); // Compliant
    closeAfterRead(fis);
  }

  private void closeAfterRead(InputStream is) {
    is.read();
    is.close();
  }

  public void qix1(File f, boolean shouldClose) throws IOException {
    InputStream fis = new FileInputStream(f); // Noncompliant
    closeIfTrue(fis, shouldClose);
  }

  public void qix2(File f, boolean shouldClose) throws IOException {
    InputStream fis = new FileInputStream(f); // Noncompliant
    closeIfTrue(shouldClose, fis);
  }

  public void qix3(File f, boolean shouldClose) throws IOException {
    if (shouldClose) {
      InputStream fis = new FileInputStream(f); // Compliant
      closeIfTrue(fis, shouldClose);
    }
  }

  public void qix4(File f, boolean shouldClose) throws IOException {
    if (shouldClose) {
      InputStream fis = new FileInputStream(f); // Compliant
      closeIfTrue(shouldClose, fis);
    }
  }

  private void closeIfTrue(InputStream is, boolean shouldClose) {
    if (shouldClose) {
      IOUtils.closeQuietly(is);
    }
  }

  private void closeIfTrue(boolean shouldClose, InputStream is) {
    if (shouldClose) {
      IOUtils.closeQuietly(is);
    }
  }

  public void gul(File f) throws IOException {
    InputStream fis = new FileInputStream(f); // Compliant
    maybeCloseInImplem(fis);

    InputStream fis2 = new FileInputStream(f); // Compliant
    callMaybeCloseInImplem(fis2);
  }

  private void callMaybeCloseInImplem(InputStream is) {
    maybeCloseInImplem(is);
  }

  public abstract void maybeCloseInImplem(InputStream is);
}
