package moe.tqlwsl.aicemu;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class xp implements IXposedHookLoadPackage {
    private final String TAG = "AICEmu";

    // from https://github.com/OLIET2357/HCEFUnlocker
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("In " + lpparam.packageName);
        if (lpparam.packageName.equals("com.android.nfc")) {

            XposedHelpers.findAndHookMethod("android.nfc.cardemulation.NfcFCardEmulation", lpparam.classLoader,
                    "isValidSystemCode", String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    String systemCode = (String) param.args[0];
                    if (systemCode == null) {
                        return false;
                    }

                    if (systemCode.length() != 4) {
                        Log.e(TAG, "System Code " + systemCode + " is not a valid System Code.");
                        return false;
                    }
                    // check if the value is between "4000" and "4FFF" (excluding "4*FF")
                    if (!systemCode.startsWith("4") || systemCode.toUpperCase().endsWith("FF")) {
                        // Log.e(TAG, "System Code " + systemCode + " is not a valid System Code.");
                        // return false;
                    }
                    try {
                        Integer.parseInt(systemCode, 16);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "System Code " + systemCode + " is not a valid System Code.");
                        return false;
                    }
                    return true;
                }
            });

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

            XposedBridge.log("Hook succeeded!!!");
        }
    }
}
