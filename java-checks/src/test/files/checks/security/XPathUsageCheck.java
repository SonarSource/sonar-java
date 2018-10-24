import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

// === javax.xml.xpath.XPath ===
abstract class A {
  void foo(XPath xpath, String expression, InputSource source, QName returnType, Object item) throws Exception {
    xpath.compile(expression); // Noncompliant [[sc=11;ec=18]] {{Make sure that executing this XPATH expression is safe here.}}
    xpath.evaluate(expression, source); // Noncompliant
    xpath.evaluate(expression, source, returnType); // Noncompliant
    xpath.evaluate(expression, item); // Noncompliant
    xpath.evaluate(expression, item, returnType); // Noncompliant

    compile(expression);
    evaluate(expression);
    selectNodeList();
  }

  abstract void compile(String str);

  abstract void evaluate(String str);

  abstract void selectNodeList();
}

// === Apache XML Security ===
class B {
  void foo(org.apache.xml.security.utils.XPathAPI api, Node contextNode, String str, Node namespaceNode, PrefixResolver prefixResolver,
    Node xpathnode) throws Exception {
    api.evaluate(contextNode, xpathnode, str, namespaceNode); // Noncompliant
    api.selectNodeList(contextNode, xpathnode, str, namespaceNode); // Noncompliant
  }
}

// === Apache Xalan ===
class C {
  void foo(XPathAPI api, Node contextNode, String str, Node namespaceNode, PrefixResolver prefixResolver)
    throws Exception {
    XPathAPI.eval(contextNode, str); // Noncompliant
    XPathAPI.eval(contextNode, str, namespaceNode); // Noncompliant
    XPathAPI.eval(contextNode, str, prefixResolver); // Noncompliant
    XPathAPI.selectNodeIterator(contextNode, str); // Noncompliant
    XPathAPI.selectNodeIterator(contextNode, str, namespaceNode); // Noncompliant
    XPathAPI.selectNodeList(contextNode, str); // Noncompliant
    XPathAPI.selectNodeList(contextNode, str, namespaceNode); // Noncompliant
    XPathAPI.selectSingleNode(contextNode, str); // Noncompliant
    XPathAPI.selectSingleNode(contextNode, str, namespaceNode); // Noncompliant
  }
}

// === org.apache.commons.jxpath ===
abstract class D extends JXPathContext {
  D(JXPathContext compilationContext, Object contextBean) {
    super(compilationContext, contextBean);
  }

  void foo(JXPathContext context, String str, Object obj, Class<?> requiredType) {
    JXPathContext.compile(str); // Noncompliant
    this.compilePath(str); // Noncompliant
    context.createPath(str); // Noncompliant
    context.createPathAndSetValue(str, obj); // Noncompliant
    context.getPointer(str); // Noncompliant
    context.getValue(str); // Noncompliant
    context.getValue(str, requiredType); // Noncompliant
    context.iterate(str); // Noncompliant
    context.iteratePointers(str); // Noncompliant
    context.removeAll(str); // Noncompliant
    context.removePath(str); // Noncompliant
    context.selectNodes(str); // Noncompliant
    context.selectSingleNode(str); // Noncompliant
    context.setValue(str, obj); // Noncompliant
  }
}
