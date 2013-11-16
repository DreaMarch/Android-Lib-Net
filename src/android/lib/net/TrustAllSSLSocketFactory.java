package android.lib.net;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

final class TrustAllSSLSocketFactory extends SSLSocketFactory {
    private static final TrustManager[] TRUST_MANAGERS = new TrustManager[] { new X509TrustManager() {
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    } };

    private final SSLContext context = SSLContext.getInstance(SSLSocketFactory.TLS);

    public TrustAllSSLSocketFactory(final KeyStore keyStore) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        super(keyStore);

        this.context.init(null, TrustAllSSLSocketFactory.TRUST_MANAGERS, null);
    }

    @Override
    public Socket createSocket() throws IOException {
        return this.context.getSocketFactory().createSocket();
    }

    @Override
    public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose) throws IOException, UnknownHostException {
        return this.context.getSocketFactory().createSocket(socket, host, port, autoClose);
    }
}
