package moe.tqlwsl.aicemu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class xp implements IXposedHookLoadPackage {
    private final String TAG = "AICEmu-Xposed";
    ClassLoader mclassloader = null;
    Context mcontext = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log("In " + lpparam.packageName);
        if (lpparam.packageName.equals("com.android.nfc")) {

            XposedHelpers.findAndHookMethod("android.nfc.cardemulation.NfcFCardEmulation", lpparam.classLoader,
                    "isValidNfcid2", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;
                }
            });


            XposedHelpers.findAndHookMethod("com.android.nfc.NfcApplication",
                lpparam.classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    XposedBridge.log("Inside com.android.nfc.NfcApplication#onCreate");
                    super.beforeHookedMethod(param);
                    Application application = (Application) param.thisObject;
                    mcontext = application.getApplicationContext();
                    XposedBridge.log("Got context");
                }
            });


            XposedHelpers.findAndHookMethod("android.nfc.cardemulation.NfcFCardEmulation",
                lpparam.classLoader, "isValidSystemCode", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    XposedBridge.log("Inside android.nfc.cardemulation.NfcFCardEmulation#isValidSystemCode");

                    mclassloader = mcontext.getClassLoader();
                    XposedBridge.log("Got classloader");
                    String path = getSoPath();
                    XposedBridge.log("So path = " + path);
                    try {
                        Boolean needLoadPmmtool = false;
                        XSharedPreferences pref = getPref();
                        if (pref != null) {
                            needLoadPmmtool = pref.getBoolean("loadPmmtool", false);
                        } else {
                            XposedBridge.log("Cannot load pref for AICEmu properly");
                        }
                        XposedBridge.log("loadPmmtool: " + needLoadPmmtool.toString());
                        if (needLoadPmmtool && !path.equals("")) {
                            XposedBridge.log("Start injecting libpmm.so");
                            XposedHelpers.callMethod(Runtime.getRuntime(), "nativeLoad", path, mclassloader);
                            XposedBridge.log("Injected libpmm.so");
                        }
                    } catch (Exception e) {
                        XposedBridge.log(e);
                        e.printStackTrace();
                    }

                    // Unlocker
                    param.setResult(true);
                }
            });

            XposedBridge.log("Hook succeeded!!!");
        }
    }
    private static XSharedPreferences getPref() {
        XSharedPreferences pref = new XSharedPreferences("moe.tqlwsl.aicemu", "AICEmu");
        return pref.getFile().canRead() ? pref : null;
    }
    private String getSoPath() {
        try {
            String text = "";
            PackageManager pm = mcontext.getPackageManager();
            List<PackageInfo> pkgList = pm.getInstalledPackages(0);
            if (pkgList.size() > 0) {
                for (PackageInfo pi: pkgList) {
                    if (pi.applicationInfo.publicSourceDir.contains("moe.tqlwsl.aicemu")) {
                        text = pi.applicationInfo.publicSourceDir.replace("base.apk", "lib/arm64/libpmm.so");
                        return text;
                    }
                }
            }
        } catch (Exception e) {
            XposedBridge.log(e);
            e.printStackTrace();
        }
        return "";
    }
}
