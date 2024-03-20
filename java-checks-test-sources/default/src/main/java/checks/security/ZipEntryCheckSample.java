package checks.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipEntryCheckSample {

  ZipFile zipFile;

  Enumeration<? extends ZipEntry> entries = zipFile.entries(); // Noncompliant {{Make sure that expanding this archive file is safe here.}}

  public void noncompliant() throws Exception {

    File f = new File("/home/eric/testzipbomb/zbsm.zip");
    ZipFile zipFile = new ZipFile(f);
    Enumeration<? extends ZipEntry> entries = zipFile.entries(); // Noncompliant

    while (entries.hasMoreElements()) {
      ZipEntry ze = entries.nextElement();

      if (ze.getSize() > 555) { // Noncompliant
        // die
      }
    }
  }

  public ZipEntry noncompliant2(ZipInputStream zis) throws Exception {
    return zis.getNextEntry(); // Noncompliant
  }

  public JarEntry noncompliant3(JarFile file) {
    return file.entries().nextElement(); // Noncompliant [[sc=17;ec=24]]
  }

  public String compliant() throws java.lang.Exception {

    File f = new File("/home/eric/testzipbomb/zbsm.zip");
    ZipFile zipFile = new ZipFile(f);
    Enumeration<? extends ZipEntry> entries = zipFile.entries();

    int THRESHOLD_ENTRIES = 10000;
    int THRESHOLD_SIZE = 1000000000; // 1 GB
    double THRESHOLD_RATIO = 10;
    int totalSizeArchive = 0;
    int totalEntryArchive = 0;

    while (entries.hasMoreElements()) {
      ZipEntry ze = entries.nextElement();
      InputStream in = new BufferedInputStream(zipFile.getInputStream(ze)); // Compliant
      OutputStream out = new BufferedOutputStream(new FileOutputStream("./output_onlyfortesting.txt"));

      totalEntryArchive++;

      int nBytes = -1;
      byte[] buffer = new byte[2048];
      int totalSizeEntry = 0;

      while ((nBytes = in.read(buffer)) > 0) {
        out.write(buffer, 0, nBytes);
        totalSizeEntry += nBytes;
        totalSizeArchive += nBytes;

        double compressionRatio = totalSizeEntry / ze.getCompressedSize();
        if (compressionRatio > THRESHOLD_RATIO) {
          // ratio between compressed and uncompressed data is highly suspicious, looks like a Zip Bomb Attack
          break;
        }
      }

      if (totalSizeArchive > THRESHOLD_SIZE) {
        // the uncompressed data size is too much for the application resource capacity
        break;
      }

      if (totalEntryArchive > THRESHOLD_ENTRIES) {
        // too much entries in this archive, can lead to inodes exhaustion of the system
        break;
      }
    }

    return "thymeleaf/welcome";
  }

}
