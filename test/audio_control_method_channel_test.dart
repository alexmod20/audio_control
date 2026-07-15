import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:audio_control/audio_control_method_channel.dart';
import 'package:audio_control/media_info.dart';
import 'package:audio_control/media_app_details.dart';

void main() {
  MethodChannelAudioControl platform = MethodChannelAudioControl();
  const MethodChannel channel = MethodChannel('audio_control');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'getPlatformVersion':
          return '42';
        case 'initialize':
        case 'isInit':
          return true;
        default:
          return null;
      }
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

  group('native → Dart method call routing', () {
    const codec = StandardMethodCodec();

    Future<void> simulateNativeCall(MethodCall call) async {
      final byteData = codec.encodeMethodCall(call);
      await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage('audio_control', byteData, (_) {});
      await Future<void>.delayed(Duration.zero);
    }

    test('stateChanged pushes a decoded MediaInfo onto the state-change stream',
        () async {
      final freshPlatform = MethodChannelAudioControl();
      await freshPlatform.initialize();
      final controller = StreamController<MediaInfo>();
      final received = <MediaInfo>[];
      final sub = controller.stream.listen(received.add);
      freshPlatform.setStateChangeListener(controller);

      await simulateNativeCall(MethodCall('stateChanged', {
        'title': 'Song',
        'artist': 'Artist',
        'album': 'Album',
        'image': null,
        'state': 3,
        'customAction': [],
        'mediaId': 'media-id-123',
        'mediaUri': 'content://media/track/123',
      }));

      expect(received, hasLength(1));
      expect(received.single.title, 'Song');
      expect(received.single.mediaId, 'media-id-123');
      expect(received.single.mediaUri, 'content://media/track/123');

      await sub.cancel();
      await controller.close();
    });

    test('sessionChanged pushes a decoded MediaAppDetails list onto the session stream',
        () async {
      final freshPlatform = MethodChannelAudioControl();
      await freshPlatform.initialize();
      final controller = StreamController<List<MediaAppDetails>>();
      final received = <List<MediaAppDetails>>[];
      final sub = controller.stream.listen(received.add);
      freshPlatform.setSessionChangedListener(controller);

      await simulateNativeCall(MethodCall('sessionChanged', [
        {'packageName': 'com.example.app', 'appName': 'Example', 'icon': null, 'banner': null},
      ]));

      expect(received, hasLength(1));
      expect(received.single.single.packageName, 'com.example.app');

      await sub.cancel();
      await controller.close();
    });

    test('sessionDestroyed pushes an event onto the session-destroyed stream',
        () async {
      final freshPlatform = MethodChannelAudioControl();
      await freshPlatform.initialize();
      final controller = StreamController();
      var eventCount = 0;
      final sub = controller.stream.listen((_) => eventCount++);
      freshPlatform.setSessionDestroyedListener(controller);

      await simulateNativeCall(MethodCall('sessionDestroyed', null));

      expect(eventCount, 1);

      await sub.cancel();
      await controller.close();
    });
  });
}
