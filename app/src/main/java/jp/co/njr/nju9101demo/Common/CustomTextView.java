package jp.co.njr.nju9101demo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class CustomTextView extends TextView {

    private String mFont = "DroidSans.ttf";
    private int mStyle = Typeface.NORMAL;

    public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getFont(context, attrs);
        init();
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getFont(context, attrs);
        init();
    }

    public CustomTextView(Context context) {
        super(context);
        init();
    }

    /**
     * フォントファイルを読み込む
     *
     * @param context
     * @param attrs
     */
    private void getFont(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
        mFont = a.getString(R.styleable.CustomTextView_font);
        String style = a.getString(R.styleable.CustomTextView_style);
        if (style != null) {
            if (style.equals("normal")) {
                mStyle = Typeface.NORMAL;
            }
            else if (style.equals("bold")) {
                mStyle = Typeface.BOLD;
            }
            else if (style.equals("italic")) {
                mStyle = Typeface.ITALIC;
            }
            else {
                mStyle = Typeface.NORMAL;
            }
        }
        a.recycle();
    }

    /**
     * フォントを反映
     */
    private void init() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), mFont);
        setTypeface(tf, mStyle);
    }
}

