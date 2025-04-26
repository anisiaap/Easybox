package com.example.network.mqtt;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;

public class SniSslSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;
    private final String hostname;

    public SniSslSocketFactory(String hostname) throws Exception {
        this.hostname = hostname;
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        this.delegate = context.getSocketFactory();
    }

    private void enableSni(SSLSocket sslSocket) {
        SSLParameters sslParameters = sslSocket.getSSLParameters();
        sslParameters.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
        sslSocket.setSSLParameters(sslParameters);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocket sslSocket = (SSLSocket) delegate.createSocket(s, host, port, autoClose);
        enableSni(sslSocket);
        return sslSocket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port);
        enableSni(sslSocket);
        return sslSocket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        SSLSocket sslSocket = (SSLSocket) delegate.createSocket(address, port);
        enableSni(sslSocket);
        return sslSocket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port, localAddress, localPort);
        enableSni(sslSocket);
        return sslSocket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket sslSocket = (SSLSocket) delegate.createSocket(address, port, localAddress, localPort);
        enableSni(sslSocket);
        return sslSocket;
    }
}
