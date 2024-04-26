package checks.security;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.apache.wicket.protocol.http.BufferedWebResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class DisclosingTechnologyFingerprints {
  
  public String noncompliants5689httpservletresponse(HttpServletResponse response) {
    response.addHeader("x-powered-by", "myproduct"); // Noncompliant {{Make sure disclosing version information of this web technology is safe here.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    response.addHeader("Server", "apache"); // Noncompliant {{Make sure disclosing version information of this web technology is safe here.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    return "thymeleaf/welcome";
  }

  public ResponseEntity<String> noncompliants5689responseentity() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("x-powered-by", "myproduct"); // Noncompliant {{Make sure disclosing version information of this web technology is safe here.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    return new ResponseEntity<String>("Hello World", responseHeaders, HttpStatus.CREATED);
  }

  public ResponseEntity<String> noncompliants5689responseentitybuilder() throws URISyntaxException {
    ResponseEntity.BodyBuilder location = ResponseEntity.created(new URI("location"));
    return location.header("server", "apache").body("Hello World"); // Noncompliant
  }

  public ResponseEntity<String> noncompliants5689responseentitybuilderhttpheaders() {

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("x-powered-by", "myproduct"); // Noncompliant
    responseHeaders.add("Server", "myproduct"); // Noncompliant
    return ResponseEntity.ok().headers(responseHeaders).body("Hello World");
  }

  public ResponseEntity<String> compliants5689responseentity() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.remove("x-powered-by"); // Compliant

    return new ResponseEntity<String>("Hello World", responseHeaders, HttpStatus.CREATED);
  }
}

class Servlet implements javax.servlet.Servlet {

  @Override
  public void init(ServletConfig servletConfig) throws ServletException { }

  @Override
  public ServletConfig getServletConfig() {
    return null;
  }

  @Override
  public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
    ((HttpServletResponse) servletResponse).setHeader("Server", ""); // Noncompliant
    ((HttpServletResponse) servletResponse).addHeader("x-powered-by", ""); // Noncompliant
    ((HttpServletResponseWrapper) servletResponse).setHeader("x-powered-by", ""); // Noncompliant
    ((HttpServletResponseWrapper) servletResponse).addHeader("server", ""); // Noncompliant

    ((org.apache.wicket.request.http.WebResponse) servletResponse).addHeader("x-powered-by",""); // Noncompliant
    ((org.apache.wicket.request.http.WebResponse) servletResponse).setHeader("Server",""); // Noncompliant

    ((BufferedWebResponse) servletResponse).addHeader("x-powered-by",""); // Noncompliant
    ((BufferedWebResponse) servletResponse).setHeader("Server",""); // Noncompliant

    ((BufferedWebResponse) servletResponse).addHeader("x-powered-by2",""); // Compliant
    ((BufferedWebResponse) servletResponse).setHeader("Server2",""); // Compliant

    ((org.apache.wicket.protocol.http.servlet.ServletWebResponse) servletResponse).addHeader("Server",""); // Noncompliant
    ((org.apache.wicket.protocol.http.servlet.ServletWebResponse) servletResponse).setHeader("x-powered-by",""); // Noncompliant

    ((org.rapidoid.http.Resp) servletResponse).header("SeRver", "XXX"); // Noncompliant
    ((org.rapidoid.http.Resp) servletResponse).header("x-powered-by", "XXX"); // Noncompliant
    ((org.rapidoid.http.Resp) servletResponse).header("header", "XXX"); // Compliant

  }

  @Override
  public String getServletInfo() {
    return null;
  }

  @Override
  public void destroy() { }
}
