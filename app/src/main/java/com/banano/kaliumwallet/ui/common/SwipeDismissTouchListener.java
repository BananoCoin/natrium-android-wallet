package com.banano.kaliumwallet.ui.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

public class SwipeDismissTouchListener implements View.OnTouchListener {
    public static final int TOP_TO_BOTTOM = 1;
    public static final int LEFT_TO_RIGHT = 2;

    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private View mView;
    private DismissCallbacks mCallbacks;
    private int mViewHeight = 1; // 1 and not 0 to prevent dividing by zero
    private int mViewWidth = 1;
    private int mDirection;

    // Transient properties
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private Object mToken;
    private VelocityTracker mVelocityTracker;
    private float mTranslationY;
    private float mTranslationX;

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given view.
     *
     * @param view      The view to make dismissable.
     * @param token     An optional token/cookie object to be passed through to the callback.
     * @param callbacks The callback to trigger when the user has indicated that she would like to
     *                  dismiss this view.
     * @param direction The direction to swipe
     */
    public SwipeDismissTouchListener(View view, Object token, DismissCallbacks callbacks, int direction) {
        ViewConfiguration vc = ViewConfiguration.get(view.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 8;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = 150; //view.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        mView = view;
        mToken = token;
        mCallbacks = callbacks;
        if (direction != TOP_TO_BOTTOM && direction != LEFT_TO_RIGHT) {
            mDirection = TOP_TO_BOTTOM;
        } else {
            mDirection = direction;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // offset because the view is translated during swipe
        if (mDirection == TOP_TO_BOTTOM) {
            motionEvent.offsetLocation(mTranslationY, 0);
        } else {
            motionEvent.offsetLocation(mTranslationX, 0);
        }

        mCallbacks.onTap(view);

        if (mViewHeight < 2) {
            mViewHeight = mView.getHeight();
        }
        if (mViewWidth < 2) {
            mViewWidth = mView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mDownX = motionEvent.getRawX();
                mDownY = motionEvent.getRawY();
                if (mCallbacks.canDismiss(mToken)) {
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(motionEvent);
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaY = motionEvent.getRawY() - mDownY;
                float deltaX = motionEvent.getRawX() - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityY = mVelocityTracker.getYVelocity();
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityY = Math.abs(velocityY);
                float absVelocityX = Math.abs(velocityX);
                boolean dismiss = false;
                if (mDirection == TOP_TO_BOTTOM) {
                    if (deltaY > 0 && Math.abs(deltaY) > mViewHeight / 5 && mSwiping) {
                        dismiss = true;
                    } else if (absVelocityX < absVelocityY
                            && absVelocityX < absVelocityY && mSwiping) {
                        // dismiss only if flinging down
                        dismiss = mVelocityTracker.getYVelocity() > 100;
                    }
                } else {
                    if (deltaX < 0 && Math.abs(deltaX) > mViewWidth / 5 && mSwiping) {
                        dismiss = true;
                    } else if (absVelocityY < absVelocityX
                            && absVelocityY < absVelocityX && mSwiping) {
                        dismiss = mVelocityTracker.getXVelocity() < -50;
                    }
                }
                if (dismiss) {
                    // dismiss
                    if (mDirection == TOP_TO_BOTTOM) {
                        mView.animate()
                                .translationY(mViewHeight)
                                .alpha(0)
                                .setDuration(mAnimationTime)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        performDismiss();
                                    }
                                });
                    } else {
                        mView.animate()
                                .translationX(-mViewWidth)
                                .alpha(0)
                                .setDuration(mAnimationTime)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        performDismiss();
                                    }
                                });
                    }
                    return true;
                } else if (mSwiping) {
                    // cancel
                    if (mDirection == TOP_TO_BOTTOM) {
                        mView.animate()
                                .translationY(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    } else {
                        mView.animate()
                                .translationX(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    }
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mTranslationY = 0;
                mTranslationX = 0;
                mDownX = 0;
                mDownY = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                if (mDirection == TOP_TO_BOTTOM) {
                    mView.animate()
                            .translationY(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                } else {
                    mView.animate()
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mTranslationY = 0;
                mTranslationX = 0;
                mDownX = 0;
                mDownY = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                if (mDirection == TOP_TO_BOTTOM) {
                    if (Math.abs(deltaY) > mSlop && Math.abs(deltaX) < Math.abs(deltaY) / 2) {
                        mSwiping = true;
                        mSwipingSlop = mSlop;
                        mView.getParent().requestDisallowInterceptTouchEvent(true);

                        // Cancel listview's touch
                        MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                                (motionEvent.getActionIndex() <<
                                        MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        mView.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    }

                    if (mSwiping && deltaY >= 0) {
                        mTranslationY = deltaY;
                        mView.setTranslationY(deltaY - mSwipingSlop);
                        // TODO: use an ease-out interpolator or such
                        mView.setAlpha(Math.max(0f, Math.min(1f,
                                1f - 2f * Math.abs(deltaY) / mViewHeight)));
                        return true;
                    }
                } else {
                    if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                        mSwiping = true;
                        mSwipingSlop = mSlop;
                        mView.getParent().requestDisallowInterceptTouchEvent(true);

                        // Cancel listview's touch
                        MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                                (motionEvent.getActionIndex() <<
                                        MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        mView.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    }

                    if (mSwiping && deltaX <= 0) {
                        mTranslationX = deltaX;
                        mView.setTranslationX(deltaX - mSwipingSlop);
                        // TODO: use an ease-out interpolator or such
                        mView.setAlpha(Math.max(0f, Math.min(1f,
                                1f - 2f * Math.abs(deltaX) / mViewWidth)));
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    private void performDismiss() {
        mCallbacks.onDismiss(mView, mToken);
    }

    /**
     * The callback interface used by {@link SwipeDismissTouchListener} to inform its client
     * about a successful dismissal of the view for which it was created.
     */
    public interface DismissCallbacks {
        /**
         * Called to determine whether the view can be dismissed.
         */
        boolean canDismiss(Object token);

        /**
         * Called when the user has indicated they she would like to dismiss the view.
         *
         * @param view  The originating {@link View} to be dismissed.
         * @param token The optional token passed to this object's constructor.
         */
        void onDismiss(View view, Object token);

        /**
         * Called when the user has tapped anywhere in the view.
         *
         * @param view The originating {@link View}
         */
        void onTap(View view);
    }
}