import 'package:flutter/foundation.dart';

enum PlaybackState {
  NONE,
  STOPPED,
  PAUSED,
  PLAYING,
  FAST_FORWARDING,
  REWINDING,
  BUFFERING,
  ERROR,
  CONNECTING,
  SKIPPING_TO_PREVIOUS,
  SKIPPING_TO_NEXT,
  SKIPPING_TO_QUEUE_ITEM,
  POSITION_UNKNOWN
}

// enum PlayerActions {
//   STOP,
//   PAUSE,
//   PLAY,
//   FAST_FORWARD,
//   REWIND,
//   SKIP_TO_PREVIOUS,
//   SKIP_TO_NEXT,
//   SEEK_TO,
// }

class MediaInfo {
  String? title;
  String? artist;
  String? album;
  Uint8List? image;
  PlaybackState state;
  List<Map<String, dynamic>> customAction;

  // static final Map<int, PlayerActions> _playerActionsMap = {
  //   0: PlayerActions.STOP,
  //   1: PlayerActions.PAUSE,
  //   2: PlayerActions.PLAY,
  //   3: PlayerActions.REWIND,
  //   4: PlayerActions.SKIP_TO_PREVIOUS,
  //   5: PlayerActions.SKIP_TO_NEXT,
  //   6: PlayerActions.FAST_FORWARD,
  //   //rating action
  //   8: PlayerActions.SEEK_TO,
  //   //play/pause
  // };

  static final Map<int, PlaybackState> _playbackStateMap = {
    0: PlaybackState.NONE,
    1: PlaybackState.STOPPED,
    2: PlaybackState.PAUSED,
    3: PlaybackState.PLAYING,
    4: PlaybackState.FAST_FORWARDING,
    5: PlaybackState.REWINDING,
    6: PlaybackState.BUFFERING,
    7: PlaybackState.ERROR,
    8: PlaybackState.CONNECTING,
    9: PlaybackState.SKIPPING_TO_PREVIOUS,
    10: PlaybackState.SKIPPING_TO_NEXT,
    11: PlaybackState.SKIPPING_TO_QUEUE_ITEM,
    -1: PlaybackState.POSITION_UNKNOWN,
  };

  MediaInfo.fromMap(Map<String, dynamic> map)
      : title = map['title'],
        artist = map['artist'],
        album = map['album'],
        state = _playbackStateMap[map['state']]!,
        image = map['image'],
        customAction = List.from(map['customAction'])
            .map((e) => Map<String, dynamic>.from(e))
            .toList();
}
