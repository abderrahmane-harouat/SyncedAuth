import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';
import 'dart:async';
import 'dart:io';
import 'package:crypto/crypto.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as path;
import 'package:file_picker/file_picker.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await DatabaseHelper().database; // Initialize database
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return CupertinoApp(
      debugShowCheckedModeBanner: false,
      title: 'Authenticator',
      theme: CupertinoThemeData(
        brightness: Brightness.light,
        primaryColor: CupertinoColors.activeBlue,
        barBackgroundColor: CupertinoColors.systemGrey6,
        scaffoldBackgroundColor: CupertinoColors.systemGroupedBackground,
      ),
      home: AuthenticatorHome(),
    );
  }
}

class TOTPAccount {
  final String name;
  final String issuer;
  final String secret;
  final int digits;
  final int period;

  TOTPAccount({
    required this.name,
    required this.issuer,
    required this.secret,
    this.digits = 6,
    this.period = 30,
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'issuer': issuer,
      'secret': secret,
      'digits': digits,
      'period': period,
    };
  }

  factory TOTPAccount.fromJson(Map<String, dynamic> json) {
    return TOTPAccount(
      name: json['name'],
      issuer: json['issuer'],
      secret: json['secret'],
      digits: json['digits'] ?? 6,
      period: json['period'] ?? 30,
    );
  }
}

class TOTPGenerator {
  static String generateTOTP(String secret, {int digits = 6, int period = 30}) {
    final int timestamp = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    final int timeStep = timestamp ~/ period;

    // Decode base32 secret
    final List<int> secretBytes = _base32Decode(secret);

    // Create time-based counter
    final ByteData timeData = ByteData(8);
    timeData.setUint64(0, timeStep, Endian.big);

    // HMAC-SHA1
    final Hmac hmac = Hmac(sha1, secretBytes);
    final List<int> hash = hmac.convert(timeData.buffer.asUint8List()).bytes;

    // Dynamic truncation
    final int offset = hash[hash.length - 1] & 0x0F;
    final int truncatedHash =
        ((hash[offset] & 0x7F) << 24) |
        ((hash[offset + 1] & 0xFF) << 16) |
        ((hash[offset + 2] & 0xFF) << 8) |
        (hash[offset + 3] & 0xFF);

    final int otp = truncatedHash % pow(10, digits).toInt();
    return otp.toString().padLeft(digits, '0');
  }

  static int getRemainingSeconds({int period = 30}) {
    final int timestamp = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    return period - (timestamp % period);
  }

  static List<int> _base32Decode(String input) {
    const String base32Chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    input = input.toUpperCase().replaceAll(RegExp(r'[^A-Z2-7]'), '');

    final List<int> output = [];
    int buffer = 0;
    int bitsLeft = 0;

    for (int i = 0; i < input.length; i++) {
      final int val = base32Chars.indexOf(input[i]);
      if (val == -1) continue;

      buffer = (buffer << 5) | val;
      bitsLeft += 5;

      if (bitsLeft >= 8) {
        output.add((buffer >> (bitsLeft - 8)) & 255);
        bitsLeft -= 8;
      }
    }

    return output;
  }
}

class DatabaseHelper {
  static final DatabaseHelper _instance = DatabaseHelper._internal();
  factory DatabaseHelper() => _instance;
  DatabaseHelper._internal();

