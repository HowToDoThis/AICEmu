package moe.tqlwsl.aicemu;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class xp implements IXposedHookLoadPackage {
    private final String TAG = "AICEmu-Xposed";
    ClassLoader mclassloader = null;
    Context mcontext = null;

    // from https://github.com/OLIET2357/HCEFUnlocker
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log("In " + lpparam.packageName);

        if (lpparam.packageName.equals("com.android.nfc")) {

//            XposedHelpers.findAndHookMethod("android.nfc.cardemulation.NfcFCardEmulation", lpparam.classLoader,
//                    "isValidSystemCode", String.class, new XC_MethodReplacement() {
//                @Override
//                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
//                    String systemCode = (String) param.args[0];
//                    if (systemCode == null) {
//                        return false;
//                    }
//
//                    if (systemCode.length() != 4) {
//                        Log.e(TAG, "System Code " + systemCode + " is not a valid System Code.");
//                        return false;
//                    }
//                    // check if the value is between "4000" and "4FFF" (excluding "4*FF")
//                    if (!systemCode.startsWith("4") || systemCode.toUpperCase().endsWith("FF")) {
//                        // Log.e(TAG, "System Code " + systemCode + " is not a valid System Code.");
//                        // return false;
//                    }
//                    try {
//                        Integer.parseInt(systemCode, 16);
//                    } catch (NumberFormatException e) {
//                        Log.e(TAG, "System Code " + systemCode + " is not a valid System Code.");
//                        return false;
//                    }
//                    return true;
//                }
//            });

            XposedHelpers.findAndHookMethod("android.nfc.cardemulation.NfcFCardEmulation", lpparam.classLoader,
                    "isValidNfcid2", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    String nfcid2 = (String) param.args[0];
                    if (nfcid2 == null) {
                        return false;
                    }
                    if (nfcid2.length() != 16) {
                        Log.e(TAG, "NFCID2 " + nfcid2 + " is not a valid NFCID2.");
                        return false;
                    }
                    // check if the the value starts with "02FE"
                    if (!nfcid2.toUpperCase().startsWith("02FE")) {
                        // Log.e(TAG, "NFCID2 " + nfcid2 + " is not a valid NFCID2.");
                        // return false;
                    }
                    try {
                        Long.parseLong(nfcid2, 16);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "NFCID2 " + nfcid2 + " is not a valid NFCID2.");
                        return false;
                    }
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
                    super.afterHookedMethod(param);
                    XposedBridge.log("Inside android.nfc.cardemulation.NfcFCardEmulation#isValidSystemCode");
                    XposedBridge.log("Got classloader");
                    String path = getSoPath();
                    XposedBridge.log("So path = " + path);
                    int version = android.os.Build.VERSION.SDK_INT;
                    try {
                        if (!path.equals("")) {
                            XposedBridge.log("Start injecting libpmm.so");
                            if (version >= 28) {
                                XposedHelpers.callMethod(Runtime.getRuntime(), "nativeLoad", path, mclassloader);
                            } else {
                                XposedHelpers.callMethod(Runtime.getRuntime(), "doLoad", path, mclassloader);
                            }
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
