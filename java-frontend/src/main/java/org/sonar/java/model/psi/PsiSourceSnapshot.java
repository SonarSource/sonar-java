/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.psi;

import java.util.Arrays;
import java.util.Objects;

/** Keeps Java Unicode translation and PSI backslash protection separate from original source locations. */
public final class PsiSourceSnapshot {

  private final String original;
  private final String translated;
  private final String psiText;
  private final int[] originalOffsets;
  private final int[] translatedOffsets;

  private PsiSourceSnapshot(String original, String translated, String psiText, int[] originalOffsets, int[] translatedOffsets) {
    this.original = original;
    this.translated = translated;
    this.psiText = psiText;
    this.originalOffsets = originalOffsets;
    this.translatedOffsets = translatedOffsets;
  }

  static PsiSourceSnapshot identity(String source) {
    return new PsiSourceSnapshot(source, source, source, null, null);
  }

  /** Applies JLS 3.3 once, then shields remaining backslashes from PSI's own Unicode translation. */
  public static PsiSourceSnapshot from(String original) {
    Objects.requireNonNull(original);
    if (original.indexOf("\\u") < 0) {
      return identity(original);
    }
    var text = new StringBuilder(original.length());
    int[] boundaries = new int[original.length() + 1];
    int backslashes = 0;
    boolean previousWasUnicode = false;
    boolean changed = false;
    int offset = 0;
    while (offset < original.length()) {
      char c = original.charAt(offset);
      int end = offset + 1;
      boolean unicode = c == '\\' && (previousWasUnicode || backslashes % 2 == 0) && end < original.length() && original.charAt(end) == 'u';
      if (unicode) {
        while (end < original.length() && original.charAt(end) == 'u') {
          end++;
        }
        if (original.length() - end < 4) {
          throw malformed(offset);
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
          char digit = original.charAt(end++);
          int hex = digit >= '0' && digit <= '9' ? digit - '0'
            : digit >= 'a' && digit <= 'f' ? digit - 'a' + 10 : digit >= 'A' && digit <= 'F' ? digit - 'A' + 10 : -1;
          if (hex < 0) {
            throw malformed(offset);
          }
          value = (value << 4) | hex;
        }
        c = (char) value;
        changed = true;
      }
      text.append(c);
      boundaries[text.length()] = end;
      backslashes = c == '\\' ? backslashes + 1 : 0;
      previousWasUnicode = unicode;
      offset = end;
    }
    if (!changed) {
      return identity(original);
    }
    return protect(original, text.toString(), boundaries);
  }

  private static PsiSourceSnapshot protect(String original, String translated, int[] boundaries) {
    int slashCount = (int) translated.chars().filter(c -> c == '\\').count();
    if (slashCount == 0) {
      return new PsiSourceSnapshot(original, translated, translated, Arrays.copyOf(boundaries, translated.length() + 1), null);
    }
    int length = Math.addExact(translated.length(), Math.multiplyExact(slashCount, 5));
    var protectedText = new StringBuilder(length);
    int[] originalOffsets = new int[length + 1];
    int[] translatedOffsets = new int[length + 1];
    for (int i = 0; i < translated.length(); i++) {
      char c = translated.charAt(i);
      if (c == '\\') {
        protectedText.append(c).append("u005c");
      } else {
        protectedText.append(c);
      }
      int end = protectedText.length();
      int start = end - (c == '\\' ? 6 : 1);
      Arrays.fill(originalOffsets, start, end, boundaries[i]);
      Arrays.fill(translatedOffsets, start, end, i);
      originalOffsets[end] = boundaries[i + 1];
      translatedOffsets[end] = i + 1;
    }
    return new PsiSourceSnapshot(original, translated, protectedText.toString(), originalOffsets, translatedOffsets);
  }

  private static IllegalArgumentException malformed(int offset) {
    return new IllegalArgumentException("Malformed Java Unicode escape at original offset " + offset);
  }

  public String psiText() {
    return psiText;
  }

  String original() {
    return original;
  }

  String translated() {
    return translated;
  }

  /** Synthetic buffer interiors map to the original character's start; the final boundary maps to its exclusive end. */
  int originalOffset(int psiOffset) {
    Objects.checkIndex(psiOffset, psiText.length() + 1);
    return originalOffsets == null ? psiOffset : originalOffsets[psiOffset];
  }

  String originalText(int psiStart, int psiEnd) {
    Objects.checkFromToIndex(psiStart, psiEnd, psiText.length());
    return original.substring(originalOffset(psiStart), originalOffset(psiEnd));
  }

  String logicalText(int psiStart, int psiEnd) {
    Objects.checkFromToIndex(psiStart, psiEnd, psiText.length());
    return translated.substring(translatedOffsets == null ? psiStart : translatedOffsets[psiStart], translatedOffsets == null ? psiEnd : translatedOffsets[psiEnd]);
  }
}
