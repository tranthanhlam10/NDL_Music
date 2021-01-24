package com.naman14.timber.slidinguppanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.naman14.timber.R;

public class SlidingUpPanelLayout extends ViewGroup {

    private static final String TAG = SlidingUpPanelLayout.class.getSimpleName();


    private static final int DEFAULT_PANEL_HEIGHT = 68;


    private static final float DEFAULT_ANCHOR_POINT = 1.0f;

    private static final int DEFAULT_SHADOW_HEIGHT = 4;

    private static final int DEFAULT_FADE_COLOR = 0x99000000;

    private static final boolean DEFAULT_DRAG_VIEW_CLICKABLE = true;

    private static final int DEFAULT_MIN_FLING_VELOCITY = 400;

    private static final boolean DEFAULT_OVERLAY_FLAG = false;

    private static final int[] DEFAULT_ATTRS = new int[]{
            android.R.attr.gravity
    };

    private static final int DEFAULT_PARALAX_OFFSET = 0;

    private static final int DEFAULT_SLIDE_PANEL_OFFSET = 0;

    private static final boolean DEFAULT_DIRECT_OFFSET_FLAG = false;

    private static SlideState DEFAULT_SLIDE_STATE = SlideState.COLLAPSED;

    private final Paint mCoveredFadePaint = new Paint();

    private final Drawable mShadowDrawable;
    private final ViewDragHelper mDragHelper;
    private final Rect mTmpRect = new Rect();

    private int mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY;

    private int mCoveredFadeColor = DEFAULT_FADE_COLOR;

    private int mPanelHeight = -1;

    private int mSlidePanelOffset = 0;

    private int mShadowHeight = -1;

    private int mParallaxOffset = -1;

    private boolean mDirectOffset = false;

    private boolean mIsSlidingUp;

    private boolean mOverlayContent = DEFAULT_OVERLAY_FLAG;

    private View mDragView;

    private int mDragViewResId = -1;

    private boolean mDragViewClickable = DEFAULT_DRAG_VIEW_CLICKABLE;

    private View mSlideableView;

    private View mMainView;

    private View mBackgroundView;
    private SlideState mSlideState = SlideState.COLLAPSED;
    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = collapsed, 1 = expanded.
     */
    private float mSlideOffset;

    private int mSlideRange;

    private boolean mIsUnableToDrag;

    private boolean mIsSlidingEnabled;

    private boolean mIsUsingDragViewTouchEvents;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private float mAnchorPoint = 1.f;
    private PanelSlideListener mPanelSlideListener;

    private boolean mFirstLayout = true;

    public SlidingUpPanelLayout(Context context) {
        this(context, null);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            mShadowDrawable = null;
            mDragHelper = null;
            return;
        }

        if (attrs != null) {
            TypedArray defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS);

            if (defAttrs != null) {
                int gravity = defAttrs.getInt(0, Gravity.NO_GRAVITY);
                if (gravity != Gravity.TOP && gravity != Gravity.BOTTOM) {
                    throw new IllegalArgumentException("gravity must be set to either top or bottom");
                }
                mIsSlidingUp = gravity == Gravity.BOTTOM;
            }

