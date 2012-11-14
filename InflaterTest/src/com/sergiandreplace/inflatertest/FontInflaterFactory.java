package com.sergiandreplace.inflatertest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;

/**
 * This is a inflater factory that will try to change the font after creation on all those widgets that:
 * 
 * a) are located on package android.widget
 * 
 * -OR-
 * 
 * b) are located on package android.webkit
 * 
 * -OR-
 * 
 * c) the tag is the whole object fully qualified name
 * 
 * -OR-
 * 
 * d) are listed in the comma-separated list of values in the string resource fontInflaterPackages
 * 
 * The font to use will taken from assets folder, on the path specified by string resource fontInflaterTypeface
 * 
 * Suggestion is to override Activity class and hide the use of this factory on the onCreateMethod with a call like:
 * 
 * getLayoutInflater().setFactory(new FontInflaterFactory());
 * 
 * @see http://www.gitshah.com/2011/06/how-to-change-text-color-of-android.
 * @see https://github.com/android/platform_frameworks_policies_base/blob/master/phone/com/android/internal/policy/impl/PhoneLayoutInflater.java
 * 
 * @author Sergi Martínez (@sergiandreplace)
 * 
 */
public class FontInflaterFactory implements LayoutInflater.Factory {
	/**
	 * The name of the string resource that should contain the comma-separated list of packages where to look for the tags to instantiate
	 */
	public final static String SETTING_PACKAGES = "fontInflaterPackages";
	/**
	 * The name of the string resource that should contain path (starting from assets folder) to the font file
	 */
	public final static String SETTING_TYPEFACE = "fontInflaterTypeface";	
	

	/**
	 * The standard signature that will be used for the constructor of the views when creating them via reflection
	 */
	private static final Class<?>[] constructorSignature = new Class[] {
			Context.class, AttributeSet.class };
	
	/**
	 * List of default packages to look for classes with tag names. The "" is included for the FQN tags
	 */
	private static final String[] defaultPackages = { "android.widget.",
			"android.webkit.","" };
	
	/**
	 * Final list of packages (default + custom)
	 */
	private static String[] packages;
	
	/**
	 * The typeface once loaded
	 */
	private static Typeface typeface;
	
	
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		loadSettings(context);
		for (String packageName : packages) {
			try {

				final View v = createView(context, name, packageName, attrs);
				if (v != null) {
					new Handler().post(new Runnable() {
						public void run() {
							setViewConfiguration(v);
						}
					});
					return v;
				}
			} catch (ClassNotFoundException e) {
				// In this case we want to let the base class take a crack
				// at it.
			}
		}
		return null;
	}

	/**
	 * Load the list of settings. Currently packages and font;
	 * @param context Context to use for loading resource
	 */
	private void loadSettings(Context context) {
		loadPackages(context);
		loadFont(context);
	}

	/**
	 * Loads the font stablished on the settings and stores it in the typeface field
	 * @param context
	 */
	private void loadFont(Context context) {
		if (typeface == null) {
			final String typefaceName = loadSetting(context, SETTING_TYPEFACE);
			typeface = Typeface.createFromAsset(context.getAssets(),
					typefaceName);
		}
	}

	/**
	 * Loads the comma separated list of packages, splits it, and crated a list based on custom + defualt packages
	 * @param context
	 */
	private void loadPackages(Context context) {
		if (packages == null) {
			final String packagesList = loadSetting(context, SETTING_PACKAGES);
			final String[] customPackages = packagesList.split(",");
			packages = new String[defaultPackages.length
					+ customPackages.length];
			System.arraycopy(defaultPackages, 0, packages, 0,
					defaultPackages.length);
			System.arraycopy(customPackages, 0, packages,
					defaultPackages.length, customPackages.length);
		}
	}

	/**
	 * Generic loader of a string resource from the string name
	 * @param context
	 * @param settingName the name of the string to load
	 * @return The string resource loaded, or null if not exists.
	 */
	private String loadSetting(Context context, String settingName) {
		int resourceId = context.getResources().getIdentifier(settingName,
				"string", context.getApplicationContext().getPackageName());
		if (resourceId == 0) {
			return null;
		}
		final String value = context.getString(resourceId);
		return value;
	}

	/**
	 * This method configures the recently created view. In this case, it stablishes the font, but the limit is our imagination
	 * @param v the view to personalize
	 */
	private void setViewConfiguration(final View v) {
		try {
			final Method setTextColor = v.getClass().getMethod("setTypeface",
					Typeface.class);
			setTextColor.invoke(v, typeface);
		} catch (Exception e) {
			return;
		}
	}

	/**
	 * This is a small copy of the one createView from LayoutInflater. Simplified and with a lot of room
	 * for improvement.
	 * 
	 * It probably will make StubViews unusable (deleted the line that stores the LayoutInflater for future inflation). Probably will need to catch them
	 * before to make them out of the game. Testing pending.
	 * 
	 * 
	 * @param context needed context to instantiate the view
	 * @param name The name of the view class
	 * @param prefix The package of the class (if any)
	 * @param attrs The set of attributes extracted from the xml
	 * @return a brand new view created from the specified in the parameters
	 * @throws ClassNotFoundException <a href="http://icanhascheezburger.files.wordpress.com/2009/11/funny-pictures-cat-does-not-believe.jpg">explanation</a>
	 * @throws InflateException <a href="http://patrick.ripp.eu/images/lolcat_dude.png">explanation</a>
	 */
	public final View createView(Context context, String name, String prefix,
			AttributeSet attrs) throws ClassNotFoundException, InflateException {
		Constructor<? extends View> constructor;
		Class<? extends View> clazz = null;
		try {
			clazz = context.getClassLoader()
					.loadClass(prefix != null ? (prefix + name) : name)
					.asSubclass(View.class);
			constructor = clazz.getConstructor(constructorSignature);
			Object[] args = new Object[2];
			args[0] = context;
			args[1] = attrs;

			final View view = constructor.newInstance(args);

			return view;

		} catch (NoSuchMethodException e) {
			InflateException ie = new InflateException(
					attrs.getPositionDescription() + ": Error inflating class "
							+ (prefix != null ? (prefix + name) : name));
			ie.initCause(e);
			throw ie;

		} catch (ClassCastException e) {
			// If loaded class is not a View subclass
			InflateException ie = new InflateException(
					attrs.getPositionDescription() + ": Class is not a View "
							+ (prefix != null ? (prefix + name) : name));
			ie.initCause(e);
			throw ie;
		} catch (ClassNotFoundException e) {
			// If loadClass fails, we should propagate the exception.
			throw e;
		} catch (Exception e) {
			InflateException ie = new InflateException(
					attrs.getPositionDescription() + ": Error inflating class "
							+ (clazz == null ? "<unknown>" : clazz.getName()));
			ie.initCause(e);
			throw ie;
		}
	}
}
