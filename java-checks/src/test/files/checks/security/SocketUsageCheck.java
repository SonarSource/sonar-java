import io.netty.channel.ChannelInitializer;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import javax.net.SocketFactory;

// === java.net ===
class A {
  void foo(SocketFactory factory, String address, int port, InetAddress localAddr, int localPort, boolean stream,
    String host, Proxy proxy, int backlog, InetAddress bindAddr)
    throws Exception {
    new Socket(); // Noncompliant
    new Socket(address, port); // Noncompliant
    new Socket(address, port, localAddr, localPort); // Noncompliant
    new Socket(host, port, stream); // Noncompliant
    new Socket(proxy); // Noncompliant
    new Socket(host, port); // Noncompliant
    new Socket(host, port, stream); // Noncompliant
    new Socket(host, port, localAddr, localPort); // Noncompliant

    new ServerSocket(); // Noncompliant
    new ServerSocket(port); // Noncompliant
    new ServerSocket(port, backlog); // Noncompliant
    new ServerSocket(port, backlog, bindAddr); // Noncompliant

    factory.createSocket(); // Noncompliant
  }

  void bar() {
    new SocketFactory() { // Noncompliant
      @Override
      public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        return null;
      }

      @Override
      public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        return null;
      }

      @Override
      public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return null;
      }

      @Override
      public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return null;
      }
    };
  }
}

abstract class mySocketFactory extends SocketFactory { // Noncompliant
  // ...
}

// === java.nio.channels ===
class B {
  void foo(AsynchronousChannelGroup group, SocketAddress remote) throws Exception {
    AsynchronousServerSocketChannel.open(); // Noncompliant
    AsynchronousServerSocketChannel.open(group); // Noncompliant
    AsynchronousSocketChannel.open(); // Noncompliant
    AsynchronousSocketChannel.open(group); // Noncompliant
    SocketChannel.open(); // Noncompliant
    SocketChannel.open(remote); // Noncompliant
    ServerSocketChannel.open(); // Noncompliant
  }
}

// === Netty ===
class C {
  void foo() {
    new ChannelInitializer<io.netty.channel.socket.SocketChannel>() { // Noncompliant
      @Override
      public void initChannel(io.netty.channel.socket.SocketChannel ch) throws Exception {
        // ...
      }
    };
  }
}

class D extends ChannelInitializer<io.netty.channel.socket.SocketChannel> { // Noncompliant
  @Override
  public void initChannel(io.netty.channel.socket.SocketChannel ch) throws Exception {
    // ...
  }
}

class E extends D { } // Noncompliant

class F extends Object { } // For coverage
