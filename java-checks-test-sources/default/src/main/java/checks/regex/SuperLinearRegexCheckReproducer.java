package checks.regex;

import java.util.regex.Pattern;

class SuperLinearRegexCheckReproducer {
    private SuperLinearRegexCheckReproducer() {
        Pattern.compile("\\d+$");
        Pattern.compile("(\\d+)$");
        Pattern.compile("\\d++$");
        Pattern.compile("[0-9]+$");
        Pattern.compile("\\p{Digit}+$");
    }         
}