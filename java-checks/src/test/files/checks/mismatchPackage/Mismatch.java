package org.foo.mismatchPackage; // Noncompliant {{File path "/src/test/files/checks/mismatchPackage" should match package name "org/foo/mismatchPackage". Move file or change package name.}}
//      ^^^^^^^^^^^^^^^^^^^^^^^

class Mismatch {}
