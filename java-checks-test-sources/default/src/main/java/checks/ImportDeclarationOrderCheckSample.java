package checks;

// Basic violation: package import after single-type import
import java.util.List; // Compliant
import java.io.*; // Noncompliant {{Reorder this on-demand package import to come before single-type imports.}}
import java.util.Map; // Compliant

// Static import violations
import static java.lang.Math.PI; // Compliant
import static java.util.Collections.*; // Noncompliant {{Reorder this static on-demand package import to come before static single-type imports.}}

// Module import violation - comes after static imports
import module java.base; // Noncompliant {{Reorder this module import to come before static on-demand package imports.}}

// These are compliant since they come after module import (which resets the ordering)
import java.sql.*; // Compliant
import java.time.Instant; // Compliant
import static java.lang.System.out; // Compliant

class ImportDeclarationOrderCheckSample {
}
