package org.sonar.java.checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Krzysztof Suszyński <krzysztof.suszynski@wavesoftware.pl>
 */
class RegularExpressionCheckTestFile {

    private static final Logger LOG = LoggerFactory.getLogger(RegularExpressionCheckTestFile.class);

    private void sampleMethod() {
        // a comment
        LOG.debug(new Eid("20150822:222000", "A test mess"));
        LOG.warn("A message");

        // a split line
        LoggerFactory.getLogger(RegularExpressionCheckTestFile.class).error("ddd");
        LoggerFactory.getLogger(RegularExpressionCheckTestFile.class).error(ex);

        LoggerFactory.getLogger(RegularExpressionCheckTestFile.class).error(new Eid("20150822:222140"));
        LOG.error(new Cin("20150822:222140"));
    }
}