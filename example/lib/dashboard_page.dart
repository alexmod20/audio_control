import 'dart:async';

import 'package:audio_control/audio_control.dart';
import 'package:audio_control/media_app_details.dart';
import 'package:audio_control_example/app_card.dart';
import 'package:audio_control_example/player_page.dart';
import 'package:external_app_launcher/external_app_launcher.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class LifecycleEventHandler extends WidgetsBindingObserver {
  final AsyncCallback? resumeCallBack;
  final AsyncCallback? suspendingCallBack;

  LifecycleEventHandler({
    this.resumeCallBack,
    this.suspendingCallBack,
  });

  @override
  Future<void> didChangeAppLifecycleState(AppLifecycleState state) async {
    switch (state) {
      case AppLifecycleState.resumed:
        if (resumeCallBack != null) {
          await resumeCallBack!();
        }
        break;
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
      case AppLifecycleState.detached:
        if (suspendingCallBack != null) {
          await suspendingCallBack!();
        }
        break;
    }
  }
}

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  final StreamController<List<MediaAppDetails>>
      _sessionChangedStreamController = StreamController();
  List<MediaAppDetails> mediaAppDetailsList = [];
  List<MediaAppDetails> sessionAppDetailsList = [];
  bool connectionResult = false;
  late LifecycleEventHandler _observer;

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    _observer = LifecycleEventHandler(
      resumeCallBack: () async {
        setState(() {});
      },
    );
    WidgetsBinding.instance.addObserver(_observer);
    AudioControl.instance
        .setSessionChangedListener(_sessionChangedStreamController);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: FutureBuilder<bool?>(
        future: AudioControl.instance.initialize(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }
          if (snapshot.hasData && snapshot.data!) {
            WidgetsBinding.instance.removeObserver(_observer);
            return Center(
              child: Column(
                children: [
                  const SizedBox(
                    height: 100.0,
                  ),
                  Text(
                    "Sessioni attive",
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  SizedBox(
                    height: 200.0,
                    child: FutureBuilder<List<MediaAppDetails>>(
                      future: AudioControl.instance.getActiveSession(),
                      builder: (context, snapshot) {
                        if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                          sessionAppDetailsList = snapshot.data!;
                        }
                        return StreamBuilder<List<MediaAppDetails>>(
                          stream: _sessionChangedStreamController.stream,
                          builder: (context, snapshot) {
                            if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                              sessionAppDetailsList = snapshot.data!;
                            }
                            if (sessionAppDetailsList.isNotEmpty) {
                              return GridView.builder(
                                gridDelegate:
                                    const SliverGridDelegateWithFixedCrossAxisCount(
                                  crossAxisCount: 4,
                                  childAspectRatio: 0.8,
                                ),
                                itemCount: sessionAppDetailsList.length,
                                itemBuilder: ((context, index) =>
                                    GestureDetector(
                                      onTap: () async {
                                        try {
                                          final result = await AudioControl
                                              .instance
                                              .controlMediaApp(
                                                  sessionAppDetailsList[index]
                                                      .packageName);
                                          if (result) {
                                            Navigator.of(context)
                                                .push(MaterialPageRoute(
                                              builder: (context) =>
                                                  const PlayerPage(),
                                            ));
                                          }
                                        } catch (e) {
                                          showDialog(
                                              context: context,
                                              builder: (context) => AlertDialog(
                                                    content: Text(e.toString()),
                                                    actions: [
                                                      TextButton(
                                                        child: const Text('Ok'),
                                                        onPressed: () {
                                                          Navigator.of(context)
                                                              .pop();
                                                        },
                                                      ),
                                                    ],
                                                  ));
                                        }
                                      },
                                      child: AppCard(
                                        name: sessionAppDetailsList[index]
                                            .appName,
                                        icon: sessionAppDetailsList[index].icon,
                                      ),
                                    )),
                              );
                            } else {
                              return Container();
                            }
                          },
                        );
                      },
                    ),
                  ),
                  Text(
                    "App disponibili",
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  Expanded(
                    child: FutureBuilder<List<MediaAppDetails>>(
                      future: AudioControl.instance.getMediaApps(),
                      builder: (context, snapshot) {
                        if (snapshot.hasData) {
                          mediaAppDetailsList = snapshot.data!;
                          return GridView.builder(
                            gridDelegate:
                                const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 4,
                              childAspectRatio: 0.8,
                            ),
                            itemCount: mediaAppDetailsList.length,
                            itemBuilder: ((context, index) => GestureDetector(
                                  onTap: () async {
                                    LaunchApp.openApp(
                                        androidPackageName:
                                            mediaAppDetailsList[index]
                                                .packageName);
                                  },
                                  child: AppCard(
                                    name: mediaAppDetailsList[index].appName,
                                    icon: mediaAppDetailsList[index].icon,
                                  ),
                                )),
                          );
                        } else {
                          return Center(
                            child: Text(
                              "No Media Apps found",
                              style: Theme.of(context).textTheme.headlineLarge,
                            ),
                          );
                        }
                      },
                    ),
                  ),
                ],
              ),
            );
          } else {
            return Center(
              child: Column(
                children: [
                  const Text("Devi fornire l'autorizzazione per usare l'app."),
                  const SizedBox(
                    height: 12.0,
                  ),
                  ElevatedButton(
                    onPressed: () {
                      AudioControl.instance.initialize();
                      setState(() {});
                    },
                    child: const Text("Inizializza"),
                  ),
                ],
              ),
            );
          }
        },
      ),
    );
  }
}
