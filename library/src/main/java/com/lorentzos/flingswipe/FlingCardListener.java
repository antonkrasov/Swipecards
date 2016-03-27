package com.lorentzos.flingswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.PointF;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dionysis_lorentzos on 5/8/14
 * for package com.lorentzos.swipecards
 * and project Swipe cards.
 * Use with caution dinausaurs might appear!
 */


public class FlingCardListener implements View.OnTouchListener {

    private static final String TAG = FlingCardListener.class.getSimpleName();
    private static final int INVALID_POINTER_ID = -1;

    private final float objectX;
    private final float objectY;
    private final int objectH;
    private final int objectW;
    private final int parentWidth;
    private final FlingListener mFlingListener;
    private final Object dataObject;
    private final float halfWidth;
    private float BASE_ROTATION_DEGREES;

    private float aPosX;
    private float aPosY;
    private float aDownTouchX;
    private float aDownTouchY;

    private List<Pair<Float, Float>> newScale = new ArrayList<>();
    private int topMargin;

    // The active pointer is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;
    private List<View> frames = null;

    private final int TOUCH_ABOVE = 0;
    private final int TOUCH_BELOW = 1;
    private int touchPosition;
    private final Object obj = new Object();
    private boolean isAnimationRunning = false;
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));

    public FlingCardListener(List<View> frames, List<Pair<Float, Float>> newScale, int topMargin, Object itemAtPosition, FlingListener flingListener) {
        this(frames, newScale, topMargin, itemAtPosition, 15f, flingListener);
    }

    public FlingCardListener(List<View> frames, List<Pair<Float, Float>> newScale, int topMargin, Object itemAtPosition, float rotation_degrees, FlingListener flingListener) {
        super();
        this.frames = new ArrayList<>(frames);
        this.newScale = new ArrayList<>(newScale);
        this.topMargin = topMargin;
        this.objectX = frames.get(0).getX();
        this.objectY = frames.get(0).getY();
        this.objectH = frames.get(0).getHeight();
        this.objectW = frames.get(0).getWidth();
        this.halfWidth = objectW / 2f;
        this.dataObject = itemAtPosition;
        this.parentWidth = ((ViewGroup) frames.get(0).getParent()).getWidth();
        this.BASE_ROTATION_DEGREES = rotation_degrees;
        this.mFlingListener = flingListener;
    }

    public boolean onTouch(View view, MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                // from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Save the ID of this pointer

                mActivePointerId = event.getPointerId(0);
                float x = 0;
                float y = 0;
                boolean success = false;
                try {
                    x = event.getX(mActivePointerId);
                    y = event.getY(mActivePointerId);
                    success = true;
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Exception in onTouch(view, event) : " + mActivePointerId, e);
                }
                if (success) {
                    // Remember where we started
                    aDownTouchX = x;
                    aDownTouchY = y;
                    //to prevent an initial jump of the magnifier, aposX and aPosY must
                    //have the values from the magnifier frame
                    if (aPosX == 0) {
                        aPosX = frames.get(0).getX();
                    }
                    if (aPosY == 0) {
                        aPosY = frames.get(0).getY();
                    }

                    if (y < objectH / 2) {
                        touchPosition = TOUCH_ABOVE;
                    } else {
                        touchPosition = TOUCH_BELOW;
                    }
                }

                view.getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                resetCardViewOnStack();
                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (event.getAction() &
                        MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            case MotionEvent.ACTION_MOVE:

                // Find the index of the active pointer and fetch its position
                final int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                final float xMove = event.getX(pointerIndexMove);
                final float yMove = event.getY(pointerIndexMove);

                //from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Calculate the distance moved
                final float dx = xMove - aDownTouchX;
                final float dy = yMove - aDownTouchY;


                // Move the frame
                aPosX += dx;
                aPosY += dy;

                // calculate the rotation degrees
                float distobjectX = aPosX - objectX;
                float rotation = BASE_ROTATION_DEGREES * 2.f * distobjectX / parentWidth;
                if (touchPosition == TOUCH_BELOW) {
                    rotation = -rotation;
                }

                //in this area would be code for doing something with the view as the frame moves.
                frames.get(0).setX(aPosX);
                frames.get(0).setY(aPosY);
                frames.get(0).setRotation(rotation);

                float scrollProgressPercent = getScrollProgressPercent();
                float absScrollProgressPercent = Math.abs(scrollProgressPercent);

                if (frames.get(1) != null) {
                    float scalingValueX = 1.0f - newScale.get(0).first;
                    float scalingValueY = 1.0f - newScale.get(0).second;

                    float currentScaleValueX = absScrollProgressPercent * scalingValueX;
                    float currentScaleValueY = absScrollProgressPercent * scalingValueY;

                    frames.get(1).setScaleX(newScale.get(0).first + currentScaleValueX);
                    frames.get(1).setScaleY(newScale.get(0).second + currentScaleValueY);

                    frames.get(1).setTranslationY(topMargin * absScrollProgressPercent);
                }

                if (frames.get(2) != null) {
                    float scalingValueX = newScale.get(0).first - newScale.get(1).first;
                    float scalingValueY = newScale.get(0).second - newScale.get(1).second;

                    float currentScaleValueX = absScrollProgressPercent * scalingValueX;
                    float currentScaleValueY = absScrollProgressPercent * scalingValueY;

                    frames.get(2).setScaleX(newScale.get(1).first + currentScaleValueX);
                    frames.get(2).setScaleY(newScale.get(1).second + currentScaleValueY);

                    frames.get(2).setTranslationY(topMargin * absScrollProgressPercent);
                }

                if (frames.get(3) != null) {
                    float scalingValueX = newScale.get(1).first - newScale.get(2).first;
                    float scalingValueY = newScale.get(1).second - newScale.get(2).second;

                    float currentScaleValueX = absScrollProgressPercent * scalingValueX;
                    float currentScaleValueY = absScrollProgressPercent * scalingValueY;

                    frames.get(3).setScaleX(newScale.get(2).first + currentScaleValueX);
                    frames.get(3).setScaleY(newScale.get(2).second + currentScaleValueY);

                    frames.get(3).setTranslationY(topMargin * absScrollProgressPercent);

                    frames.get(3).setAlpha(absScrollProgressPercent);
                }

                mFlingListener.onScroll(scrollProgressPercent);
                break;

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;
            }
        }

        return true;
    }

    private float getScrollProgressPercent() {
        if (movedBeyondLeftBorder()) {
            return -1f;
        } else if (movedBeyondRightBorder()) {
            return 1f;
        } else {
            float zeroToOneValue = (aPosX + halfWidth - leftBorder()) / (rightBorder() - leftBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }

    private boolean resetCardViewOnStack() {
        if (movedBeyondLeftBorder()) {
            // Left Swipe
            onSelected(true, getExitPoint(-objectW), 100);
            mFlingListener.onScroll(-1.0f);
        } else if (movedBeyondRightBorder()) {
            // Right Swipe
            onSelected(false, getExitPoint(parentWidth), 100);
            mFlingListener.onScroll(1.0f);
        } else {
            float abslMoveDistance = Math.abs(aPosX - objectX);
            aPosX = 0;
            aPosY = 0;
            aDownTouchX = 0;
            aDownTouchY = 0;
            frames.get(0).animate()
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .x(objectX)
                    .y(objectY)
                    .rotation(0);

            if (frames.get(1) != null) {
                frames.get(1).animate().scaleX(newScale.get(0).first).scaleY(newScale.get(0).second).translationY(0).setDuration(200).setInterpolator(new OvershootInterpolator(1.5f));
            }

            if (frames.get(2) != null) {
                frames.get(2).animate().scaleX(newScale.get(1).first).scaleY(newScale.get(1).second).translationY(0).setDuration(200).setInterpolator(new OvershootInterpolator(1.5f));
            }

            if (frames.get(3) != null) {
                frames.get(3).animate().scaleX(newScale.get(2).first).scaleY(newScale.get(2).second).alpha(0f).translationY(0).setDuration(200).setInterpolator(new OvershootInterpolator(1.5f));
            }

            mFlingListener.onScroll(0.0f);
            if (abslMoveDistance < 4.0) {
                mFlingListener.onClick(dataObject);
            }
        }
        return false;
    }

    private boolean movedBeyondLeftBorder() {
        return aPosX + halfWidth < leftBorder();
    }

    private boolean movedBeyondRightBorder() {
        return aPosX + halfWidth > rightBorder();
    }


    public float leftBorder() {
        return parentWidth / 4.f;
    }

    public float rightBorder() {
        return 3 * parentWidth / 4.f;
    }


    public void onSelected(final boolean isLeft,
                           float exitY, long duration) {

        isAnimationRunning = true;
        float exitX;
        if (isLeft) {
            exitX = -objectW - getRotationWidthOffset();
        } else {
            exitX = parentWidth + getRotationWidthOffset();
        }

        this.frames.get(0).animate()
                .setDuration(duration)
                .setInterpolator(new AccelerateInterpolator())
                .x(exitX)
                .y(exitY)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isLeft) {
                            mFlingListener.onCardExited();
                            mFlingListener.leftExit(dataObject);
                        } else {
                            mFlingListener.onCardExited();
                            mFlingListener.rightExit(dataObject);
                        }
                        isAnimationRunning = false;
                    }
                })
                .rotation(getExitRotation(isLeft));

        if (frames.get(1) != null) {
            frames.get(1).animate().scaleX(1.0f).scaleY(1.0f).translationY(topMargin).setDuration(duration).setInterpolator(new AccelerateInterpolator());
        }

        if (frames.get(2) != null) {
            frames.get(2).animate().scaleX(newScale.get(0).first).scaleY(newScale.get(0).second).translationY(topMargin).setDuration(duration).setInterpolator(new AccelerateInterpolator());
        }

        if (frames.get(3) != null) {
            frames.get(3).animate().scaleX(newScale.get(1).first).scaleY(newScale.get(1).second).translationY(topMargin).alpha(1f).setDuration(duration).setInterpolator(new AccelerateInterpolator());
        }
    }


    /**
     * Starts a default left exit animation.
     */
    public void selectLeft() {
        if (!isAnimationRunning)
            onSelected(true, objectY, 200);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRight() {
        if (!isAnimationRunning)
            onSelected(false, objectY, 200);
    }


    private float getExitPoint(int exitXPoint) {
        float[] x = new float[2];
        x[0] = objectX;
        x[1] = aPosX;

        float[] y = new float[2];
        y[0] = objectY;
        y[1] = aPosY;

        LinearRegression regression = new LinearRegression(x, y);

        //Your typical y = ax+b linear regression
        return (float) regression.slope() * exitXPoint + (float) regression.intercept();
    }

    private float getExitRotation(boolean isLeft) {
        float rotation = BASE_ROTATION_DEGREES * 2.f * (parentWidth - objectX) / parentWidth;
        if (touchPosition == TOUCH_BELOW) {
            rotation = -rotation;
        }
        if (isLeft) {
            rotation = -rotation;
        }
        return rotation;
    }


    /**
     * When the object rotates it's width becomes bigger.
     * The maximum width is at 45 degrees.
     * <p/>
     * The below method calculates the width offset of the rotation.
     */
    private float getRotationWidthOffset() {
        return objectW / MAX_COS - objectW;
    }


    public void setRotationDegrees(float degrees) {
        this.BASE_ROTATION_DEGREES = degrees;
    }

    public boolean isTouching() {
        return this.mActivePointerId != INVALID_POINTER_ID;
    }

    public PointF getLastPoint() {
        return new PointF(this.aPosX, this.aPosY);
    }

    protected interface FlingListener {
        void onCardExited();

        void leftExit(Object dataObject);

        void rightExit(Object dataObject);

        void onClick(Object dataObject);

        void onScroll(float scrollProgressPercent);
    }

}





