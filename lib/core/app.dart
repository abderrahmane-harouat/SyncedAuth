import 'package:flutter/cupertino.dart';
import '../screens/authenticator_home.dart';
import '../theme.dart';

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return CupertinoApp(
      debugShowCheckedModeBanner: false,
      title: 'تطبيق التحقق',
      theme: AppTheme.lightTheme,
      home: AuthenticatorHome(),
    );
  }
}