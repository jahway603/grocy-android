/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.WebDialogActivity;
import xyz.zedler.patrick.grocy.databinding.ViewHtmlCardBinding;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class HtmlCardView extends LinearLayout {

  private static final String TAG = HtmlCardView.class.getSimpleName();

  private ViewHtmlCardBinding binding;
  private Context context;
  private String html;
  private AlertDialog dialog;
  private String title;
  private Activity activity;

  public HtmlCardView(Context context) {
    super(context);
    init(context);
  }

  public HtmlCardView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    this.context = context;
    binding = ViewHtmlCardBinding.inflate(
        LayoutInflater.from(context), this, true
    );
    setSaveEnabled(true);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
  }

  public void setDialogTitle(@StringRes int titleResId) {
    title = context.getString(titleResId);
  }

  @SuppressLint("ClickableViewAccessibility")
  public void setHtml(String html, Activity activity) {
    if (html != null) {
      html = html.replaceAll("</?font[^>]*>", ""); // remove font
      html = html.replaceAll(
          "<p[^>]*> *(<br ?/>)*</p[^>]*>[\\n\\r\\s]*$", "" // trim empty paragraphs at end
      );
      html = html.replaceAll(
          "^[\\n\\r\\s]*<p[^>]*> *(<br ?/>)*</p[^>]*>", "" // trim empty paragraphs at start
      );
      this.html = html;
      binding.webview.getSettings().setJavaScriptEnabled(false);
      binding.webview.loadDataWithBaseURL(
          "file:///android_asset/",
          getFormattedHtml(html, true),
          "text/html; charset=utf-8", "utf8", null
      );

      binding.webview.setBackgroundColor(Color.TRANSPARENT);
      binding.webview.setOnTouchListener((v, event) -> binding.card.onTouchEvent(event));
      binding.webview.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
          if (binding == null) {
            return;
          }
          binding.webview.measure(
              MeasureSpec.makeMeasureSpec(binding.card.getWidth(), MeasureSpec.EXACTLY),
              MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
          );
          int height = binding.webview.getMeasuredHeight();
          int maxHeight = UiUtil.dpToPx(context, 80);
          int padding = UiUtil.dpToPx(context, 8);
          boolean isStartEndParagraph = HtmlCardView.this.html.matches("^<p>.*</p>$");
          LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT, Math.min(height, maxHeight)
          );
          params.setMargins(
              padding, isStartEndParagraph ? 0 : padding,
              padding, isStartEndParagraph ? 0 : padding
          );
          binding.webview.setLayoutParams(params);
          if (height > maxHeight) {
            binding.divider.setVisibility(VISIBLE);
            binding.textHelp.setVisibility(VISIBLE);
          }
        }
      });
      binding.card.setOnClickListener(v -> showHtmlDialog(activity));
    } else {
      setVisibility(View.GONE);
    }
  }

  public void showHtmlDialog(Activity activity) {
    Intent intent = new Intent(context, WebDialogActivity.class);
    intent.putExtra("html", getFormattedHtml(html, false));
    intent.putExtra("background", ResUtil.getColorAttr(context, R.attr.colorSurface));
    context.startActivity(intent);
    activity.overridePendingTransition(R.anim.fade_in, 0);
    this.activity = activity;
  }

  private String getFormattedHtml(String html, boolean useOnSurfaceVariant) {
    int textColor = ResUtil.getColorAttr(
        context, useOnSurfaceVariant ? R.attr.colorOnSurfaceVariant : R.attr.colorOnSurface
    );
    int linkColor = ResUtil.getColorAttr(context, R.attr.colorPrimary);
    return  "<!DOCTYPE html><html><head><meta charset='UTF-8'><style type='text/css'>"
        + "@font-face{font-family: Jost; src: url('fonts/jost_400_book.otf')}"
        + "body{font-family: Jost;color:#" + String.format("%06X", (0xFFFFFF & textColor)) + ";}"
        + "a{color:#" + String.format("%06X", (0xFFFFFF & linkColor)) + ";}"
        + "</style></head><body>" + html + "</body></html>";
  }

  @Nullable
  @Override
  protected Parcelable onSaveInstanceState() {
    SavedState state = new SavedState(super.onSaveInstanceState());
    state.isDialogShown = dialog != null && dialog.isShowing();
    return state;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());
    if (savedState.isDialogShown) {
      new Handler(Looper.getMainLooper()).postDelayed(() -> showHtmlDialog(activity), 1);
    }
  }

  private static class SavedState extends BaseSavedState {
    private boolean isDialogShown;

    SavedState(Parcelable superState) {
      super(superState);
    }

    private SavedState(Parcel in) {
      super(in);
      isDialogShown = in.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeInt(isDialogShown ? 1 : 0);
    }

    public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<>() {

      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }
}
