package org.duhen.dglyphs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class GlyphPreviewView extends FrameLayout {

    private static final int LIT_THRESHOLD = 0;
    private static final int[][] DRAWABLE_PAIRS = {
            {R.drawable.glyph_camera, R.drawable.glyph_camera_lit},
            {R.drawable.glyph_diagonal, R.drawable.glyph_diagonal_lit},
            {R.drawable.glyph_main, R.drawable.glyph_main_lit},
            {R.drawable.glyph_line, R.drawable.glyph_line_lit},
            {R.drawable.glyph_dot, R.drawable.glyph_dot_lit},
    };

    private final ImageView[] glyphViews = new ImageView[5];
    private final boolean[] litState = new boolean[5];

    public GlyphPreviewView(Context context) {
        super(context);
        init(context);
    }

    public GlyphPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GlyphPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_glyph_preview, this);
        glyphViews[0] = findViewById(R.id.glyphCamera);
        glyphViews[1] = findViewById(R.id.glyphDiagonal);
        glyphViews[2] = findViewById(R.id.glyphMain);
        glyphViews[3] = findViewById(R.id.glyphLine);
        glyphViews[4] = findViewById(R.id.glyphDot);
        resetAll();
    }

    public void setGlyphState(int[] brightness) {
        if (brightness == null || brightness.length < 5) return;
        for (int i = 0; i < 5; i++) {
            boolean lit = brightness[i] > LIT_THRESHOLD;
            if (lit != litState[i]) {
                litState[i] = lit;
                glyphViews[i].setImageResource(DRAWABLE_PAIRS[i][lit ? 1 : 0]);
            }
        }
    }

    public void resetAll() {
        for (int i = 0; i < 5; i++) {
            litState[i] = false;
            glyphViews[i].setImageResource(DRAWABLE_PAIRS[i][0]);
        }
    }
}