#import "RCTAppNexusBannerView.h"

#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#import <React/UIView+React.h>
#import <React/RCTLog.h>
#else
#import "RCTBridgeModule.h"
#import "UIView+React.h"
#import "RCTLog.h"
#endif

typedef NS_ENUM(NSInteger, ANInstreamVideoEventType)
{
    BannerEventAdWasClicked            = -1,
    BannerEventAdClose                 = 0,
    BannerEventDidPresent              = 1,
    BannerEventAdWillDisappear         = 2,
    BannerEventAdWillAppear            = 3,
    BannerEventWillLeaveApp            = 4,

    BANNER_NOT_VISIBLE                 = 0,
    BANNER_PARTIALLY_VISIBLE           = 1,
    BANNER_FULLY_VISIBLE               = 2
};

@implementation RCTAppNexusBannerView
{
    ANBannerAdView *_bannerView;
    BOOL _isLoaded;
    BOOL _isView;
    NSInteger _isVisible;
}

- (instancetype)initWithFrame:(CGRect)frame
                       bridge:(RCTBridge *)bridge
                     reactTag:(NSNumber *)reactTag
{
  if ((self = [super initWithFrame:frame])) {
    _bridge = bridge;
    _reactTag = reactTag;

    _isLoaded = NO;
    _isView = NO;
    _isVisible = 0;
  }

  return self;
}

- (void)dealloc
{
    [self removeBanner];
}

- (void)removeBanner
{
    _bannerView.delegate = nil;
    if (_bannerView) {
        [_bannerView removeFromSuperview];
        [[NSURLCache sharedURLCache] removeAllCachedResponses];
        [[NSURLCache sharedURLCache] setDiskCapacity:0];
        [[NSURLCache sharedURLCache] setMemoryCapacity:0];
    }
}

- (void)didSetProps:(NSArray<NSString *> *)changedProps
{

}

- (void)forceReloadBanner
{
    if (_bannerView != nil) {
        [_bannerView loadLazyAd];
    } else {
        _isLoaded = NO;
        _isView = NO;
        [self lazyLoadAdBanner];
    }
}

- (void)loadAdBanner
{
    NSLog(@"Zoftify - loadAdBanner");
    [self createAdBanner:NO];
}

- (void)lazyLoadAdBanner
{
    NSLog(@"Zoftify - lazyLoadAdBanner");
    [self createAdBanner:YES];
}

- (void)viewLazyAdBanner
{
    if (!_isView) {
        [_bannerView loadLazyAd];
        _isView = YES;
    }
}

- (void)createAdBanner:(BOOL)enableLazyLoad {
    if (_isLoaded){
        return;
    }

    super.backgroundColor = [UIColor clearColor];

    UIWindow *keyWindow = [[UIApplication sharedApplication] keyWindow];
    UIViewController *rootViewController = [keyWindow rootViewController];

    if([_sizes count] == 0) {
        RCTLogError(@"Invalid sizes property");
    }
    CGSize size = CGSizeMake([_sizes[0][0] doubleValue], [_sizes[0][1] doubleValue]);

    _bannerView = [ANBannerAdView adViewWithFrame:self.bounds placementId:_placementId adSize:size];
    _bannerView.enableLazyLoad = enableLazyLoad;
    _bannerView.enableNativeRendering = YES;
    _bannerView.translatesAutoresizingMaskIntoConstraints = NO;
    _bannerView.delegate = self;
    _bannerView.rootViewController = rootViewController;

    _bannerView.shouldAllowVideoDemand = _allowVideo;

    _bannerView.adSizes = [self toCGSizes: _sizes];
    _bannerView.autoRefreshInterval = _autoRefreshInterval;
    [self addKeywords:_keywords toBanner:_bannerView];

    [self addSubview:_bannerView];

    [_bannerView.leftAnchor constraintEqualToAnchor:self.leftAnchor].active = YES;
    [_bannerView.rightAnchor constraintEqualToAnchor:self.rightAnchor].active = YES;
    [_bannerView.topAnchor constraintEqualToAnchor:self.topAnchor].active = YES;
    [_bannerView.bottomAnchor constraintEqualToAnchor:self.bottomAnchor].active = YES;

    [_bannerView loadAd];
}

- (void)react_updateClippedSubviewsWithClipRect:(CGRect)clipRect relativeToView:(UIView *)clipView
{
    [self setVisibleBanner:clipRect relativeToView:clipView];
}

