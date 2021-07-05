package extension.tapsellads;

import haxe.Json;
import nme.Lib;
import nme.JNI;


class TapsellAds {

	private static var initialized:Bool=false;
	private static var testingAds:Bool=false;

	private static var __init:String->String->Bool->Dynamic->Void = function(rewardedId:String, appId:String, testingAds:Bool, callback:Dynamic){};

	private static var __showRewarded:String->Bool = function(rewardedId:String){ return false; };
	private static var __rateApp:String->Void = function(package_name:String) { };
	private static var __reloadRewardedFunc:Void->Void = function() { };

	private static var completeCB:Void->Void;
	private static var skipCB:Void->Void;
	private static var viewCB:Void->Void;
	private static var rewardFlag:Bool;
	private static var _rewardedId:String;
	private static var _appId:String;
	private static var canshow:Bool = false;


	public static function rateApp(package_name:String) {
		try {
			return __rateApp(package_name);
		} catch (e:Dynamic){
			trace("rateApp Exception: " + e);
		}
	}

	public static function reloadRewardedFunc() {
		try {
			return __reloadRewardedFunc();
		} catch (e:Dynamic){
			trace("reloadRewardedFunc Exception: " + e);
		}
	}

	public static function canShowAds():Bool {
		return canshow;
	}

	public static function showRewarded(cb, skip, view):Bool {
		completeCB = cb;
		skipCB = skip;
		viewCB = view;
		canshow = false;

		try{
			return __showRewarded(TapsellAds._rewardedId);
		}catch(e:Dynamic){
			trace("ShowRewarded Exception: "+e);
		}
		return false;
	}
	
	public static function enableTestingAds() {
		if ( testingAds ) return;
		if ( initialized ) {
			var msg:String;
			msg = "FATAL ERROR: If you want to enable Testing Ads, you must enable them before calling INIT!.\n";
			msg+= "Throwing an exception to avoid displaying read ads when you want testing ads.";
			trace(msg);
			throw msg;
			return;
		}
		testingAds = true;
	}

	public static function init(rewardedId:String, appId:String){

		TapsellAds._rewardedId = rewardedId;
		TapsellAds._appId = appId;

		if (initialized) return;
		initialized = true;

		#if android
		try{
			__init = JNI.createStaticMethod("tapselladsex/TapsellEx", "init", "(Ljava/lang/String;Ljava/lang/String;ZLorg/haxe/lime/HaxeObject;)V");
			__showRewarded = JNI.createStaticMethod("tapselladsex/TapsellEx", "showRewarded", "(Ljava/lang/String;)Z");
			__reloadRewardedFunc = JNI.createStaticMethod("tapselladsex/TapsellEx", "reloadRewardedFunc", "()V");
			__rateApp = JNI.createStaticMethod("tapselladsex/TapsellEx", "rateApp", "(Ljava/lang/String;)V");
			__init(rewardedId, appId, testingAds, instance);
		}catch(e:Dynamic){
			trace("Android INIT Exception: "+e);
		}
		#end
	}
	
	public static inline var FAILED:String = "FAILED";
	public static inline var CLOSED:String = "CLOSED";
	public static inline var LOADED:String = "LOADED";
	public static inline var DISPLAYING:String = "DISPLAYING";
	public static inline var EARNED_REWARD:String = "EARNED_REWARD";

	public static var onRewardedEvent:String->Void = null;
	private static var instance:TapsellAds = new TapsellAds();

	private function new(){}

	public function _onEvent(event:String){
		
	}

	public function _onRewardedEvent(event:String) {
		if (onRewardedEvent != null) {
			try{
				
				trace("tapsell onRewardedEvent", event);

				switch (event)
				{
					case LOADED:
						canshow = true;

					case EARNED_REWARD:
						rewardFlag = true;

					case DISPLAYING:
						if (viewCB != null)
							viewCB();

					case FAILED:
						if (skipCB != null)
							skipCB();

						rewardFlag = false;
						canshow = false;

					case CLOSED:
						if (rewardFlag)
						{
							if (completeCB != null)
								completeCB();
						}
						else
						{
							if (skipCB != null)
								skipCB();
						}
						rewardFlag = false;
						canshow = false;
				}

				onRewardedEvent(event);
			}
			catch(err:Dynamic){
				trace("tapsell ERROR PARSING ", " err : ", err);
			}
		}
		else trace("Rewarded event: "+event+ " (assign TapsellAds.onRewardedEvent to get this events and avoid this traces)");
	}
	
}
