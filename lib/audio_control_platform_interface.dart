import 'dart:async';

import 'package:audio_control/media_app_details.dart';
import 'package:audio_control/media_info.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'audio_control_method_channel.dart';

abstract class AudioControlPlatform extends PlatformInterface {
  /// Constructs a AudioControlPlatform.
  AudioControlPlatform() : super(token: _token);

  static final Object _token = Object();

  static AudioControlPlatform _instance = MethodChannelAudioControl();

  /// The default instance of [AudioControlPlatform] to use.
  ///
  /// Defaults to [MethodChannelAudioControl].
  static AudioControlPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [AudioControlPlatform] when
  /// they register themselves.
  static set instance(AudioControlPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  dispose() {
    throw UnimplementedError('dispose() has not been implemented.');
  }

  setSessionDestroyedListener(
      StreamController sessionDestroyedStreamController) {
    throw UnimplementedError(
        'setSessionDestroyedListener() has not been implemented.');
  }

  setStateChangeListener(
      StreamController<MediaInfo> stateChangeStreamController) {
    throw UnimplementedError(
        'setStateChangeListener() has not been implemented.');
  }

  setSessionChangedListener(
      StreamController<List<MediaAppDetails>>? sessionChangedStreamController) {
    throw UnimplementedError(
        'setSessionChangedListener() has not been implemented.');
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<bool?> initialize() {
    throw UnimplementedError('initialize() has not been implemented.');
  }

  Future<bool?> isInit() {
    throw UnimplementedError('isInit() has not been implemented.');
  }

  Future<List<MediaAppDetails>> getMediaApps() {
    throw UnimplementedError('getMediaApps() has not been implemented.');
  }

  Future<List<MediaAppDetails>> getActiveSession() {
    throw UnimplementedError('initialize() has not been implemented.');
  }

  Future<bool> controlMediaApp(String packageName) {
    throw UnimplementedError('controlMediaApp() has not been implemented.');
  }

  Future<MediaInfo> getMediaInfo() {
    throw UnimplementedError('getMediaInfo() has not been implemented.');
  }

  Future<bool> sendAction(int action, int? seek) {
    throw UnimplementedError('getMediaInfo() has not been implemented.');
  }
}
