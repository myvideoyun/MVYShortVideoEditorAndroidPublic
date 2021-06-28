package com.myvideoyun.shortvideo.customUI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.myvideoyun.shortvideo.R;

public class StickerView extends View {
    private float moveX;
    private float moveY;
    private Paint paint;
    private Rect rect;
    private int parentViewHeight;
    private int parentViewWidth;
    private int parentTop;
    private int parentLeft;
    private int parentRight;
    private int parentBottom;
    private ViewGroup parentView;

    public StickerView(Context context) {
        super(context);
        initView(context);
    }

    public StickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public StickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        paint = new Paint();
        paint.setColor(getResources().getColor(R.color.white));
        paint.setStyle(Paint.Style.STROKE);
        rect = new Rect();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.left = 2;
        rect.top = 2;
        rect.right = getMeasuredWidth() - 2;
        rect.bottom = getMeasuredHeight() - 2;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            parentView = (ViewGroup) parent;
            parentTop = parentView.getTop();
            parentLeft = parentView.getLeft();
            parentRight = parentView.getRight();
            parentBottom = parentView.getBottom();
            Log.d("moose", "top " + parentTop + " left=" + parentLeft + "right =" + parentRight + " bottom=" + parentBottom);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                moveX = event.getX();
                moveY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("moose", "parent x=" + parentView.getX() + "，y="+ parentView.getY());
                float translationX = parentView.getX() + (event.getX() - moveX);
                float translationY = parentView.getY() + (event.getY() - moveY);
                if (translationX >= parentLeft && translationX <= parentRight
                        && translationY >= parentTop && translationY <= parentBottom) {
                    setTranslationX(translationX);
                    setTranslationY(translationY);
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rect, paint);
    }
}