            defAttrs.recycle();

            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpPanelLayout);

            if (ta != null) {
                mPanelHeight = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_panelHeight, -1);
                mSlidePanelOffset = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_slidePanelOffset, DEFAULT_SLIDE_PANEL_OFFSET);
                mShadowHeight = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_shadowHeight, -1);
                mParallaxOffset = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_paralaxOffset, -1);
                mDirectOffset = ta.getBoolean(R.styleable.SlidingUpPanelLayout_directOffset, DEFAULT_DIRECT_OFFSET_FLAG);

                mMinFlingVelocity = ta.getInt(R.styleable.SlidingUpPanelLayout_flingVelocity, DEFAULT_MIN_FLING_VELOCITY);
                mCoveredFadeColor = ta.getColor(R.styleable.SlidingUpPanelLayout_fadeColor, DEFAULT_FADE_COLOR);

                mDragViewResId = ta.getResourceId(R.styleable.SlidingUpPanelLayout_dragView, -1);
                mDragViewClickable = ta.getBoolean(R.styleable.SlidingUpPanelLayout_dragViewClickable, DEFAULT_DRAG_VIEW_CLICKABLE);

                mOverlayContent = ta.getBoolean(R.styleable.SlidingUpPanelLayout_overlay, DEFAULT_OVERLAY_FLAG);

                mAnchorPoint = ta.getFloat(R.styleable.SlidingUpPanelLayout_anchorPoint, DEFAULT_ANCHOR_POINT);

                mSlideState = SlideState.values()[ta.getInt(R.styleable.SlidingUpPanelLayout_initialState, DEFAULT_SLIDE_STATE.ordinal())];
            }

            ta.recycle();
        }

        final float density = context.getResources().getDisplayMetrics().density;
        if (mPanelHeight == -1) {
            mPanelHeight = (int) (DEFAULT_PANEL_HEIGHT * density + 0.5f);
        }
        if (mShadowHeight == -1) {
            mShadowHeight = (int) (DEFAULT_SHADOW_HEIGHT * density + 0.5f);
        }
        if (mParallaxOffset == -1) {
            mParallaxOffset = (int) (DEFAULT_PARALAX_OFFSET * density);
        }
        // If the shadow height is zero, don't show the shadow
        if (mShadowHeight > 0) {
            if (mIsSlidingUp) {
                mShadowDrawable = ContextCompat.getDrawable(context, R.drawable.above_shadow);
            } else {
                mShadowDrawable = ContextCompat.getDrawable(context, R.drawable.below_shadow);
            }

        } else {
            mShadowDrawable = null;
        }

        setWillNotDraw(false);

        mDragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        mDragHelper.setMinVelocity(mMinFlingVelocity * density);

        mIsSlidingEnabled = true;
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDragViewResId != -1) {
            setDragView(findViewById(mDragViewResId));
        }
    }


    public int getCoveredFadeColor() {
        return mCoveredFadeColor;
    }


    public void setCoveredFadeColor(int color) {
        mCoveredFadeColor = color;
        invalidate();
    }

    public boolean isSlidingEnabled() {
        return mIsSlidingEnabled && mSlideableView != null;
    }


    public void setSlidingEnabled(boolean enabled) {
        mIsSlidingEnabled = enabled;
    }


    public int getPanelHeight() {
        return mPanelHeight;
    }


    public void setPanelHeight(int val) {
        mPanelHeight = val;
        requestLayout();
    }


    public void setSlidePanelOffset(int val) {
        mSlidePanelOffset = val;
        requestLayout();
    }


    public int getCurrentParalaxOffset() {
        if (mParallaxOffset < 0) {
            return 0;
        }

        return (int) (mParallaxOffset * getDirectionalSlideOffset());
    }


    protected float getDirectionalSlideOffset() {
        return mIsSlidingUp ? -mSlideOffset : mSlideOffset;
    }


    public void setPanelSlideListener(PanelSlideListener listener) {
        mPanelSlideListener = listener;
    }


    public void setDragView(View dragView) {
        if (mDragView != null && mDragViewClickable) {
            mDragView.setOnClickListener(null);
        }
        mDragView = dragView;
        if (mDragView != null) {
            mDragView.setClickable(true);
            mDragView.setFocusable(false);
            mDragView.setFocusableInTouchMode(false);
            if (mDragViewClickable) {
                mDragView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isEnabled()) return;
                        if (!isPanelExpanded() && !isPanelAnchored()) {
                            expandPanel(mAnchorPoint);
                        } else {
                            collapsePanel();
                        }
                    }
                });
            }
        }
    }


    public float getAnchorPoint() {
        return mAnchorPoint;
    }


    public void setAnchorPoint(float anchorPoint) {
        if (anchorPoint > 0 && anchorPoint <= 1) {
            mAnchorPoint = anchorPoint;
        }
    }


    public boolean isOverlayed() {
        return mOverlayContent;
    }


    public void setOverlayed(boolean overlayed) {
        mOverlayContent = overlayed;
    }

    void dispatchOnPanelSlide(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelSlide(panel, mSlideOffset);
        }
    }

    void dispatchOnPanelExpanded(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelExpanded(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelCollapsed(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelCollapsed(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelAnchored(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelAnchored(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelHidden(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelHidden(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void updateObscuredViewVisibility() {
        if (getChildCount() == 0) {
            return;
        }
        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - getPaddingRight();
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (mSlideableView != null && hasOpaqueBackground(mSlideableView)) {
            left = mSlideableView.getLeft();
            right = mSlideableView.getRight();
            top = mSlideableView.getTop();
            bottom = mSlideableView.getBottom();
        } else {
            left = right = top = bottom = 0;
        }
        View child = mMainView;
        final int clampedChildLeft = Math.max(leftBound, child.getLeft());
        final int clampedChildTop = Math.max(topBound, child.getTop());
        final int clampedChildRight = Math.min(rightBound, child.getRight());
        final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
        final int vis;
        if (clampedChildLeft >= left && clampedChildTop >= top &&
                clampedChildRight <= right && clampedChildBottom <= bottom) {
            vis = INVISIBLE;
        } else {
            vis = VISIBLE;
        }
        child.setVisibility(vis);
    }

    void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }

        final int childCount = getChildCount();

        if (childCount != 2 && childCount != 3) {
            throw new IllegalStateException("Sliding up panel layout must have exactly 2 or 3 children!");
        }

        if (childCount == 2) {
            mMainView = getChildAt(0);
            mSlideableView = getChildAt(1);
        } else {
            mBackgroundView = getChildAt(0);
            mMainView = getChildAt(1);
            mSlideableView = getChildAt(2);
        }

        if (mDragView == null) {
            setDragView(mSlideableView);
        }


        if (mSlideableView.getVisibility() == GONE) {
            mSlideState = SlideState.HIDDEN;
        }

        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();


        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();


            if (child.getVisibility() == GONE && child == mMainView) {
                continue;
            }

            int height = layoutHeight;
            if (child == mMainView && !mOverlayContent && mSlideState != SlideState.HIDDEN) {
                height -= mPanelHeight;
            }

            int childWidthSpec;
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
            } else if (lp.height == LayoutParams.MATCH_PARENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }

            if (child == mSlideableView) {
                mSlideRange = MeasureSpec.getSize(childHeightSpec) - mPanelHeight + mSlidePanelOffset;
                childHeightSpec += mSlidePanelOffset;
            }

            child.measure(childWidthSpec, childHeightSpec);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();

        if (mFirstLayout) {
            switch (mSlideState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;
                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;
                case HIDDEN:
                    int newTop = computePanelTopPosition(0.0f) + (mIsSlidingUp ? +mPanelHeight : -mPanelHeight);
                    mSlideOffset = computeSlideOffset(newTop);
                    break;
                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);


            if (child.getVisibility() == GONE && (child == mMainView || mFirstLayout)) {
                continue;
            }

            final int childHeight = child.getMeasuredHeight();
            int childTop = paddingTop;

            if (child == mSlideableView) {
                childTop = computePanelTopPosition(mSlideOffset);
            }

            if (!mIsSlidingUp) {
                if (child == mMainView && !mOverlayContent) {
                    childTop = computePanelTopPosition(mSlideOffset) + mSlideableView.getMeasuredHeight();
                }
            }
            final int childBottom = childTop + childHeight;
            final int childLeft = paddingLeft;
            final int childRight = childLeft + child.getMeasuredWidth();

            child.layout(childLeft, childTop, childRight, childBottom);
        }

        if (mFirstLayout) {
            updateObscuredViewVisibility();
        }

        mFirstLayout = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (h != oldh) {
            mFirstLayout = true;
        }
    }


    public void setEnableDragViewTouchEvents(boolean enabled) {
        mIsUsingDragViewTouchEvents = enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            collapsePanel();
        }
        super.setEnabled(enabled);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);


        if (!isEnabled() || !mIsSlidingEnabled || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.cancel();
            return super.onInterceptTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final float adx = Math.abs(x - mInitialMotionX);
                final float ady = Math.abs(y - mInitialMotionY);
                final int dragSlop = mDragHelper.getTouchSlop();

                // Handle any horizontal scrolling on the drag view.
                if (mIsUsingDragViewTouchEvents && adx > dragSlop && ady < dragSlop) {
                    return super.onInterceptTouchEvent(ev);
                }

                if ((ady > dragSlop && adx > ady) || !isDragViewUnder((int) mInitialMotionX, (int) mInitialMotionY)) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }
        }

        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isSlidingEnabled()) {
            return super.onTouchEvent(ev);
        }
        mDragHelper.processTouchEvent(ev);
        return true;
    }

    private boolean isDragViewUnder(int x, int y) {
        if (mDragView == null) return false;
        int[] viewLocation = new int[2];
        mDragView.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + mDragView.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + mDragView.getHeight();
    }

    private boolean expandPanel(View pane, int initialVelocity, float mSlideOffset) {
        return mFirstLayout || smoothSlideTo(mSlideOffset, initialVelocity);
    }

    private boolean collapsePanel(View pane, int initialVelocity) {
        return mFirstLayout || smoothSlideTo(0.0f, initialVelocity);
    }


    private int computePanelTopPosition(float slideOffset) {
        int slidingViewHeight = mSlideableView != null ? mSlideableView.getMeasuredHeight() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);

        return mIsSlidingUp
                ? getMeasuredHeight() - getPaddingBottom() - mPanelHeight - slidePixelOffset
                : getPaddingTop() - slidingViewHeight + mPanelHeight + slidePixelOffset;
    }


    private float computeSlideOffset(int topPosition) {

        final int topBoundCollapsed = computePanelTopPosition(0);

        return (mIsSlidingUp
                ? (float) (topBoundCollapsed - topPosition) / mSlideRange
                : (float) (topPosition - topBoundCollapsed) / mSlideRange);
    }


    public boolean collapsePanel() {
        if (mFirstLayout) {
            mSlideState = SlideState.COLLAPSED;
            return true;
        } else {
            if (mSlideState == SlideState.HIDDEN || mSlideState == SlideState.COLLAPSED)
                return false;
            return collapsePanel(mSlideableView, 0);
        }
    }


    public boolean expandPanel() {
        if (mFirstLayout) {
            mSlideState = SlideState.EXPANDED;
            return true;
        } else {
            return expandPanel(1.0f);
        }
    }


    public boolean anchorPanel() {
        if (mFirstLayout) {
            mSlideState = SlideState.ANCHORED;
            return true;
        } else {
            return expandPanel(mAnchorPoint);
        }
    }


    public boolean expandPanel(float mSlideOffset) {
        if (mSlideableView == null || mSlideState == SlideState.EXPANDED) return false;
        mSlideableView.setVisibility(View.VISIBLE);
        return expandPanel(mSlideableView, 0, mSlideOffset);
    }


    public boolean isPanelExpanded() {
        return mSlideState == SlideState.EXPANDED;
    }


    public boolean isPanelAnchored() {
        return mSlideState == SlideState.ANCHORED;
    }


    public boolean isPanelHidden() {
        return mSlideState == SlideState.HIDDEN;
    }

    public void showPanel() {
        if (mFirstLayout) {
            mSlideState = SlideState.COLLAPSED;
        } else {
            if (mSlideableView == null || mSlideState != SlideState.HIDDEN) return;
            mSlideableView.setVisibility(View.VISIBLE);
            requestLayout();
            smoothSlideTo(0, 0);
        }
    }


    public void hidePanel() {
        if (mFirstLayout) {
            mSlideState = SlideState.HIDDEN;
        } else {
            if (mSlideState == SlideState.DRAGGING || mSlideState == SlideState.HIDDEN) return;
            int newTop = computePanelTopPosition(0.0f) + (mIsSlidingUp ? +mPanelHeight : -mPanelHeight);
            smoothSlideTo(computeSlideOffset(newTop), 0);
        }
    }

    @SuppressLint("NewApi")
    private void onPanelDragged(int newTop) {
        mSlideState = SlideState.DRAGGING;

        mSlideOffset = computeSlideOffset(newTop);

        if ((mParallaxOffset > 0 || mDirectOffset) && mSlideOffset >= 0) {
            int mainViewOffset = 0;
            if (mParallaxOffset > 0) {
                mainViewOffset = getCurrentParalaxOffset();
            } else {
                mainViewOffset = (int) (getDirectionalSlideOffset() * mSlideRange);
            }

            mMainView.setTranslationY(mainViewOffset);
        }


        dispatchOnPanelSlide(mSlideableView);

        if (mSlideOffset <= 0 && !mOverlayContent) {

            LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
            lp.height = mIsSlidingUp ? (newTop - getPaddingBottom()) : (getHeight() - getPaddingBottom() - mSlideableView.getMeasuredHeight() - newTop);
            mMainView.requestLayout();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result;
        final int save = canvas.save(Canvas.CLIP_SAVE_FLAG);

        if (isSlidingEnabled() && mMainView == child) {

            if (!mOverlayContent) {
                canvas.getClipBounds(mTmpRect);
                if (mIsSlidingUp) {
                    mTmpRect.bottom = Math.min(mTmpRect.bottom, mSlideableView.getTop());
                } else {
                    mTmpRect.top = Math.max(mTmpRect.top, mSlideableView.getBottom());
                }
                canvas.clipRect(mTmpRect);
            }
        }

        result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(save);

        if (mCoveredFadeColor != 0 && mSlideOffset > 0) {
            final int baseAlpha = (mCoveredFadeColor & 0xff000000) >>> 24;
            final int imag = (int) (baseAlpha * mSlideOffset);
            final int color = imag << 24 | (mCoveredFadeColor & 0xffffff);
            mCoveredFadePaint.setColor(color);
            canvas.drawRect(mTmpRect, mCoveredFadePaint);
        }

        return result;
    }


    boolean smoothSlideTo(float slideOffset, int velocity) {
        if (!isSlidingEnabled()) {
            // Nothing to do.
            return false;
        }

        int panelTop = computePanelTopPosition(slideOffset);
        if (mDragHelper.smoothSlideViewTo(mSlideableView, mSlideableView.getLeft(), panelTop)) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            if (!isSlidingEnabled()) {
                mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        if (!isSlidingEnabled()) {

            return;
        }

        final int right = mSlideableView.getRight();
        final int top;
        final int bottom;
        if (mIsSlidingUp) {
            top = mSlideableView.getTop() - mShadowHeight;
            bottom = mSlideableView.getTop();
        } else {
            top = mSlideableView.getBottom();
            bottom = mSlideableView.getBottom() + mShadowHeight;
        }
        final int left = mSlideableView.getLeft();

        if (mShadowDrawable != null) {
            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(c);
        }
    }


    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
                        y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
                        canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && ViewCompat.canScrollHorizontally(v, -dx);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.mSlideState = mSlideState;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mSlideState = ss.mSlideState;
    }


    private enum SlideState {
        EXPANDED,
        COLLAPSED,
        ANCHORED,
        HIDDEN,
        DRAGGING
    }


    public interface PanelSlideListener {

        void onPanelSlide(View panel, float slideOffset);


        void onPanelCollapsed(View panel);


        void onPanelExpanded(View panel);


        void onPanelAnchored(View panel);

        void onPanelHidden(View panel);
    }


    public static class SimplePanelSlideListener implements PanelSlideListener {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
        }

        @Override
        public void onPanelCollapsed(View panel) {
        }

        @Override
        public void onPanelExpanded(View panel) {
        }

        @Override
        public void onPanelAnchored(View panel) {
        }

        @Override
        public void onPanelHidden(View panel) {
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        private static final int[] ATTRS = new int[]{
                android.R.attr.layout_weight
        };

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
            a.recycle();
        }

    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        SlideState mSlideState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            try {
                mSlideState = Enum.valueOf(SlideState.class, in.readString());
            } catch (IllegalArgumentException e) {
                mSlideState = SlideState.COLLAPSED;
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(mSlideState.toString());
        }
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mIsUnableToDrag) {
                return false;
            }

            return child == mSlideableView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                mSlideOffset = computeSlideOffset(mSlideableView.getTop());

                if (mSlideOffset == 1) {
                    if (mSlideState != SlideState.EXPANDED) {
                        updateObscuredViewVisibility();
                        mSlideState = SlideState.EXPANDED;
                        dispatchOnPanelExpanded(mSlideableView);
                    }
                } else if (mSlideOffset == 0) {
                    if (mSlideState != SlideState.COLLAPSED) {
                        mSlideState = SlideState.COLLAPSED;
                        dispatchOnPanelCollapsed(mSlideableView);
                    }
                } else if (mSlideOffset < 0) {
                    mSlideState = SlideState.HIDDEN;
                    mSlideableView.setVisibility(View.GONE);
                    dispatchOnPanelHidden(mSlideableView);
                } else if (mSlideState != SlideState.ANCHORED) {
                    updateObscuredViewVisibility();
                    mSlideState = SlideState.ANCHORED;
                    dispatchOnPanelAnchored(mSlideableView);
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            onPanelDragged(top);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int target = 0;


            float direction = mIsSlidingUp ? -yvel : yvel;

            if (direction > 0) {

                target = computePanelTopPosition(1.0f);
            } else if (direction < 0) {

                target = computePanelTopPosition(0.0f);
            } else if (mAnchorPoint != 1 && mSlideOffset >= (1.f + mAnchorPoint) / 2) {

                target = computePanelTopPosition(1.0f);
            } else if (mAnchorPoint == 1 && mSlideOffset >= 0.5f) {

                target = computePanelTopPosition(1.0f);
            } else if (mAnchorPoint != 1 && mSlideOffset >= mAnchorPoint) {
                target = computePanelTopPosition(mAnchorPoint);
            } else if (mAnchorPoint != 1 && mSlideOffset >= mAnchorPoint / 2) {
                target = computePanelTopPosition(mAnchorPoint);
            } else {

                target = computePanelTopPosition(0.0f);
            }

            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), target);
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int collapsedTop = computePanelTopPosition(0.f);
            final int expandedTop = computePanelTopPosition(1.0f);
            if (mIsSlidingUp) {
                return Math.min(Math.max(top, expandedTop), collapsedTop);
            } else {
                return Math.min(Math.max(top, collapsedTop), expandedTop);
            }
        }
    }
}
