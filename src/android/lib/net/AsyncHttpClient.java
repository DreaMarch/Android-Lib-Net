package android.lib.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

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
    private AsyncHttpClient() {
    }

    /**
     * Performs HTTP GET asynchronously.
     * @param url the URL to get.
     * @param listener a callback when the operation completes or when there is an error.
     */
    public static void get(final String url, final HttpEventListener listener) {
        AsyncHttpClient.get(url, null, listener);
    }

    /**
     * Performs HTTP GET asynchronously.
     * @param uri the URI to get.
     * @param parameters the parameters to append to the URI.
     * @param listener a callback when the operation completes or when there is an error.
     */
    public static void get(final String uri, final Map<String, String> parameters, final HttpEventListener listener) {
        final Uri.Builder builder = Uri.parse(uri).buildUpon();

        if (parameters != null) {
            for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
                builder.appendQueryParameter(parameter.getKey(), parameter.getValue());
            }
        }

        AsyncHttpClient.execute(new HttpGet(builder.build().toString()), listener);
    }

    /**
     * Performs HTTP POST asynchronously.
     * @param url the URL to post.
     * @param listener a callback when the operation completes or when there is an error.
     */
    public static void post(final String url, final HttpEventListener listener) {
        AsyncHttpClient.post(url, null, listener);
    }

    /**
     * Performs HTTP POST asynchronously.
     * @param uri the URI to post.
     * @param parameters the FORM data to post.
     * @param listener a callback when the operation completes or when there is an error.
     */
    public static void post(final String uri, final Map<String, String> parameters, final HttpEventListener listener) {
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

        AsyncHttpClient.execute(request, listener);
    }

    private static void execute(final HttpUriRequest request, final HttpEventListener listener) {
        final Handler handler = AsyncHttpClient.createHandler(listener);

        new AsyncTask<HttpUriRequest, Void, Message>() {
            @Override
            protected Message doInBackground(final HttpUriRequest... requests) {
                final DefaultHttpClient client = HttpClient.createHttpClient();

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
