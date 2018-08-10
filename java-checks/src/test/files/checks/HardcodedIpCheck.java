class A {
  String ip = "10.0.0.0"; // Noncompliant [[sc=15;ec=25]] {{Make sure using this hardcoded IP address is safe here.}}
  String ipAndPort = "10.0.0.0:0"; // Noncompliant [[sc=22;ec=34]] {{Make sure using this hardcoded IP address is safe here.}}
  String url = "http://192.168.0.1/admin.html"; // Noncompliant {{Make sure using this hardcoded IP address is safe here.}}
  String urlWithPort = "http://192.168.0.1:8000/admin.html"; // Noncompliant {{Make sure using this hardcoded IP address is safe here.}}
  String url2 = "http://www.example.org";
  int a = 42;

  String broadcastAddress = "255.255.255.255";
  String loopbackAddress1 = "127.0.0.1";
  String loopbackAddress2 = "127.2.3.4";
  String nonRoutableAddress = "0.0.0.0";

  String notAnIp1 = "0.0.0.1234";
  String notAnIp2 = "1234.0.0.0";
  String notAnIp3 = "1234.0.0.0.0.1234";
  String notAnIp4 = ".10.0.0.0";
  String notAnIp5 = "0.256.0.0";

  String fileName = "v0.0.1.200__do_something.sql"; // Compliant - suffixed and prefixed
  String version = "1.0.0.0-1"; // Compliant - suffixed
}
