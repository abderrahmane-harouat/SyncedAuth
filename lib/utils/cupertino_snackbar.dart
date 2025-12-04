import 'dart:async';
import 'package:flutter/cupertino.dart';

void showCupertinoSnackBar(BuildContext context, String message) {
  showCupertinoModalPopup(
    context: context,
    builder: (context) => Container(
      margin: EdgeInsets.all(20),
      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: CupertinoColors.systemBackground.withValues(alpha: 0.9),
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: CupertinoColors.black.withValues(alpha: 0.1),
            blurRadius: 10,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            CupertinoIcons.checkmark_circle_fill,
            color: CupertinoColors.systemGreen,
            size: 16,
          ),
          SizedBox(width: 8),
          Text(
            message,
            style: CupertinoTheme.of(context).textTheme.textStyle.copyWith(
              fontSize: 14,
              color: CupertinoColors.label,
            ),
          ),
        ],
      ),
    ),
  );

  // Auto dismiss after 2 seconds
  Timer(Duration(seconds: 2), () {
    Navigator.of(context, rootNavigator: true).pop();
  });
}