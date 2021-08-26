import { UIManager, Platform, findNodeHandle } from 'react-native';

import { BANNER_EVENT_TYPE_RAW, BANNER_EVENT_TYPE } from '../Constants';

const removeBanner = (bannerRef: any) => {
  if (bannerRef && Platform.OS === 'ios') {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(bannerRef.current),
      UIManager.getViewManagerConfig('RCTAppNexusVideoBanner').Commands
        .removeBanner,
      []
    );
  }
};

const loadAdVideoBanner = (bannerRef: any) => {
  if (bannerRef) {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(bannerRef.current),
      UIManager.getViewManagerConfig('RCTAppNexusVideoBanner').Commands
        .loadAdVideoBanner,
      []
    );
  }
};

const viewAdVideoBanner = (bannerRef: any) => {
  if (bannerRef) {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(bannerRef.current),
      UIManager.getViewManagerConfig('RCTAppNexusVideoBanner').Commands
        .viewAdVideoBanner,
      []
    );
  }
};

const forceReloadBanner = (bannerRef: any) => {
  if (bannerRef) {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(bannerRef.current),
      UIManager.getViewManagerConfig('RCTAppNexusVideoBanner').Commands
        .forceReloadBanner,
      []
    );
  }
};

const bannerEventChangeAction = (rawEventType: number | undefined) => {
  switch (rawEventType) {
    case BANNER_EVENT_TYPE_RAW.RAW_BannerEventAdWasClicked:
      return BANNER_EVENT_TYPE.BannerEventAdWasClicked;

    case BANNER_EVENT_TYPE_RAW.RAW_BannerEventAdClose:
      return BANNER_EVENT_TYPE.BannerEventAdClose;

    case BANNER_EVENT_TYPE_RAW.RAW_BannerEventDidPresent:
      return BANNER_EVENT_TYPE.BannerEventDidPresent;

    case BANNER_EVENT_TYPE_RAW.RAW_BannerEventAdWillDisappear:
      return BANNER_EVENT_TYPE.BannerEventAdWillDisappear;

    case BANNER_EVENT_TYPE_RAW.RAW_BannerEventAdWillAppear:
      return BANNER_EVENT_TYPE.BannerEventAdWillAppear;

    case BANNER_EVENT_TYPE_RAW.RAW_BannerEventWillLeaveApp:
      return BANNER_EVENT_TYPE.BannerEventWillLeaveApp;

    default:
      return 'unknownEvent';
  }
};

export {
  removeBanner,
  loadAdVideoBanner,
  bannerEventChangeAction,
  forceReloadBanner,
  viewAdVideoBanner,
};
