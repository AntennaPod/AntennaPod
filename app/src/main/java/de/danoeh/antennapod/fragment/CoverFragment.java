//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package android.view;

import android.animation.StateListAnimator;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ClipData;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.Property;
import android.util.SparseArray;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent.DispatcherState;
import android.view.ViewDebug.CapturedViewProperty;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewDebug.FlagToString;
import android.view.ViewDebug.IntToString;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowInsetsAnimation.Bounds;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityEventSource;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.Animation;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.contentcapture.ContentCaptureSession;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class View implements Callback, android.view.KeyEvent.Callback, AccessibilityEventSource {
    public static final int ACCESSIBILITY_LIVE_REGION_ASSERTIVE = 2;
    public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0;
    public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 1;
    public static final Property<View, Float> ALPHA = null;
    public static final int AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 1;
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE = "creditCardExpirationDate";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY = "creditCardExpirationDay";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH = "creditCardExpirationMonth";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR = "creditCardExpirationYear";
    public static final String AUTOFILL_HINT_CREDIT_CARD_NUMBER = "creditCardNumber";
    public static final String AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE = "creditCardSecurityCode";
    public static final String AUTOFILL_HINT_EMAIL_ADDRESS = "emailAddress";
    public static final String AUTOFILL_HINT_NAME = "name";
    public static final String AUTOFILL_HINT_PASSWORD = "password";
    public static final String AUTOFILL_HINT_PHONE = "phone";
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS = "postalAddress";
    public static final String AUTOFILL_HINT_POSTAL_CODE = "postalCode";
    public static final String AUTOFILL_HINT_USERNAME = "username";
    public static final int AUTOFILL_TYPE_DATE = 4;
    public static final int AUTOFILL_TYPE_LIST = 3;
    public static final int AUTOFILL_TYPE_NONE = 0;
    public static final int AUTOFILL_TYPE_TEXT = 1;
    public static final int AUTOFILL_TYPE_TOGGLE = 2;
    public static final int DRAG_FLAG_GLOBAL = 256;
    public static final int DRAG_FLAG_GLOBAL_PERSISTABLE_URI_PERMISSION = 64;
    public static final int DRAG_FLAG_GLOBAL_PREFIX_URI_PERMISSION = 128;
    public static final int DRAG_FLAG_GLOBAL_URI_READ = 1;
    public static final int DRAG_FLAG_GLOBAL_URI_WRITE = 2;
    public static final int DRAG_FLAG_OPAQUE = 512;
    /** @deprecated */
    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_AUTO = 0;
    /** @deprecated */
    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_HIGH = 1048576;
    /** @deprecated */
    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_LOW = 524288;
    protected static final int[] EMPTY_STATE_SET = new int[0];
    protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET = new int[0];
    protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] ENABLED_FOCUSED_STATE_SET = new int[0];
    protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] ENABLED_SELECTED_STATE_SET = new int[0];
    protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] ENABLED_STATE_SET = new int[0];
    protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET = new int[0];
    public static final int FIND_VIEWS_WITH_CONTENT_DESCRIPTION = 2;
    public static final int FIND_VIEWS_WITH_TEXT = 1;
    public static final int FOCUSABLE = 1;
    public static final int FOCUSABLES_ALL = 0;
    public static final int FOCUSABLES_TOUCH_MODE = 1;
    public static final int FOCUSABLE_AUTO = 16;
    protected static final int[] FOCUSED_SELECTED_STATE_SET = new int[0];
    protected static final int[] FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] FOCUSED_STATE_SET = new int[0];
    protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET = new int[0];
    public static final int FOCUS_BACKWARD = 1;
    public static final int FOCUS_DOWN = 130;
    public static final int FOCUS_FORWARD = 2;
    public static final int FOCUS_LEFT = 17;
    public static final int FOCUS_RIGHT = 66;
    public static final int FOCUS_UP = 33;
    public static final int GONE = 8;
    public static final int HAPTIC_FEEDBACK_ENABLED = 268435456;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 2;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 4;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 1;
    public static final int IMPORTANT_FOR_AUTOFILL_AUTO = 0;
    public static final int IMPORTANT_FOR_AUTOFILL_NO = 2;
    public static final int IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS = 8;
    public static final int IMPORTANT_FOR_AUTOFILL_YES = 1;
    public static final int IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS = 4;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_AUTO = 0;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO = 2;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS = 8;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES = 1;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS = 4;
    public static final int INVISIBLE = 4;
    public static final int KEEP_SCREEN_ON = 67108864;
    public static final int LAYER_TYPE_HARDWARE = 2;
    public static final int LAYER_TYPE_NONE = 0;
    public static final int LAYER_TYPE_SOFTWARE = 1;
    public static final int LAYOUT_DIRECTION_INHERIT = 2;
    public static final int LAYOUT_DIRECTION_LOCALE = 3;
    public static final int LAYOUT_DIRECTION_LTR = 0;
    public static final int LAYOUT_DIRECTION_RTL = 1;
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;
    public static final int MEASURED_SIZE_MASK = 16777215;
    public static final int MEASURED_STATE_MASK = -16777216;
    public static final int MEASURED_STATE_TOO_SMALL = 16777216;
    public static final int NOT_FOCUSABLE = 0;
    public static final int NO_ID = -1;
    public static final int OVER_SCROLL_ALWAYS = 0;
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;
    public static final int OVER_SCROLL_NEVER = 2;
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = new int[0];
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET = new int[0];
    protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_ENABLED_STATE_SET = new int[0];
    protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET = new int[0];
    protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_SELECTED_STATE_SET = new int[0];
    protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_STATE_SET = new int[0];
    protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET = new int[0];
    public static final Property<View, Float> ROTATION = null;
    public static final Property<View, Float> ROTATION_X = null;
    public static final Property<View, Float> ROTATION_Y = null;
    public static final Property<View, Float> SCALE_X = null;
    public static final Property<View, Float> SCALE_Y = null;
    public static final int SCREEN_STATE_OFF = 0;
    public static final int SCREEN_STATE_ON = 1;
    public static final int SCROLLBARS_INSIDE_INSET = 16777216;
    public static final int SCROLLBARS_INSIDE_OVERLAY = 0;
    public static final int SCROLLBARS_OUTSIDE_INSET = 50331648;
    public static final int SCROLLBARS_OUTSIDE_OVERLAY = 33554432;
    public static final int SCROLLBAR_POSITION_DEFAULT = 0;
    public static final int SCROLLBAR_POSITION_LEFT = 1;
    public static final int SCROLLBAR_POSITION_RIGHT = 2;
    public static final int SCROLL_AXIS_HORIZONTAL = 1;
    public static final int SCROLL_AXIS_NONE = 0;
    public static final int SCROLL_AXIS_VERTICAL = 2;
    public static final int SCROLL_INDICATOR_BOTTOM = 2;
    public static final int SCROLL_INDICATOR_END = 32;
    public static final int SCROLL_INDICATOR_LEFT = 4;
    public static final int SCROLL_INDICATOR_RIGHT = 8;
    public static final int SCROLL_INDICATOR_START = 16;
    public static final int SCROLL_INDICATOR_TOP = 1;
    protected static final int[] SELECTED_STATE_SET = new int[0];
    protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET = new int[0];
    public static final int SOUND_EFFECTS_ENABLED = 134217728;
    /** @deprecated */
    @Deprecated
    public static final int STATUS_BAR_HIDDEN = 1;
    /** @deprecated */
    @Deprecated
    public static final int STATUS_BAR_VISIBLE = 0;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 4;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 2048;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 4096;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 256;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 16;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 8192;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 1;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;
    /** @deprecated */
    @Deprecated
    public static final int SYSTEM_UI_LAYOUT_FLAGS = 1536;
    public static final int TEXT_ALIGNMENT_CENTER = 4;
    public static final int TEXT_ALIGNMENT_GRAVITY = 1;
    public static final int TEXT_ALIGNMENT_INHERIT = 0;
    public static final int TEXT_ALIGNMENT_TEXT_END = 3;
    public static final int TEXT_ALIGNMENT_TEXT_START = 2;
    public static final int TEXT_ALIGNMENT_VIEW_END = 6;
    public static final int TEXT_ALIGNMENT_VIEW_START = 5;
    public static final int TEXT_DIRECTION_ANY_RTL = 2;
    public static final int TEXT_DIRECTION_FIRST_STRONG = 1;
    public static final int TEXT_DIRECTION_FIRST_STRONG_LTR = 6;
    public static final int TEXT_DIRECTION_FIRST_STRONG_RTL = 7;
    public static final int TEXT_DIRECTION_INHERIT = 0;
    public static final int TEXT_DIRECTION_LOCALE = 5;
    public static final int TEXT_DIRECTION_LTR = 3;
    public static final int TEXT_DIRECTION_RTL = 4;
    public static final Property<View, Float> TRANSLATION_X = null;
    public static final Property<View, Float> TRANSLATION_Y = null;
    public static final Property<View, Float> TRANSLATION_Z = null;
    protected static final String VIEW_LOG_TAG = "View";
    public static final int VISIBLE = 0;
    protected static final int[] WINDOW_FOCUSED_STATE_SET = new int[0];
    public static final Property<View, Float> X = null;
    public static final Property<View, Float> Y = null;
    public static final Property<View, Float> Z = null;

    public View(Context context) {
        throw new RuntimeException("Stub!");
    }

    public View(Context context, @Nullable AttributeSet attrs) {
        throw new RuntimeException("Stub!");
    }

    public View(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        throw new RuntimeException("Stub!");
    }

    public View(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public int[] getAttributeResolutionStack(int attribute) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public Map<Integer, Integer> getAttributeSourceResourceMap() {
        throw new RuntimeException("Stub!");
    }

    public int getExplicitStyle() {
        throw new RuntimeException("Stub!");
    }

    public final boolean isShowingLayoutBounds() {
        throw new RuntimeException("Stub!");
    }

    public final void saveAttributeDataForStyleable(@NonNull Context context, @NonNull int[] styleable, @Nullable AttributeSet attrs, @NonNull TypedArray t, int defStyleAttr, int defStyleRes) {
        throw new RuntimeException("Stub!");
    }

    public String toString() {
        throw new RuntimeException("Stub!");
    }

    public int getVerticalFadingEdgeLength() {
        throw new RuntimeException("Stub!");
    }

    public void setFadingEdgeLength(int length) {
        throw new RuntimeException("Stub!");
    }

    public int getHorizontalFadingEdgeLength() {
        throw new RuntimeException("Stub!");
    }

    public int getVerticalScrollbarWidth() {
        throw new RuntimeException("Stub!");
    }

    protected int getHorizontalScrollbarHeight() {
        throw new RuntimeException("Stub!");
    }

    public void setVerticalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        throw new RuntimeException("Stub!");
    }

    public void setVerticalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        throw new RuntimeException("Stub!");
    }

    public void setHorizontalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        throw new RuntimeException("Stub!");
    }

    public void setHorizontalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public Drawable getVerticalScrollbarThumbDrawable() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public Drawable getVerticalScrollbarTrackDrawable() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public Drawable getHorizontalScrollbarThumbDrawable() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public Drawable getHorizontalScrollbarTrackDrawable() {
        throw new RuntimeException("Stub!");
    }

    public void setVerticalScrollbarPosition(int position) {
        throw new RuntimeException("Stub!");
    }

    public int getVerticalScrollbarPosition() {
        throw new RuntimeException("Stub!");
    }

    public void setScrollIndicators(int indicators) {
        throw new RuntimeException("Stub!");
    }

    public void setScrollIndicators(int indicators, int mask) {
        throw new RuntimeException("Stub!");
    }

    public int getScrollIndicators() {
        throw new RuntimeException("Stub!");
    }

    public void setOnScrollChangeListener(View.OnScrollChangeListener l) {
        throw new RuntimeException("Stub!");
    }

    public void setOnFocusChangeListener(View.OnFocusChangeListener l) {
        throw new RuntimeException("Stub!");
    }

    public void addOnLayoutChangeListener(View.OnLayoutChangeListener listener) {
        throw new RuntimeException("Stub!");
    }

    public void removeOnLayoutChangeListener(View.OnLayoutChangeListener listener) {
        throw new RuntimeException("Stub!");
    }

    public void addOnAttachStateChangeListener(View.OnAttachStateChangeListener listener) {
        throw new RuntimeException("Stub!");
    }

    public void removeOnAttachStateChangeListener(View.OnAttachStateChangeListener listener) {
        throw new RuntimeException("Stub!");
    }

    public View.OnFocusChangeListener getOnFocusChangeListener() {
        throw new RuntimeException("Stub!");
    }

    public void setOnClickListener(@Nullable View.OnClickListener l) {
        throw new RuntimeException("Stub!");
    }

    public boolean hasOnClickListeners() {
        throw new RuntimeException("Stub!");
    }

    public void setOnLongClickListener(@Nullable View.OnLongClickListener l) {
        throw new RuntimeException("Stub!");
    }

    public boolean hasOnLongClickListeners() {
        throw new RuntimeException("Stub!");
    }

    public void setOnContextClickListener(@Nullable View.OnContextClickListener l) {
        throw new RuntimeException("Stub!");
    }

    public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
        throw new RuntimeException("Stub!");
    }

    public boolean performClick() {
        throw new RuntimeException("Stub!");
    }

    public boolean callOnClick() {
        throw new RuntimeException("Stub!");
    }

    public boolean performLongClick() {
        throw new RuntimeException("Stub!");
    }

    public boolean performLongClick(float x, float y) {
        throw new RuntimeException("Stub!");
    }

    public boolean performContextClick(float x, float y) {
        throw new RuntimeException("Stub!");
    }

    public boolean performContextClick() {
        throw new RuntimeException("Stub!");
    }

    public boolean showContextMenu() {
        throw new RuntimeException("Stub!");
    }

    public boolean showContextMenu(float x, float y) {
        throw new RuntimeException("Stub!");
    }

    public ActionMode startActionMode(android.view.ActionMode.Callback callback) {
        throw new RuntimeException("Stub!");
    }

    public ActionMode startActionMode(android.view.ActionMode.Callback callback, int type) {
        throw new RuntimeException("Stub!");
    }

    public void setOnKeyListener(View.OnKeyListener l) {
        throw new RuntimeException("Stub!");
    }

    public void setOnTouchListener(View.OnTouchListener l) {
        throw new RuntimeException("Stub!");
    }

    public void setOnGenericMotionListener(View.OnGenericMotionListener l) {
        throw new RuntimeException("Stub!");
    }

    public void setOnHoverListener(View.OnHoverListener l) {
        throw new RuntimeException("Stub!");
    }

    public void setOnDragListener(View.OnDragListener l) {
        throw new RuntimeException("Stub!");
    }

    public final void setRevealOnFocusHint(boolean revealOnFocus) {
        throw new RuntimeException("Stub!");
    }

    public final boolean getRevealOnFocusHint() {
        throw new RuntimeException("Stub!");
    }

    public boolean requestRectangleOnScreen(Rect rectangle) {
        throw new RuntimeException("Stub!");
    }

    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        throw new RuntimeException("Stub!");
    }

    public void clearFocus() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "focus"
    )
    public boolean hasFocus() {
        throw new RuntimeException("Stub!");
    }

    public boolean hasFocusable() {
        throw new RuntimeException("Stub!");
    }

    public boolean hasExplicitFocusable() {
        throw new RuntimeException("Stub!");
    }

    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        throw new RuntimeException("Stub!");
    }

    public void setAccessibilityPaneTitle(@Nullable CharSequence accessibilityPaneTitle) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public CharSequence getAccessibilityPaneTitle() {
        throw new RuntimeException("Stub!");
    }

    public void sendAccessibilityEvent(int eventType) {
        throw new RuntimeException("Stub!");
    }

    public void announceForAccessibility(CharSequence text) {
        throw new RuntimeException("Stub!");
    }

    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        throw new RuntimeException("Stub!");
    }

    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        throw new RuntimeException("Stub!");
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        throw new RuntimeException("Stub!");
    }

    public AccessibilityNodeInfo createAccessibilityNodeInfo() {
        throw new RuntimeException("Stub!");
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        throw new RuntimeException("Stub!");
    }

    public CharSequence getAccessibilityClassName() {
        throw new RuntimeException("Stub!");
    }

    public void onProvideStructure(ViewStructure structure) {
        throw new RuntimeException("Stub!");
    }

    public void onProvideAutofillStructure(ViewStructure structure, int flags) {
        throw new RuntimeException("Stub!");
    }

    public void onProvideContentCaptureStructure(@NonNull ViewStructure structure, int flags) {
        throw new RuntimeException("Stub!");
    }

    public void onProvideVirtualStructure(ViewStructure structure) {
        throw new RuntimeException("Stub!");
    }

    public void onProvideAutofillVirtualStructure(ViewStructure structure, int flags) {
        throw new RuntimeException("Stub!");
    }

    public void autofill(AutofillValue value) {
        throw new RuntimeException("Stub!");
    }

    public void autofill(@NonNull SparseArray<AutofillValue> values) {
        throw new RuntimeException("Stub!");
    }

    public final AutofillId getAutofillId() {
        throw new RuntimeException("Stub!");
    }

    public void setAutofillId(@Nullable AutofillId id) {
        throw new RuntimeException("Stub!");
    }

    public int getAutofillType() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    @Nullable
    public String[] getAutofillHints() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public AutofillValue getAutofillValue() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        mapping = {@IntToString(
    from = 0,
    to = "auto"
), @IntToString(
    from = 1,
    to = "yes"
), @IntToString(
    from = 2,
    to = "no"
), @IntToString(
    from = 4,
    to = "yesExcludeDescendants"
), @IntToString(
    from = 8,
    to = "noExcludeDescendants"
)}
    )
    public int getImportantForAutofill() {
        throw new RuntimeException("Stub!");
    }

    public void setImportantForAutofill(int mode) {
        throw new RuntimeException("Stub!");
    }

    public final boolean isImportantForAutofill() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        mapping = {@IntToString(
    from = 0,
    to = "auto"
), @IntToString(
    from = 1,
    to = "yes"
), @IntToString(
    from = 2,
    to = "no"
), @IntToString(
    from = 4,
    to = "yesExcludeDescendants"
), @IntToString(
    from = 8,
    to = "noExcludeDescendants"
)}
    )
    public int getImportantForContentCapture() {
        throw new RuntimeException("Stub!");
    }

    public void setImportantForContentCapture(int mode) {
        throw new RuntimeException("Stub!");
    }

    public final boolean isImportantForContentCapture() {
        throw new RuntimeException("Stub!");
    }

    public void setContentCaptureSession(@Nullable ContentCaptureSession contentCaptureSession) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public final ContentCaptureSession getContentCaptureSession() {
        throw new RuntimeException("Stub!");
    }

    public void dispatchProvideStructure(ViewStructure structure) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchProvideAutofillStructure(@NonNull ViewStructure structure, int flags) {
        throw new RuntimeException("Stub!");
    }

    public void addExtraDataToAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info, @NonNull String extraDataKey, @Nullable Bundle arguments) {
        throw new RuntimeException("Stub!");
    }

    public boolean isVisibleToUserForAutofill(int virtualId) {
        throw new RuntimeException("Stub!");
    }

    public View.AccessibilityDelegate getAccessibilityDelegate() {
        throw new RuntimeException("Stub!");
    }

    public void setAccessibilityDelegate(@Nullable View.AccessibilityDelegate delegate) {
        throw new RuntimeException("Stub!");
    }

    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "accessibility"
    )
    @Nullable
    public final CharSequence getStateDescription() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "accessibility"
    )
    public CharSequence getContentDescription() {
        throw new RuntimeException("Stub!");
    }

    public void setStateDescription(@Nullable CharSequence stateDescription) {
        throw new RuntimeException("Stub!");
    }

    public void setContentDescription(CharSequence contentDescription) {
        throw new RuntimeException("Stub!");
    }

    public void setAccessibilityTraversalBefore(int beforeId) {
        throw new RuntimeException("Stub!");
    }

    public int getAccessibilityTraversalBefore() {
        throw new RuntimeException("Stub!");
    }

    public void setAccessibilityTraversalAfter(int afterId) {
        throw new RuntimeException("Stub!");
    }

    public int getAccessibilityTraversalAfter() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "accessibility"
    )
    public int getLabelFor() {
        throw new RuntimeException("Stub!");
    }

    public void setLabelFor(int id) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "focus"
    )
    public boolean isFocused() {
        throw new RuntimeException("Stub!");
    }

    public View findFocus() {
        throw new RuntimeException("Stub!");
    }

    public boolean isScrollContainer() {
        throw new RuntimeException("Stub!");
    }

    public void setScrollContainer(boolean isScrollContainer) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public int getDrawingCacheQuality() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void setDrawingCacheQuality(int quality) {
        throw new RuntimeException("Stub!");
    }

    public boolean getKeepScreenOn() {
        throw new RuntimeException("Stub!");
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        throw new RuntimeException("Stub!");
    }

    public int getNextFocusLeftId() {
        throw new RuntimeException("Stub!");
    }

    public void setNextFocusLeftId(int nextFocusLeftId) {
        throw new RuntimeException("Stub!");
    }

    public int getNextFocusRightId() {
        throw new RuntimeException("Stub!");
    }

    public void setNextFocusRightId(int nextFocusRightId) {
        throw new RuntimeException("Stub!");
    }

    public int getNextFocusUpId() {
        throw new RuntimeException("Stub!");
    }

    public void setNextFocusUpId(int nextFocusUpId) {
        throw new RuntimeException("Stub!");
    }

    public int getNextFocusDownId() {
        throw new RuntimeException("Stub!");
    }

    public void setNextFocusDownId(int nextFocusDownId) {
        throw new RuntimeException("Stub!");
    }

    public int getNextFocusForwardId() {
        throw new RuntimeException("Stub!");
    }

    public void setNextFocusForwardId(int nextFocusForwardId) {
        throw new RuntimeException("Stub!");
    }

    public int getNextClusterForwardId() {
        throw new RuntimeException("Stub!");
    }

    public void setNextClusterForwardId(int nextClusterForwardId) {
        throw new RuntimeException("Stub!");
    }

    public boolean isShown() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    protected boolean fitSystemWindows(Rect insets) {
        throw new RuntimeException("Stub!");
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        throw new RuntimeException("Stub!");
    }

    public void setOnApplyWindowInsetsListener(View.OnApplyWindowInsetsListener listener) {
        throw new RuntimeException("Stub!");
    }

    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        throw new RuntimeException("Stub!");
    }

    public void setWindowInsetsAnimationCallback(@Nullable android.view.WindowInsetsAnimation.Callback callback) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchWindowInsetsAnimationPrepare(@NonNull WindowInsetsAnimation animation) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public Bounds dispatchWindowInsetsAnimationStart(@NonNull WindowInsetsAnimation animation, @NonNull Bounds bounds) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public WindowInsets dispatchWindowInsetsAnimationProgress(@NonNull WindowInsets insets, @NonNull List<WindowInsetsAnimation> runningAnimations) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchWindowInsetsAnimationEnd(@NonNull WindowInsetsAnimation animation) {
        throw new RuntimeException("Stub!");
    }

    public void setSystemGestureExclusionRects(@NonNull List<Rect> rects) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public List<Rect> getSystemGestureExclusionRects() {
        throw new RuntimeException("Stub!");
    }

    public void getLocationInSurface(@NonNull int[] location) {
        throw new RuntimeException("Stub!");
    }

    public WindowInsets getRootWindowInsets() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public WindowInsetsController getWindowInsetsController() {
        throw new RuntimeException("Stub!");
    }

    public WindowInsets computeSystemWindowInsets(WindowInsets in, Rect outLocalInsets) {
        throw new RuntimeException("Stub!");
    }

    public void setFitsSystemWindows(boolean fitSystemWindows) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean getFitsSystemWindows() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void requestFitSystemWindows() {
        throw new RuntimeException("Stub!");
    }

    public void requestApplyInsets() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        mapping = {@IntToString(
    from = 0,
    to = "VISIBLE"
), @IntToString(
    from = 4,
    to = "INVISIBLE"
), @IntToString(
    from = 8,
    to = "GONE"
)}
    )
    public int getVisibility() {
        throw new RuntimeException("Stub!");
    }

    public void setVisibility(int visibility) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setEnabled(boolean enabled) {
        throw new RuntimeException("Stub!");
    }

    public void setFocusable(boolean focusable) {
        throw new RuntimeException("Stub!");
    }

    public void setFocusable(int focusable) {
        throw new RuntimeException("Stub!");
    }

    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
        throw new RuntimeException("Stub!");
    }

    public void setAutofillHints(@Nullable String... autofillHints) {
        throw new RuntimeException("Stub!");
    }

    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isSoundEffectsEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isHapticFeedbackEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setLayoutDirection(int layoutDirection) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "layout",
        mapping = {@IntToString(
    from = 0,
    to = "RESOLVED_DIRECTION_LTR"
), @IntToString(
    from = 1,
    to = "RESOLVED_DIRECTION_RTL"
)}
    )
    public int getLayoutDirection() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "layout"
    )
    public boolean hasTransientState() {
        throw new RuntimeException("Stub!");
    }

    public void setHasTransientState(boolean hasTransientState) {
        throw new RuntimeException("Stub!");
    }

    public boolean isAttachedToWindow() {
        throw new RuntimeException("Stub!");
    }

    public boolean isLaidOut() {
        throw new RuntimeException("Stub!");
    }

    public void setWillNotDraw(boolean willNotDraw) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public boolean willNotDraw() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void setWillNotCacheDrawing(boolean willNotCacheDrawing) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    @ExportedProperty(
        category = "drawing"
    )
    public boolean willNotCacheDrawing() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isClickable() {
        throw new RuntimeException("Stub!");
    }

    public void setClickable(boolean clickable) {
        throw new RuntimeException("Stub!");
    }

    public boolean isLongClickable() {
        throw new RuntimeException("Stub!");
    }

    public void setLongClickable(boolean longClickable) {
        throw new RuntimeException("Stub!");
    }

    public boolean isContextClickable() {
        throw new RuntimeException("Stub!");
    }

    public void setContextClickable(boolean contextClickable) {
        throw new RuntimeException("Stub!");
    }

    public void setPressed(boolean pressed) {
        throw new RuntimeException("Stub!");
    }

    protected void dispatchSetPressed(boolean pressed) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isPressed() {
        throw new RuntimeException("Stub!");
    }

    public boolean isSaveEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setSaveEnabled(boolean enabled) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean getFilterTouchesWhenObscured() {
        throw new RuntimeException("Stub!");
    }

    public void setFilterTouchesWhenObscured(boolean enabled) {
        throw new RuntimeException("Stub!");
    }

    public boolean isSaveFromParentEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setSaveFromParentEnabled(boolean enabled) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "focus"
    )
    public final boolean isFocusable() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        mapping = {@IntToString(
    from = 0,
    to = "NOT_FOCUSABLE"
), @IntToString(
    from = 1,
    to = "FOCUSABLE"
), @IntToString(
    from = 16,
    to = "FOCUSABLE_AUTO"
)},
        category = "focus"
    )
    public int getFocusable() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "focus"
    )
    public final boolean isFocusableInTouchMode() {
        throw new RuntimeException("Stub!");
    }

    public boolean isScreenReaderFocusable() {
        throw new RuntimeException("Stub!");
    }

    public void setScreenReaderFocusable(boolean screenReaderFocusable) {
        throw new RuntimeException("Stub!");
    }

    public boolean isAccessibilityHeading() {
        throw new RuntimeException("Stub!");
    }

    public void setAccessibilityHeading(boolean isHeading) {
        throw new RuntimeException("Stub!");
    }

    public View focusSearch(int direction) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "focus"
    )
    public final boolean isKeyboardNavigationCluster() {
        throw new RuntimeException("Stub!");
    }

    public void setKeyboardNavigationCluster(boolean isCluster) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "focus"
    )
    public final boolean isFocusedByDefault() {
        throw new RuntimeException("Stub!");
    }

    public void setFocusedByDefault(boolean isFocusedByDefault) {
        throw new RuntimeException("Stub!");
    }

    public View keyboardNavigationClusterSearch(View currentCluster, int direction) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        throw new RuntimeException("Stub!");
    }

    public void setDefaultFocusHighlightEnabled(boolean defaultFocusHighlightEnabled) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "focus"
    )
    public final boolean getDefaultFocusHighlightEnabled() {
        throw new RuntimeException("Stub!");
    }

    public ArrayList<View> getFocusables(int direction) {
        throw new RuntimeException("Stub!");
    }

    public void addFocusables(ArrayList<View> views, int direction) {
        throw new RuntimeException("Stub!");
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        throw new RuntimeException("Stub!");
    }

    public void addKeyboardNavigationClusters(@NonNull Collection<View> views, int direction) {
        throw new RuntimeException("Stub!");
    }

    public void findViewsWithText(ArrayList<View> outViews, CharSequence searched, int flags) {
        throw new RuntimeException("Stub!");
    }

    public ArrayList<View> getTouchables() {
        throw new RuntimeException("Stub!");
    }

    public void addTouchables(ArrayList<View> views) {
        throw new RuntimeException("Stub!");
    }

    public boolean isAccessibilityFocused() {
        throw new RuntimeException("Stub!");
    }

    public final boolean requestFocus() {
        throw new RuntimeException("Stub!");
    }

    public boolean restoreDefaultFocus() {
        throw new RuntimeException("Stub!");
    }

    public final boolean requestFocus(int direction) {
        throw new RuntimeException("Stub!");
    }

    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        throw new RuntimeException("Stub!");
    }

    public final boolean requestFocusFromTouch() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "accessibility",
        mapping = {@IntToString(
    from = 0,
    to = "auto"
), @IntToString(
    from = 1,
    to = "yes"
), @IntToString(
    from = 2,
    to = "no"
), @IntToString(
    from = 4,
    to = "noHideDescendants"
)}
    )
    public int getImportantForAccessibility() {
        throw new RuntimeException("Stub!");
    }

    public void setAccessibilityLiveRegion(int mode) {
        throw new RuntimeException("Stub!");
    }

    public int getAccessibilityLiveRegion() {
        throw new RuntimeException("Stub!");
    }

    public void setImportantForAccessibility(int mode) {
        throw new RuntimeException("Stub!");
    }

    public boolean isImportantForAccessibility() {
        throw new RuntimeException("Stub!");
    }

    public ViewParent getParentForAccessibility() {
        throw new RuntimeException("Stub!");
    }

    public void addChildrenForAccessibility(ArrayList<View> outChildren) {
        throw new RuntimeException("Stub!");
    }

    public void setTransitionVisibility(int visibility) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchNestedPrePerformAccessibilityAction(int action, Bundle arguments) {
        throw new RuntimeException("Stub!");
    }

    public boolean performAccessibilityAction(int action, Bundle arguments) {
        throw new RuntimeException("Stub!");
    }

    public final boolean isTemporarilyDetached() {
        throw new RuntimeException("Stub!");
    }

    public void dispatchStartTemporaryDetach() {
        throw new RuntimeException("Stub!");
    }

    public void onStartTemporaryDetach() {
        throw new RuntimeException("Stub!");
    }

    public void dispatchFinishTemporaryDetach() {
        throw new RuntimeException("Stub!");
    }

    public void onFinishTemporaryDetach() {
        throw new RuntimeException("Stub!");
    }

    public DispatcherState getKeyDispatcherState() {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onFilterTouchEventForSecurity(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchTrackballEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchCapturedPointerEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    protected boolean dispatchHoverEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    protected boolean dispatchGenericPointerEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    protected boolean dispatchGenericFocusedEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchWindowFocusChanged(boolean hasFocus) {
        throw new RuntimeException("Stub!");
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        throw new RuntimeException("Stub!");
    }

    public boolean hasWindowFocus() {
        throw new RuntimeException("Stub!");
    }

    protected void dispatchVisibilityChanged(@NonNull View changedView, int visibility) {
        throw new RuntimeException("Stub!");
    }

    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchDisplayHint(int hint) {
        throw new RuntimeException("Stub!");
    }

    protected void onDisplayHint(int hint) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchWindowVisibilityChanged(int visibility) {
        throw new RuntimeException("Stub!");
    }

    protected void onWindowVisibilityChanged(int visibility) {
        throw new RuntimeException("Stub!");
    }

    public void onVisibilityAggregated(boolean isVisible) {
        throw new RuntimeException("Stub!");
    }

    public int getWindowVisibility() {
        throw new RuntimeException("Stub!");
    }

    public void getWindowVisibleDisplayFrame(Rect outRect) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        throw new RuntimeException("Stub!");
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isInTouchMode() {
        throw new RuntimeException("Stub!");
    }

    @CapturedViewProperty
    public final Context getContext() {
        throw new RuntimeException("Stub!");
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onCheckIsTextEditor() {
        throw new RuntimeException("Stub!");
    }

    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        throw new RuntimeException("Stub!");
    }

    public boolean checkInputConnectionProxy(View view) {
        throw new RuntimeException("Stub!");
    }

    public void createContextMenu(ContextMenu menu) {
        throw new RuntimeException("Stub!");
    }

    protected ContextMenuInfo getContextMenuInfo() {
        throw new RuntimeException("Stub!");
    }

    protected void onCreateContextMenu(ContextMenu menu) {
        throw new RuntimeException("Stub!");
    }

    public boolean onTrackballEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean onHoverEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isHovered() {
        throw new RuntimeException("Stub!");
    }

    public void setHovered(boolean hovered) {
        throw new RuntimeException("Stub!");
    }

    public void onHoverChanged(boolean hovered) {
        throw new RuntimeException("Stub!");
    }

    public boolean onTouchEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public void cancelLongPress() {
        throw new RuntimeException("Stub!");
    }

    public void setTouchDelegate(TouchDelegate delegate) {
        throw new RuntimeException("Stub!");
    }

    public TouchDelegate getTouchDelegate() {
        throw new RuntimeException("Stub!");
    }

    public final void requestUnbufferedDispatch(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public final void requestUnbufferedDispatch(int source) {
        throw new RuntimeException("Stub!");
    }

    public void bringToFront() {
        throw new RuntimeException("Stub!");
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        throw new RuntimeException("Stub!");
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        throw new RuntimeException("Stub!");
    }

    protected void dispatchDraw(Canvas canvas) {
        throw new RuntimeException("Stub!");
    }

    public final ViewParent getParent() {
        throw new RuntimeException("Stub!");
    }

    public void setScrollX(int value) {
        throw new RuntimeException("Stub!");
    }

    public void setScrollY(int value) {
        throw new RuntimeException("Stub!");
    }

    public final int getScrollX() {
        throw new RuntimeException("Stub!");
    }

    public final int getScrollY() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "layout"
    )
    public final int getWidth() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "layout"
    )
    public final int getHeight() {
        throw new RuntimeException("Stub!");
    }

    public void getDrawingRect(Rect outRect) {
        throw new RuntimeException("Stub!");
    }

    public final int getMeasuredWidth() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "measurement",
        flagMapping = {@FlagToString(
    mask = -16777216,
    equals = 16777216,
    name = "MEASURED_STATE_TOO_SMALL"
)}
    )
    public final int getMeasuredWidthAndState() {
        throw new RuntimeException("Stub!");
    }

    public final int getMeasuredHeight() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "measurement",
        flagMapping = {@FlagToString(
    mask = -16777216,
    equals = 16777216,
    name = "MEASURED_STATE_TOO_SMALL"
)}
    )
    public final int getMeasuredHeightAndState() {
        throw new RuntimeException("Stub!");
    }

    public final int getMeasuredState() {
        throw new RuntimeException("Stub!");
    }

    public Matrix getMatrix() {
        throw new RuntimeException("Stub!");
    }

    public float getCameraDistance() {
        throw new RuntimeException("Stub!");
    }

    public void setCameraDistance(float distance) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getRotation() {
        throw new RuntimeException("Stub!");
    }

    public void setRotation(float rotation) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getRotationY() {
        throw new RuntimeException("Stub!");
    }

    public void setRotationY(float rotationY) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getRotationX() {
        throw new RuntimeException("Stub!");
    }

    public void setRotationX(float rotationX) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getScaleX() {
        throw new RuntimeException("Stub!");
    }

    public void setScaleX(float scaleX) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getScaleY() {
        throw new RuntimeException("Stub!");
    }

    public void setScaleY(float scaleY) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getPivotX() {
        throw new RuntimeException("Stub!");
    }

    public void setPivotX(float pivotX) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getPivotY() {
        throw new RuntimeException("Stub!");
    }

    public void setPivotY(float pivotY) {
        throw new RuntimeException("Stub!");
    }

    public boolean isPivotSet() {
        throw new RuntimeException("Stub!");
    }

    public void resetPivot() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getAlpha() {
        throw new RuntimeException("Stub!");
    }

    public void forceHasOverlappingRendering(boolean hasOverlappingRendering) {
        throw new RuntimeException("Stub!");
    }

    public final boolean getHasOverlappingRendering() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public boolean hasOverlappingRendering() {
        throw new RuntimeException("Stub!");
    }

    public void setAlpha(float alpha) {
        throw new RuntimeException("Stub!");
    }

    public void setTransitionAlpha(float alpha) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getTransitionAlpha() {
        throw new RuntimeException("Stub!");
    }

    public void setForceDarkAllowed(boolean allow) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public boolean isForceDarkAllowed() {
        throw new RuntimeException("Stub!");
    }

    @CapturedViewProperty
    public final int getTop() {
        throw new RuntimeException("Stub!");
    }

    public final void setTop(int top) {
        throw new RuntimeException("Stub!");
    }

    @CapturedViewProperty
    public final int getBottom() {
        throw new RuntimeException("Stub!");
    }

    public boolean isDirty() {
        throw new RuntimeException("Stub!");
    }

    public final void setBottom(int bottom) {
        throw new RuntimeException("Stub!");
    }

    @CapturedViewProperty
    public final int getLeft() {
        throw new RuntimeException("Stub!");
    }

    public final void setLeft(int left) {
        throw new RuntimeException("Stub!");
    }

    @CapturedViewProperty
    public final int getRight() {
        throw new RuntimeException("Stub!");
    }

    public final void setRight(int right) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getX() {
        throw new RuntimeException("Stub!");
    }

    public void setX(float x) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getY() {
        throw new RuntimeException("Stub!");
    }

    public void setY(float y) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getZ() {
        throw new RuntimeException("Stub!");
    }

    public void setZ(float z) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getElevation() {
        throw new RuntimeException("Stub!");
    }

    public void setElevation(float elevation) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getTranslationX() {
        throw new RuntimeException("Stub!");
    }

    public void setTranslationX(float translationX) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getTranslationY() {
        throw new RuntimeException("Stub!");
    }

    public void setTranslationY(float translationY) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public float getTranslationZ() {
        throw new RuntimeException("Stub!");
    }

    public void setTranslationZ(float translationZ) {
        throw new RuntimeException("Stub!");
    }

    public void setAnimationMatrix(@Nullable Matrix matrix) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public Matrix getAnimationMatrix() {
        throw new RuntimeException("Stub!");
    }

    public StateListAnimator getStateListAnimator() {
        throw new RuntimeException("Stub!");
    }

    public void setStateListAnimator(StateListAnimator stateListAnimator) {
        throw new RuntimeException("Stub!");
    }

    public final boolean getClipToOutline() {
        throw new RuntimeException("Stub!");
    }

    public void setClipToOutline(boolean clipToOutline) {
        throw new RuntimeException("Stub!");
    }

    public void setOutlineProvider(ViewOutlineProvider provider) {
        throw new RuntimeException("Stub!");
    }

    public ViewOutlineProvider getOutlineProvider() {
        throw new RuntimeException("Stub!");
    }

    public void invalidateOutline() {
        throw new RuntimeException("Stub!");
    }

    public void setOutlineSpotShadowColor(int color) {
        throw new RuntimeException("Stub!");
    }

    public int getOutlineSpotShadowColor() {
        throw new RuntimeException("Stub!");
    }

    public void setOutlineAmbientShadowColor(int color) {
        throw new RuntimeException("Stub!");
    }

    public int getOutlineAmbientShadowColor() {
        throw new RuntimeException("Stub!");
    }

    public void getHitRect(Rect outRect) {
        throw new RuntimeException("Stub!");
    }

    public void getFocusedRect(Rect r) {
        throw new RuntimeException("Stub!");
    }

    public boolean getGlobalVisibleRect(Rect r, Point globalOffset) {
        throw new RuntimeException("Stub!");
    }

    public final boolean getGlobalVisibleRect(Rect r) {
        throw new RuntimeException("Stub!");
    }

    public final boolean getLocalVisibleRect(Rect r) {
        throw new RuntimeException("Stub!");
    }

    public void offsetTopAndBottom(int offset) {
        throw new RuntimeException("Stub!");
    }

    public void offsetLeftAndRight(int offset) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        deepExport = true,
        prefix = "layout_"
    )
    public LayoutParams getLayoutParams() {
        throw new RuntimeException("Stub!");
    }

    public void setLayoutParams(LayoutParams params) {
        throw new RuntimeException("Stub!");
    }

    public void scrollTo(int x, int y) {
        throw new RuntimeException("Stub!");
    }

    public void scrollBy(int x, int y) {
        throw new RuntimeException("Stub!");
    }

    protected boolean awakenScrollBars() {
        throw new RuntimeException("Stub!");
    }

    protected boolean awakenScrollBars(int startDelay) {
        throw new RuntimeException("Stub!");
    }

    protected boolean awakenScrollBars(int startDelay, boolean invalidate) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void invalidate(Rect dirty) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void invalidate(int l, int t, int r, int b) {
        throw new RuntimeException("Stub!");
    }

    public void invalidate() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public boolean isOpaque() {
        throw new RuntimeException("Stub!");
    }

    public Handler getHandler() {
        throw new RuntimeException("Stub!");
    }

    public boolean post(Runnable action) {
        throw new RuntimeException("Stub!");
    }

    public boolean postDelayed(Runnable action, long delayMillis) {
        throw new RuntimeException("Stub!");
    }

    public void postOnAnimation(Runnable action) {
        throw new RuntimeException("Stub!");
    }

    public void postOnAnimationDelayed(Runnable action, long delayMillis) {
        throw new RuntimeException("Stub!");
    }

    public boolean removeCallbacks(Runnable action) {
        throw new RuntimeException("Stub!");
    }

    public void postInvalidate() {
        throw new RuntimeException("Stub!");
    }

    public void postInvalidate(int left, int top, int right, int bottom) {
        throw new RuntimeException("Stub!");
    }

    public void postInvalidateDelayed(long delayMilliseconds) {
        throw new RuntimeException("Stub!");
    }

    public void postInvalidateDelayed(long delayMilliseconds, int left, int top, int right, int bottom) {
        throw new RuntimeException("Stub!");
    }

    public void postInvalidateOnAnimation() {
        throw new RuntimeException("Stub!");
    }

    public void postInvalidateOnAnimation(int left, int top, int right, int bottom) {
        throw new RuntimeException("Stub!");
    }

    public void computeScroll() {
        throw new RuntimeException("Stub!");
    }

    public boolean isHorizontalFadingEdgeEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setHorizontalFadingEdgeEnabled(boolean horizontalFadingEdgeEnabled) {
        throw new RuntimeException("Stub!");
    }

    public boolean isVerticalFadingEdgeEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setVerticalFadingEdgeEnabled(boolean verticalFadingEdgeEnabled) {
        throw new RuntimeException("Stub!");
    }

    protected float getTopFadingEdgeStrength() {
        throw new RuntimeException("Stub!");
    }

    protected float getBottomFadingEdgeStrength() {
        throw new RuntimeException("Stub!");
    }

    protected float getLeftFadingEdgeStrength() {
        throw new RuntimeException("Stub!");
    }

    protected float getRightFadingEdgeStrength() {
        throw new RuntimeException("Stub!");
    }

    public boolean isHorizontalScrollBarEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        throw new RuntimeException("Stub!");
    }

    public boolean isVerticalScrollBarEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        throw new RuntimeException("Stub!");
    }

    public void setScrollbarFadingEnabled(boolean fadeScrollbars) {
        throw new RuntimeException("Stub!");
    }

    public boolean isScrollbarFadingEnabled() {
        throw new RuntimeException("Stub!");
    }

    public int getScrollBarDefaultDelayBeforeFade() {
        throw new RuntimeException("Stub!");
    }

    public void setScrollBarDefaultDelayBeforeFade(int scrollBarDefaultDelayBeforeFade) {
        throw new RuntimeException("Stub!");
    }

    public int getScrollBarFadeDuration() {
        throw new RuntimeException("Stub!");
    }

    public void setScrollBarFadeDuration(int scrollBarFadeDuration) {
        throw new RuntimeException("Stub!");
    }

    public int getScrollBarSize() {
        throw new RuntimeException("Stub!");
    }

    public void setScrollBarSize(int scrollBarSize) {
        throw new RuntimeException("Stub!");
    }

    public void setScrollBarStyle(int style) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        mapping = {@IntToString(
    from = 0,
    to = "INSIDE_OVERLAY"
), @IntToString(
    from = 16777216,
    to = "INSIDE_INSET"
), @IntToString(
    from = 33554432,
    to = "OUTSIDE_OVERLAY"
), @IntToString(
    from = 50331648,
    to = "OUTSIDE_INSET"
)}
    )
    public int getScrollBarStyle() {
        throw new RuntimeException("Stub!");
    }

    protected int computeHorizontalScrollRange() {
        throw new RuntimeException("Stub!");
    }

    protected int computeHorizontalScrollOffset() {
        throw new RuntimeException("Stub!");
    }

    protected int computeHorizontalScrollExtent() {
        throw new RuntimeException("Stub!");
    }

    protected int computeVerticalScrollRange() {
        throw new RuntimeException("Stub!");
    }

    protected int computeVerticalScrollOffset() {
        throw new RuntimeException("Stub!");
    }

    protected int computeVerticalScrollExtent() {
        throw new RuntimeException("Stub!");
    }

    public boolean canScrollHorizontally(int direction) {
        throw new RuntimeException("Stub!");
    }

    public boolean canScrollVertically(int direction) {
        throw new RuntimeException("Stub!");
    }

    protected final void onDrawScrollBars(Canvas canvas) {
        throw new RuntimeException("Stub!");
    }

    protected void onDraw(Canvas canvas) {
        throw new RuntimeException("Stub!");
    }

    protected void onAttachedToWindow() {
        throw new RuntimeException("Stub!");
    }

    public void onScreenStateChanged(int screenState) {
        throw new RuntimeException("Stub!");
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        throw new RuntimeException("Stub!");
    }

    public boolean canResolveLayoutDirection() {
        throw new RuntimeException("Stub!");
    }

    public boolean isLayoutDirectionResolved() {
        throw new RuntimeException("Stub!");
    }

    protected void onDetachedFromWindow() {
        throw new RuntimeException("Stub!");
    }

    protected int getWindowAttachCount() {
        throw new RuntimeException("Stub!");
    }

    public IBinder getWindowToken() {
        throw new RuntimeException("Stub!");
    }

    public WindowId getWindowId() {
        throw new RuntimeException("Stub!");
    }

    public IBinder getApplicationWindowToken() {
        throw new RuntimeException("Stub!");
    }

    public Display getDisplay() {
        throw new RuntimeException("Stub!");
    }

    public final void cancelPendingInputEvents() {
        throw new RuntimeException("Stub!");
    }

    public void onCancelPendingInputEvents() {
        throw new RuntimeException("Stub!");
    }

    public void saveHierarchyState(SparseArray<Parcelable> container) {
        throw new RuntimeException("Stub!");
    }

    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    protected Parcelable onSaveInstanceState() {
        throw new RuntimeException("Stub!");
    }

    public void restoreHierarchyState(SparseArray<Parcelable> container) {
        throw new RuntimeException("Stub!");
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        throw new RuntimeException("Stub!");
    }

    protected void onRestoreInstanceState(Parcelable state) {
        throw new RuntimeException("Stub!");
    }

    public long getDrawingTime() {
        throw new RuntimeException("Stub!");
    }

    public void setDuplicateParentStateEnabled(boolean enabled) {
        throw new RuntimeException("Stub!");
    }

    public boolean isDuplicateParentStateEnabled() {
        throw new RuntimeException("Stub!");
    }

    public void setLayerType(int layerType, @Nullable Paint paint) {
        throw new RuntimeException("Stub!");
    }

    public void setLayerPaint(@Nullable Paint paint) {
        throw new RuntimeException("Stub!");
    }

    public int getLayerType() {
        throw new RuntimeException("Stub!");
    }

    public void buildLayer() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void setDrawingCacheEnabled(boolean enabled) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    @ExportedProperty(
        category = "drawing"
    )
    public boolean isDrawingCacheEnabled() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public Bitmap getDrawingCache() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public Bitmap getDrawingCache(boolean autoScale) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void destroyDrawingCache() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void setDrawingCacheBackgroundColor(int color) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public int getDrawingCacheBackgroundColor() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void buildDrawingCache() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void buildDrawingCache(boolean autoScale) {
        throw new RuntimeException("Stub!");
    }

    public boolean isInEditMode() {
        throw new RuntimeException("Stub!");
    }

    protected boolean isPaddingOffsetRequired() {
        throw new RuntimeException("Stub!");
    }

    protected int getLeftPaddingOffset() {
        throw new RuntimeException("Stub!");
    }

    protected int getRightPaddingOffset() {
        throw new RuntimeException("Stub!");
    }

    protected int getTopPaddingOffset() {
        throw new RuntimeException("Stub!");
    }

    protected int getBottomPaddingOffset() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public boolean isHardwareAccelerated() {
        throw new RuntimeException("Stub!");
    }

    public void setClipBounds(Rect clipBounds) {
        throw new RuntimeException("Stub!");
    }

    public Rect getClipBounds() {
        throw new RuntimeException("Stub!");
    }

    public boolean getClipBounds(Rect outRect) {
        throw new RuntimeException("Stub!");
    }

    public void draw(Canvas canvas) {
        throw new RuntimeException("Stub!");
    }

    public ViewOverlay getOverlay() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "drawing"
    )
    public int getSolidColor() {
        throw new RuntimeException("Stub!");
    }

    public boolean isLayoutRequested() {
        throw new RuntimeException("Stub!");
    }

    public void layout(int l, int t, int r, int b) {
        throw new RuntimeException("Stub!");
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        throw new RuntimeException("Stub!");
    }

    public final void setLeftTopRightBottom(int left, int top, int right, int bottom) {
        throw new RuntimeException("Stub!");
    }

    protected void onFinishInflate() {
        throw new RuntimeException("Stub!");
    }

    public Resources getResources() {
        throw new RuntimeException("Stub!");
    }

    public void invalidateDrawable(@NonNull Drawable drawable) {
        throw new RuntimeException("Stub!");
    }

    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        throw new RuntimeException("Stub!");
    }

    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        throw new RuntimeException("Stub!");
    }

    public void unscheduleDrawable(Drawable who) {
        throw new RuntimeException("Stub!");
    }

    protected boolean verifyDrawable(@NonNull Drawable who) {
        throw new RuntimeException("Stub!");
    }

    protected void drawableStateChanged() {
        throw new RuntimeException("Stub!");
    }

    public void drawableHotspotChanged(float x, float y) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchDrawableHotspotChanged(float x, float y) {
        throw new RuntimeException("Stub!");
    }

    public void refreshDrawableState() {
        throw new RuntimeException("Stub!");
    }

    public final int[] getDrawableState() {
        throw new RuntimeException("Stub!");
    }

    protected int[] onCreateDrawableState(int extraSpace) {
        throw new RuntimeException("Stub!");
    }

    protected static int[] mergeDrawableStates(int[] baseState, int[] additionalState) {
        throw new RuntimeException("Stub!");
    }

    public void jumpDrawablesToCurrentState() {
        throw new RuntimeException("Stub!");
    }

    public void setBackgroundColor(int color) {
        throw new RuntimeException("Stub!");
    }

    public void setBackgroundResource(int resid) {
        throw new RuntimeException("Stub!");
    }

    public void setBackground(Drawable background) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void setBackgroundDrawable(Drawable background) {
        throw new RuntimeException("Stub!");
    }

    public Drawable getBackground() {
        throw new RuntimeException("Stub!");
    }

    public void setBackgroundTintList(@Nullable ColorStateList tint) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public ColorStateList getBackgroundTintList() {
        throw new RuntimeException("Stub!");
    }

    public void setBackgroundTintMode(@Nullable Mode tintMode) {
        throw new RuntimeException("Stub!");
    }

    public void setBackgroundTintBlendMode(@Nullable BlendMode blendMode) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public Mode getBackgroundTintMode() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public BlendMode getBackgroundTintBlendMode() {
        throw new RuntimeException("Stub!");
    }

    public Drawable getForeground() {
        throw new RuntimeException("Stub!");
    }

    public void setForeground(Drawable foreground) {
        throw new RuntimeException("Stub!");
    }

    public int getForegroundGravity() {
        throw new RuntimeException("Stub!");
    }

    public void setForegroundGravity(int gravity) {
        throw new RuntimeException("Stub!");
    }

    public void setForegroundTintList(@Nullable ColorStateList tint) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public ColorStateList getForegroundTintList() {
        throw new RuntimeException("Stub!");
    }

    public void setForegroundTintMode(@Nullable Mode tintMode) {
        throw new RuntimeException("Stub!");
    }

    public void setForegroundTintBlendMode(@Nullable BlendMode blendMode) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public Mode getForegroundTintMode() {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public BlendMode getForegroundTintBlendMode() {
        throw new RuntimeException("Stub!");
    }

    public void onDrawForeground(Canvas canvas) {
        throw new RuntimeException("Stub!");
    }

    public void setPadding(int left, int top, int right, int bottom) {
        throw new RuntimeException("Stub!");
    }

    public void setPaddingRelative(int start, int top, int end, int bottom) {
        throw new RuntimeException("Stub!");
    }

    public int getSourceLayoutResId() {
        throw new RuntimeException("Stub!");
    }

    public int getPaddingTop() {
        throw new RuntimeException("Stub!");
    }

    public int getPaddingBottom() {
        throw new RuntimeException("Stub!");
    }

    public int getPaddingLeft() {
        throw new RuntimeException("Stub!");
    }

    public int getPaddingStart() {
        throw new RuntimeException("Stub!");
    }

    public int getPaddingRight() {
        throw new RuntimeException("Stub!");
    }

    public int getPaddingEnd() {
        throw new RuntimeException("Stub!");
    }

    public boolean isPaddingRelative() {
        throw new RuntimeException("Stub!");
    }

    public void setSelected(boolean selected) {
        throw new RuntimeException("Stub!");
    }

    protected void dispatchSetSelected(boolean selected) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isSelected() {
        throw new RuntimeException("Stub!");
    }

    public void setActivated(boolean activated) {
        throw new RuntimeException("Stub!");
    }

    protected void dispatchSetActivated(boolean activated) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public boolean isActivated() {
        throw new RuntimeException("Stub!");
    }

    public ViewTreeObserver getViewTreeObserver() {
        throw new RuntimeException("Stub!");
    }

    public View getRootView() {
        throw new RuntimeException("Stub!");
    }

    public void transformMatrixToGlobal(@NonNull Matrix matrix) {
        throw new RuntimeException("Stub!");
    }

    public void transformMatrixToLocal(@NonNull Matrix matrix) {
        throw new RuntimeException("Stub!");
    }

    public void getLocationOnScreen(int[] outLocation) {
        throw new RuntimeException("Stub!");
    }

    public void getLocationInWindow(int[] outLocation) {
        throw new RuntimeException("Stub!");
    }

    public final <T extends View> T findViewById(int id) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public final <T extends View> T requireViewById(int id) {
        throw new RuntimeException("Stub!");
    }

    public final <T extends View> T findViewWithTag(Object tag) {
        throw new RuntimeException("Stub!");
    }

    public void setId(int id) {
        throw new RuntimeException("Stub!");
    }

    @CapturedViewProperty
    public int getId() {
        throw new RuntimeException("Stub!");
    }

    public long getUniqueDrawingId() {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public Object getTag() {
        throw new RuntimeException("Stub!");
    }

    public void setTag(Object tag) {
        throw new RuntimeException("Stub!");
    }

    public Object getTag(int key) {
        throw new RuntimeException("Stub!");
    }

    public void setTag(int key, Object tag) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "layout"
    )
    public int getBaseline() {
        throw new RuntimeException("Stub!");
    }

    public boolean isInLayout() {
        throw new RuntimeException("Stub!");
    }

    public void requestLayout() {
        throw new RuntimeException("Stub!");
    }

    public void forceLayout() {
        throw new RuntimeException("Stub!");
    }

    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        throw new RuntimeException("Stub!");
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        throw new RuntimeException("Stub!");
    }

    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        throw new RuntimeException("Stub!");
    }

    public static int combineMeasuredStates(int curState, int newState) {
        throw new RuntimeException("Stub!");
    }

    public static int resolveSize(int size, int measureSpec) {
        throw new RuntimeException("Stub!");
    }

    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        throw new RuntimeException("Stub!");
    }

    public static int getDefaultSize(int size, int measureSpec) {
        throw new RuntimeException("Stub!");
    }

    protected int getSuggestedMinimumHeight() {
        throw new RuntimeException("Stub!");
    }

    protected int getSuggestedMinimumWidth() {
        throw new RuntimeException("Stub!");
    }

    public int getMinimumHeight() {
        throw new RuntimeException("Stub!");
    }

    public void setMinimumHeight(int minHeight) {
        throw new RuntimeException("Stub!");
    }

    public int getMinimumWidth() {
        throw new RuntimeException("Stub!");
    }

    public void setMinimumWidth(int minWidth) {
        throw new RuntimeException("Stub!");
    }

    public Animation getAnimation() {
        throw new RuntimeException("Stub!");
    }

    public void startAnimation(Animation animation) {
        throw new RuntimeException("Stub!");
    }

    public void clearAnimation() {
        throw new RuntimeException("Stub!");
    }

    public void setAnimation(Animation animation) {
        throw new RuntimeException("Stub!");
    }

    protected void onAnimationStart() {
        throw new RuntimeException("Stub!");
    }

    protected void onAnimationEnd() {
        throw new RuntimeException("Stub!");
    }

    protected boolean onSetAlpha(int alpha) {
        throw new RuntimeException("Stub!");
    }

    public void playSoundEffect(int soundConstant) {
        throw new RuntimeException("Stub!");
    }

    public boolean performHapticFeedback(int feedbackConstant) {
        throw new RuntimeException("Stub!");
    }

    public boolean performHapticFeedback(int feedbackConstant, int flags) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void setSystemUiVisibility(int visibility) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public int getSystemUiVisibility() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public int getWindowSystemUiVisibility() {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void onWindowSystemUiVisibilityChanged(int visible) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void dispatchWindowSystemUiVisiblityChanged(int visible) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void setOnSystemUiVisibilityChangeListener(View.OnSystemUiVisibilityChangeListener l) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public void dispatchSystemUiVisibilityChanged(int visibility) {
        throw new RuntimeException("Stub!");
    }

    /** @deprecated */
    @Deprecated
    public final boolean startDrag(ClipData data, View.DragShadowBuilder shadowBuilder, Object myLocalState, int flags) {
        throw new RuntimeException("Stub!");
    }

    public final boolean startDragAndDrop(ClipData data, View.DragShadowBuilder shadowBuilder, Object myLocalState, int flags) {
        throw new RuntimeException("Stub!");
    }

    public final void cancelDragAndDrop() {
        throw new RuntimeException("Stub!");
    }

    public final void updateDragShadow(View.DragShadowBuilder shadowBuilder) {
        throw new RuntimeException("Stub!");
    }

    public boolean onDragEvent(DragEvent event) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchDragEvent(DragEvent event) {
        throw new RuntimeException("Stub!");
    }

    public static View inflate(Context context, int resource, ViewGroup root) {
        throw new RuntimeException("Stub!");
    }

    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        throw new RuntimeException("Stub!");
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        throw new RuntimeException("Stub!");
    }

    public int getOverScrollMode() {
        throw new RuntimeException("Stub!");
    }

    public void setOverScrollMode(int overScrollMode) {
        throw new RuntimeException("Stub!");
    }

    public void setNestedScrollingEnabled(boolean enabled) {
        throw new RuntimeException("Stub!");
    }

    public boolean isNestedScrollingEnabled() {
        throw new RuntimeException("Stub!");
    }

    public boolean startNestedScroll(int axes) {
        throw new RuntimeException("Stub!");
    }

    public void stopNestedScroll() {
        throw new RuntimeException("Stub!");
    }

    public boolean hasNestedScrollingParent() {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        throw new RuntimeException("Stub!");
    }

    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        throw new RuntimeException("Stub!");
    }

    public void setTextDirection(int textDirection) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "text",
        mapping = {@IntToString(
    from = 0,
    to = "INHERIT"
), @IntToString(
    from = 1,
    to = "FIRST_STRONG"
), @IntToString(
    from = 2,
    to = "ANY_RTL"
), @IntToString(
    from = 3,
    to = "LTR"
), @IntToString(
    from = 4,
    to = "RTL"
), @IntToString(
    from = 5,
    to = "LOCALE"
), @IntToString(
    from = 6,
    to = "FIRST_STRONG_LTR"
), @IntToString(
    from = 7,
    to = "FIRST_STRONG_RTL"
)}
    )
    public int getTextDirection() {
        throw new RuntimeException("Stub!");
    }

    public boolean canResolveTextDirection() {
        throw new RuntimeException("Stub!");
    }

    public boolean isTextDirectionResolved() {
        throw new RuntimeException("Stub!");
    }

    public void setTextAlignment(int textAlignment) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty(
        category = "text",
        mapping = {@IntToString(
    from = 0,
    to = "INHERIT"
), @IntToString(
    from = 1,
    to = "GRAVITY"
), @IntToString(
    from = 2,
    to = "TEXT_START"
), @IntToString(
    from = 3,
    to = "TEXT_END"
), @IntToString(
    from = 4,
    to = "CENTER"
), @IntToString(
    from = 5,
    to = "VIEW_START"
), @IntToString(
    from = 6,
    to = "VIEW_END"
)}
    )
    public int getTextAlignment() {
        throw new RuntimeException("Stub!");
    }

    public boolean canResolveTextAlignment() {
        throw new RuntimeException("Stub!");
    }

    public boolean isTextAlignmentResolved() {
        throw new RuntimeException("Stub!");
    }

    public static int generateViewId() {
        throw new RuntimeException("Stub!");
    }

    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex) {
        throw new RuntimeException("Stub!");
    }

    public void setPointerIcon(PointerIcon pointerIcon) {
        throw new RuntimeException("Stub!");
    }

    public PointerIcon getPointerIcon() {
        throw new RuntimeException("Stub!");
    }

    public boolean hasPointerCapture() {
        throw new RuntimeException("Stub!");
    }

    public void requestPointerCapture() {
        throw new RuntimeException("Stub!");
    }

    public void releasePointerCapture() {
        throw new RuntimeException("Stub!");
    }

    public void onPointerCaptureChange(boolean hasCapture) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchPointerCaptureChanged(boolean hasCapture) {
        throw new RuntimeException("Stub!");
    }

    public boolean onCapturedPointerEvent(MotionEvent event) {
        throw new RuntimeException("Stub!");
    }

    public void setOnCapturedPointerListener(View.OnCapturedPointerListener l) {
        throw new RuntimeException("Stub!");
    }

    public ViewPropertyAnimator animate() {
        throw new RuntimeException("Stub!");
    }

    public final void setTransitionName(String transitionName) {
        throw new RuntimeException("Stub!");
    }

    @ExportedProperty
    public String getTransitionName() {
        throw new RuntimeException("Stub!");
    }

    public void setTooltipText(@Nullable CharSequence tooltipText) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public CharSequence getTooltipText() {
        throw new RuntimeException("Stub!");
    }

    public void addOnUnhandledKeyEventListener(View.OnUnhandledKeyEventListener listener) {
        throw new RuntimeException("Stub!");
    }

    public void removeOnUnhandledKeyEventListener(View.OnUnhandledKeyEventListener listener) {
        throw new RuntimeException("Stub!");
    }

    public interface OnUnhandledKeyEventListener {
        boolean onUnhandledKeyEvent(View var1, KeyEvent var2);
    }

    public interface OnTouchListener {
        boolean onTouch(View var1, MotionEvent var2);
    }

    /** @deprecated */
    @Deprecated
    public interface OnSystemUiVisibilityChangeListener {
        /** @deprecated */
        @Deprecated
        void onSystemUiVisibilityChange(int var1);
    }

    public interface OnScrollChangeListener {
        void onScrollChange(View var1, int var2, int var3, int var4, int var5);
    }

    public interface OnLongClickListener {
        boolean onLongClick(View var1);
    }

    public interface OnLayoutChangeListener {
        void onLayoutChange(View var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9);
    }

    public interface OnKeyListener {
        boolean onKey(View var1, int var2, KeyEvent var3);
    }

    public interface OnHoverListener {
        boolean onHover(View var1, MotionEvent var2);
    }

    public interface OnGenericMotionListener {
        boolean onGenericMotion(View var1, MotionEvent var2);
    }

    public interface OnFocusChangeListener {
        void onFocusChange(View var1, boolean var2);
    }

    public interface OnDragListener {
        boolean onDrag(View var1, DragEvent var2);
    }

    public interface OnCreateContextMenuListener {
        void onCreateContextMenu(ContextMenu var1, View var2, ContextMenuInfo var3);
    }

    public interface OnContextClickListener {
        boolean onContextClick(View var1);
    }

    public interface OnClickListener {
        void onClick(View var1);
    }

    public interface OnCapturedPointerListener {
        boolean onCapturedPointer(View var1, MotionEvent var2);
    }

    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View var1);

        void onViewDetachedFromWindow(View var1);
    }

    public interface OnApplyWindowInsetsListener {
        WindowInsets onApplyWindowInsets(View var1, WindowInsets var2);
    }

    public static class MeasureSpec {
        public static final int AT_MOST = -2147483648;
        public static final int EXACTLY = 1073741824;
        public static final int UNSPECIFIED = 0;

        public MeasureSpec() {
            throw new RuntimeException("Stub!");
        }

        public static int makeMeasureSpec(int size, int mode) {
            throw new RuntimeException("Stub!");
        }

        public static int getMode(int measureSpec) {
            throw new RuntimeException("Stub!");
        }

        public static int getSize(int measureSpec) {
            throw new RuntimeException("Stub!");
        }

        public static String toString(int measureSpec) {
            throw new RuntimeException("Stub!");
        }
    }

    public static class DragShadowBuilder {
        public DragShadowBuilder(View view) {
            throw new RuntimeException("Stub!");
        }

        public DragShadowBuilder() {
            throw new RuntimeException("Stub!");
        }

        public final View getView() {
            throw new RuntimeException("Stub!");
        }

        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            throw new RuntimeException("Stub!");
        }

        public void onDrawShadow(Canvas canvas) {
            throw new RuntimeException("Stub!");
        }
    }

    public static class BaseSavedState extends AbsSavedState {
        @NonNull
        public static final Creator<View.BaseSavedState> CREATOR = null;

        public BaseSavedState(Parcel source) {
            super((Parcelable)null);
            throw new RuntimeException("Stub!");
        }

        public BaseSavedState(Parcel source, ClassLoader loader) {
            super((Parcelable)null);
            throw new RuntimeException("Stub!");
        }

        public BaseSavedState(Parcelable superState) {
            super((Parcelable)null);
            throw new RuntimeException("Stub!");
        }

        public void writeToParcel(Parcel out, int flags) {
            throw new RuntimeException("Stub!");
        }
    }

    public static class AccessibilityDelegate {
        public AccessibilityDelegate() {
            throw new RuntimeException("Stub!");
        }

        public void sendAccessibilityEvent(View host, int eventType) {
            throw new RuntimeException("Stub!");
        }

        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            throw new RuntimeException("Stub!");
        }

        public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
            throw new RuntimeException("Stub!");
        }

        public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            throw new RuntimeException("Stub!");
        }

        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            throw new RuntimeException("Stub!");
        }

        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            throw new RuntimeException("Stub!");
        }

        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            throw new RuntimeException("Stub!");
        }

        public void addExtraDataToAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfo info, @NonNull String extraDataKey, @Nullable Bundle arguments) {
            throw new RuntimeException("Stub!");
        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
            throw new RuntimeException("Stub!");
        }

        public AccessibilityNodeProvider getAccessibilityNodeProvider(View host) {
            throw new RuntimeException("Stub!");
        }
    }
}
