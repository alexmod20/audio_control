import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'audio_control_platform_interface.dart';
import 'media_app_details.dart';
import 'media_info.dart';

class MethodChannelAudioControl extends AudioControlPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('audio_control');

  StreamController<MediaInfo>? _stateChangeStreamController;
  StreamController<List<MediaAppDetails>>? _sessionChangedStreamController;
  StreamController? _sessionDestroyedStreamController;
  MediaInfo? _cachedMediaInfoValue;

  Future<void> _methodCallHandler(MethodCall call) async {
    switch (call.method) {
      case 'stateChanged':
        _cachedMediaInfoValue =
            MediaInfo.fromMap(Map<String, dynamic>.from(call.arguments));
        _stateChangeStreamController?.add(_cachedMediaInfoValue!);
        break;
      case 'sessionDestroyed':
        _sessionDestroyedStreamController?.add(null);
        break;
      case 'sessionChanged':
        final list = List.from(call.arguments);
        _sessionChangedStreamController?.add(list
            .map((item) =>
                MediaAppDetails.fromMap(Map<String, dynamic>.from(item)))
            .toList());
        break;
      default:
        return;
    }
  }

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  setSessionDestroyedListener(
      StreamController sessionDestroyedStreamController) {
    _sessionDestroyedStreamController = sessionDestroyedStreamController;
  }

  @override
  setStateChangeListener(
      StreamController<MediaInfo> stateChangeStreamController) {
    _stateChangeStreamController = stateChangeStreamController;
    _stateChangeStreamController?.add(_cachedMediaInfoValue!);
  }

  @override
  setSessionChangedListener(
      StreamController<List<MediaAppDetails>>? sessionChangedStreamController) {
    _sessionChangedStreamController = sessionChangedStreamController;
  }

  @override
  Future<bool?> initialize() async {
    methodChannel.setMethodCallHandler(_methodCallHandler);
    return await methodChannel.invokeMethod<bool>('initialize');
  }

  @override
  Future<bool?> isInit() async {
    return await methodChannel.invokeMethod<bool>('isInit');
  }

  @override
  Future<List<MediaAppDetails>> getMediaApps() async {
    final list =
        await methodChannel.invokeMethod<List<dynamic>>('getMediaApps');
    if (list != null) {
      return list
          .map((item) =>
              MediaAppDetails.fromMap(Map<String, dynamic>.from(item)))
          .toList();
    } else {
      return [];
    }
  }

  @override
  Future<List<MediaAppDetails>> getActiveSession() async {
    final list =
        await methodChannel.invokeMethod<List<dynamic>>('getActiveSession');
    if (list != null) {
      return list
          .map((item) =>
              MediaAppDetails.fromMap(Map<String, dynamic>.from(item)))
          .toList();
    } else {
      return [];
    }
  }

  @override
  Future<bool> controlMediaApp(String packageName) async {
    return await methodChannel.invokeMethod<dynamic>(
        'controlMediaApp', packageName);
  }

  @override
  Future<MediaInfo> getMediaInfo() async {
    final result = await methodChannel.invokeMethod<dynamic>('getMediaInfo');
    return MediaInfo.fromMap(result);
  }

  @override
  Future<bool> sendAction(int action, int? seek) async {
    return await methodChannel
        .invokeMethod<dynamic>('sendAction', {"action": action, "seek": seek});
  }
}