- (void)setVisibleBanner:(CGRect)clipRect relativeToView:(UIView *)clipView {

    if (!_isLoaded) {
        return;
    }

    clipRect = [clipView convertRect:clipRect toView:self];
    clipRect = CGRectIntersection(clipRect, [UIScreen mainScreen].bounds);

    if (!CGSizeEqualToSize(CGRectIntersection(clipRect, self.frame).size, CGSizeZero)) {
        if (CGRectContainsRect(clipRect,_bannerView.frame)) {
            // View is fully visible, so remount all subviews
            if (_isVisible != BANNER_FULLY_VISIBLE) {
                _isVisible = BANNER_FULLY_VISIBLE;
                _onAdVisibleChange(@{@"visible": @(BANNER_FULLY_VISIBLE)});
            }
        } else {
            // View is partially visible, so update clipped subviews
            if (_isVisible != BANNER_PARTIALLY_VISIBLE) {
                _isVisible = BANNER_PARTIALLY_VISIBLE;
                _onAdVisibleChange(@{@"visible": @(BANNER_PARTIALLY_VISIBLE)});
            }
        }
    } else {
        if (_isVisible != BANNER_NOT_VISIBLE) {
            _isVisible = BANNER_NOT_VISIBLE;
            _onAdVisibleChange(@{@"visible": @(BANNER_NOT_VISIBLE)});
        }
    }
}

- (void)layoutSubviews
{
    super.removeClippedSubviews = YES;
    [super layoutSubviews];
    _bannerView.frame = self.bounds;
}

- (void)adDidReceiveAd:(id)ad
{
    _isLoaded = YES;
    [self setNeedsLayout];
    CGSize actualSize = _bannerView.loadedAdSize;

    if (_onAdLoadSuccess) {
        _onAdLoadSuccess(@{
                           @"width": [NSNumber numberWithUnsignedInt:actualSize.width],
                           @"height": [NSNumber numberWithUnsignedInt:actualSize.height],
                           @"creativeId": [ad creativeId],
                           });
    }
}

- (void)lazyAdDidReceiveAd:(id)ad
{
    _isLoaded = YES;
    [self setNeedsLayout];
    CGSize actualSize = _bannerView.loadedAdSize;

    if (_onAdLazyLoadSuccess) {
        _onAdLazyLoadSuccess(@{
                           @"width": [NSNumber numberWithUnsignedInt:actualSize.width],
                           @"height": [NSNumber numberWithUnsignedInt:actualSize.height],
                           @"creativeId": [ad creativeId],
                           });
    }
}

- (void)ad:(id)ad requestFailedWithError:(NSError *)error
{
    _isLoaded = NO;
    _isView = NO;

    if (_onAdLoadFail) {
        if(error){
        _onAdLoadFail(@{@"error": [error localizedDescription]});
        } else {
        _onAdLoadFail(@{@"error": @"Call to loadAd failed"});
        }
    }
}

- (NSArray *)toCGSizes:(NSArray *)sizes
{
    NSMutableArray *adSizes = [NSMutableArray array];

    for (int i = 0; i < [sizes count]; i++) {
        CGSize size = CGSizeMake([sizes[i][0] doubleValue], [sizes[i][1] doubleValue]);
        [adSizes addObject:[NSValue valueWithCGSize:size]];
    }

    return adSizes;
}

- (void)addKeywords:(NSDictionary *)keywords toBanner:(ANBannerAdView *)bannerView
{
    NSArray *keywordKeys = [keywords allKeys];
    for (NSString *key in keywordKeys) {
        [bannerView addCustomKeywordWithKey:key value:keywords[key]];
    }
}

- (void)adWasClicked:(nonnull id)ad
{
    if (_onEventChange) {
        _onEventChange(@{ @"eventType": @(BannerEventAdWasClicked) });
    }
}

- (void)adWasClicked:(nonnull id)ad withURL:(nonnull NSString *)urlString
{
    if (_onEventChange) {
        _onEventChange(@{ @"eventType": @(BannerEventAdWasClicked) });
    }
}

- (void)adWillClose:(nonnull id)ad{
    if (_onEventChange) {
        _onEventChange(@{ @"eventType": @(BannerEventAdWillDisappear) });
    }
}

- (void)adDidClose:(nonnull id)ad{
    if (_onEventChange) {
        _onEventChange(@{ @"eventType": @(BannerEventAdClose) });
    }
}

- (void)adWillPresent:(nonnull id)ad{
    if (_onEventChange) {
        _onEventChange(@{ @"eventType": @(BannerEventAdWillAppear) });
    }
}

- (void)adDidPresent:(nonnull id)ad{
    if (_onEventChange) {
        _onEventChange(@{ @"eventType": @(BannerEventDidPresent) });
    }
}


- (void)adWillLeaveApplication:(nonnull id)ad{
    if (_onEventChange) {
        _onEventChange(@{ @"eventType": @(BannerEventWillLeaveApp) });
    }
}

@end
