//
//  CDVWechat.h
//  cordova-plugin-wechat
//
//  Created by xu.li on 12/23/13.
//
//

#import "CDVPluginExt.h"
#import "WXApi.h"
#import "WXApiObject.h"

#define OPT_APPID           @"appId"
#define OPT_APPKEY          @"appKey"
#define OPT_APPNAME         @"appName"

enum  CDVWechatSharingType {
    CDVWXSharingTypeApp = 1,
    CDVWXSharingTypeEmotion,
    CDVWXSharingTypeFile,
    CDVWXSharingTypeImage,
    CDVWXSharingTypeMusic,
    CDVWXSharingTypeVideo,
    CDVWXSharingTypeWebPage
};

@interface CDVWechat:CDVPluginExt <WXApiDelegate>

@property (nonatomic, strong) NSString *currentCallbackId;

- (void)isWXAppInstalled:(CDVInvokedUrlCommand *)command;

- (void) setOptions:(CDVInvokedUrlCommand *)command;

- (void) share:(CDVInvokedUrlCommand *)command;

- (void)sendAuthRequest:(CDVInvokedUrlCommand *)command;

@property (assign) BOOL inited;

@property (nonatomic, retain) NSString* appId;
@property (nonatomic, retain) NSString* appKey;
@property (nonatomic, retain) NSString* appName;

#pragma mark virtual methods

- (void)pluginInitialize;

- (void) parseOptions:(NSDictionary*) options;
- (void) validateAppReg;

@end
