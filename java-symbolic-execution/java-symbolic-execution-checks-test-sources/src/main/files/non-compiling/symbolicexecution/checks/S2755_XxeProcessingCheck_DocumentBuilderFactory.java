package symbolicexecution.checks;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

class DocumentBuilderFactoryTest {
  private DocumentBuilder foo() throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory dbf = unknown();
    DocumentBuilder db = dbf.newDocumentBuilder();
    db.parse(new File(""));
    return db;
  }
}
