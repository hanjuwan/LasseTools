package com.lasselindh.tools;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lasselindh.lassetools.BuildConfig;
import com.lasselindh.lassetools.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class LasseTools {
    private static LasseTools instance;
    private WebView mWebView;
    private Context mContext;
    private HashMap<String, Object> activityStack;
    private static Toast mCToast;
    private String file_name;
    private boolean isDevOnly = true;

    public LasseTools() {
        activityStack = new HashMap<>();
    }

    public static LasseTools getInstance() {
        if (instance == null) {
            instance = new LasseTools();
        }

        return instance;
    }

    public void init(Context context, boolean isDev) {
        this.isDevOnly = isDev;
        mContext = context;
        if(isDevOnly) {
            if(Build.VERSION.SDK_INT >= 19) {
                mWebView = new WebView(mContext);
                mWebView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                mWebView.setPadding(0, 0, 0, 0);
                mWebView.setInitialScale(1);
                mWebView.addJavascriptInterface(new CustomJavascriptInterface(mContext), this.getClass().getSimpleName());
                WebView.setWebContentsDebuggingEnabled(true);
                mWebView.setHorizontalScrollBarEnabled(false);
                mWebView.setVerticalScrollBarEnabled(false);
                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.getSettings().setDatabaseEnabled(true);
                mWebView.getSettings().setLoadWithOverviewMode(true);
                mWebView.getSettings().setUseWideViewPort(true);;
                mWebView.getSettings().setDomStorageEnabled(true);
                mWebView.setWebViewClient(new WebViewClient());
                mWebView.setWebChromeClient(new WebChromeClient());
//            mWebView.loadUrl("file:///android_asset/index.html");

                String content =
//                    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+
                        "<html><head><title>LasseTools(" + BuildConfig.VERSION_NAME + ")</title>"+
                                "</head></html>";
                mWebView.loadDataWithBaseURL("", content, "text/html", "UTF-8", "http://lasselindh.tistory.com/");
            }
        }
    }

    public void setScreen(Object name) {
        if(isDevOnly)
            activityStack.put(name.getClass().getSimpleName(), name);
    }

    private static Toast toast;
    private static String mMsg = "";
    private static long mPrevTime = 0;
    public static void DToast(Context context, String msg) {
        String preString = "";
        if(toast != null && toast.getView().getVisibility() == View.VISIBLE) {
            toast.cancel();
            if(mPrevTime != 0 && System.currentTimeMillis() - mPrevTime > 2000) {
                preString = "";
            } else {
                preString = mMsg;
            }
        }

        if(!"".equals(preString)) {
            msg = preString + "\n" + msg;
        }

        mMsg = msg;
        toast = new Toast(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.lasse_toast, null);
        (view.findViewById(R.id.ctoast_layout)).setBackgroundColor(PreferenceManager.getDefaultSharedPreferences(context).getInt("color", Color.parseColor("#656D78")));
        TextView tv = view.findViewById(R.id.tv_toast);
        tv.setText(msg);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 400);
        toast.setDuration(Toast.LENGTH_LONG);


        toast.setGravity(Gravity.CENTER_VERTICAL, 0, -220);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();

        mPrevTime = System.currentTimeMillis();
    }

    public  boolean isDevModeEnabled() {
        if(Build.VERSION.SDK_INT == 16) {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED , 0) != 0;
        } else if (Build.VERSION.SDK_INT >= 17) {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED , 0) != 0;
        } else return false;
    }

    public boolean isUsbDebuggingEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0) != 0;
    }

    @TargetApi(19)
    private void saveLog() {
        String[] permissions = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        LassePermission.getPermission(mContext, permissions, new LassePermission.PermissionListener() {
            @Override
            public void onRequestResult(boolean allGranted, ArrayList<String> deniedPermissions) {
                if (allGranted) {
                    String ex_storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                    // Get Absolute Path in External Sdcard
                    file_name = "/" + System.currentTimeMillis() + ".log";

                    File logFile = new File(ex_storage + file_name); // log file name
                    int sizePerFile = 1024; // size in kilobytes
                    int rotationCount = 10; // file rotation count

                    String[] args = new String[]{"logcat",
                            "-v", "time",
                            "-f", logFile.getAbsolutePath(),
                            "-r", Integer.toString(sizePerFile),
                            "-n", Integer.toString(rotationCount),
                            "*:V"};

                    try {
                        Runtime.getRuntime().exec(args);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mWebView.evaluateJavascript("console.log('logcat save completed, plz command LasseTools.showLogcat()')", null);
                }
            }
        });
    }

    public void captureView(View View) {
        View.buildDrawingCache();
        Bitmap captureView = View.getDrawingCache();
        FileOutputStream fos;

        String strFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        File folder = new File(strFolderPath);
        if(!folder.exists()) {
            folder.mkdirs();
        }

        String strFilePath = strFolderPath + "/" + System.currentTimeMillis() + ".png";
        File fileCacheItem = new File(strFilePath);

        try {
            fos = new FileOutputStream(fileCacheItem);
            captureView.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    String bitmapString;
    public void captureActivity(final Activity context, final boolean isSave) {
        String[] permissions = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        LassePermission.getPermission(context, permissions, new LassePermission.PermissionListener() {
            @Override
            public void onRequestResult(boolean allGranted, ArrayList<String> deniedPermissions) {
                if(!allGranted) return;
                if(context == null) return;
                View root = context.getWindow().getDecorView().getRootView();
                root.setDrawingCacheEnabled(true);
                root.buildDrawingCache();
                Bitmap screenshot = root.getDrawingCache();

                // get view coordinates
                int[] location = new int[2];
                root.getLocationInWindow(location);

                Bitmap bmp = Bitmap.createBitmap(screenshot, location[0], location[1], root.getWidth(), root.getHeight(), null, false);

                if(isSave) {
                    String strFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                    File folder = new File(strFolderPath);
                    if(!folder.exists()) {
                        folder.mkdirs();
                    }

                    String strFilePath = strFolderPath + "/" + System.currentTimeMillis() + ".png";
                    File fileCacheItem = new File(strFilePath);
                    OutputStream out = null;
                    try {
                        fileCacheItem.createNewFile();
                        out = new FileOutputStream(fileCacheItem);
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                        root.setDrawingCacheEnabled(false);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            out.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    root.setDrawingCacheEnabled(false);
                    bitmapString = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
                    String content =
                            "<html><head><title>LasseTools</title>"+
                                    "</head><body><img style=\"width:100%;\" src=\"data:image/png;base64," +bitmapString +"\"/></body></html>";
                    mWebView.loadDataWithBaseURL("", content, "text/html", "UTF-8", "http://lasselindh.tistory.com/");
                    mWebView.setVisibility(View.VISIBLE);
                }

                bitmapString = "";
            }
        });
    }

    @TargetApi(19)
    public class CustomJavascriptInterface {
        private Context mContext;
        public CustomJavascriptInterface(Context context) {
            mContext = context;
        }

//        @JavascriptInterface
//        public final void CToast(final String message) {
//            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
//        }

        @JavascriptInterface
        public final void saveScreen () {
            if(!activityStack.isEmpty()) {
                ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> info = am.getRunningTasks(1);
                ComponentName topActivity = info.get(0).topActivity;
                String topActivityName = topActivity.getShortClassName();
                int pos = topActivityName.lastIndexOf( "." );
                topActivityName = topActivityName.substring( pos + 1 );
                final Object mObject = activityStack.get(topActivityName);
                if(mObject != null)
                    captureActivity((Activity)mObject, true);
            }
        }

        @JavascriptInterface
        public final void showScreen () {
            if(!activityStack.isEmpty()) {
                ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> info = am.getRunningTasks(1);
                ComponentName topActivity = info.get(0).topActivity;
                String topActivityName = topActivity.getShortClassName();
                int pos = topActivityName.lastIndexOf( "." );
                topActivityName = topActivityName.substring( pos + 1 );
                final Object mObject = activityStack.get(topActivityName);
                if(mObject != null)
                    captureActivity((Activity)mObject, false);
            }
        }

        @JavascriptInterface
        public final void runMethod(String activityName, String methodName) {
            if(!activityStack.isEmpty()) {
                final Object mObject = activityStack.get(activityName);
                final String mOkMethod = methodName;
                if(mObject!=null && mOkMethod != null) {
                    try {
                        Class<?> cls = mObject.getClass();
                        Method method = cls.getMethod(mOkMethod);
                        method.setAccessible(true);
                        method.invoke(mObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @JavascriptInterface
        public final void runMethod(String activityName, String methodName, String param1, String param1Type) {
            if(!activityStack.isEmpty()) {
                final Object mObject = activityStack.get(activityName);
                final String mOkMethod = methodName;
                String paramString = "";
                boolean paramBoolean = false;
                int paramInt = -1;
                if(mObject!=null && mOkMethod != null) {
                    switch (param1Type) {
                        case "String": {
                            paramString = param1;
                            try {
                                Class<?> cls = mObject.getClass();
                                Method method = cls.getDeclaredMethod(mOkMethod, String.class);
                                method.setAccessible(true);
                                method.invoke(mObject, paramString);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } break;
                        case "int": {
                            paramInt = Integer.parseInt(param1);
                            try {
                                Class<?> cls = mObject.getClass();
                                Method method = cls.getDeclaredMethod(mOkMethod, int.class);
                                method.setAccessible(true);
                                method.invoke(mObject, paramInt);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } break;
                        case "boolean": {
                            paramBoolean = Boolean.parseBoolean(param1);
                            try {
                                Class<?> cls = mObject.getClass();
                                Method method = cls.getDeclaredMethod(mOkMethod, boolean.class);
                                method.setAccessible(true);
                                method.invoke(mObject, paramBoolean);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } break;
                        default:
                            break;
                    }
                }
            }
        }


        @JavascriptInterface
        public final void saveLogcat() {
            saveLog();
        }

        @JavascriptInterface
        public final void showLogcat() {
            String[] permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            LassePermission.getPermission(mContext, permissions, new LassePermission.PermissionListener() {
                @Override
                public void onRequestResult(boolean allGranted, ArrayList<String> deniedPermissions) {
                    if(!allGranted) return;

                    if(file_name == null || file_name.equals("")) {
                        mWebView.evaluateJavascript("console.log('run after saveLogcat()')", null);
                    } else {
                        String everything = "";
                        String ex_storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                        try {
                            BufferedReader br = new BufferedReader(new FileReader(ex_storage + file_name));
                            try {
                                StringBuilder sb = new StringBuilder();
                                String line = br.readLine();

                                while (line != null) {
                                    sb.append(line);
                                    sb.append(System.lineSeparator());
                                    line = br.readLine();

                                    mWebView.evaluateJavascript("console.log('"+ line +"')", null);
                                }
                            } finally {
                                br.close();
                            }
                        } catch (Exception e) {

                        }
                    }

                }
            });
        }
    }
}
