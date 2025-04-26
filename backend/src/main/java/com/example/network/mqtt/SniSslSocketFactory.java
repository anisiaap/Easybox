// com/example/network/mqtt/SniSslSocketFactory.java

package com.example.network.mqtt;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Collections;

public class SniSslSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;
    private final String hostname;
    private final int port;

    /**
     * Creates an SSLSocketFactory that trusts all certificates AND sets SNI.
     */
    public SniSslSocketFactory(String hostname, int port) throws Exception {
        this.hostname = hostname;
        this.port = port;

        // trust-all trust manager
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
        };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAll, new SecureRandom());
        this.delegate = context.getSocketFactory();
    }

    private SSLSocket enableSni(Socket rawSocket) throws IOException {
        SSLSocket sslSocket = (SSLSocket) delegate.createSocket(rawSocket, hostname, port, true);
        SSLParameters sslParameters = sslSocket.getSSLParameters();
        sslParameters.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
        sslSocket.setSSLParameters(sslParameters);
        return sslSocket;
    }

    // All createSocket methods delegate to enableSni(...)
    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableSni(s);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket plain = new Socket();
        plain.connect(new InetSocketAddress(host, port));
        return enableSni(plain);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket(host.getHostAddress(), port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket plain = new Socket();
        plain.bind(new InetSocketAddress(localHost, localPort));
        plain.connect(new InetSocketAddress(host, port));
        return enableSni(plain);
    }

    @Override
    public Socket createSocket(InetAddress addr, int port, InetAddress local, int localPort) throws IOException {
        return createSocket(addr.getHostAddress(), port, local, localPort);
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
