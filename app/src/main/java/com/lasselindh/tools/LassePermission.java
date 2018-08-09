package com.lasselindh.tools;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import java.util.ArrayList;

public class LassePermission extends AppCompatActivity {
    public static final int DENIED = 0;
    public static final int GRANTED = 1;
    public static final int ALWAYS_DENIED = -1;

    private static final int REQUEST_OVERLAY = 1000;
    private static final int REQUEST_USAGE_STATS = 2000;
    private static final int REQUEST_PERMISSION = 3000;

    private String[] mPermissions;
    private static PermissionListener mPermissionListener;
    public interface PermissionListener {
        void onRequestResult(boolean allOk, ArrayList<String> denided);
    }

    private static OverlayListener mOverlayListener;
    public interface OverlayListener {
        void onCheckCompleted(boolean result);
    }

    private static UsageStatsListener mUsageStatsListener;
    public interface UsageStatsListener {
        void onCheckCompleted(boolean result);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        switch (getIntent().getIntExtra("request", 0)) {
            case REQUEST_OVERLAY: {
                if (canDrawOverlays(this)) {
                    mOverlayListener.onCheckCompleted(true);
                    finish();
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY);
                }
                break;
            }

            case REQUEST_PERMISSION: {
                CheckPermissionApp(getIntent().getStringArrayExtra("permission"));
                break;
            }

            case REQUEST_USAGE_STATS: {
                startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), REQUEST_USAGE_STATS);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_OVERLAY: {
                if(canDrawOverlays(this)) {
                    mOverlayListener.onCheckCompleted(true);
                    finish();
                } else {
                    mOverlayListener.onCheckCompleted(false);
                    finish();
                }
                break;
            }

            case REQUEST_USAGE_STATS: {
                if(checkUsageStatsPermission(this)) {
                    mUsageStatsListener.onCheckCompleted(true);
                } else {
                    mUsageStatsListener.onCheckCompleted(false);
                }
            }
        }
    }

    public boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        else if (Build.VERSION.SDK_INT >= 26) {
            return Settings.canDrawOverlays(context);
        } else {
            if (Settings.canDrawOverlays(context))
                return true;
            return false;
        }
    }


    private void CheckPermissionApp(String[] pers) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPermissions = pers;
            int permissionTotal = 0;
            for(int i=0 ; i<mPermissions.length ; i++) {
                if(checkSelfPermission(mPermissions[i]) == PackageManager.PERMISSION_DENIED) {
                    permissionTotal++;
                    if((shouldShowRequestPermissionRationale(mPermissions[i]))) {
                    }
                }
            }

            if(permissionTotal == 0) {
                mPermissionListener.onRequestResult(true, null);
                finish();
            } else {
                requestPermissions(mPermissions, 0);
            }
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != 0) {
            return;
        }

        boolean isDenied = false;
        ArrayList<String> deniedPermission = new ArrayList<>();
        for(int i=0 ; i<grantResults.length ; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                isDenied = true;
                deniedPermission.add(mPermissions[i]);
            }
        }

        if(isDenied) {
            for(int i=0 ; i<mPermissions.length ; i++) {
                if((shouldShowRequestPermissionRationale(mPermissions[i]))) {
//                    mPermissionListener.onCheckCompleted(DENIED);
                } else {
//                    mPermissionListener.onCheckCompleted(ALWAYS_DENIED);
                }
            }

            mPermissionListener.onRequestResult(false, deniedPermission);
            finish();
        } else {
            mPermissionListener.onRequestResult(true, null);
            finish();
        }
    }

    public static void getPermission(Context context, String[] permissions, PermissionListener listener) {
        mPermissionListener = listener;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPermissionListener.onRequestResult(true, null);
        } else {
            Intent intent = new Intent(context, LassePermission.class);
            intent.putExtra("request", REQUEST_PERMISSION);
            intent.putExtra("permission", permissions);
            context.startActivity(intent);
        }
    }


    public static void getOverlay(Context context, OverlayListener listener) {
        mOverlayListener = listener;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mOverlayListener.onCheckCompleted(true);
        } else {
            if (Settings.canDrawOverlays(context)) {
                mOverlayListener.onCheckCompleted(true);
            } else {
                Intent intent = new Intent(context, LassePermission.class);
                intent.putExtra("request", REQUEST_OVERLAY);
                context.startActivity(intent);
            }
        }
    }

    public static void getUsageStats(Context context, UsageStatsListener listener) {
        mUsageStatsListener = listener;

        if(checkUsageStatsPermission(context)) {
            mUsageStatsListener.onCheckCompleted(true);
        } else {
            Intent intent = new Intent(context, LassePermission.class);
            intent.putExtra("request", REQUEST_USAGE_STATS);
            context.startActivity(intent);
        }
    }

    private static boolean checkUsageStatsPermission(Context context) {
        if(Build.VERSION.SDK_INT >= 19) {
            try {
                PackageManager packageManager = context.getPackageManager();
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
                AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
                return (mode == AppOpsManager.MODE_ALLOWED);

            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return false;
    }

}