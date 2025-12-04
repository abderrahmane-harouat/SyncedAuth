import 'package:flutter/cupertino.dart';
import 'core/app.dart';
import 'services/database_helper.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await DatabaseHelper().database; // Initialize database
  runApp(MyApp());
}
