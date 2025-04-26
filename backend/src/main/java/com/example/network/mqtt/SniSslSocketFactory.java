// src/main/java/com/example/network/mqtt/SniSslSocketFactory.java
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

    public SniSslSocketFactory(String hostname, int port) throws Exception {
        this.hostname = hostname;
        this.port = port;

        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
        };

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new SecureRandom());
        this.delegate = ctx.getSocketFactory();
    }

    /**
     * Paho may call createSocket() with no args, so override it here.
     */
    @Override
    public Socket createSocket() throws IOException {
        SSLSocket ssl = (SSLSocket) delegate.createSocket();
        SSLParameters params = ssl.getSSLParameters();
        params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
        ssl.setSSLParameters(params);
        return ssl;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableSni(s);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket sock = new Socket();
        sock.connect(new InetSocketAddress(host, port));
        return enableSni(sock);
    }

    @Override
    public Socket createSocket(InetAddress addr, int port) throws IOException {
        return createSocket(addr.getHostAddress(), port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket sock = new Socket();
        sock.bind(new InetSocketAddress(localHost, localPort));
        sock.connect(new InetSocketAddress(host, port));
        return enableSni(sock);
    }

    @Override
    public Socket createSocket(InetAddress addr, int port, InetAddress local, int localPort) throws IOException {
        return createSocket(addr.getHostAddress(), port, local, localPort);
    }

    private SSLSocket enableSni(Socket raw) throws IOException {
        SSLSocket ssl = (SSLSocket) delegate.createSocket(
                raw, hostname, port, true
        );
        SSLParameters params = ssl.getSSLParameters();
        params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
        ssl.setSSLParameters(params);
        return ssl;
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
