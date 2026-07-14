import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:audio_control/audio_control.dart';
import 'package:audio_control/audio_control_platform_interface.dart';
import 'package:audio_control/audio_control_method_channel.dart';
import 'package:audio_control/media_app_details.dart';
import 'package:audio_control/media_info.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockAudioControlPlatform
    with MockPlatformInterfaceMixin
    implements AudioControlPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  void dispose() {}

  @override
  void setSessionDestroyedListener(StreamController sessionDestroyedStreamController) {}

  @override
  void setStateChangeListener(StreamController<MediaInfo> stateChangeStreamController) {}

  @override
  void setSessionChangedListener(
      StreamController<List<MediaAppDetails>>? sessionChangedStreamController) {}

  @override
  Future<bool?> initialize() => Future.value(true);

  @override
  Future<bool?> isInit() => Future.value(true);

  @override
  Future<List<MediaAppDetails>> getMediaApps() => Future.value([]);

  @override
  Future<List<MediaAppDetails>> getActiveSession() => Future.value([]);

  @override
  Future<bool> controlMediaApp(String packageName) => Future.value(true);

  @override
  Future<MediaInfo> getMediaInfo() => throw UnimplementedError();

  @override
  Future<bool> sendAction(int action, int? seek) => Future.value(true);

  @override
  Future<bool> sendCustomAction(String action) => Future.value(true);
}

class _NullInitializePlatform extends MockAudioControlPlatform {
  @override
  Future<bool?> initialize() => Future.value(null);
}

void main() {
  final AudioControlPlatform initialPlatform = AudioControlPlatform.instance;

  test('$MethodChannelAudioControl is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelAudioControl>());
  });

  test('getPlatformVersion', () async {
    AudioControl audioControlPlugin = AudioControl();
    MockAudioControlPlatform fakePlatform = MockAudioControlPlatform();
    AudioControlPlatform.instance = fakePlatform;

    expect(await audioControlPlugin.getPlatformVersion(), '42');
  });

  test('isInit stays false, and does not crash, when initialize() resolves to null',
      () async {
    AudioControl audioControlPlugin = AudioControl();
    AudioControlPlatform.instance = _NullInitializePlatform();

    await audioControlPlugin.initialize();

    expect(audioControlPlugin.isInit, false);
  });
}
