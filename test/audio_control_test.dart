import 'package:flutter_test/flutter_test.dart';
import 'package:audio_control/audio_control.dart';
import 'package:audio_control/audio_control_platform_interface.dart';
import 'package:audio_control/audio_control_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockAudioControlPlatform
    with MockPlatformInterfaceMixin
    implements AudioControlPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
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
}
