package com.example.network.mqtt;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.Collections;

public class SniSslSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;
    private final String hostname;
    private final int port;

    public SniSslSocketFactory(String hostname, int port) throws Exception {
        this.hostname = hostname;
        this.port = port;
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        this.delegate = context.getSocketFactory();
    }

    private SSLSocket enableSni(Socket rawSocket) throws IOException {
        SSLSocket sslSocket = (SSLSocket) delegate.createSocket(
                rawSocket,
                hostname,
                port,
                true
        );
        SSLParameters sslParameters = sslSocket.getSSLParameters();
        sslParameters.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
        sslSocket.setSSLParameters(sslParameters);
        return sslSocket;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableSni(s);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket plainSocket = new Socket();
        plainSocket.connect(new InetSocketAddress(host, port));
        return enableSni(plainSocket);
    }
    static SSLSocketFactory createInsecureSslSocketFactory() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc.getSocketFactory();
    }


    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket plainSocket = new Socket();
        plainSocket.connect(new InetSocketAddress(host, port));
        return enableSni(plainSocket);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket plainSocket = new Socket();
        plainSocket.bind(new InetSocketAddress(localHost, localPort));
        plainSocket.connect(new InetSocketAddress(host, port));
        return enableSni(plainSocket);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket plainSocket = new Socket();
        plainSocket.bind(new InetSocketAddress(localAddress, localPort));
        plainSocket.connect(new InetSocketAddress(address, port));
        return enableSni(plainSocket);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }
}
