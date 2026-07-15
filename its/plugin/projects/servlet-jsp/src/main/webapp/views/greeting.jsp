<!DOCTYPE html>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="e" uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" %>

<%@ page import="org.owasp.encoder.Encode" %> 

<html lang="en">

<body>
	<!-- Case: data returned by the Controller -->
	Message: Hello ${name}! <!-- Noncompliant: ${name} is not sanitized -->
	Message: Hello ${safeName}! <!-- Compliant: ${safeName} is sanitized with Encoder in the Controller -->

	<e:forHtml value="${name}" /> <!-- Compliant: Sanitized with e:forHtml where "e" stands for OWASP Encoder -->

	<c:out value="${name}" /> <!-- Compliant: JSTL Core - c:out -->

	<!-- Case: quering the request parameters directly -->
	<%=request.getParameter("name")%> <!-- Noncompliant: URL parameter injected directly in the JSP and so not sanitized -->
	<%= Encode.forHtml(request.getParameter("name")) %> <!-- Compliant: request.getParameter("name") sanitized by Encoder -->
	<e:forHtml value="${param.name}" /> <!-- Compliant: Sanitized with e:forHtml where "e" stands for OWASP Encoder -->
    <h1>${e:forHtml(param.name)}</h1> <!-- Compliant: Sanitized with e:forHtml where "e" stands for OWASP Encoder -->

  <c:if test="true">
    <b>true</b>
  </c:if>
</body>

</html>