  static Database? _database;

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    String pathStr = path.join(await getDatabasesPath(), 'authenticator.db');
    return await openDatabase(pathStr, version: 1, onCreate: _onCreate);
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE accounts(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        issuer TEXT NOT NULL,
        secret TEXT NOT NULL,
        digits INTEGER NOT NULL DEFAULT 6,
        period INTEGER NOT NULL DEFAULT 30
      )
    ''');
  }

  Future<List<TOTPAccount>> getAccounts() async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query('accounts');
    return List.generate(maps.length, (i) {
      return TOTPAccount(
        name: maps[i]['name'],
        issuer: maps[i]['issuer'],
        secret: maps[i]['secret'],
        digits: maps[i]['digits'],
        period: maps[i]['period'],
      );
    });
  }

  Future<void> insertAccount(TOTPAccount account) async {
    final db = await database;
    await db.insert('accounts', {
      'name': account.name,
      'issuer': account.issuer,
      'secret': account.secret,
      'digits': account.digits,
      'period': account.period,
    }, conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<void> deleteAccount(TOTPAccount account) async {
    final db = await database;
    await db.delete(
      'accounts',
      where: 'name = ? AND issuer = ? AND secret = ?',
      whereArgs: [account.name, account.issuer, account.secret],
    );
  }

  Future<void> clearAllAccounts() async {
    final db = await database;
    await db.delete('accounts');
  }

  Future<String> exportAccountsToJson() async {
    final accounts = await getAccounts();
    final exportData = {
      'version': '1.0',
      'exported_at': DateTime.now().toIso8601String(),
      'accounts': accounts.map((account) => account.toJson()).toList(),
    };
    return jsonEncode(exportData);
  }

  Future<List<TOTPAccount>> importAccountsFromJson(String jsonString) async {
    try {
      final Map<String, dynamic> data = jsonDecode(jsonString);
      final List<dynamic> accountsData = data['accounts'] ?? [];
      return accountsData
          .map((accountData) => TOTPAccount.fromJson(accountData))
          .toList();
    } catch (e) {
      throw Exception('Invalid JSON format: $e');
    }
  }
}

class AuthenticatorHome extends StatefulWidget {
  @override
  _AuthenticatorHomeState createState() => _AuthenticatorHomeState();
}

class _AuthenticatorHomeState extends State<AuthenticatorHome> {
  List<TOTPAccount> accounts = [];
  final DatabaseHelper _dbHelper = DatabaseHelper();

  @override
  void initState() {
    super.initState();
    _loadAccounts();
  }

  Future<void> _loadAccounts() async {
    try {
      final loadedAccounts = await _dbHelper.getAccounts();
      setState(() {
        accounts = loadedAccounts;
      });
    } catch (e) {
      print('Error loading accounts: $e');
      setState(() {
        accounts = [];
      });
    }
  }

  Future<void> _addAccount(TOTPAccount account) async {
    try {
      await _dbHelper.insertAccount(account);
      await _loadAccounts();
    } catch (e) {
      print('Error adding account: $e');
    }
  }

  Future<void> _deleteAccount(int index) async {
    try {
      await _dbHelper.deleteAccount(accounts[index]);
      await _loadAccounts();
    } catch (e) {
      print('Error deleting account: $e');
    }
  }

  Future<void> _exportData() async {
    try {
      final jsonData = await _dbHelper.exportAccountsToJson();

      String? outputFile = await FilePicker.platform.saveFile(
        dialogTitle: 'Export Authenticator Data',
        fileName:
            'authenticator_backup_${DateTime.now().millisecondsSinceEpoch}.json',
        type: FileType.custom,
        allowedExtensions: ['json'],
        bytes: utf8.encode(jsonData),
      );

      if (outputFile != null && mounted) {
        // Show success dialog
        showCupertinoDialog(
          context: context,
          builder: (ctx) => CupertinoAlertDialog(
            title: Text("Success"),
            content: Text("Data exported successfully."),
            actions: [
              CupertinoDialogAction(
                isDefaultAction: true,
                onPressed: () => Navigator.pop(ctx),
                child: Text("OK"),
              ),
            ],
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        // Show error dialog
        showCupertinoDialog(
          context: context,
          builder: (ctx) => CupertinoAlertDialog(
            title: Text("Error"),
            content: Text("Export failed: $e"),
            actions: [
              CupertinoDialogAction(
                isDestructiveAction: true,
                onPressed: () => Navigator.pop(ctx),
                child: Text("OK"),
              ),
            ],
          ),
        );
      }
    }
  }

  Future<void> _importData() async {
    try {
      FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
        dialogTitle: 'Import Authenticator Data',
      );

      if (result != null && result.files.single.path != null) {
        final file = File(result.files.single.path!);
        final jsonString = await file.readAsString();
        final importedAccounts = await _dbHelper.importAccountsFromJson(
          jsonString,
        );

        if (mounted) {
          showCupertinoDialog(
            context: context,
            builder: (ctx) => CupertinoAlertDialog(
              title: Text('Import Data'),
              content: Text(
                'Found ${importedAccounts.length} accounts. This will replace all existing accounts. Continue?',
              ),
              actions: [
                CupertinoDialogAction(
                  onPressed: () => Navigator.pop(ctx),
                  child: Text('Cancel'),
                ),
                CupertinoDialogAction(
                  isDefaultAction: true,
                  onPressed: () async {
                    Navigator.pop(ctx);
                    await _dbHelper.clearAllAccounts();
                    for (final account in importedAccounts) {
                      await _dbHelper.insertAccount(account);
                    }
                    await _loadAccounts();

                    if (mounted) {
                      // Show success dialog
                      showCupertinoDialog(
                        context: context,
                        builder: (ctx2) => CupertinoAlertDialog(
                          title: Text("Success"),
                          content: Text("Data imported successfully."),
                          actions: [
                            CupertinoDialogAction(
                              isDefaultAction: true,
                              onPressed: () => Navigator.pop(ctx2),
                              child: Text("OK"),
                            ),
                          ],
                        ),
                      );
                    }
                  },
                  child: Text('Import'),
                ),
              ],
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        showCupertinoDialog(
          context: context,
          builder: (ctx) => CupertinoAlertDialog(
            title: Text("Error"),
            content: Text("Import failed: $e"),
            actions: [
              CupertinoDialogAction(
                isDestructiveAction: true,
                onPressed: () => Navigator.pop(ctx),
                child: Text("OK"),
              ),
            ],
          ),
        );
      }
    }
  }

void _showAddAccountDialog() {
  Navigator.of(context, rootNavigator: true).push(
    CupertinoPageRoute(
      fullscreenDialog: true, // This makes the page a modal dialog with a top-down animation
      builder: (context) => AddAccountPage(onAccountAdded: _addAccount),
    ),
  );
}

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        middle: Text('Authenticator'),
        trailing: Row(
          mainAxisSize: MainAxisSize
              .min, // Use min size to prevent the Row from taking up full width.
          children: [
            // The 'Add Account' button.
            if (accounts.isNotEmpty)
              CupertinoButton(
                onPressed: _showAddAccountDialog,
                padding: EdgeInsets.zero,
                child: Icon(
                  CupertinoIcons.add_circled,
                  size: 28,
                ), // Use a different icon for better visual balance.
              ),
            // The three-dot ellipsis button.
            CupertinoButton(
              padding: EdgeInsets.zero,
              child: Icon(CupertinoIcons.ellipsis, size: 28),
              onPressed: () {
                showCupertinoModalPopup(
                  context: context,
                  builder: (BuildContext context) => CupertinoActionSheet(
                    title: Text("Options"),
                    actions: [
                      CupertinoActionSheetAction(
                        onPressed: () {
                          Navigator.pop(context);
                          _exportData();
                        },
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              CupertinoIcons.upload_circle,
                              color: CupertinoColors.activeBlue,
                            ),
                            SizedBox(width: 8),
                            Text("Export Data"),
                          ],
                        ),
                      ),
                      CupertinoActionSheetAction(
                        onPressed: () {
                          Navigator.pop(context);
                          _importData();
                        },
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              CupertinoIcons.download_circle,
                              color: CupertinoColors.activeBlue,
                            ),
                            SizedBox(width: 8),
                            Text("Import Data"),
                          ],
                        ),
                      ),
                    ],
                    cancelButton: CupertinoActionSheetAction(
                      onPressed: () {
                        Navigator.pop(context);
                      },
                      isDefaultAction: true,
                      child: Text("Cancel"),
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
      child: accounts.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    padding: EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: CupertinoColors.activeBlue,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      CupertinoIcons.lock_shield, // iOS-style icon
                      size: 64,
                      color: CupertinoColors.white,
                    ),
                  ),
                  SizedBox(height: 24),
                  Text(
                    'No accounts added yet',
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w500,
                      color: CupertinoColors.label,
                    ),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'Add your first account to get started',
                    style: TextStyle(
                      fontSize: 16,
                      color: CupertinoColors.secondaryLabel,
                    ),
                  ),
                  SizedBox(height: 32),
                  CupertinoButton.filled(
                    onPressed: _showAddAccountDialog,
                    padding: EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(CupertinoIcons.add, size: 20),
                        SizedBox(width: 8),
                        Text('Add Account'),
                      ],
                    ),
                  ),
                ],
              ),
            )
          : ListView.builder(
              padding: EdgeInsets.all(16),
              itemCount: accounts.length,
              itemBuilder: (context, index) {
                return Padding(
                  padding: EdgeInsets.only(bottom: 12),
                  child: AccountCard(
                    account: accounts[index],
                    onDelete: () => _deleteAccount(index),
                  ),
                );
              },
            ),
    );
  }
}

class AccountCard extends StatefulWidget {
  final TOTPAccount account;
  final VoidCallback onDelete;

  const AccountCard({Key? key, required this.account, required this.onDelete})
    : super(key: key);

  @override
  State<AccountCard> createState() => _AccountCardState();
}

class _AccountCardState extends State<AccountCard> {
  late Timer _timer;
  late String _currentCode;
  late int _remainingSeconds;
  late double _progress;

  @override
  void initState() {
    super.initState();
    _updateCode();
    _timer = Timer.periodic(Duration(seconds: 1), (timer) {
      _updateCode();
    });
  }

  @override
  void dispose() {
    _timer.cancel();
    super.dispose();
  }

  void _updateCode() {
    setState(() {
      _currentCode = TOTPGenerator.generateTOTP(
        widget.account.secret,
        digits: widget.account.digits,
      );
      _remainingSeconds = TOTPGenerator.getRemainingSeconds(
        period: widget.account.period,
      );
      _progress = _remainingSeconds / widget.account.period;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: CupertinoColors.systemGroupedBackground,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: CupertinoColors.black.withOpacity(0.1),
            blurRadius: 10,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Padding(
        padding: EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (widget.account.issuer.isNotEmpty)
                        Container(
                          padding: EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: CupertinoColors.systemBlue.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Text(
                            widget.account.issuer,
                            style: CupertinoTheme.of(context)
                                .textTheme
                                .textStyle
                                .copyWith(
                                  color: CupertinoColors.systemBlue,
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                ),
                          ),
                        ),
                      SizedBox(
                        height: widget.account.issuer.isNotEmpty ? 8 : 0,
                      ),
                      Text(
                        widget.account.name,
                        style: CupertinoTheme.of(context).textTheme.textStyle
                            .copyWith(
                              fontSize: 18,
                              fontWeight: FontWeight.w500,
                              color: CupertinoColors.label,
                            ),
                      ),
                    ],
                  ),
                ),
                CupertinoButton(
                  padding: EdgeInsets.zero,
                  minSize: 44,
                  child: Icon(
                    CupertinoIcons.ellipsis_vertical,
                    color: CupertinoColors.systemGrey,
                    size: 20,
                  ),
                  onPressed: () {
                    showCupertinoModalPopup(
                      context: context,
                      builder: (context) => CupertinoActionSheet(
                        actions: [
                          CupertinoActionSheetAction(
                            isDestructiveAction: true,
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(
                                  CupertinoIcons.delete,
                                  color: CupertinoColors.destructiveRed,
                                  size: 18,
                                ),
                                SizedBox(width: 8),
                                Text('Delete'),
                              ],
                            ),
                            onPressed: () {
                              Navigator.pop(context);
                              showCupertinoDialog(
                                context: context,
                                builder: (dialogContext) => CupertinoAlertDialog(
                                  title: Text("Delete Account"),
                                  content: Text(
                                    "Are you sure you want to delete this account?",
                                  ),
                                  actions: [
                                    CupertinoDialogAction(
                                      child: Text("Cancel"),
                                      onPressed: () =>
                                          Navigator.pop(dialogContext),
                                    ),
                                    CupertinoDialogAction(
                                      isDestructiveAction: true,
                                      child: Text("Delete"),
                                      onPressed: () {
                                        Navigator.pop(dialogContext);
                                        WidgetsBinding.instance
                                            .addPostFrameCallback((_) {
                                              widget.onDelete();
                                            });
                                      },
                                    ),
                                  ],
                                ),
                              );
                            },
                          ),
                        ],
                        cancelButton: CupertinoActionSheetAction(
                          child: Text('Cancel'),
                          onPressed: () => Navigator.pop(context),
                        ),
                      ),
                    );
                  },
                ),
              ],
            ),
            SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: GestureDetector(
                    onTap: () {
                      Clipboard.setData(ClipboardData(text: _currentCode));
                      showCupertinoSnackBar(
                        context,
                        'Code copied to clipboard',
                      );
                    },
                    child: Container(
                      padding: EdgeInsets.symmetric(
                        vertical: 16,
                        horizontal: 20,
                      ),
                      decoration: BoxDecoration(
                        color: CupertinoColors.systemBlue.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: CupertinoColors.systemBlue.withOpacity(0.3),
                          width: 1,
                        ),
                      ),
                      child: Text(
                        _formatCode(_currentCode),
                        style: CupertinoTheme.of(context).textTheme.textStyle
                            .copyWith(
                              fontSize: 24,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 4,
                              fontFamily: 'SF Mono',
                              color: CupertinoColors.systemBlue,
                            ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                ),
                SizedBox(width: 12),
                CupertinoButton(
                  padding: EdgeInsets.all(12),
                  minSize: 48,
                  borderRadius: BorderRadius.circular(12),
                  color: CupertinoColors.systemGrey5,
                  child: Icon(
                    CupertinoIcons.doc_on_clipboard,
                    color: CupertinoColors.systemBlue,
                    size: 20,
                  ),
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: _currentCode));
                    showCupertinoSnackBar(context, 'Code copied to clipboard');
                  },
                ),
                SizedBox(width: 12),
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: CupertinoColors.systemGrey6,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      SizedBox(
                        width: 32,
                        height: 32,
                        child: CustomPaint(
                          painter: CupertinoCircularProgressPainter(
                            progress: _progress,
                            backgroundColor: CupertinoColors.systemGrey4,
                            progressColor: _remainingSeconds <= 5
                                ? CupertinoColors.destructiveRed
                                : CupertinoColors.systemBlue,
                            strokeWidth: 3,
                          ),
                        ),
                      ),
                      Text(
                        _remainingSeconds.toString(),
                        style: CupertinoTheme.of(context).textTheme.textStyle
                            .copyWith(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              color: _remainingSeconds <= 5
                                  ? CupertinoColors.destructiveRed
                                  : CupertinoColors.systemBlue,
                            ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatCode(String code) {
    if (code.length == 6) {
      return '${code.substring(0, 3)} ${code.substring(3)}';
    }
    return code;
  }

  // Helper function for showing Cupertino-style snackbar
  void showCupertinoSnackBar(BuildContext context, String message) {
    showCupertinoModalPopup(
      context: context,
      builder: (context) => Container(
        margin: EdgeInsets.all(20),
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: CupertinoColors.systemBackground.withOpacity(0.9),
          borderRadius: BorderRadius.circular(12),
          boxShadow: [
            BoxShadow(
              color: CupertinoColors.black.withOpacity(0.1),
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
}

// Custom Cupertino circular progress painter
class CupertinoCircularProgressPainter extends CustomPainter {
  final double progress;
  final Color backgroundColor;
  final Color progressColor;
  final double strokeWidth;

  CupertinoCircularProgressPainter({
    required this.progress,
    required this.backgroundColor,
    required this.progressColor,
    this.strokeWidth = 3.0,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = (size.width - strokeWidth) / 2;

    // Background circle
    final backgroundPaint = Paint()
      ..color = backgroundColor
      ..strokeWidth = strokeWidth
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    canvas.drawCircle(center, radius, backgroundPaint);

    // Progress arc
    if (progress > 0) {
      final progressPaint = Paint()
        ..color = progressColor
        ..strokeWidth = strokeWidth
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round;

      const startAngle = -pi / 2; // Start from top
      final sweepAngle = 2 * pi * progress;

      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius),
        startAngle,
        sweepAngle,
        false,
        progressPaint,
      );
    }
  }

  @override
  bool shouldRepaint(CupertinoCircularProgressPainter oldDelegate) {
    return progress != oldDelegate.progress ||
        backgroundColor != oldDelegate.backgroundColor ||
        progressColor != oldDelegate.progressColor ||
        strokeWidth != oldDelegate.strokeWidth;
  }
}


class AddAccountPage extends StatefulWidget {
  final Function(TOTPAccount) onAccountAdded;

  const AddAccountPage({Key? key, required this.onAccountAdded})
      : super(key: key);

  @override
  _AddAccountPageState createState() => _AddAccountPageState();
}

class _AddAccountPageState extends State<AddAccountPage> {
  final _nameController = TextEditingController();
  final _issuerController = TextEditingController();
  final _secretController = TextEditingController();

  void _addAccount() {
    if (_nameController.text.isNotEmpty && _secretController.text.isNotEmpty) {
      final account = TOTPAccount(
        name: _nameController.text,
        issuer: _issuerController.text,
        secret: _secretController.text.toUpperCase(),
      );
      widget.onAccountAdded(account);
      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext) {
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        middle: const Text('Add Account'),
        leading: CupertinoButton(
          padding: EdgeInsets.zero,
          child: const Text('Cancel'),
          onPressed: () => Navigator.of(context).pop(),
        ),
        trailing: CupertinoButton(
          padding: EdgeInsets.zero,
          child: const Text('Done', style: TextStyle(fontWeight: FontWeight.bold)),
          onPressed: _addAccount,
        ),
      ),
      child: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                "Manually Add Account",
                style: CupertinoTheme.of(context).textTheme.navTitleTextStyle,
              ),
              const SizedBox(height: 16),
              CupertinoTextField(
                placeholder: "Account Name",
                controller: _nameController,
                padding: const EdgeInsets.all(12.0),
              ),
              const SizedBox(height: 12),
              CupertinoTextField(
                placeholder: "Issuer (optional)",
                controller: _issuerController,
                padding: const EdgeInsets.all(12.0),
              ),
              const SizedBox(height: 12),
              CupertinoTextField(
                placeholder: "Secret Key",
                controller: _secretController,
                padding: const EdgeInsets.all(12.0),
              ),
              const SizedBox(height: 12),
              const Text(
                'Enter the secret key for your account. This is a sequence of letters and numbers.',
                style: TextStyle(color: CupertinoColors.systemGrey),
              ),
            ],
          ),
        ),
      ),
    );
  }
}