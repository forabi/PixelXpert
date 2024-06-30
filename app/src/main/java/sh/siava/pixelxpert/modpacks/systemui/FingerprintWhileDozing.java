package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class FingerprintWhileDozing extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static boolean fingerprintWhileDozing = true;

	public FingerprintWhileDozing(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		fingerprintWhileDozing = Xprefs.getBoolean("fingerprintWhileDozing", true);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		Class<?> KeyguardUpdateMonitorClass = findClass("com.android.keyguard.KeyguardUpdateMonitor", lpParam.classLoader);

		hookAllMethods(KeyguardUpdateMonitorClass,
				"shouldListenForFingerprint", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (fingerprintWhileDozing) return;

						if(!getBooleanField(param.thisObject, "mDeviceInteractive"))
						{
							param.setResult(false);
						}
					}
				});
	}
}