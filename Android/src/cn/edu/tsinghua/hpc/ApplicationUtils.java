
package cn.edu.tsinghua.hpc;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class ApplicationUtils {

    private static ApplicationUtils mApplicationUtils = null;

    private static TransAppHttpClient hc = null;

    private static AndroidHttpClient androidClient = null;

    // private static String URL = "http://202.104.141.244:8082/";
    private static String URL = "http://123.99.230.173:8080/";

    // private static String URL = "http://172.16.1.53:8080/";

    private ApplicationUtils() {
        if (hc == null) {
            BasicHttpParams httpParams = new BasicHttpParams();
            // set timeout in milliseconds until a connection is established.
            HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
            // default socket timeout in milliseconds waiting for data
            HttpConnectionParams.setSoTimeout(httpParams, 5000);
            hc = new TransAppHttpClient(httpParams);
            androidClient = AndroidHttpClient.newInstance("android");

            // if (!SystemProperties.get("ro.tserver.address", "").equals("")) {
            // URL = "http://" + SystemProperties.get("ro.tserver.address", "")
            // + "/";
            // }
        }
    }

    public static final ApplicationUtils getInstance() {
        if (mApplicationUtils == null) {
            mApplicationUtils = new ApplicationUtils();
        }
        return mApplicationUtils;
    }

    public List<TransAppInfo> downloadTAppInfo() {
        List<TransAppInfo> results = new ArrayList<TransAppInfo>();
        try {
            URL url = new URL(URL + "packages");
            URLConnection urlConnection = url.openConnection();
            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            try {
                String content = br.readLine();
                JSONArray result = new JSONArray(content);
                for (int i = 0; i < result.length(); i++) {
                    JSONObject obj = result.getJSONObject(i);
                    results.add(new TransAppInfo(obj.getString("name"), obj.getString("label"), obj
                            .optInt("version", 0), obj.optLong("size", 0)));
                }

            } catch (Exception e) {
            }
        } catch (IOException e) {

        }
        return results;
    }

    public boolean downloadIcon(String packageName, Context context) {
        try {
            HttpGet httpget = new HttpGet(new URI(URL + packageName + "/icon"));
            HttpEntity entity = androidClient.execute(httpget).getEntity();
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)
                    && Environment.getExternalStorageDirectory().canWrite()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                entity.writeTo(buffer);
                File d = new File("/mnt/sdcard/transapp/" + packageName + "/");
                if (d.canWrite()) {
                    FileOutputStream f = new FileOutputStream(d.getAbsolutePath() + "/icon");
                    f.write(buffer.toByteArray());
                    f.close();
                }

            }
            return true;
        } catch (IOException e) {

        } catch (URISyntaxException e) {
        }
        return false;
    }

    public String downloadTApp(String packageName, String name, Context context) {
        HttpGet get = new HttpGet();
        try {
            get.setURI(new URI(URL + packageName + "/package"));
            return hc.downloadAPK(get, name, context);
        } catch (URISyntaxException e) {

        }
        return null;
    }

    public class TransAppInfo {
        public TransAppInfo(String pack, String label, int versionCode, long bytes) {
            packageName = pack;
            title = label;
            version = versionCode;
            size = bytes;
        }

        String packageName;

        String title;

        int version;

        long size;
    }

    private class TransAppHttpClient extends DefaultHttpClient {
        private static final String TAG = "transapphttpclient";

        public TransAppHttpClient(HttpParams httpParams) {
            super(httpParams);
        }

        public List<TransAppInfo> downloadApplicationInfo(HttpGet get) {
            HttpResponse rp = null;
            List<TransAppInfo> results = new ArrayList<TransAppInfo>();
            try {
                rp = super.execute(get);
                Log.d("test", "downloadApplicationInfo stage1");

                JSONArray result = new JSONArray(EntityUtils.toString(rp.getEntity()));
                Log.d("test", "downloadApplicationInfo stage2");
                for (int i = 0; i < result.length(); i++) {
                    JSONObject obj = result.getJSONObject(i);
                    results.add(new TransAppInfo(obj.getString("name"), obj.getString("label"), obj
                            .optInt("version", 0), obj.optLong("size", 0)));
                }

            } catch (Exception e) {
                Log.d("test", "downloadApplicationInfo error " + e.getMessage());
            }
            return results;
        }

        public boolean downloadIcon_(HttpGet get, String packageName, Context context) {
            HttpResponse rp = null;
            try {
                rp = super.execute(get);
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)
                        && Environment.getExternalStorageDirectory().canWrite()) {
                    File d = new File("/mnt/sdcard/transapp/" + packageName + "/");
                    if (d.canWrite()) {
                        FileOutputStream f = new FileOutputStream(d.getAbsolutePath() + "/icon");
                        f.write(EntityUtils.toByteArray(rp.getEntity()));
                        f.close();
                    }
                    return true;
                }
            } catch (Exception e) {
            }
            return false;
        }

        public String downloadAPK(HttpGet get, String name, Context context) {
            HttpResponse rp = null;
            try {
                rp = super.execute(get);
                File apkfile = context.getFileStreamPath(name + ".apk");
                if (!apkfile.exists()) {
                    apkfile.createNewFile();
                }
                FileOutputStream f = new FileOutputStream(apkfile);
                // InputStream in = rp.getEntity().getContent();

                // byte[] buffer = new byte[1024];
                // int result = -1;
                // while ((result = in.read(buffer)) > 0) {
                // f.write(buffer);
                // }
                rp.getEntity().writeTo(f);
                //f.write(EntityUtils.toByteArray(rp.getEntity()));
                f.close();
                Runtime.getRuntime().exec("chmod 666 " + apkfile.getAbsolutePath());
                return apkfile.getAbsolutePath();
            } catch (Exception e) {
            }
            return null;
        }
    }
}
