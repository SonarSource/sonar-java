class A {
  String ip = "0.0.0.0"; // Noncompliant
  String url = "http://192.168.0.1/admin.html"; // Noncompliant
  String url2 = "http://www.example.org"; // Compliant
  int a = 42; // Compliant
  String notAnIp1 = "0.0.0.1234"; // Compliant
  String notAnIp2 = "1234.0.0.0"; // Compliant
  String notAnIp3 = "1234.0.0.0.0.1234"; // Compliant
  String notAnIp4 = ".0.0.0.0"; // Compliant
  String notAnIp5 = "0.256.0.0"; // Compliant
}
