package org.foo;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

abstract class A {

  ZipEntry myZipEntry;                          // Compliant - fields are OK
  ZipEntry myOtherZipEntry = createZE();        // Compliant
  MyUnrelatedZipEntry myUnrelatedZipEntry;

  A(ZipEntry zipEntry) {                        // Compliant - constructor
    this.myZipEntry = zipEntry;
    this.myZipEntry = createZE();
  }

  void foo(
    ZipFile zipFile,
    ZipEntry zipEntry,                          // Noncompliant {{Make sure that expanding this archive file is safe here.}}
    JarEntry jarEntry,                          // Noncompliant
    MyOwnZipEntry myOwnZipEntry,                // Noncompliant
    ArchiveEntry archiveEntry,                  // Noncompliant
    ZipArchiveEntry zipArchiveEntry,            // Noncompliant
    MyOwnZipArchiveEntry myOwnZipArchiveEntry,  // Noncompliant
    MyUnrelatedZipEntry myUnrelatedZipEntry) {

    ZipEntry entry = zipFile.getEntry("foo");  // Noncompliant [[sc=22;ec=45]] {{Make sure that expanding this archive file is safe here.}}
    ZipEntry moze = getMOZE();                 // Noncompliant [[sc=21;ec=30]]
    getZE().getName();                         // Noncompliant [[sc=5;ec=12]]
    getJE().isDirectory();                     // Noncompliant
    getAE(1, 2, 3, 4, 5);                      // Noncompliant [[sc=5;ec=25]]
    ArchiveEntry zae = getZAE();               // Noncompliant

    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    entries.nextElement();                     // Noncompliant

    getMUZE().toString();                      // Compliant - not a zip entry

    bar(getMOZAE());                           // Noncompliant

    doSomething(zE -> zE.orElse(null));        // Noncompliant
  }

  abstract ZipEntry getZE();
  abstract JarEntry getJE();
  abstract MyOwnZipEntry getMOZE();
  abstract ArchiveEntry getAE(Object ... os);
  abstract ZipArchiveEntry getZAE();
  abstract MyOwnZipArchiveEntry getMOZAE();
  abstract MyUnrelatedZipEntry getMUZE();
  abstract void foo(ZipEntry ze);               // Compliant - abstract method
  abstract void bar(Object o);
  abstract void doSomething(java.util.function.Consumer<java.util.Optional<ZipEntry>> consumer);
  static ZipEntry createZE() { return null; }

  static class MyOwnZipEntry extends ZipEntry { }
  abstract static class MyOwnZipArchiveEntry implements ArchiveEntry { }
  static class MyUnrelatedZipEntry { }

  interface ZipEntryFilter {
    boolean accept(ZipEntry entry);             // Compliant - part of an interface
  }
}

abstract class B extends A {
  @Override
  void foo(ZipEntry ze) { } // Noncompliant - overrides are still reporting issues
}
