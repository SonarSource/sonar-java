package org.sonar.java.checks.quickfixes;

public final class PrettyprintStringBuilder {
  private final FileConfig fileConfig;

  private final StringBuilder sb = new StringBuilder();
  private int indentLevel = 0;

  public PrettyprintStringBuilder(FileConfig fileConfig) {
    this.fileConfig = fileConfig;
  }

  public String endOfLine() {
    return fileConfig.endOfLine();
  }

  public PrettyprintStringBuilder incIndent() {
    indentLevel += 1;
    return this;
  }

  public PrettyprintStringBuilder decIndent() {
    indentLevel -= 1;
    return this;
  }

  public PrettyprintStringBuilder add(String str) {
    var remLines = str.lines().iterator();
    while (remLines.hasNext()){
      var line = remLines.next();
      sb.append(line);
      if (remLines.hasNext()){
        newLine();
      }
    }
    return this;
  }

  public PrettyprintStringBuilder addln(String str){
    add(str);
    newLine();
    return this;
  }

  public PrettyprintStringBuilder addSpace(){
    return add(" ");
  }

  public PrettyprintStringBuilder addComma(){
    return add(", ");
  }

  public PrettyprintStringBuilder newLine() {
    sb.append(fileConfig.endOfLine()).append(fileConfig.indent().repeat(indentLevel));
    return this;
  }

  public PrettyprintStringBuilder newLineIfNotEmpty() {
    if (!lastLineIsEmpty()) {
      newLine();
    }
    return this;
  }

  @Override
  public String toString() {
    return sb.toString();
  }

  public boolean endsWith(char c){
    return !sb.isEmpty() && sb.charAt(sb.length()-1) == c;
  }

  private boolean lastLineIsEmpty() {
    var eol = fileConfig.endOfLine();
    var lastIdx = sb.lastIndexOf(eol);
    return lastIdx != -1 && lastIdx + eol.length() == sb.length();
  }

}
