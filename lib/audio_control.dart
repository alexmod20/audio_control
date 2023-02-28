import 'dart:async';

import 'audio_control_platform_interface.dart';
import 'media_app_details.dart';
import 'media_info.dart';

enum PlayerActions {
  STOP,
  PAUSE,
  PLAY,
  FAST_FORWARD,
  REWIND,
  SKIP_TO_PREVIOUS,
  SKIP_TO_NEXT,
  SEEK_TO,
}

final Map<PlayerActions, int> actionId = {
  PlayerActions.STOP: 0,
  PlayerActions.PAUSE: 1,
  PlayerActions.PLAY: 2,
  PlayerActions.FAST_FORWARD: 6,
  PlayerActions.REWIND: 3,
  PlayerActions.SKIP_TO_PREVIOUS: 4,
  PlayerActions.SKIP_TO_NEXT: 5,
  PlayerActions.SEEK_TO: 8,
};

class AudioControl {
  static final AudioControl _instance = AudioControl();
  static AudioControl get instance => _instance;

  bool? _isInit = false;
  bool get isInit => _isInit!;

  setSessionDestroyedListener(StreamController streamController) {
    AudioControlPlatform.instance.setSessionDestroyedListener(streamController);
  }

  setStateChangeListener(StreamController<MediaInfo> streamController) {
    AudioControlPlatform.instance.setStateChangeListener(streamController);
  }

  setSessionChangedListener(
      StreamController<List<MediaAppDetails>> sessionChangedStreamController) {
    AudioControlPlatform.instance
        .setSessionChangedListener(sessionChangedStreamController);
  }

  Future<String?> getPlatformVersion() {
    return AudioControlPlatform.instance.getPlatformVersion();
  }

  Future<bool?> initialize() async {
    _isInit = await AudioControlPlatform.instance.initialize();
    return _isInit;
  }

  // Future<bool?> isInit() {
  //   return AudioControlPlatform.instance.isInit();
  // }

  Future<List<MediaAppDetails>> getMediaApps() {
    return AudioControlPlatform.instance.getMediaApps();
  }

  Future<List<MediaAppDetails>> getActiveSession() {
    return AudioControlPlatform.instance.getActiveSession();
  }

  Future<bool> controlMediaApp(String packageName) {
    return AudioControlPlatform.instance.controlMediaApp(packageName);
  }

  Future<MediaInfo> getMediaInfo() {
    return AudioControlPlatform.instance.getMediaInfo();
  }

  Future<bool> sendAction(PlayerActions action, {int? seek}) {
    return AudioControlPlatform.instance.sendAction(actionId[action]!, seek);
  }

  Future<bool> sendCustomAction(String action) {
    return AudioControlPlatform.instance.sendCustomAction(action);
  }
}
