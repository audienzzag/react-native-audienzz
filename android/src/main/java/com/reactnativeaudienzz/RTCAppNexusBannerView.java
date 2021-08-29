package com.reactnativeaudienzz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.appnexus.opensdk.ANClickThroughAction;
import com.appnexus.opensdk.AdListener;
import com.appnexus.opensdk.AdView;
import com.appnexus.opensdk.NativeAdResponse;
import com.appnexus.opensdk.ResultCode;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.scroll.ReactScrollView;
import com.facebook.react.views.view.ReactViewGroup;

import javax.annotation.Nullable;

@SuppressLint("ViewConstructor")
class RTCAppNexusBannerView extends ReactViewGroup {
    private ReactScrollView scrollView;
    private final RCTEventEmitter mEventEmitter;
    private final RTCAppNexusBanner banner;

    public RTCAppNexusBannerView(ThemedReactContext themedReactContext) {
        super(themedReactContext);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
        banner = new RTCAppNexusBanner(themedReactContext.getCurrentActivity());
        this.addView(banner);
        attachEvents(banner);
        banner.setResizeAdToFitContainer(true);
        banner.enableNativeRendering(true);
        banner.enableLazyLoad();
        banner.setClickThroughAction(ANClickThroughAction.OPEN_SDK_BROWSER);
        banner.setIsVisible(Constants.BANNER_NOT_VISIBLE);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed) {
            final RTCAppNexusBanner banner = (RTCAppNexusBanner) this.getChildAt(0);
            banner.measure(
                    MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(this.getMeasuredHeight(), MeasureSpec.EXACTLY));
            banner.layout(0, 0, this.getMeasuredWidth(), this.getMeasuredHeight());
        }
    }

    private final View.OnLayoutChangeListener childLayoutListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (scrollView == null) {
                scrollView = RTCAppNexusBannerView.fintSpecifyParent(ReactScrollView.class, v.getParent());
                if (scrollView != null) {
                    RTCAppNexusBannerView.this.handleScroll(scrollView);
                } else {
                    final RTCAppNexusBanner banner = (RTCAppNexusBanner) RTCAppNexusBannerView.this.getChildAt(0);
                    onAdVisibleChange(Constants.BANNER_FULLY_VISIBLE);
                    banner.setIsVisible(Constants.BANNER_FULLY_VISIBLE);
                }
            } else {
                RTCAppNexusBannerView.this.handleScroll(scrollView);
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        final RTCAppNexusBanner banner = (RTCAppNexusBanner) this.getChildAt(0);

        this.addOnLayoutChangeListener(childLayoutListener);

        if (scrollView == null) {
            scrollView = RTCAppNexusBannerView.fintSpecifyParent(ReactScrollView.class, this.getParent());
            if (scrollView != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scrollView.setOnScrollChangeListener(new OnScrollChangeListener() {
                        @Override
                        public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                            RTCAppNexusBannerView.this.handleScroll(scrollView);
                        }
                    });
                } else {
                    scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged() {
                            RTCAppNexusBannerView.this.handleScroll(scrollView);
                        }
                    });
                }
            } else {
                if (banner != null && banner.getIsVisible() != Constants.BANNER_FULLY_VISIBLE) {
                    onAdVisibleChange(Constants.BANNER_FULLY_VISIBLE);
                    banner.setIsVisible(Constants.BANNER_FULLY_VISIBLE);
                }
            }
        }
    }

    private void handleScroll(ScrollView scrollView) {
        final RTCAppNexusBanner banner = (RTCAppNexusBanner) this.getChildAt(0);

        Rect scrollBounds = new Rect();
        scrollView.getHitRect(scrollBounds);
        if (RTCAppNexusBannerView.this.getLocalVisibleRect(scrollBounds)) {
            if (banner != null && banner.getIsVisible() != Constants.BANNER_FULLY_VISIBLE) {
                onAdVisibleChange(Constants.BANNER_FULLY_VISIBLE);
                banner.setIsVisible(Constants.BANNER_FULLY_VISIBLE);
            }
        } else {
            if (banner != null && banner.getIsVisible() != Constants.BANNER_NOT_VISIBLE) {
                onAdVisibleChange(Constants.BANNER_NOT_VISIBLE);
                banner.setIsVisible(Constants.BANNER_NOT_VISIBLE);
            }
        }
    }

    protected void attachEvents(final RTCAppNexusBanner banner) {
        AdListener adListener = new AdListener() {
            @Override
            public void onAdRequestFailed(AdView adView, ResultCode errorCode) {
                if (errorCode == null) {
                    onAdLoadFail(adView.getId(),"Call to loadAd failed");
                } else {
                    onAdLoadFail(adView.getId(),"Ad request failed: " + errorCode);
                }
            }

            @Override
            public void onAdLoaded(AdView adView) {
                onAdLoadSuccess(adView.getId(),banner.getCreativeWidth(),banner.getCreativeHeight(), adView.getAdResponseInfo().getCreativeId());
            }

            @Override
            public void onAdLoaded(NativeAdResponse nativeAdResponse) {
                // onAdLoadSuccess(banner.getId(),banner.getCreativeWidth(),banner.getCreativeHeight(),banner.getAdResponseInfo().getCreativeId());
            }

            @Override
            public void onAdExpanded(AdView adView) {
                onAdEventChange(adView.getId(), Constants.BANNER_EVENT_AD_PRESENT);
            }

            @Override
            public void onAdCollapsed(AdView adView) {
                onAdEventChange(adView.getId(), Constants.BANNER_EVENT_AD_CLOSE);
            }

            @Override
            public void onAdClicked(AdView adView) {
                onAdEventChange(adView.getId(), Constants.BANNER_EVENT_AD_WAS_CLICKED);
            }

            @Override
            public void onAdClicked(AdView adView, String clickUrl) {
                onAdEventChange(adView.getId(), Constants.BANNER_EVENT_AD_WAS_CLICKED);
            }

            @Override
            public void onLazyAdLoaded(AdView adView) {
                onAdLoadSuccess(adView.getId(),banner.getAdWidth(),banner.getAdHeight(), adView.getAdResponseInfo().getCreativeId());
            }
        };

        banner.setAdListener(adListener);
    }

    public void onAdLoadSuccess(int id, int width, int height, String creativeId) {
        WritableMap event = Arguments.createMap();
        event.putString("creativeId",creativeId);
        event.putInt("width",width);
        event.putInt("height",height);

        mEventEmitter.receiveEvent(this.getId(), "onAdLoadSuccess", event);
    }

    public void onAdLoadFail(int id, String error) {
        WritableMap event = Arguments.createMap();
        event.putString("error",error);

        mEventEmitter.receiveEvent(this.getId(), "onAdLoadFail", event);
    }

    public void onAdEventChange(int id, int eventType) {
        WritableMap event = Arguments.createMap();
        event.putInt("eventType",eventType);

        mEventEmitter.receiveEvent(this.getId(), "onEventChange", event);
    }

    public void onAdVisibleChange(int visible) {
        WritableMap event = Arguments.createMap();
        event.putInt("visible",visible);

        mEventEmitter.receiveEvent(this.getId(), "onAdVisibleChange", event);
    }

    void loadBannerAd(RTCAppNexusBannerView bannerView, @Nullable ReadableArray args) {
        final RTCAppNexusBanner banner = (RTCAppNexusBanner) this.getChildAt(0);

        if (!banner.getIsLoading()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    banner.loadAd();
                    banner.setIsLoading(true);
                }
            }, 0);
        }
    }

    void lazyLoadBannerAd(RTCAppNexusBannerView bannerView, @Nullable ReadableArray args) {
        final RTCAppNexusBanner banner = (RTCAppNexusBanner) this.getChildAt(0);

        if (!banner.getIsLoading()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    banner.loadAd();
                    banner.setIsLoading(true);
                }
            }, 0);
        }
    }

    void visibleLazyBannerAd(final RTCAppNexusBannerView bannerView, @Nullable ReadableArray args) {
        final RTCAppNexusBanner banner = (RTCAppNexusBanner) this.getChildAt(0);

        if (banner.getIsLoading()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    banner.loadLazyAd();
                }
            }, 0);
        }
    }

    public static <T> T fintSpecifyParent(Class<T> tClass, ViewParent parent) {
        if (parent == null) {
            return null;
        } else {
            if (tClass.isInstance(parent)) {
                return (T) parent;
            } else {
                return fintSpecifyParent(tClass, parent.getParent());
            }
        }
    }
}






