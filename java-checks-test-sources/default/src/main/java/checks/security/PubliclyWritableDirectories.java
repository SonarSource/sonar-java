package checks.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class PubliclyWritableDirectories {
  private static final String PATH_NAME = "/run/lock";

  public String noncompliant() throws IOException {
    
    File f1 = File.createTempFile("prefix", "suffix"); // Noncompliant {{Make sure publicly writable directories are used safely here.}}
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    f1.deleteOnExit();

    File f2 = new File("/var/tmp"); // Noncompliant
//            ^^^^^^^^^^^^^^^^^^^^
    File f3 = File.createTempFile("prefix", "suffix", f2); // Already reported in line 25
    f3.deleteOnExit();
    File f4 = File.createTempFile("prefix", "suffix", new File("/tmp")); // Noncompliant
//                                                    ^^^^^^^^^^^^^^^^
    f4.deleteOnExit();
    File f5 = File.createTempFile("prefix", "suffix", new File(PATH_NAME)); // Noncompliant
    f5.deleteOnExit();
    // Files library: https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html

    Path p1 = Files.createTempDirectory("prefix"); // Noncompliant {{Make sure publicly writable directories are used safely here.}}
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    p1.toFile().deleteOnExit();
    Path p2 = Files.createTempFile("prefix", "suffix"); // Noncompliant {{Make sure publicly writable directories are used safely here.}}
    p2.toFile().deleteOnExit();

    // Reading files
    try {
      File myObj = new File("/tmp/myfile.txt"); // Noncompliant {{Make sure publicly writable directories are used safely here.}}
      Scanner myReader = new Scanner(myObj);
      while (myReader.hasNextLine()) {
        String data = myReader.nextLine();
        System.out.println(data);
      }
      myReader.close();
    } catch (FileNotFoundException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }

    FileReader fr = new FileReader("/tmp/myfile.txt"); // Noncompliant {{Make sure publicly writable directories are used safely here.}}
    fr.close();

    Path path = Paths.get("/tmp/myfile.txt"); // Noncompliant {{Make sure publicly writable directories are used safely here.}}
    Files.readAllLines(path); // Not reported as sensitive, only when creating the Path
    Files.readAllLines(Paths.get("/tmp/myfile.txt")); // Noncompliant {{Make sure publicly writable directories are used safely here.}}

    String data = new String(Files.readAllBytes(Paths.get("/tmp/myfile.txt"))); // Noncompliant {{Make sure publicly writable directories are used safely here.}}

    // Get from environment variable
    Map<String, String> env = System.getenv();
    String env1 = env.get("TMP"); // Noncompliant {{Make sure publicly writable directories are used safely here.}}
//                ^^^^^^^^^^^^^^
    String env2 = env.get("TMPDIR"); // Noncompliant {{Make sure publicly writable directories are used safely here.}}
//                ^^^^^^^^^^^^^^^^^

    File f6 = new File(env1);
    File f7 = new File(env2);

    File f8 = new File("/tmp/my.txt"); // Noncompliant
    File f88 = new File("/var/tmp/my.txt"); // Noncompliant
    File f888 = new File("/usr/tmp/my.txt"); // Noncompliant
    File f8888 = new File("/dev/shm/my.txt"); // Noncompliant
    File f88888 = new File("/dev/mqueue/my.txt"); // Noncompliant
    File f888888 = new File("/run/lock/my.txt"); // Noncompliant
    File f8888888 = new File("/var/run/lock/my.txt"); // Noncompliant
    File f9 = new File("/Users/Shared/my.txt"); // Noncompliant
    File f99 = new File("/Library/Caches/my.txt"); // Noncompliant
    File f999 = new File("/var/run/lock/my.txt"); // Noncompliant
    File f9999 = new File("/private/tmp/my.txt"); // Noncompliant
    File f99999 = new File("/private/var/tmp/my.txt"); // Noncompliant
    File f999999 = new File("\\Windows\\Temp\\my.txt"); // Noncompliant
    File f9999999 = new File("\\Temp\\my.txt"); // Noncompliant
    File f99999999 = new File("\\TMP\\my.txt"); // Noncompliant

    return "thymeleaf/welcome";
  }

  public String compliant() throws IOException {
    // File library: https://docs.oracle.com/javase/7/docs/api/java/io/File.html
    File f1 = new File("/myDirectory"); // Compliant
    File f2 = File.createTempFile("prefix", "suffix", f1);
    f2.deleteOnExit();
    File f3 = File.createTempFile("prefix", "suffix", new File("/myDirectory")); // Compliant
    f3.deleteOnExit();
    // Files library: https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html
    FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("w+"));
    // As soon as we have an attribute, do not report an issue, even if it is not with correct permission. (probably done on purpose).
    Path p1 = Files.createTempFile("prefix", "suffix", attr); // Compliant
    p1.toFile().deleteOnExit();
    Path p2 = Files.createTempDirectory("prefix", attr); // Compliant
    p2.toFile().deleteOnExit();

    FileReader fr = new FileReader("/mySafeDirectory/myfile.txt"); // Compliant
    fr.close();

    Path path = Paths.get("/mySafeDirectory/myfile.txt"); // Compliant
    Files.readAllLines(path);

    // Get from environment variable
    Map<String, String> env = System.getenv();
    env.get("PATH"); // Compliant

    return "thymeleaf/welcome";
  }
}
