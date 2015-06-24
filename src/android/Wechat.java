package com.rjfun.cordova.wechat;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.rjfun.cordova.ext.CordovaPluginExt;
import com.tencent.mm.sdk.modelmsg.*;
import com.tencent.mm.sdk.openapi.*;

public class Wechat extends CordovaPluginExt {
	private static final String LOGTAG = "WechatPlugin";

	public static final String WXAPPID_PROPERTY_KEY = "wechatappid";

	public static final String ACTION_SET_OPTIONS = "setOptions";
	public static final String ACTION_SHARE = "share";
	public static final String ACTION_SEND_AUTH_REQUEST = "sendAuthRequest";
	public static final String ACTION_IS_WXAPP_INSTALLED = "isWXAppInstalled";
	
	public static final String OPT_APPID = "appId";
    public static final String OPT_APPKEY = "appKey";
    public static final String OPT_APPNAME = "appName";

	public static final String ERROR_WX_NOT_INSTALLED = "Not installed";
	public static final String ERROR_ARGUMENTS = "Argument Error";

	public static final String KEY_ARG_MESSAGE = "message";
	public static final String KEY_ARG_SCENE = "scene";
	public static final String KEY_ARG_TEXT = "text";
	public static final String KEY_ARG_MESSAGE_TITLE = "title";
	public static final String KEY_ARG_MESSAGE_DESCRIPTION = "description";
	public static final String KEY_ARG_MESSAGE_THUMB = "thumb";
	public static final String KEY_ARG_MESSAGE_MEDIA = "media";
	public static final String KEY_ARG_MESSAGE_MEDIA_TYPE = "type";
	public static final String KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL = "webpageUrl";
	public static final String KEY_ARG_MESSAGE_MEDIA_TEXT = "text";
	public static final String KEY_ARG_MESSAGE_MEDIA_IMAGE = "image";
	public static final String KEY_ARG_MESSAGE_MEDIA_MUSICURL = "musicUrl";
	public static final String KEY_ARG_MESSAGE_MEDIA_MUSICDATAURL = "musicDataUrl";
	public static final String KEY_ARG_MESSAGE_MEDIA_VIDEOURL = "videoUrl";
	public static final String KEY_ARG_MESSAGE_MEDIA_FILE = "file";
	public static final String KEY_ARG_MESSAGE_MEDIA_EMOTION = "emotion";
	public static final String KEY_ARG_MESSAGE_MEDIA_EXTINFO = "extInfo";
	public static final String KEY_ARG_MESSAGE_MEDIA_URL = "url";

	public static final String ERR_WECHAT_NOT_INSTALLED = "ERR_WECHAT_NOT_INSTALLED";
    public static final String ERR_INVALID_OPTIONS = "ERR_INVALID_OPTIONS";
    public static final String ERR_UNSUPPORTED_MEDIA_TYPE = "ERR_UNSUPPORTED_MEDIA_TYPE";
    public static final String ERR_USER_CANCEL = "ERR_USER_CANCEL";
    public static final String ERR_AUTH_DENIED = "ERR_AUTH_DENIED";
    public static final String ERR_SENT_FAILED = "ERR_SENT_FAILED";
    public static final String ERR_UNSUPPORT = "ERR_UNSUPPORT";
    public static final String ERR_COMM = "ERR_COMM";
    public static final String ERR_UNKNOWN = "ERR_UNKNOWN";
    public static final String NO_RESULT = "NO_RESULT";
    
	public static final int TYPE_WX_SHARING_APP = 1;
	public static final int TYPE_WX_SHARING_EMOTION = 2;
	public static final int TYPE_WX_SHARING_FILE = 3;
	public static final int TYPE_WX_SHARING_IMAGE = 4;
	public static final int TYPE_WX_SHARING_MUSIC = 5;
	public static final int TYPE_WX_SHARING_VIDEO = 6;
	public static final int TYPE_WX_SHARING_WEBPAGE = 7;
	public static final int TYPE_WX_SHARING_TEXT = 8;
	
    public static final int SCENE_SESSION = 0;
    public static final int SCENE_TIMELINE = 1;
    public static final int SCENE_FAVORITE = 2;

    protected boolean appRegistered = false;
    protected String appId = "wx0c5bd30aa791c589"; // if null, then find it with preferences.getString(WXAPPID_PROPERTY_KEY, "");
    protected String appKey = "";
    protected String appName = "";

