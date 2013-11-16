Network Utilities for Android
=============================

A collection of network utilities for Android.

1. AsyncHttpClient
------------------

An <a href="http://developer.android.com/reference/org/apache/http/client/HttpClient.html">HttpClient</a> implementation that performs HTTP GET and POST on a non-blocking thread.

For convenience, AsyncHttpClient ignores any SSL certification errors.

**Example**

    ProgressDialog progressDialog = ProgressDialog.show(getContext(), null, R.string.wait, true, false);
    
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("username", "abc");
    parameters.put("password", "def");
    
    AsyncHttpClient.post("http://www.example.com/login", parameters, new HttpEventListener() {
        @Override
        public void onComplete(byte[] receivedData) {
            progressDialog.dismiss();
            
            Toast.makeText(getActivity(), R.string.success_message, Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onError(int statusCode, Exception exception) {
            progressDialog.dismiss();
            
            Toast.makeText(getActivity(), R.string.error_message, Toast.LENGTH_SHORT).show();
        }
    });