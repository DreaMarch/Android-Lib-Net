package android.lib.net;

public interface HttpEventListener {
    void onComplete(byte[] receivedData);

    void onError(int statusCode, Exception exception);
}
