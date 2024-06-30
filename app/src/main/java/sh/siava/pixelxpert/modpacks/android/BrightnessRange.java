package sh.siava.pixelxpert.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class BrightnessRange extends XposedModPack {
	private final List<String> listenPacks = new ArrayList<>();

	private static float minimumBrightnessLevel = 0f;
	private static float maximumBrightnessLevel = 1f;

	public BrightnessRange(Context context) {
		super(context);
		listenPacks.add(Constants.SYSTEM_FRAMEWORK_PACKAGE);
		listenPacks.add(Constants.SYSTEM_UI_PACKAGE);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		try {
			List<Float> BrightnessRange = Xprefs.getSliderValues("BrightnessRange", 100f);
			if (BrightnessRange.size() == 2) {
				minimumBrightnessLevel = BrightnessRange.get(0) / 100;
				maximumBrightnessLevel = BrightnessRange.get(1) / 100;
			}
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPacks.contains(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try { //framework
			Class<?> DisplayPowerControllerClass = findClass("com.android.server.display.DisplayPowerController", lpParam.classLoader);

			hookAllMethods(DisplayPowerControllerClass, "clampScreenBrightness", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (minimumBrightnessLevel == 0f && maximumBrightnessLevel == 1f) return;

					param.args[0] = Math.min(
							Math.max(
									(float) param.args[0],
									minimumBrightnessLevel),
							maximumBrightnessLevel);
				}
			});
		} catch (Throwable ignored) {
		}

		try { //SystemUI
			Class<?> BrightnessInfoClass = findClass("android.hardware.display.BrightnessInfo", lpParam.classLoader);

			hookAllConstructors(BrightnessInfoClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (minimumBrightnessLevel > 0f) {
						setObjectField(param.thisObject, "brightnessMinimum", minimumBrightnessLevel);
					}
					if (maximumBrightnessLevel < 1f) {
						setObjectField(param.thisObject, "brightnessMaximum", maximumBrightnessLevel);
					}
				}
			});
		} catch (Throwable ignored) {
		}
	}
}