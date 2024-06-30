package sh.siava.pixelxpert.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class ScreenRotation extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static final int USER_ROTATION_LOCKED = 1;

	private static boolean allScreenRotations = false;

	public ScreenRotation(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		allScreenRotations = Xprefs.getBoolean("allScreenRotations", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			Class<?> DisplayRotationClass = findClass("com.android.server.wm.DisplayRotation", lpParam.classLoader);

			hookAllMethods(DisplayRotationClass, "rotationForOrientation", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if (!allScreenRotations) return;

						final int lastRotation = (int) param.args[1];

						if (getIntField(param.thisObject, "mUserRotationMode") == USER_ROTATION_LOCKED) {
							param.setResult(lastRotation);
							return;
						}

						Object mOrientationListener = getObjectField(param.thisObject, "mOrientationListener");
						int sensorRotation = mOrientationListener != null
								? (int) callMethod(mOrientationListener, "getProposedRotation") // may be -1
								: -1;
						if (sensorRotation < 0) {
							sensorRotation = lastRotation;
						}
						param.setResult(sensorRotation);
					} catch (Throwable ignored) {
					}
				}
			});
		} catch (Exception ignored) {
		}
	}
}
