package org.sonar.java.javac.resources;

public final class launcher_de extends java.util.ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "launcher.err.cant.access.constructor", "Kein Zugriff auf No-Argument-Konstruktor in Klasse: {0}" },
            { "launcher.err.cant.access.main.method", "kein Zugriff auf Methode \"main\" in Klasse: {0}" },
            { "launcher.err.cant.find.class", "Klasse nicht gefunden: {0}" },
            { "launcher.err.cant.find.constructor", "No-Argument-Konstruktor nicht gefunden in Klasse: {0}" },
            { "launcher.err.cant.find.main.method", "main(String[])- oder main()-Methode nicht gefunden in Klasse: {0}" },
            { "launcher.err.cant.instantiate", "Abstrakte Klasse: {0} kann nicht instanziiert werden" },
            { "launcher.err.cant.read.file", "Fehler beim Lesen von Quelldatei {0}: {1}" },
            { "launcher.err.cant.use.private.constructor", "Kein nicht privater Null-Argument-Konstruktor in Klasse {0} gefunden\nEntfernen Sie die Eigenschaft \"private\" aus dem vorhandenen Konstruktor, oder definieren Sie ihn als:\n   public {0}()" },
            { "launcher.err.compilation.failed", "Kompilierung nicht erfolgreich" },
            { "launcher.err.file.not.found", "Quelldatei nicht gefunden: {0}" },
            { "launcher.err.invalid.filename", "ung\u00FCltiger Pfad f\u00FCr Quelldatei: {0}" },
            { "launcher.err.invalid.value.for.source", "ung\u00FCltiger Wert f\u00FCr Option --source: {0}" },
            { "launcher.err.main.not.public.static", "Methode \"main\" ist nicht als \"public static\" deklariert" },
            { "launcher.err.main.not.void", "Methode \"main\" ist nicht mit R\u00FCckgabetyp \"void\" deklariert" },
            { "launcher.err.mismatch.end.of.path.and.package.name", "Ende des Pfades f\u00FCr die Ausgangsdatei stimmt nicht mit dem zugeh\u00F6rigen Packagenamen {0} \u00FCberein: {1}" },
            { "launcher.err.no.args", "kein Pfad f\u00FCr Quelldatei" },
            { "launcher.err.no.class", "keine Klasse deklariert in Quelldatei" },
            { "launcher.err.no.value.for.option", "kein Wert angegeben f\u00FCr Option: {0}" },
            { "launcher.err.unnamed.pkg.not.allowed.named.modules", "Unbenanntes Package ist in benannten Modulen nicht zul\u00E4ssig" },
            { "launcher.error", "Fehler: " },
        };
    }
}
