package com.myvideoyun.shortvideo.customUI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.myvideoyun.shortvideo.R;

public class FrameHorizontalView extends View {

    private String TAG = "FrameHorizontalView";
    private Paint paint;
    private int measuredHeight;
    private int measuredWidth;
    private Rect lineRect;
    private float lastX;
    // 实际 left
//    private int realLeft;
    // 实际 right
//    private int realRight;
    private int start = 100;
    private int end;
    private Rect viewRect;
    private OnSlidingListener listener;

    //距离屏幕开始100位置的阈值
    private int startPointX = 100;
    //距离屏幕宽度100位置的阈值
    private int endPointX = 0;

    /**
     * 一个屏幕宽度代表多长的显示大小
     */
    private float showLength = 15000;
    private float maxLength  = 15000;

    public FrameHorizontalView(Context context) {
        super(context);
        initView(context);
    }

    public FrameHorizontalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public FrameHorizontalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        // init view
        paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.grayColor));
        lineRect = new Rect();
    }

    public void setValue(float showLength, float maxLength){
        this.showLength = showLength;
        this.maxLength = maxLength;

        int measuredWidth = getMeasuredWidth();
        if (measuredWidth > 0) {
            end = (int) (maxLength / showLength * measuredWidth);
            lineRect.right = end;
            Log.d(TAG, "set end=" + end);
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        measuredHeight = getMeasuredHeight();
        measuredWidth = getMeasuredWidth();
        start = 100;
//        end = measuredWidth * 2;
        endPointX = measuredWidth - 100;
        Log.e(TAG, "start 阈值：" + startPointX + "，end 阈值："+ endPointX);

        lineRect.left = start;
        lineRect.top = 0;
        lineRect.bottom = measuredHeight;
        viewRect = new Rect(0, 0, measuredWidth, measuredHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "horizontal view onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (end <= 0) {
            end = (int) (maxLength / showLength * (measuredWidth - startPointX));
            lineRect.right = end;
            Log.d(TAG, "set end=" + end);
        }
        canvas.clipRect(viewRect);
        canvas.drawRect(lineRect, paint);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean need = super.dispatchTouchEvent(event);
//        Log.d(TAG, "horizontal view dispatchTouchEvent need:" + need);
        return need;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        Log.i(TAG, "horizontal view onTouchEvent");
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
//                Log.d(TAG, "down horizontal view onTouchEvent");
                lastX = event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
//                Log.d(TAG, "move horizontal view gonTouchEvent");
                float curX = event.getX();
                float interval = curX - lastX;
                if (interval > 0){
                    // 大于0，向右滑动
                    // 如果矩形 left 大于等于0，停止向右滑动，并重置矩形 left = 0
                    if (lineRect.left >= start || lineRect.left + interval >= start){
                        lineRect.left = start;
//                        lineRect.right = end;
                        return false;// 不处理这个move 事件
                    }
                } else if (interval < 0){
                    // 小于0，向左滑动
                    // 如果矩形 right 小于等于 measureWidth，停止向左滑动，并重置矩形 right = measureWidth
                    if (lineRect.right <= measuredWidth - 100 || lineRect.right + interval <= measuredWidth - 1000){
//                        lineRect.left = 100;
                        lineRect.right = measuredWidth - 100;
                        return false;
                    }
                }
//                Log.e(TAG, "interval=" + interval);
                lineRect.left += interval;
                lineRect.right += interval;
                lastX = curX;
                invalidate();
                if (listener != null) listener.onSliding();
                return true;// use touch event
            case MotionEvent.ACTION_UP:
//                Log.d(TAG, "up horizontal view onTouchEvent");
                // 回调一下当前位置
                Log.d(TAG, "rect: " + lineRect);
                // todo 计算选中的两个点
                // 拿当前的 lineRect 的值与两个固定阈值作对比
//                Log.e(TAG, "start 阈值：" + startPointX + "，end 阈值："+ endPointX);
                if (listener != null) {
                    //                    w             w

                    float i = showLength / (measuredWidth - startPointX - startPointX );// 当前屏幕宽度- 200 后，对应的progress
                    float end = (endPointX - lineRect.left) * i;
                    float start = (startPointX - lineRect.left) * i;
                    Log.e(TAG, "回调的数据是：start = " + start +", end = " + end );
                    listener.onSlideSelected((int) start, (int) end);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setSlidingListener(OnSlidingListener l){
        this.listener = l;
    }

    public interface OnSlidingListener{
        /**
         * 移动中监听
         */
        void onSliding();

        /**
         * 选中监听，手指移开 view， ACTION_UP
         * @param start
         * @param end
         */
        void onSlideSelected(int start, int end);
    }
}
