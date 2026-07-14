import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:audio_control/audio_control_method_channel.dart';
import 'package:audio_control/media_info.dart';

void main() {
  MethodChannelAudioControl platform = MethodChannelAudioControl();
  const MethodChannel channel = MethodChannel('audio_control');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });

  test(
      'setStateChangeListener does not crash when no stateChanged event has arrived yet',
      () async {
    final freshPlatform = MethodChannelAudioControl();
    final controller = StreamController<MediaInfo>.broadcast();

    freshPlatform.setStateChangeListener(controller);

    await controller.close();
  });

  test('dispose() does not throw', () {
    final freshPlatform = MethodChannelAudioControl();

    expect(() => freshPlatform.dispose(), returnsNormally);
  });
}
