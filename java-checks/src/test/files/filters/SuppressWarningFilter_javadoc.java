package my.api.abc;

class NotDocumented {
  public String notDocumented() { // WithIssue
    return "";
  }
}

@SuppressWarnings("javadoc")
class NotDocumentedSupressed {

  public String notDocumented() {  // NoIssue
    return "";
  }
}
