package org.sonar.java.classpath;

import java.util.Objects;
import javax.annotation.Nullable;

public record Version(Integer major, @Nullable Integer minor, @Nullable Integer patch, @Nullable String qualifier) implements Comparable<Version> {

  @Override
  public int compareTo(Version o) {
    if (!Objects.equals(major, o.major)) {
      return major - o.major;
    }
    // TODO: complete this
    return 0;
  }

  @Override
  public String toString() {
    return major +
      (minor == null ? "" : "." + minor) +
      (patch == null ? "" : "." + patch) +
      (qualifier == null ? "" : qualifier);
  }
}
