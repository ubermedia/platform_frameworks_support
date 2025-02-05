/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appcompat.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import androidx.appcompat.appcompat.R;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * @hide
 */
public class CompatTextView extends TextView {

    public CompatTextView(Context context) {
        this(context, null);
    }

    public CompatTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        boolean allCaps = false;

        TypedArray style = context
                .obtainStyledAttributes(attrs, R.styleable.CompatTextView, defStyle, 0);
        allCaps = style.getBoolean(R.styleable.CompatTextView_textAllCaps, false);
        style.recycle();

        // Framework impl also checks TextAppearance for textAllCaps. This isn't needed for our
        // purposes so has been omitted.

        if (allCaps) {
            setTransformationMethod(new AllCapsTransformationMethod(context));
        }
    }

    /**
     * Transforms source text into an ALL CAPS string, locale-aware.
     */
    private static class AllCapsTransformationMethod implements TransformationMethod {

        private final Locale mLocale;

        public AllCapsTransformationMethod(Context context) {
            mLocale = context.getResources().getConfiguration().locale;
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return source != null ? source.toString().toUpperCase(mLocale) : null;
        }

        @Override
        public void onFocusChanged(View view, CharSequence charSequence, boolean b, int i,
                Rect rect) {
        }
    }
}
