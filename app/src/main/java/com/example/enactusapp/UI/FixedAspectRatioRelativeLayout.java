package com.example.enactusapp.UI;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.example.enactusapp.R;


public class FixedAspectRatioRelativeLayout extends RelativeLayout {

    private final static int DEFAULT_ASPECT_RATIO_WIDTH = 480;
    private final static int DEFAULT_ASPECT_RATIO_HEIGHT = 640;

    private int mAspectRatioWidth = DEFAULT_ASPECT_RATIO_WIDTH;
    private int mAspectRatioHeight = DEFAULT_ASPECT_RATIO_HEIGHT;

    public FixedAspectRatioRelativeLayout(Context context) {
        super(context);
    }

    public FixedAspectRatioRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateAttributes(context, attrs);
    }

    public FixedAspectRatioRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        updateAttributes(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);

        int calculatedHeight = originalWidth * mAspectRatioHeight / mAspectRatioWidth;
        int finalWidth, finalHeight;

        if (calculatedHeight > originalHeight) {
            finalWidth = originalHeight * mAspectRatioWidth / mAspectRatioHeight;
            finalHeight = originalHeight;
        } else {
            finalWidth = originalWidth;
            finalHeight = calculatedHeight;
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
    }

    private void updateAttributes(Context context, AttributeSet attrs) {
        //获取配置属性
        TypedArray tArray = context.obtainStyledAttributes(attrs, R.styleable.FixedAspectRatioRelativeLayout);
        mAspectRatioWidth = tArray.getInt(R.styleable.FixedAspectRatioRelativeLayout_aspect_ratio_width, DEFAULT_ASPECT_RATIO_WIDTH);
        mAspectRatioHeight = tArray.getInt(R.styleable.FixedAspectRatioRelativeLayout_aspect_ratio_height, DEFAULT_ASPECT_RATIO_HEIGHT);
        //回收资源
        tArray.recycle();
    }
}
