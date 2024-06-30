package sh.siava.pixelxpert.modpacks.launcher;

import static android.view.View.GONE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.ResourceManager;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class ClearAllButtonMod extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;

	private Object recentView;
	private static boolean RecentClearAllReposition = false;
	private ImageView clearAllIcon;
	private FrameLayout clearAllButton;

	public ClearAllButtonMod(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if(Key.length > 0 && Key[0].equals("RecentClearAllReposition"))
		{
			SystemUtils.killSelf();
		}

		RecentClearAllReposition = Xprefs.getBoolean("RecentClearAllReposition", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		Class<?> OverviewActionsViewClass = findClass("com.android.quickstep.views.OverviewActionsView", lpParam.classLoader);
		Class<?> RecentsViewClass = findClass("com.android.quickstep.views.RecentsView", lpParam.classLoader);
		Method dismissAllTasksMethod = findMethodBestMatch(RecentsViewClass, "dismissAllTasks", View.class);

		hookAllConstructors(RecentsViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				recentView = param.thisObject;
			}
		});

		hookAllMethods(RecentsViewClass, "setColorTint", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!RecentClearAllReposition) return;

				clearAllIcon.getDrawable().setTintList(getThemedColor(mContext));
			}
		});

		hookAllMethods(RecentsViewClass, "setVisibility", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(clearAllButton == null) return;

				clearAllButton.setVisibility((Integer) param.args[0]);
			}
		});

		hookAllMethods(OverviewActionsViewClass, "onFinishInflate", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(!RecentClearAllReposition) return;

				clearAllButton = new FrameLayout(mContext);

				clearAllIcon = new ImageView(mContext);
				clearAllIcon.setImageDrawable(ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_clear_all, mContext.getTheme()));
				clearAllIcon.getDrawable().setTintList(getThemedColor(mContext));
				clearAllButton.addView(clearAllIcon);

				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

				params.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, Resources.getSystem().getDisplayMetrics());

				params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;

				clearAllButton.setLayoutParams(params);
				clearAllButton.setOnClickListener(v -> {
					if(recentView != null)
					{
						try {
							dismissAllTasksMethod.invoke(recentView, v);
						} catch (Throwable ignored) {}
					}
				});

//				clearAllButton.setPadding(margins/2,0,margins/2,0);
				FrameLayout parent = (FrameLayout)param.thisObject;
				parent.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT; //resize to whole screen
				parent.addView(clearAllButton);
				clearAllButton.setVisibility(GONE);

			}
		});
	}

	public static ColorStateList getThemedColor(Context context) {
		return getSystemAttrColor(context, SystemUtils.isDarkMode()
				? android.R.attr.textColorPrimaryInverse
				: android.R.attr.textColorPrimary);
	}

	public static ColorStateList getSystemAttrColor(Context context, int attr) {
		TypedArray a = context.obtainStyledAttributes(new int[] { attr });
		ColorStateList color = a.getColorStateList(a.getIndex(0));
		a.recycle();
		return color;
	}
}