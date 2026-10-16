package org.sonar.java.javac.resources;

public final class version extends java.util.ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "full", "27-internal-adhoc.alban.jdk" },
            { "jdk", "27" },
            { "release", "27-internal" },
        };
    }
}
