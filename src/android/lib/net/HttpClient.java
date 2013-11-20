package android.lib.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.net.Uri;
import android.net.http.AndroidHttpClient;

/**
 * An <a href="http://developer.android.com/reference/org/apache/http/client/HttpClient.html">HttpClient</a>
 * implementation that performs HTTP GET and POST on the main thread.
 * <p>For convenience, {@link AsyncHttpClient} ignores any SSL certification errors.</p>
 */
public final class HttpClient {
    private static final String HTTP             = "http";  //$NON-NLS-1$
    private static final String HTTPS            = "https"; //$NON-NLS-1$
    private static final String VALUE_USER_AGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)"; //$NON-NLS-1$

    private static final int HTTP_PORT  = 80;
    private static final int HTTPS_PORT = 443;
    private static final int TIMEOUT    = 30000;

    private HttpClient() {
    }

    /**
     * Performs a HTTP GET operation.
     * @param url the URL to get.
     * @return the data returned from the server, if any.
     */
    public static byte[] get(final String url) throws HttpException {
        return HttpClient.get(url, null);
    }

    /**
     * Performs a HTTP GET operation.
     * @param uri the URI to get.
     * @param parameters the parameters to append to the URI.
     * @return the data returned from the server, if any.
     */
    public static byte[] get(final String uri, final Map<String, String> parameters) throws HttpException {
        final Uri.Builder builder = Uri.parse(uri).buildUpon();

        if (parameters != null) {
            for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
                builder.appendQueryParameter(parameter.getKey(), parameter.getValue());
            }
        }

        final DefaultHttpClient client = HttpClient.createHttpClient();

        try {
            final HttpResponse response = client.execute(new HttpGet(builder.build().toString()));

            if (response.getStatusLine().getStatusCode() / 100 == 2) {
                return IOUtils.toByteArray(AndroidHttpClient.getUngzippedContent(response.getEntity()));
            }

            throw new HttpException("Server responded status " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$
        } catch (final IOException e) {
            throw new HttpException(e.getMessage(), e);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Performs a HTTP POST operation.
     * @param url the URL to post.
     * @return the data returned from the server, if any.
     */
    public static byte[] post(final String url) throws HttpException {
        return HttpClient.post(url, null);
    }

    /**
     * Performs a HTTP POST operation.
     * @param uri the URI to post.
     * @param parameters the FORM data to post.
     * @return the data returned from the server, if any.
     */
    public static byte[] post(final String uri, final Map<String, String> parameters) throws HttpException {
        final HttpPost request = new HttpPost(uri);

        if (parameters != null) {
            final List<NameValuePair> pairs = new ArrayList<NameValuePair>();

            for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
                pairs.add(new BasicNameValuePair(parameter.getKey(), parameter.getValue()));
            }

            try {
                request.setEntity(new UrlEncodedFormEntity(pairs, org.apache.http.protocol.HTTP.UTF_8));
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        final DefaultHttpClient client = HttpClient.createHttpClient();

        try {
            final HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() / 100 == 2) {
                return IOUtils.toByteArray(AndroidHttpClient.getUngzippedContent(response.getEntity()));
            }

            throw new HttpException("Server responded status " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$
        } catch (final IOException e) {
            throw new HttpException(e.getMessage(), e);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    static DefaultHttpClient createHttpClient() {
        final HttpParams params = new BasicHttpParams();

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, org.apache.http.protocol.HTTP.UTF_8);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpProtocolParams.setUserAgent(params, HttpClient.VALUE_USER_AGENT);
        HttpConnectionParams.setConnectionTimeout(params, HttpClient.TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, HttpClient.TIMEOUT);

        try {
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            final SSLSocketFactory factory = new TrustAllSSLSocketFactory(keyStore);
            factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme(HttpClient.HTTP, PlainSocketFactory.getSocketFactory(), HttpClient.HTTP_PORT));
            registry.register(new Scheme(HttpClient.HTTPS, factory, HttpClient.HTTPS_PORT));

            return new DefaultHttpClient(new ThreadSafeClientConnManager(params, registry), params);
        } catch (final KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (final CertificateException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (final UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
