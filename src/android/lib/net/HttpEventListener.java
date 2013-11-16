package android.lib.net;

public interface HttpEventListener {
    /**
     * Called when the request completes.
     * @param receivedData the data received from the server; or <code>null</code> if the server did not return anything.
     */
    void onComplete(byte[] receivedData);

    /*
     * Called if there was any error during the process.
     * @param statusCode the HTTP status code of the response.
     * <p>Any status code that is not 2xx is considered an error.</p>
     * @param exception the exception thrown, if any.
     */
    void onError(int statusCode, Exception exception);
}
