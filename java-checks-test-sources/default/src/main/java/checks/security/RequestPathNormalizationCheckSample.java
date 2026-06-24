package checks.security;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;

class RequestPathNormalizationCheckSample {

  void servletDirectPathCheck(HttpServletRequest request) {
    if (request.getRequestURI().startsWith("/admin")) { // Noncompliant {{Normalize this path before using it in a security check.}}
      checkAdminPermission();
    }
  }

  void servletPathVariable(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void servletPathContainsCheck(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.contains("/protected")) {
      checkPermission();
    }
  }

  void servletPathEqualsCheck(HttpServletRequest request) {
    String requestPath = request.getRequestURI();
    if (requestPath.equals("/api/admin")) { // Noncompliant
      return;
    }
  }

  void servletPathMatchesCheck(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.matches("/admin.*")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void servletPathIndexOfCheck(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.indexOf("/secure") >= 0) {
      checkPermission();
    }
  }

  void servletPathEndsWithCheck(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.endsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void servletGetPathInfo(HttpServletRequest request) {
    String path = request.getPathInfo();
    if (path.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void jakartaServlet(jakarta.servlet.http.HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void jaxRsUriInfo(UriInfo uriInfo) {
    String path = uriInfo.getPath();
    if (path.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void containerRequestContext(ContainerRequestContext requestContext) {
    String path = requestContext.getUriInfo().getPath();
    if (path.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void throwException(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (!path.startsWith("/public")) { // Noncompliant
      throw new ForbiddenException();
    }
  }

  boolean returnStatement(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/admin"); // Noncompliant
  }

  void ternaryOperator(HttpServletRequest request) {
    String path = request.getRequestURI();
    boolean isAdmin = path.startsWith("/admin") ? true : false; // Noncompliant
  }

  void normalizedInline(HttpServletRequest request) {
    if (request.getRequestURI().replaceAll("/+", "/").startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void normalizedVariable(HttpServletRequest request) {
    String path = request.getRequestURI();
    String normalizedPath = path.replaceAll("/+", "/");
    if (normalizedPath.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void normalizedWithDoubleSlashPattern(HttpServletRequest request) {
    String path = request.getRequestURI();
    String normalizedPath = path.replaceAll("//+", "/");
    if (normalizedPath.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void normalizedWithRegexPattern(HttpServletRequest request) {
    String path = request.getRequestURI();
    String normalizedPath = path.replaceAll("/{2,}", "/");
    if (normalizedPath.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void normalizedWithURI(HttpServletRequest request) {
    String path = request.getRequestURI();
    String normalizedPath = java.net.URI.create(path).normalize().toString();
    if (normalizedPath.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void normalizedReversedEquals(HttpServletRequest request) {
    String path = request.getRequestURI();
    String normalizedPath = path.replaceAll("/+", "/");
    if ("/admin".equals(normalizedPath)) {
      checkAdminPermission();
    }
  }

  void normalizedObjectsEquals(HttpServletRequest request) {
    String path = request.getRequestURI();
    String normalizedPath = path.replaceAll("/+", "/");
    if (java.util.Objects.equals(normalizedPath, "/admin")) {
      checkAdminPermission();
    }
  }

  void normalizedPatternMatches(HttpServletRequest request) {
    String path = request.getRequestURI();
    String normalizedPath = path.replaceAll("/+", "/");
    if (java.util.regex.Pattern.matches("/admin.*", normalizedPath)) {
      checkAdminPermission();
    }
  }

  void nonSecurityStringOperation(HttpServletRequest request) {
    String path = request.getRequestURI();
    System.out.println(path.startsWith("/admin"));
  }

  void logging(HttpServletRequest request) {
    String path = request.getRequestURI();
    log(path);
  }

  void nonPathString(String input) {
    if (input.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void multipleAssignmentsReassigned(HttpServletRequest request) {
    String path = request.getRequestURI();
    path = path.replaceAll("/+", "/");
    if (path.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void pathNotUsedInCheck(HttpServletRequest request) {
    String path = request.getRequestURI();
    String otherString = "/admin";
    if (otherString.startsWith("/api")) {
      checkAdminPermission();
    }
  }

  void complexConditional(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.startsWith("/admin") && isAuthenticated()) { // Noncompliant
      grantAccess();
    }
  }

  // Test cases for reversed/alternative comparison forms
  void reversedEquals(HttpServletRequest request) {
    String path = request.getRequestURI();
    if ("/admin".equals(path)) { // Noncompliant
      checkAdminPermission();
    }
  }

  void objectsEquals(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (java.util.Objects.equals(path, "/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void objectsEqualsReversed(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (java.util.Objects.equals("/admin", path)) { // Noncompliant
      checkAdminPermission();
    }
  }

  void equalsIgnoreCase(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.equalsIgnoreCase("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void equalsIgnoreCaseReversed(HttpServletRequest request) {
    String path = request.getRequestURI();
    if ("/admin".equalsIgnoreCase(path)) { // Noncompliant
      checkAdminPermission();
    }
  }

  void patternMatches(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (java.util.regex.Pattern.matches("/admin.*", path)) { // Noncompliant
      checkAdminPermission();
    }
  }

  void regionMatches(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.regionMatches(0, "/admin", 0, 6)) { // Noncompliant
      checkAdminPermission();
    }
  }

  void regionMatchesReversed(HttpServletRequest request) {
    String path = request.getRequestURI();
    if ("/admin".regionMatches(0, path, 0, 6)) { // Noncompliant
      checkAdminPermission();
    }
  }

  // Negative test cases for non-security contexts
  void staticResourceCheck(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.endsWith(".css")) {
      serveStaticFile();
    }
  }

  void contentTypeRouting(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.endsWith(".json")) {
      setJsonContentType();
    }
  }

  void featureToggle(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.contains("/beta")) {
      enableBetaFeatures();
    }
  }

  // Test assignment expressions
  void assignmentExpression(HttpServletRequest request) {
    String path;
    path = request.getRequestURI();
    if (path.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void assignmentWithNormalization(HttpServletRequest request) {
    String path;
    path = request.getRequestURI();
    path = path.replaceAll("/+", "/");
    if (path.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void getServletPath(HttpServletRequest request) {
    String path = request.getServletPath();
    if (path.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void jakartaUriInfo(jakarta.ws.rs.core.UriInfo uriInfo) {
    String path = uriInfo.getPath();
    if (path.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void memberSelectExpression(HttpServletRequest request) {
    this.pathField = request.getRequestURI();
    if (this.pathField.startsWith("/admin")) { // Noncompliant
      checkAdminPermission();
    }
  }

  void memberSelectNormalized(HttpServletRequest request) {
    this.pathField = request.getRequestURI();
    this.pathField = this.pathField.replaceAll("/+", "/");
    if (this.pathField.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void uriNormalize(HttpServletRequest request) {
    String path = request.getRequestURI();
    java.net.URI uri = java.net.URI.create(path);
    String normalized = uri.normalize().toString();
    if (normalized.startsWith("/admin")) {
      checkAdminPermission();
    }
  }

  void conditionalOr(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.startsWith("/admin") || path.startsWith("/root")) { // Noncompliant 2
      checkAdminPermission();
    }
  }

  String pathField;

  private void checkAdminPermission() {}
  private void checkPermission() {}
  private void log(String msg) {}
  private boolean isAuthenticated() { return true; }
  private void grantAccess() {}
  private void serveStaticFile() {}
  private void setJsonContentType() {}
  private void enableBetaFeatures() {}
}
