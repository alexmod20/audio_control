import 'dart:async';

import 'package:audio_control/audio_control.dart';
import 'package:audio_control/media_info.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:wakelock/wakelock.dart';

final Map<PlayerActions, IconData> actionIcons = {
  PlayerActions.STOP: Icons.stop,
  PlayerActions.PAUSE: Icons.pause,
  PlayerActions.PLAY: Icons.play_arrow,
  PlayerActions.FAST_FORWARD: Icons.fast_forward,
  PlayerActions.REWIND: Icons.fast_rewind,
  PlayerActions.SKIP_TO_PREVIOUS: Icons.skip_previous,
  PlayerActions.SKIP_TO_NEXT: Icons.skip_next,
};

class PlayerPage extends StatefulWidget {
  const PlayerPage({super.key});

  @override
  State<PlayerPage> createState() => _PlayerPageState();
}

class _PlayerPageState extends State<PlayerPage> {
  StreamSubscription? sessionDestroyedSubscription;
  StreamController sessionDestroyedStreamController = StreamController();
  StreamController<MediaInfo> stateChangeStreamController = StreamController();
  late MediaInfo _currentMediaInfo;

  @override
  void initState() {
    super.initState();
    Wakelock.enable();
    AudioControl.instance
        .setSessionDestroyedListener(sessionDestroyedStreamController);
    AudioControl.instance.setStateChangeListener(stateChangeStreamController);
    sessionDestroyedSubscription =
        sessionDestroyedStreamController.stream.listen((event) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Session is expired"),
          duration: Duration(seconds: 2),
        ),
      );
      Navigator.of(context).pop();
    });
  }

  @override
  void dispose() {
    super.dispose();
    sessionDestroyedSubscription?.cancel();
    sessionDestroyedStreamController.close();
    stateChangeStreamController.close();
    Wakelock.disable();
  }

  Widget _buildPlayerButtons(PlaybackState state) {
    Widget centerButton;
    switch (state) {
      case PlaybackState.PLAYING:
        centerButton = IconButton(
          iconSize: 60.0,
          onPressed: () {
            AudioControl.instance.sendAction(PlayerActions.PAUSE);
          },
          icon: Icon(actionIcons[PlayerActions.PAUSE]!),
        );
        break;
      case PlaybackState.PAUSED:
        centerButton = IconButton(
          iconSize: 60.0,
          onPressed: () {
            AudioControl.instance.sendAction(PlayerActions.PLAY);
          },
          icon: Icon(actionIcons[PlayerActions.PLAY]!),
        );
        break;
      case PlaybackState.BUFFERING:
        centerButton = const CircularProgressIndicator();
        break;
      default:
        centerButton = IconButton(
          iconSize: 60.0,
          onPressed: () {},
          icon: const Icon(Icons.question_mark),
        );
        break;
    }
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          IconButton(
            iconSize: 60.0,
            onPressed: () {
              AudioControl.instance.sendAction(PlayerActions.SKIP_TO_PREVIOUS);
            },
            icon: Icon(actionIcons[PlayerActions.SKIP_TO_PREVIOUS]),
          ),
          centerButton,
          IconButton(
            iconSize: 60.0,
            onPressed: () {
              AudioControl.instance.sendAction(PlayerActions.SKIP_TO_NEXT);
            },
            icon: Icon(actionIcons[PlayerActions.SKIP_TO_NEXT]),
          ),
        ],
      ),
    );
  }

  Widget _buildCustomActionButtons(List<Map<String, dynamic>> actions) {
    if (actions.isEmpty) {
      return Container();
    } else {
      return Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: actions
            .map((action) => SizedBox(
                  height: 50.0,
                  width: 50.0,
                  child: IconButton(
                    onPressed: () {
                      AudioControl.instance.sendCustomAction(action["action"]);
                    },
                    icon: FadeInImage(
                      image: MemoryImage(action["icon"]),
                      placeholder: MemoryImage(action["icon"]),
                    ),
                  ),
                ))
            .toList(),
      );
    }
  }

  Widget _buildAlbumImage(Uint8List? image, String title) {
    if (image != null) {
      return FadeInImage(
        image: MemoryImage(image),
        placeholder: MemoryImage(image),
      );
    } else {
      return const Icon(Icons.album);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: StreamBuilder<MediaInfo>(
        stream: stateChangeStreamController.stream,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const CircularProgressIndicator();
          }
          if (!snapshot.hasData) {
            return Center(
              child: IconButton(
                onPressed: () {},
                icon: const Icon(Icons.warning),
              ),
            );
          }
          _currentMediaInfo = snapshot.data!;
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SizedBox(
                height: MediaQuery.of(context).viewPadding.top,
              ),
              Align(
                alignment: Alignment.centerRight,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: IconButton(
                    icon: const Icon(Icons.close_rounded),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20.0,
                  vertical: 50.0,
                ),
                child: Column(
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        _buildAlbumImage(
                          _currentMediaInfo.image,
                          _currentMediaInfo.title!,
                        ),
                        const SizedBox(
                          height: 50.0,
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            Text(
                              _currentMediaInfo.title.toString(),
                              style: Theme.of(context).textTheme.bodyLarge,
                              textAlign: TextAlign.center,
                            ),
                            Text(
                              _currentMediaInfo.artist.toString(),
                              style: Theme.of(context).textTheme.bodyLarge,
                              textAlign: TextAlign.center,
                            ),
                            Text(
                              _currentMediaInfo.album.toString(),
                              style: Theme.of(context).textTheme.bodyLarge,
                              textAlign: TextAlign.center,
                            ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(
                      height: 50.0,
                    ),
                    _buildCustomActionButtons(_currentMediaInfo.customAction),
                    const SizedBox(
                      height: 12.0,
                    ),
                    _buildPlayerButtons(_currentMediaInfo.state),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
