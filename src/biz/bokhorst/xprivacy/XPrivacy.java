package biz.bokhorst.xprivacy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XC_MethodHook;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class XPrivacy implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		// Log load
		XUtil.log(null, XUtil.LOG_INFO, String.format("load package=%s", lpparam.packageName));

		// Check version
		if (Build.VERSION.SDK_INT != 16)
			XUtil.log(null, XUtil.LOG_WARNING, String.format("Build version %d", Build.VERSION.SDK_INT));

		// Load providers.contacts
		if (lpparam.packageName.equals("com.android.providers.contacts"))
			hook(new XContentProvider("contacts"), lpparam, "com.android.providers.contacts.ContactsProvider2",
					"query", true);

		// Load providers.calendar
		else if (lpparam.packageName.equals("com.android.providers.calendar"))
			hook(new XContentProvider("calendar"), lpparam, "com.android.providers.calendar.CalendarProvider2",
					"query", true);

		// Load settings
		else if (lpparam.packageName.equals("com.android.settings"))
			hook(new XInstalledAppDetails(), lpparam, "com.android.settings.applications.InstalledAppDetails",
					"refreshUi", false);
	}

	private void hook(final XHook hook, final LoadPackageParam lpparam, String className, String methodName,
			boolean visible) {
		try {
			// Create hook
			XC_MethodHook methodHook = new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						XUtil.log(hook, XUtil.LOG_DEBUG, "before");
						hook.before(param);
					} catch (Exception ex) {
						XUtil.bug(null, ex);
						throw ex;
					}
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					try {
						XUtil.log(hook, XUtil.LOG_DEBUG, "after");
						hook.after(param);
					} catch (Exception ex) {
						XUtil.bug(null, ex);
						throw ex;
					}
				}
			};

			// Add hook
			Set<XC_MethodHook.Unhook> hookSet = new HashSet<XC_MethodHook.Unhook>();
			Class<?> hookClass = findClass(className, lpparam.classLoader);
			if (methodName == null) {
				for (Constructor<?> constructor : hookClass.getDeclaredConstructors())
					if (Modifier.isPublic(constructor.getModifiers()) ? visible : !visible)
						hookSet.add(XposedBridge.hookMethod(constructor, methodHook));
			} else {
				for (Method method : hookClass.getDeclaredMethods())
					if (method.getName().equals(methodName)
							&& (Modifier.isPublic(method.getModifiers()) ? visible : !visible))
						hookSet.add(XposedBridge.hookMethod(method, methodHook));
			}

			// Log
			for (XC_MethodHook.Unhook unhook : hookSet) {
				XUtil.log(hook, XUtil.LOG_INFO, String.format("hooked %s in %s (%d)", unhook.getHookedMethod()
						.getName(), lpparam.packageName, hookSet.size()));
				break;
			}
		} catch (ClassNotFoundError ignored) {
			XUtil.log(hook, XUtil.LOG_ERROR, "class not found");
		} catch (NoSuchMethodError ignored) {
			XUtil.log(hook, XUtil.LOG_ERROR, "method not found");
		} catch (Exception ex) {
			XUtil.bug(null, ex);
		}
	}
}
