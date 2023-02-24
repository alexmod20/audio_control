import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class AppCard extends StatelessWidget {
  final Uint8List? icon;
  final String name;

  const AppCard({required this.name, this.icon, super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (icon != null)
              SizedBox(
                height: 48.0,
                child: Image.memory(icon!),
              ),
            const SizedBox(
              height: 8.0,
            ),
            Text(name)
          ],
        ),
      ),
    );
  }
}
