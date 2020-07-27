package tapselladsex;

import android.provider.Settings.Secure;
import android.util.Log;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.opengl.GLSurfaceView;

import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import ir.tapsell.plus.AdRequestCallback;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.AdShowListener;
import ir.tapsell.sdk.Tapsell;
import ir.tapsell.sdk.TapsellShowOptions;
import ir.tapsell.sdk.TapsellAdRequestListener;
import ir.tapsell.sdk.TapsellAdRequestOptions;
import ir.tapsell.sdk.TapsellAdShowListener;

import android.content.Intent;
import android.net.Uri;


public class TapsellEx extends Extension { 

	private static Boolean failRewarded=false;
	private static Boolean loadingRewarded=false;
	private static String rewardedId= null;
	private static String adId = null;

	private static TapsellEx instance=null;
	private static Boolean testingAds=false;

	private static HaxeObject callback=null;

	public static final String FAILED = "FAILED";
	public static final String CLOSED = "CLOSED";
	public static final String DISPLAYING = "DISPLAYING";
	public static final String LOADED = "LOADED";
	public static final String LOADING = "LOADING";
	public static final String EARNED_REWARD = "EARNED_REWARD";

	private TapsellEx() {
		if (testingAds) {
			String android_id = Secure.getString(mainActivity.getContentResolver(), Secure.ANDROID_ID);
			String deviceId = md5(android_id).toUpperCase();
			Log.d("TapsellEx","DEVICE ID: " + deviceId);
			
			TapsellPlus.addFacebookTestDevice(deviceId);
		}

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Log.d("TapsellEx", "reloadRewarded");
				TapsellEx.getInstance().reloadRewarded(TapsellEx.rewardedId);
			}
		}, 5000);
	}

	public static TapsellEx getInstance() {
		if (TapsellEx.instance == null) TapsellEx.instance = new TapsellEx();
		return TapsellEx.instance;
	}

	public static void init(String rewardedId, String appId, boolean testingAds, HaxeObject callback){
		TapsellEx.rewardedId=rewardedId;
		TapsellEx.testingAds=testingAds;
		TapsellEx.callback=callback;

		Log.d("TapsellEx ","init");

        //TapsellPlus.initialize(mainActivity, appId);
        Tapsell.initialize(mainActivity, appId);

		mainActivity.runOnUiThread(new Runnable() {
			public void run() { getInstance(); }
		});
	}

	private static void reportRewardedEvent(final String event) {
		if (callback == null) return;

		if (Extension.mainView == null) return;
		GLSurfaceView view = (GLSurfaceView) Extension.mainView;
		view.queueEvent(new Runnable() {
			public void run() {
				Log.d("TapsellEx ","reportRewardedEvent " + event);
				callback.call("_onRewardedEvent", new Object[] { event } );
			}
		});
	}

	public static void reloadRewardedFunc() {
		Log.d("TapsellEx", "reloadRewardedFunc");
		TapsellEx.getInstance().reloadRewarded(TapsellEx.rewardedId);
	}

	public static void rateApp(final String package_name) {
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setData(Uri.parse("bazaar://details?id=" + package_name));
		intent.setPackage("com.farsitel.bazaar");
		mainActivity.startActivity(intent);
	}

	public static boolean showRewarded(final String rewardedId) {
		if (loadingRewarded) return false;

		if (failRewarded){
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {
					getInstance().reloadRewarded(TapsellEx.rewardedId);
				}
			});
			Log.d("TapsellEx showRewarded","Show Rewarded: Rewarded not loaded... reloading.");
			return false;
		}

		if (TapsellEx.rewardedId=="") {
			Log.d("TapsellEx showRewarded","Show Rewarded: RewardedID is empty... ignoring.");
			return false;
		}

		mainActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {

					//AdShowListener adShowListener = new AdShowListener() {
					TapsellAdShowListener adShowListener = new TapsellAdShowListener() {
						@Override
						public void onOpened() {
							Log.d("Tapsell", "Ad Displaying");
							reportRewardedEvent(TapsellEx.DISPLAYING);
						}
						@Override
						public void onRewarded(boolean completed) { ///
							Log.d("Tapsell", "Reward");
							reportRewardedEvent(TapsellEx.EARNED_REWARD);
						}
						@Override
						public void onClosed() {
							Log.d("Tapsell", "Ad Closed");
							reportRewardedEvent(TapsellEx.CLOSED);
							
							new Handler().postDelayed(new Runnable() {
								@Override
								public void run() {
									TapsellEx.getInstance().reloadRewarded(TapsellEx.rewardedId);
								}
							}, 5000);
						}
						
						@Override
						public void onError(String s) {
							Log.d("Tapsell", "error :" + s);
							TapsellEx.getInstance().failRewarded = true;
							reportRewardedEvent(TapsellEx.FAILED);
						}
					};
					
					int rotation = mainActivity.getWindowManager().getDefaultDisplay().getRotation();
					Log.d("Tapsell rotation", String.valueOf(rotation));
					Log.d("Tapsell orientation", String.valueOf(mainActivity.getResources().getConfiguration().orientation));
					
					TapsellShowOptions showOptions = new TapsellShowOptions();
					if (rotation == 3) showOptions.setRotationMode(TapsellShowOptions.ROTATION_LOCKED_REVERSED_LANDSCAPE);
					else showOptions.setRotationMode(TapsellShowOptions.ROTATION_LOCKED_LANDSCAPE);

					Tapsell.showAd(mainActivity,
							TapsellEx.rewardedId,
							TapsellEx.adId,
							showOptions,
							adShowListener
							);

					/*
					TapsellPlus.showAd(
						mainActivity,
						TapsellEx.rewardedId,
						adShowListener
					);
					*/

			}
		});

		return true;
	}

	private void reloadRewarded(String rewardedId){
		if(rewardedId=="") return;
		if(loadingRewarded) return;
		Log.d("TapsellEx","Reload Rewarded");

		loadingRewarded=true;
		failRewarded=false;
		
		reportRewardedEvent(TapsellEx.LOADING);

		/*
		AdRequestCallback adRequestCallback = new AdRequestCallback() {
			@Override
			public void response() {
				Log.d("Tapsell", "Ad Response");
				TapsellEx.getInstance().loadingRewarded=false;
				reportRewardedEvent(TapsellEx.LOADED);
			}
			@Override
			public void error(String message) {
				Log.e("Tapsell error", message);
				TapsellEx.getInstance().loadingRewarded=false;
				TapsellEx.getInstance().failRewarded=true;
				reportRewardedEvent(TapsellEx.FAILED);
			}
		};
		
		TapsellPlus.requestRewardedVideo(
			mainActivity,
			TapsellEx.rewardedId,
			adRequestCallback
		);
		*/

		Tapsell.requestAd(mainActivity,
			TapsellEx.rewardedId,
			new TapsellAdRequestOptions(),
			new TapsellAdRequestListener() {

				@Override
				public void onAdAvailable(String adId) {
					Log.d("tapsell", "AdAvailable");

					TapsellEx.getInstance().adId = adId;
					TapsellEx.getInstance().loadingRewarded=false;
				
					reportRewardedEvent(TapsellEx.LOADED);
				}

				@Override
				public void onError(String message) {

					TapsellEx.getInstance().loadingRewarded=false;
					TapsellEx.getInstance().failRewarded=true;
					reportRewardedEvent(TapsellEx.FAILED);
					
					Log.d("tapsell", "ad error" + message);
					
				}
			});
		
	}

	private static String md5(String s)  {
		MessageDigest digest;
		try  {
			digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes(),0,s.length());
			String hexDigest = new java.math.BigInteger(1, digest.digest()).toString(16);
			if (hexDigest.length() >= 32) return hexDigest;
			else return "00000000000000000000000000000000".substring(hexDigest.length()) + hexDigest;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

}
