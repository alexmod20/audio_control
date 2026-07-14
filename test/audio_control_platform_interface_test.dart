import 'package:flutter_test/flutter_test.dart';
import 'package:audio_control/audio_control_platform_interface.dart';

class _BareAudioControlPlatform extends AudioControlPlatform {}

void main() {
  late _BareAudioControlPlatform platform;

  setUp(() {
    platform = _BareAudioControlPlatform();
  });

  test('getActiveSession() reports its own method name when unimplemented', () {
    expect(
      () => platform.getActiveSession(),
      throwsA(isA<UnimplementedError>().having(
        (e) => e.message,
        'message',
        'getActiveSession() has not been implemented.',
      )),
    );
  });

  test('sendAction() reports its own method name when unimplemented', () {
    expect(
      () => platform.sendAction(0, null),
      throwsA(isA<UnimplementedError>().having(
        (e) => e.message,
        'message',
        'sendAction() has not been implemented.',
      )),
    );
  });

  test('sendCustomAction() reports its own method name when unimplemented', () {
    expect(
      () => platform.sendCustomAction('foo'),
      throwsA(isA<UnimplementedError>().having(
        (e) => e.message,
        'message',
        'sendCustomAction() has not been implemented.',
      )),
    );
  });
}
