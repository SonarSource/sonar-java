package org.sonar.java.javac.resources;

public final class launcher extends java.util.ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "launcher.err.cant.access.constructor", "can''t access no argument constructor in class: {0}" },
            { "launcher.err.cant.access.main.method", "can''t access main method in class: {0}" },
            { "launcher.err.cant.find.class", "can''t find class: {0}" },
            { "launcher.err.cant.find.constructor", "can''t find no argument constructor in class: {0}" },
            { "launcher.err.cant.find.main.method", "can''t find main(String[]) or main() method in class: {0}" },
            { "launcher.err.cant.instantiate", "abstract class: {0} can not be instantiated" },
            { "launcher.err.cant.read.file", "error reading source file {0}: {1}" },
            { "launcher.err.cant.use.private.constructor", "no non-private zero argument constructor found in class {0}\nremove private from existing constructor or define as:\n   public {0}()" },
            { "launcher.err.compilation.failed", "compilation failed" },
            { "launcher.err.file.not.found", "source file not found: {0}" },
            { "launcher.err.invalid.filename", "invalid path for source file: {0}" },
            { "launcher.err.invalid.value.for.source", "invalid value for --source option: {0}" },
            { "launcher.err.main.not.public.static", "''main'' method is not declared ''public static''" },
            { "launcher.err.main.not.void", "''main'' method is not declared with a return type of ''void''" },
            { "launcher.err.mismatch.end.of.path.and.package.name", "end of path to source file does not match its package name {0}: {1}" },
            { "launcher.err.no.args", "no path for source file" },
            { "launcher.err.no.class", "no class declared in source file" },
            { "launcher.err.no.value.for.option", "no value given for option: {0}" },
            { "launcher.err.unnamed.pkg.not.allowed.named.modules", "unnamed package is not allowed in named modules" },
            { "launcher.error", "error: " },
        };
    }
}
