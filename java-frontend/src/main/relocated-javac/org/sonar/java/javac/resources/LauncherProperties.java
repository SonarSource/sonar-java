package org.sonar.java.javac.resources;

import java.nio.file.Path;
import org.sonar.java.javac.util.JCDiagnostic.DiagnosticFlag;
import org.sonar.java.javac.util.JCDiagnostic.Error;
import org.sonar.java.javac.util.JCDiagnostic.Warning;
import org.sonar.java.javac.util.JCDiagnostic.LintWarning;
import org.sonar.java.javac.util.JCDiagnostic.Note;
import org.sonar.java.javac.util.JCDiagnostic.Fragment;
import org.sonar.java.javac.code.Lint.LintCategory;

import java.util.EnumSet;

import static org.sonar.java.javac.util.JCDiagnostic.DiagnosticFlag.*;

public class LauncherProperties {
    public static class Errors {
        /**
         * launcher.err.cant.access.constructor=\
         *    can''t access no argument constructor in class: {0}
         */
        public static Error CantAccessConstructor(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "cant.access.constructor", arg0);
        }
        
        /**
         * launcher.err.cant.access.main.method=\
         *    can''t access main method in class: {0}
         */
        public static Error CantAccessMainMethod(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "cant.access.main.method", arg0);
        }
        
        /**
         * launcher.err.cant.find.class=\
         *    can''t find class: {0}
         */
        public static Error CantFindClass(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "cant.find.class", arg0);
        }
        
        /**
         * launcher.err.cant.find.constructor=\
         *    can''t find no argument constructor in class: {0}
         */
        public static Error CantFindConstructor(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "cant.find.constructor", arg0);
        }
        
        /**
         * launcher.err.cant.find.main.method=\
         *    can''t find main(String[]) or main() method in class: {0}
         */
        public static Error CantFindMainMethod(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "cant.find.main.method", arg0);
        }
        
        /**
         * launcher.err.cant.instantiate=\
         *    abstract class: {0} can not be instantiated
         */
        public static Error CantInstantiate(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "cant.instantiate", arg0);
        }
        
        /**
         * launcher.err.cant.read.file=\
         *    error reading source file {0}: {1}
         */
        public static Error CantReadFile(Path arg0, Object arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "cant.read.file", arg0, arg1);
        }
        
        /**
         * launcher.err.cant.use.private.constructor=\
         *    no non-private zero argument constructor found in class {0}\n\
         *    remove private from existing constructor or define as:\n\
         *\   public {0}()
         */
        public static Error CantUsePrivateConstructor(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "cant.use.private.constructor", arg0);
        }
        
        /**
         * "compilation.failed"
         */
        public static final Error CompilationFailed = new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "compilation.failed");
        
        /**
         * launcher.err.file.not.found=\
         *    source file not found: {0}
         */
        public static Error FileNotFound(Path arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "file.not.found", arg0);
        }
        
        /**
         * launcher.err.invalid.filename=\
         *    invalid path for source file: {0}
         */
        public static Error InvalidFilename(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "invalid.filename", arg0);
        }
        
        /**
         * launcher.err.invalid.value.for.source=\
         *    invalid value for --source option: {0}
         */
        public static Error InvalidValueForSource(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "invalid.value.for.source", arg0);
        }
        
        /**
         * "main.not.public.static"
         */
        public static final Error MainNotPublicStatic = new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "main.not.public.static");
        
        /**
         * "main.not.void"
         */
        public static final Error MainNotVoid = new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "main.not.void");
        
        /**
         * launcher.err.mismatch.end.of.path.and.package.name=\
         *    end of path to source file does not match its package name {0}: {1}
         */
        public static Error MismatchEndOfPathAndPackageName(String arg0, Path arg1) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "mismatch.end.of.path.and.package.name", arg0, arg1);
        }
        
        /**
         * "no.args"
         */
        public static final Error NoArgs = new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "no.args");
        
        /**
         * "no.class"
         */
        public static final Error NoClass = new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "no.class");
        
        /**
         * launcher.err.no.value.for.option=\
         *    no value given for option: {0}
         */
        public static Error NoValueForOption(String arg0) {
            return new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "no.value.for.option", arg0);
        }
        
        /**
         * "unnamed.pkg.not.allowed.named.modules"
         */
        public static final Error UnnamedPkgNotAllowedNamedModules = new Error(EnumSet.noneOf(DiagnosticFlag.class), "launcher", "unnamed.pkg.not.allowed.named.modules");
    }
}
