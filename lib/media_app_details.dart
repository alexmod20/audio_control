// ignore_for_file: public_member_api_docs, sort_constructors_first
import 'package:flutter/foundation.dart';

class MediaAppDetails {
  String packageName;
  String appName;
  Uint8List? icon;
  // String? icon;
  Uint8List? banner;

  MediaAppDetails({
    required this.packageName,
    required this.appName,
    this.icon,
    this.banner,
  });

  MediaAppDetails.fromMap(Map<String, dynamic> map)
      : packageName = map["packageName"],
        appName = map["appName"],
        icon = map["icon"],
        banner = map["banner"];
}
