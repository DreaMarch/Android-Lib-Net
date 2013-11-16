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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

/**
 * An <a href="http://developer.android.com/reference/org/apache/http/client/HttpClient.html">HttpClient</a>
 * implementation that performs HTTP GET and POST on a non-blocking thread.
 * <p>For convenience, {@link AsyncHttpClient} ignores any SSL certification errors.</p>
 */
public final class AsyncHttpClient {
    private static final String HTTP             = "http"; //$NON-NLS-1$
    private static final String HTTPS            = "https"; //$NON-NLS-1$
    private static final String VALUE_USER_AGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)"; //$NON-NLS-1$

    private static final int HTTP_PORT  = 80;
    private static final int HTTPS_PORT = 443;
    private static final int TIMEOUT    = 30000;

    private AsyncHttpClient() {
    }

    /**
     * Performs HTTP GET asynchronously.
     * @param uri the URI to get.
     * @param parameters the parameters to append to the URI.
     * @param listener a callback when the operation completes or when there is an error.
     */
    public static void get(final String uri, final Map<String, String> parameters, final HttpEventListener listener) {
        final Uri.Builder builder = Uri.parse(uri).buildUpon();

        for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
            builder.appendQueryParameter(parameter.getKey(), parameter.getValue());
        }

        AsyncHttpClient.execute(new HttpGet(builder.build().toString()), listener);
    }

    /**
     * Performs HTTP POST asynchronously.
     * @param uri the URI to post.
     * @param parameters the FORM data to post.
     * @param listener a callback when the operation completes or when there is an error.
     */
    public static void post(final String uri, final Map<String, String> parameters, final HttpEventListener listener) {
        final List<NameValuePair> pairs = new ArrayList<NameValuePair>();

        for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
            pairs.add(new BasicNameValuePair(parameter.getKey(), parameter.getValue()));
        }

        final HttpPost request = new HttpPost(uri);

        try {
            request.setEntity(new UrlEncodedFormEntity(pairs, org.apache.http.protocol.HTTP.UTF_8));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        AsyncHttpClient.execute(request, listener);
    }

    private static void execute(final HttpUriRequest request, final HttpEventListener listener) {
        final Handler handler = AsyncHttpClient.createHandler(listener);

        new AsyncTask<HttpUriRequest, Void, Message>() {
            @Override
            protected Message doInBackground(final HttpUriRequest... requests) {
                final DefaultHttpClient client = AsyncHttpClient.createHttpClient();

                AndroidHttpClient.modifyRequestToAcceptGzipResponse(request);

                try {
                    final HttpResponse response = client.execute(request);

                    if (response.getStatusLine().getStatusCode() / 100 == 2) {
                        return handler.obtainMessage(0, IOUtils.toByteArray(AndroidHttpClient.getUngzippedContent(response.getEntity())));
                    }

                    return handler.obtainMessage(response.getStatusLine().getStatusCode());
                } catch (final IOException e) {
                    return handler.obtainMessage(0, e);
                } finally {
                    client.getConnectionManager().shutdown();
                }
            }

            @Override
            protected void onPostExecute(final Message message) {
                handler.sendMessage(message);
            }
        }.execute(request);
    }

    protected static DefaultHttpClient createHttpClient() {
        final HttpParams params = new BasicHttpParams();

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, org.apache.http.protocol.HTTP.UTF_8);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpProtocolParams.setUserAgent(params, AsyncHttpClient.VALUE_USER_AGENT);
        HttpConnectionParams.setConnectionTimeout(params, AsyncHttpClient.TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, AsyncHttpClient.TIMEOUT);

        try {
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            final SSLSocketFactory factory = new TrustAllSSLSocketFactory(keyStore);
            factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme(AsyncHttpClient.HTTP, PlainSocketFactory.getSocketFactory(), AsyncHttpClient.HTTP_PORT));
            registry.register(new Scheme(AsyncHttpClient.HTTPS, factory, AsyncHttpClient.HTTPS_PORT));

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

    private static Handler createHandler(final HttpEventListener listener) {
        return new Handler() {
            @Override
            public void handleMessage(final Message message) {
                if (listener != null) {
                    if (message.what == 0) {
                        if (message.obj instanceof Exception) {
                            listener.onError(HttpStatus.SC_OK, (Exception)message.obj);
                        } else {
                            listener.onComplete((byte[])message.obj);
                        }
                    } else {
                        listener.onError(message.what, null);
                    }
                }
            }
        };
    }
}