	public static IWXAPI wxAPI = null;
	public static CallbackContext currentCallbackContext = null;

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (ACTION_SET_OPTIONS.equals(action)) {
			JSONObject options = args.optJSONObject(0);
			if(options != null) {
				this.setOptions(options);
			}
			callbackContext.success();
			return true;
			
		} else if (ACTION_SHARE.equals(action)) {
			// sharing
			return share(args, callbackContext);

		} else if(ACTION_SEND_AUTH_REQUEST.equals(action)) {
			return sendAuthRequest(args, callbackContext);

		} else if(ACTION_IS_WXAPP_INSTALLED.equals(action)) {
			return isInstalled(callbackContext);
		}

		return super.execute(action, args, callbackContext);
	}

    public void setOptions(JSONObject options) {
    	Log.d(LOGTAG, "setOptions" );
    	
    	if(options != null) {
            if(options.has(OPT_APPID)) this.appId = options.optString(OPT_APPID);
            if(options.has(OPT_APPKEY)) this.appKey = options.optString(OPT_APPKEY);
            if(options.has(OPT_APPNAME)) this.appName = options.optString(OPT_APPNAME);
            
            if((appId != null) && (appId.length()>0)) {
            	validateAppReg();
            }
    	}
    }

	protected IWXAPI getWXAPI() {
		if (wxAPI == null) {
            if(appId == null) appId = preferences.getString(WXAPPID_PROPERTY_KEY, "");
			wxAPI = WXAPIFactory.createWXAPI(this.getActivity(), appId, true);
		}

		return wxAPI;
	}

    protected void validateAppReg() {
        if(! this.appRegistered) {
            if(appId == null) appId = preferences.getString(WXAPPID_PROPERTY_KEY, "");

            final IWXAPI api = getWXAPI();
            api.registerApp( appId );

            this.appRegistered = true;
        }
    }
	
	protected boolean sendAuthRequest(JSONArray args, CallbackContext callbackContext) {
        validateAppReg();

		final IWXAPI api = getWXAPI();

		final SendAuth.Req req = new SendAuth.Req();
		req.state = "wechat_auth";
		
		// check if # of arguments is correct
		if (args.length() > 0) {
			try {
				req.scope = args.getString(0);
			} catch (Exception e) {
				Log.e(Wechat.class.getName()
						, "sendAuthRequest parameter parsing failure"
						, e);
			}
		}
		else
		{
			req.scope = "snsapi_userinfo";
		}
		api.sendReq(req);
		currentCallbackContext = callbackContext;
		
		return true;
	}

	protected boolean share(JSONArray args, CallbackContext callbackContext) throws JSONException {
        validateAppReg();

		final IWXAPI api = getWXAPI();

		// check if installed
		if (!api.isWXAppInstalled()) {
			callbackContext.error(ERROR_WX_NOT_INSTALLED);
			return false;
		}

		// check if # of arguments is correct
		if (args.length() != 1) {
			callbackContext.error(ERROR_ARGUMENTS);
		}

		final JSONObject params = args.getJSONObject(0);
		final SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction(null);

		if (params.has(KEY_ARG_SCENE)) {
			int scene = params.getInt(KEY_ARG_SCENE);
			switch(scene)
			{
			case SCENE_FAVORITE:
				req.scene = SendMessageToWX.Req.WXSceneFavorite;
				break;
			case SCENE_TIMELINE:
				req.scene = SendMessageToWX.Req.WXSceneTimeline;
				break;
			case SCENE_SESSION:
				req.scene = SendMessageToWX.Req.WXSceneSession;
				break;
			}
		} else {
			req.scene = SendMessageToWX.Req.WXSceneTimeline;
		}

		// run in background, do not run it in main thread, or else will trigger android.os.NetworkOnMainThreadException
		cordova.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				try {
					req.message = buildSharingMessage(params);
				} catch (JSONException e) {
					Log.e("Wechat", "Sharing error", e);
				}
				api.sendReq(req);
			}
		});

		// save the current callback context
		currentCallbackContext = callbackContext;
		return true;
	}

	protected boolean isInstalled(CallbackContext callbackContext){
        validateAppReg();

		final IWXAPI api = getWXAPI();
		if (! api.isWXAppInstalled()) {
			callbackContext.error(ERROR_WX_NOT_INSTALLED);
			return false;
		} else {
			callbackContext.success("true");
		}
		currentCallbackContext = callbackContext;
		return true;
	}

    protected InputStream inputStreamFromURL(String url) throws IOException {
        if(url.startsWith("http://") || url.startsWith("https://")) {
            URL netUrl = new URL( url );
            return netUrl.openConnection().getInputStream();

        } else if(url.startsWith("/")) {
            return new FileInputStream( new File(url) );

        } else {
            return getClass().getResourceAsStream("/assets/" + url);
        }
	}

	protected WXMediaMessage buildSharingMessage(JSONObject params) throws JSONException {

		WXMediaMessage wxMediaMessage = new WXMediaMessage();
		
		if(params.has(KEY_ARG_TEXT))
		{
            WXTextObject textObject = new WXTextObject();
            textObject.text = params.getString(KEY_ARG_TEXT);

            wxMediaMessage.description = textObject.text;
            wxMediaMessage.mediaObject = textObject;

		} else {
			JSONObject message = params.getJSONObject(KEY_ARG_MESSAGE);
			JSONObject media = message.getJSONObject(KEY_ARG_MESSAGE_MEDIA);

			wxMediaMessage.title = message.getString(KEY_ARG_MESSAGE_TITLE);
			wxMediaMessage.description = message.getString(KEY_ARG_MESSAGE_DESCRIPTION);

            if(message.has(KEY_ARG_MESSAGE_THUMB)) {
                try {
                    String thumb = message.getString(KEY_ARG_MESSAGE_THUMB);
                    if((thumb != null) && (thumb.length()>0)) {
                        InputStream in = inputStreamFromURL( thumb );
                        if(in != null) {
                            byte[] data = IOUtils.toByteArray(in);
                            if(data != null) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                if (bitmap != null) {
                                    wxMediaMessage.setThumbImage(bitmap);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("Wechat", "Thumb URL parsing error", e);
                }
            }

			// check types
			int type = media.has(KEY_ARG_MESSAGE_MEDIA_TYPE) ?
					media.getInt(KEY_ARG_MESSAGE_MEDIA_TYPE) : TYPE_WX_SHARING_WEBPAGE;

			switch (type) {
			case TYPE_WX_SHARING_APP:
				WXAppExtendObject appObject = new WXAppExtendObject();
				appObject.extInfo = media.getString(KEY_ARG_MESSAGE_MEDIA_EXTINFO);
				appObject.filePath = media.getString(KEY_ARG_MESSAGE_MEDIA_URL);
				wxMediaMessage.mediaObject = appObject;
				break;

			case TYPE_WX_SHARING_EMOTION:
				WXEmojiObject emoObject = new WXEmojiObject();
				emoObject.emojiPath = media.getString(KEY_ARG_MESSAGE_MEDIA_EMOTION);
				wxMediaMessage.mediaObject = emoObject;
				break;

			case TYPE_WX_SHARING_FILE:
				WXFileObject fileObject = new WXFileObject();
				fileObject.filePath = media.getString(KEY_ARG_MESSAGE_MEDIA_FILE);
				wxMediaMessage.mediaObject = fileObject;
				break;

			case TYPE_WX_SHARING_IMAGE:
				WXImageObject imgObject = new WXImageObject();
				String img = media.getString(KEY_ARG_MESSAGE_MEDIA_IMAGE);
				try {
					InputStream in = inputStreamFromURL(img);
					if(in != null) {
						imgObject.imageData = IOUtils.toByteArray(in);
					}
				} catch (IOException e) {
					Log.e("Wechat", "bad image url or path", e);
					e.printStackTrace();
				}
				wxMediaMessage.mediaObject = imgObject;
				break;

			case TYPE_WX_SHARING_MUSIC:
				WXMusicObject musicObject = new WXMusicObject();
				musicObject.musicUrl = media.getString(KEY_ARG_MESSAGE_MEDIA_MUSICURL);
				musicObject.musicDataUrl = media.getString(KEY_ARG_MESSAGE_MEDIA_MUSICDATAURL);
				wxMediaMessage.mediaObject = musicObject;
				break;

			case TYPE_WX_SHARING_VIDEO:
				WXVideoObject videoObject = new WXVideoObject();
				videoObject.videoUrl = media.getString(KEY_ARG_MESSAGE_MEDIA_VIDEOURL);
				wxMediaMessage.mediaObject = videoObject;
				break;
				
			case TYPE_WX_SHARING_WEBPAGE:
			default:
				WXWebpageObject webObject = new WXWebpageObject();
				webObject.webpageUrl = media.getString(KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL);
				wxMediaMessage.mediaObject = webObject;
			}
		}

		return wxMediaMessage;
	}

	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
	}
}

