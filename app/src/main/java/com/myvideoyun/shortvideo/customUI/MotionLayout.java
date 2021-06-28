package com.myvideoyun.shortvideo.customUI;

import android.Manifest;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.myvideoyun.shortvideo.R;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

public class MotionLayout extends ViewGroup {

    private float downX;
    private float downY;
    private int lastTouchDistance;
    private long lastTouchTime;
    private float width;
    private float scale;
    private OnSlidingChildViewListener listener;

    public MotionLayout(Context context) {
        super(context);
        init(context);
    }

    public MotionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MotionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
//        LinearLayout
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int childCount = getChildCount();
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

//        for (int i = 0; i < childCount; i++ ) {
//            View child = getChildAt(i);
//            child.measure(widthMeasureSpec, heightMeasureSpec);
//        }
        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0),
                resolveSizeAndState(height, heightMeasureSpec, 0));
//        Log.e("moose", "motion layout , width = " + width + ", height = " + height);
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        int measuredHeight = getMeasuredHeight();
        int measuredWidth = getMeasuredWidth();
        for (int i = 0; i < childCount; i++ ){
            View child = getChildAt(i);
            MotionLayout.LayoutParams params = (LayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            int childLeft = (int) (params.percent_x * measuredWidth - childWidth / 2);
            int childTop = (int) (params.percent_y * measuredHeight - childHeight / 2);
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
//            Log.e("moose", "motion layout child view , "
//                    + ", left: " + childLeft + ", top: " + childTop + ", right: " + childWidth + ", bottom: " + childHeight);

        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new MotionLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f, 0.5f);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new ViewGroup.LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MotionLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * The percent percent_x of the child within the view group.
         */
        public float percent_x;
        /**
         * The percent percent_y of the child within the view group.
         */
        public float percent_y;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray t = c.obtainStyledAttributes(attrs, R.styleable.MotionLayout_layout);
            percent_x = t.getFloat(R.styleable.MotionLayout_layout_percent_x, 0.5f);
            percent_y = t.getFloat(R.styleable.MotionLayout_layout_percent_y, 0.5f);
            t.recycle();
        }

        public LayoutParams(int width, int height, float x, float y) {
            super(width, height);
            this.percent_x = x;
            this.percent_y = y;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    private View touchingView;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int childCount = getChildCount();
        if (childCount > 0){
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    lastTouchDistance = 0;
                    for (int i = childCount - 1; i >= 0 ; i--) {
                        View lastView = getChildAt(i);
                        int left = lastView.getLeft();
                        int top = lastView.getTop();
                        int right = lastView.getRight();
                        int bottom = lastView.getBottom();

                        if (downX >= left && downX <= right && downY >= top && downY <= bottom) {
                            touchingView = lastView;
                            if (i != childCount - 1) bringChildToFront(i);
                            break;
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (touchingView != null){
                        int right = touchingView.getRight();
                        int top = touchingView.getTop();
                        int left = touchingView.getLeft();
                        int bottom = touchingView.getBottom();
                        int moveX = (int) (event.getX() - downX);
                        int moveY = (int) (event.getY() - downY);
                        touchingView.layout((left + moveX), (top + moveY), (right + moveX), (bottom + moveY));
                        downX = event.getX();
                        downY = event.getY();


                        ViewGroup.LayoutParams params = touchingView.getLayoutParams();
                        int x = (int) (touchingView.getLeft() + touchingView.getMeasuredWidth() / 2 - touchingView.getScaleX() * params.width / 2);
                        int y = (int) (touchingView.getTop() + touchingView.getMeasuredHeight() / 2 - touchingView.getScaleX() * params.width / 2);
                        listener.slidingView(touchingView, views.get(touchingView), (int)(params.width * touchingView.getScaleX()), (int)(params.height * touchingView.getScaleX()), x, y);
                    }
                    if (event.getPointerCount() >=2){
                        float x0 = event.getX(0);
                        float x1 = event.getX(1);
                        float y0 = event.getY(0);
                        float y1 = event.getY(1);
                        int offsetX = (int) (x0 - x1);
                        int offsetY = (int) (y0 - y1);
                        int currentDistance = (int) Math.sqrt(offsetX * offsetX + offsetY * offsetY);

                        if (lastTouchDistance == 0){
                            lastTouchDistance = currentDistance;
                            break;
                        }

                        int touchScale = currentDistance - lastTouchDistance;
                        View view = getChildAt(childCount - 1);
                        int right = view.getRight();
                        int top = view.getTop();
                        int left = view.getLeft();
                        int bottom = view.getBottom();
                        if (left >= x0 && left <= x1 || left <= x0 && left >= x1
                            || right >= x0 && right <= x1 || right <= x0 && right >= x1
                                || top >= y0 && top <= y1 || top <= y0 && top >= y1
                                || bottom >= y0 && bottom <= y1 || bottom <= y0 && bottom >= y1){
                            zoomView(touchScale, view);
//                            lastTouchDistance = currentDistance;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
//                    Log.e("moose", "up or cancel");
                    lastTouchDistance = 0;
                    width = 0;
                    if (touchingView != null) {
                        ViewGroup.LayoutParams params = touchingView.getLayoutParams();
                        int x = (int) (touchingView.getLeft() + touchingView.getMeasuredWidth() / 2 - touchingView.getScaleX() * params.width / 2);
                        int y = (int) (touchingView.getTop() + touchingView.getMeasuredHeight() / 2 - touchingView.getScaleX() * params.width / 2);
                        if (listener != null)
                            listener.start(touchingView, views.get(touchingView), (int) (params.width * touchingView.getScaleX()), (int) (params.height * touchingView.getScaleX()), x, y);
                    }
                    touchingView = null;

                    break;
            }
        }
        return true;
    }

    private void zoomView(int touchScale, View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (width == 0) {
            width = params.width;
            scale = view.getScaleX();
//            Log.w("moose", "初始化：width= " + width + " , scale = " + scale);
            int x = (int) (view.getLeft() + view.getMeasuredWidth() / 2 - view.getScaleX() * params.width / 2);
            int y = (int) (view.getTop() + view.getMeasuredHeight() / 2 - view.getScaleX() * params.width / 2);
            if ( listener != null) listener.start(view, views.get(view), (int)(params.width * view.getScaleX()), (int)(params.height * view.getScaleX()), x, y);
        }
        float scaleX = (touchScale + width * scale) / width;
        scaleX = Math.max(0.1f, scaleX);
        scaleX = Math.min(4, scaleX);
        view.setScaleX(scaleX);
        view.setScaleY(scaleX);

        if (listener != null){

            int left = (int) (view.getLeft() + view.getMeasuredWidth() / 2 - scaleX * params.width / 2);
            int top = (int) (view.getTop() + view.getMeasuredHeight() / 2 - scaleX * params.width / 2);
            listener.slidingView(view, views.get(view), (int)(params.width * view.getScaleX()), (int)(params.height * view.getScaleX()), left, top);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return onTouchEvent(ev);
    }

    /**
     * 移动点击的view 到最上方，super.bringChildToFront() 会调用 requeslayout 重新layout ，显然不是我们想要的
     * @param index
     */
    public void bringChildToFront(int index) {
        int childCount = getChildCount();
        View[] children = new View[childCount];
        for (int i = 0; i< childCount; i++){
            if (index >= i){
                children[i] = getChildAt(i + 1);
            } else {
                children[i] = getChildAt(i);
            }
        }
        children[childCount - 1] = getChildAt(index);
        try {
            Class clazz = this.getClass().getSuperclass();
            Field mChildren = clazz.getDeclaredField("mChildren");
            mChildren.setAccessible(true);
            mChildren.set(this, children);
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HashMap<TextView, String> views = new HashMap<>(5);

    public void addView(TextView view, String path){
        addView(view);
        views.put(view, path);
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        views.remove(view);
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        views.clear();
    }

    public void setOnSlidingChhildViewListener(OnSlidingChildViewListener l){
        this.listener = l;
    }

    public interface OnSlidingChildViewListener{
        void slidingView(View view, String path, int width, int height, int x, int y);
        void start(View view, String path, int width, int height, int x, int y);
        void end(View view, String path, int width, int height, int x, int y);
    }
}
