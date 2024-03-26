package checks.security;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;

class FilePermissionsCheckSample {

  // using PosixFilePermission to set file permissions 757
  public void setPermissions(Path filePath) throws Exception {

    Set<PosixFilePermission> perms = new HashSet<>();
    // user permission
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    // group permissions
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    // others permissions
    perms.add(PosixFilePermission.OTHERS_READ); // Noncompliant {{Make sure this permission is safe.}}
    perms.add(PosixFilePermission.OTHERS_WRITE); // Noncompliant
    perms.add(PosixFilePermission.OTHERS_EXECUTE); // Noncompliant

    Files.setPosixFilePermissions(filePath, perms);

    System.out.println(MyEnum.OTHERS_EXECUTE);
  }

  public void setPermissionsFromString(Path filePath, String permissions) throws Exception {
    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString(permissions)); // Compliant: because we can't say something about the content of 'permissions'
  }

  public void setOthersPermissionsHardCoded(Path filePath) throws Exception {
    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwx---")); // Compliant

    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwxr--")); // Noncompliant [[sc=77;ec=88]] {{Make sure this permission is safe.}}
    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwx-w-")); // Noncompliant
    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwx--x")); // Noncompliant

    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwxrw-")); // Noncompliant
    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwxr-x")); // Noncompliant
    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwx-wx")); // Noncompliant

    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwxrwx")); // Noncompliant

    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("invalid")); // Compliant
  }

  public void setPermissionsUsingRuntimeExec(Runtime runtime, String filename, String[] args) throws Exception {
    runtime.exec("chmod 777 file.json"); // Noncompliant
    runtime.exec("chmod 775 file.json"); // Noncompliant
    runtime.exec("chmod 774 file.json"); // Noncompliant
    runtime.exec("chmod 771 file.json"); // Noncompliant
    runtime.exec("chmod 770 file.json"); // Compliant
    runtime.exec("chmod 1770 file.json"); // Compliant - using sticky bit

    runtime.exec("chmod 770 777 file1.json file2.json"); // Noncompliant

    runtime.exec("/bin/chmod 777 file.json", new String[] {}); // Noncompliant
    runtime.exec("chmod 777 file.json", new String[] {}, new File("")); // Noncompliant

    runtime.exec("chmod o+w file.json"); // Noncompliant
    runtime.exec("chmod o=rwx file.json"); // Noncompliant
    runtime.exec("chmod o-rwx file.json"); // Compliant
    runtime.exec("chmod +w file.json"); // Noncompliant
    runtime.exec("chmod a+w file.json"); // Noncompliant
    runtime.exec("chmod =rwx file.json"); // Noncompliant
    runtime.exec("chmod u=rwx,g=rx,o=r file.json"); // Noncompliant

    runtime.exec("mvn clean install"); // Compliant
    runtime.exec(new String[] {"mvn", "clean", "install"}); // Compliant
    runtime.exec(new String[] {"mvn", "sonar:sonar"}); // Compliant

    runtime.exec(new String[] {"chmod", "777", filename}); // Noncompliant
    runtime.exec(new String[] {"chmod", "770", filename}); // Compliant
    runtime.exec(new String[] {"/bin/chmod", "--recursive", "777", filename}); // Noncompliant
    runtime.exec(new String[] {"/bin/chmod", "--R", "770", filename}); // Compliant

    runtime.exec(new String[] {"/bin/chmod", "--recursive", filename}); // Compliant - malformed chmod

    runtime.exec(args); // Compliant
  }

  public void setPermissionsSafe(Path filePath) throws Exception {
    Set<PosixFilePermission> perms = new HashSet<>();
    // user permission
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    // group permissions
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    // others permissions removed
    perms.remove(PosixFilePermission.OTHERS_READ); // Compliant
    perms.remove(PosixFilePermission.OTHERS_WRITE); // Compliant
    perms.remove(PosixFilePermission.OTHERS_EXECUTE); // Compliant

    Files.setPosixFilePermissions(filePath, perms);

    Set<PosixFilePermission> posixFilePermissions = Files.getPosixFilePermissions(filePath);
    if (posixFilePermissions.contains(PosixFilePermission.OTHERS_READ)) { // Compliant
      // ...
    }
  }

  enum MyEnum { OTHERS_EXECUTE; }
  static void foo(MyEnum myEnumValue) { }
}
