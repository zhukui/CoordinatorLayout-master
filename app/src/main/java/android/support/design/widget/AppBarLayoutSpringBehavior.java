package android.support.design.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.List;

import static android.support.v4.view.ViewCompat.TYPE_NON_TOUCH;

/**
 * AppBarLayout下拉弹簧形变
 */
public class AppBarLayoutSpringBehavior extends AppBarLayout.Behavior {

    /**
     * 形变偏移回调
     */
    public interface SpringOffsetCallback {
        void springCallback(int offset);
    }

    //插值偏移
    private int mOffsetDelta;
    //滑动偏移量
    private int mOffsetSpring;

    //恢复动画
    private ValueAnimator mSpringRecoverAnimator;
    //滑动时动画
    private ValueAnimator mFlingAnimator;
    //头部高度
    private int mPreHeadHeight;
    //下拉滑动便宜量
    private SpringOffsetCallback mSpringOffsetCallback;

    public AppBarLayoutSpringBehavior() {
    }

    public AppBarLayoutSpringBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
        final boolean started = super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type);
        if (started && mSpringRecoverAnimator != null && mSpringRecoverAnimator.isRunning()) {
            mSpringRecoverAnimator.cancel();
        }
        resetFlingAnimator();
        return started;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (dyUnconsumed < 0) {
            setHeaderTopBottomOffset(coordinatorLayout, child,
                    getTopBottomOffsetForScrollingSibling() - dyUnconsumed, -child.getDownNestedScrollRange(), 0, type);
        }
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl, View target, int type) {
        super.onStopNestedScroll(coordinatorLayout, abl, target, type);

        if (type == TYPE_NON_TOUCH) {
            resetFlingAnimator();
        }
        checkShouldSpringRecover(coordinatorLayout, abl);
    }


    /**
     * 检查是否重置动画
     */
    private void checkShouldSpringRecover(CoordinatorLayout coordinatorLayout, AppBarLayout abl) {
        if (mOffsetSpring > 0) {
            animateRecoverBySpring(coordinatorLayout, abl);
        }
    }

    /**
     * 下拉形变动画
     */
    private void animateFlingSpring(final CoordinatorLayout coordinatorLayout, final AppBarLayout abl, int originNew) {
        if (mFlingAnimator == null) {
            mFlingAnimator = new ValueAnimator();
            mFlingAnimator.setDuration(200);
            mFlingAnimator.setInterpolator(new DecelerateInterpolator());
            mFlingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    updateSpringHeaderHeight(coordinatorLayout, abl, (int) animation.getAnimatedValue());
                }
            });
            mFlingAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    checkShouldSpringRecover(coordinatorLayout, abl);
                }
            });
        } else {
            if (mFlingAnimator.isRunning()) {
                mFlingAnimator.cancel();
            }
        }
        mFlingAnimator.setIntValues(mOffsetSpring, Math.min(mPreHeadHeight * 3 / 2, originNew));
        mFlingAnimator.start();
    }

    /**
     * 重置动画
     */
    private void resetFlingAnimator() {
        if (mFlingAnimator != null) {
            if (mFlingAnimator.isRunning()) {
                mFlingAnimator.cancel();
            }
            mFlingAnimator = null;
        }
    }

    /**
     * 恢复动画
     */
    private void animateRecoverBySpring(final CoordinatorLayout coordinatorLayout, final AppBarLayout abl) {
        if (mSpringRecoverAnimator == null) {
            mSpringRecoverAnimator = new ValueAnimator();
            mSpringRecoverAnimator.setDuration(200);
            mSpringRecoverAnimator.setInterpolator(new DecelerateInterpolator());
            mSpringRecoverAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    updateSpringHeaderHeight(coordinatorLayout, abl, (int) animation.getAnimatedValue());
                }
            });
        } else {
            if (mSpringRecoverAnimator.isRunning()) {
                mSpringRecoverAnimator.cancel();
            }
        }
        mSpringRecoverAnimator.setIntValues(mOffsetSpring, 0);
        mSpringRecoverAnimator.start();
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, AppBarLayout child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        boolean b = super.onMeasureChild(parent, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
        if (mPreHeadHeight == 0 && child.getHeight() != 0) {
            mPreHeadHeight = getHeaderExpandedHeight(child);
        }
        return b;
    }

    /**
     * appBarLayout变化高度
     *
     * @param appBarLayout
     * @return
     */
    int getHeaderExpandedHeight(AppBarLayout appBarLayout) {
        int range = 0;
        for (int i = 0, z = appBarLayout.getChildCount(); i < z; i++) {
            final View child = appBarLayout.getChildAt(i);
            final AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) child.getLayoutParams();
            int childHeight = child.getMeasuredHeight();
            childHeight += lp.topMargin + lp.bottomMargin;
            range += childHeight;
        }
        return Math.max(0, range);
    }

    @Override
    void onFlingFinished(CoordinatorLayout parent, AppBarLayout layout) {
        super.onFlingFinished(parent, layout);
        animateRecoverBySpring(parent, layout);
    }

    @Override
    int setHeaderTopBottomOffset(CoordinatorLayout coordinatorLayout,
                                 AppBarLayout appBarLayout, int newOffset, int minOffset, int maxOffset) {
        return setHeaderTopBottomOffset(coordinatorLayout, appBarLayout, newOffset, minOffset, maxOffset, -1);
    }

    /**
     * 更新滚动
     */
    private int updateSpringByScroll(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, int type, int originNew) {
        int consumed;
        if (appBarLayout.getHeight() >= mPreHeadHeight && type == 1) {
            if (mFlingAnimator == null) {
                animateFlingSpring(coordinatorLayout, appBarLayout, originNew);
            }
            return originNew;
        }
        updateSpringOffsetByscroll(coordinatorLayout, appBarLayout, mOffsetSpring + originNew / 3);
        consumed = getTopBottomOffsetForScrollingSibling() - originNew;

        return consumed;
    }

    @Override
    int getTopBottomOffsetForScrollingSibling() {
        return getTopAndBottomOffset() + mOffsetDelta;
    }

    /**
     * 下拉等操作更改头部高度
     */
    private void updateSpringOffsetByscroll(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, int offset) {
        if (mSpringRecoverAnimator != null && mSpringRecoverAnimator.isRunning()) {
            mSpringRecoverAnimator.cancel();
        }
        updateSpringHeaderHeight(coordinatorLayout, appBarLayout, offset);
    }

    /**
     * 改变头部距离
     *
     * @param coordinatorLayout
     * @param appBarLayout
     * @param offset
     */
    private void updateSpringHeaderHeight(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, int offset) {
        if (appBarLayout.getHeight() < mPreHeadHeight || offset < 0) return;
        mOffsetSpring = offset;
        if (mSpringOffsetCallback != null) {
            mSpringOffsetCallback.springCallback(mOffsetSpring);
        }
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        layoutParams.height = mPreHeadHeight + offset;
        appBarLayout.setLayoutParams(layoutParams);
        coordinatorLayout.dispatchDependentViewsChanged(appBarLayout);
    }

    public int getOffsetSpring() {
        return mOffsetSpring;
    }

    public SpringOffsetCallback getSpringOffsetCallback() {
        return mSpringOffsetCallback;
    }

    public void setSpringOffsetCallback(SpringOffsetCallback springOffsetCallback) {
        mSpringOffsetCallback = springOffsetCallback;
    }

    //=================以下AppBarLayout源码部分，由于private只能复制方法修改，大神厉害===================

    /**
     * AppBarLayout源码，修改其高度、偏移等
     */
    int setHeaderTopBottomOffset(CoordinatorLayout coordinatorLayout,
                                 AppBarLayout appBarLayout, int newOffset, int minOffset, int maxOffset, int type) {
        int originNew = newOffset;
        final int curOffset = getTopBottomOffsetForScrollingSibling();
        int consumed = 0;
        if (mOffsetSpring != 0 && newOffset < 0) {
            int newSpringOffset = mOffsetSpring + originNew;
            if (newSpringOffset < 0) {
                newOffset = newSpringOffset;
                newSpringOffset = 0;
            }
            updateSpringOffsetByscroll(coordinatorLayout, appBarLayout, newSpringOffset);
            consumed = getTopBottomOffsetForScrollingSibling() - originNew;
            if (newSpringOffset >= 0)
                return consumed;
        }

        if (mOffsetSpring > 0 && appBarLayout.getHeight() >= mPreHeadHeight && newOffset > 0) {
            consumed = updateSpringByScroll(coordinatorLayout, appBarLayout, type, originNew);
            return consumed;
        }

        if (minOffset != 0 && curOffset >= minOffset && curOffset <= maxOffset) {
            newOffset = clamp(newOffset, minOffset, maxOffset);
            if (curOffset != newOffset) {
                final int interpolatedOffset = appBarLayout.hasChildWithInterpolator()
                        ? interpolateOffset(appBarLayout, newOffset)
                        : newOffset;

                final boolean offsetChanged = setTopAndBottomOffset(interpolatedOffset);
                consumed = curOffset - newOffset;
                mOffsetDelta = newOffset - interpolatedOffset;
                if (!offsetChanged && appBarLayout.hasChildWithInterpolator()) {
                    coordinatorLayout.dispatchDependentViewsChanged(appBarLayout);
                }
                appBarLayout.dispatchOffsetUpdates(getTopAndBottomOffset());
                updateAppBarLayoutDrawableState(coordinatorLayout, appBarLayout, newOffset,
                        newOffset < curOffset ? -1 : 1, false);
            } else if (curOffset != minOffset) {
                consumed = updateSpringByScroll(coordinatorLayout, appBarLayout, type, originNew);
            }
        } else {
            mOffsetDelta = 0;
        }
        return consumed;
    }

    private int interpolateOffset(AppBarLayout layout, final int offset) {
        final int absOffset = Math.abs(offset);

        for (int i = 0, z = layout.getChildCount(); i < z; i++) {
            final View child = layout.getChildAt(i);
            final AppBarLayout.LayoutParams childLp = (AppBarLayout.LayoutParams) child.getLayoutParams();
            final Interpolator interpolator = childLp.getScrollInterpolator();

            if (absOffset >= child.getTop() && absOffset <= child.getBottom()) {
                if (interpolator != null) {
                    int childScrollableHeight = 0;
                    final int flags = childLp.getScrollFlags();
                    if ((flags & AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                        // We're set to scroll so add the child's height plus margin
                        childScrollableHeight += child.getHeight() + childLp.topMargin
                                + childLp.bottomMargin;

                        if ((flags & AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                            // For a collapsing scroll, we to take the collapsed height
                            // into account.
                            childScrollableHeight -= ViewCompat.getMinimumHeight(child);
                        }
                    }

                    if (ViewCompat.getFitsSystemWindows(child)) {
                        childScrollableHeight -= layout.getTopInset();
                    }

                    if (childScrollableHeight > 0) {
                        final int offsetForView = absOffset - child.getTop();
                        final int interpolatedDiff = Math.round(childScrollableHeight *
                                interpolator.getInterpolation(
                                        offsetForView / (float) childScrollableHeight));

                        return Integer.signum(offset) * (child.getTop() + interpolatedDiff);
                    }
                }

                // If we get to here then the view on the offset isn't suitable for interpolated
                // scrolling. So break out of the loop
                break;
            }
        }

        return offset;
    }

    private boolean shouldJumpElevationState(CoordinatorLayout parent, AppBarLayout layout) {
        // We should jump the elevated state if we have a dependent scrolling view which has
        // an overlapping top (i.e. overlaps us)
        final List<View> dependencies = parent.getDependents(layout);
        for (int i = 0, size = dependencies.size(); i < size; i++) {
            final View dependency = dependencies.get(i);
            final CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) dependency.getLayoutParams();
            final CoordinatorLayout.Behavior behavior = lp.getBehavior();

            if (behavior instanceof AppBarLayout.ScrollingViewBehavior) {
                return ((AppBarLayout.ScrollingViewBehavior) behavior).getOverlayTop() != 0;
            }
        }
        return false;
    }

    private static View getAppBarChildOnOffset(final AppBarLayout layout, final int offset) {
        final int absOffset = Math.abs(offset);
        for (int i = 0, z = layout.getChildCount(); i < z; i++) {
            final View child = layout.getChildAt(i);
            if (absOffset >= child.getTop() && absOffset <= child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    private void updateAppBarLayoutDrawableState(final CoordinatorLayout parent,
                                                 final AppBarLayout layout, final int offset, final int direction,
                                                 final boolean forceJump) {
        final View child = getAppBarChildOnOffset(layout, offset);
        if (child != null) {
            final AppBarLayout.LayoutParams childLp = (AppBarLayout.LayoutParams) child.getLayoutParams();
            final int flags = childLp.getScrollFlags();
            boolean collapsed = false;

            if ((flags & AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL) != 0) {
                final int minHeight = ViewCompat.getMinimumHeight(child);

                if (direction > 0 && (flags & (AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                        | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED)) != 0) {
                    // We're set to enter always collapsed so we are only collapsed when
                    // being scrolled down, and in a collapsed offset
                    collapsed = -offset >= child.getBottom() - minHeight - layout.getTopInset();
                } else if ((flags & AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED) != 0) {
                    // We're set to exit until collapsed, so any offset which results in
                    // the minimum height (or less) being shown is collapsed
                    collapsed = -offset >= child.getBottom() - minHeight - layout.getTopInset();
                }
            }

            final boolean changed = layout.setCollapsedState(collapsed);

            if (Build.VERSION.SDK_INT >= 11 && (forceJump
                    || (changed && shouldJumpElevationState(parent, layout)))) {
                // If the collapsed state changed, we may need to
                // jump to the current state if we have an overlapping view
                layout.jumpDrawablesToCurrentState();
            }
        }
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }
}