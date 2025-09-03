import 'package:flutter/material.dart';
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
    return MaterialApp(
      title: 'Authenticator',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.deepPurple,
          brightness: Brightness.light,
        ),
        cardTheme: CardThemeData(
          elevation: 2,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
        iconButtonTheme: IconButtonThemeData(
          style: IconButton.styleFrom(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
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
    final int truncatedHash = ((hash[offset] & 0x7F) << 24) |
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
    return await openDatabase(
      pathStr,
      version: 1,
      onCreate: _onCreate,
    );
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
    await db.insert(
      'accounts',
      {
        'name': account.name,
        'issuer': account.issuer,
        'secret': account.secret,
        'digits': account.digits,
        'period': account.period,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
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
      return accountsData.map((accountData) => TOTPAccount.fromJson(accountData)).toList();
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
        fileName: 'authenticator_backup_${DateTime.now().millisecondsSinceEpoch}.json',
        type: FileType.custom,
        allowedExtensions: ['json'],
        bytes: utf8.encode(jsonData),
      );

      if (outputFile != null) {
        // File is already written by FilePicker when bytes are provided
        // No need to write again
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Row(
                children: [
                  Icon(Icons.check_circle_rounded, color: Colors.white),
                  SizedBox(width: 8),
                  Expanded(child: Text('Data exported successfully')),
                ],
              ),
              backgroundColor: Colors.green,
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                Icon(Icons.error_rounded, color: Colors.white),
                SizedBox(width: 8),
                Expanded(child: Text('Export failed: $e')),
              ],
            ),
            backgroundColor: Colors.red,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
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
        
        final importedAccounts = await _dbHelper.importAccountsFromJson(jsonString);
        
        if (mounted) {
          showDialog(
            context: context,
            builder: (context) => AlertDialog(
              title: Text('Import Data'),
              content: Text('Found ${importedAccounts.length} accounts. This will replace all existing accounts. Continue?'),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: Text('Cancel'),
                ),
                FilledButton(
                  onPressed: () async {
                    Navigator.pop(context);
                    await _dbHelper.clearAllAccounts();
                    for (final account in importedAccounts) {
                      await _dbHelper.insertAccount(account);
                    }
                    await _loadAccounts();
                    
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Row(
                            children: [
                              Icon(Icons.check_circle_rounded, color: Colors.white),
                              SizedBox(width: 8),
                              Text('Data imported successfully'),
                            ],
                          ),
                          backgroundColor: Colors.green,
                          behavior: SnackBarBehavior.floating,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
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
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                Icon(Icons.error_rounded, color: Colors.white),
                SizedBox(width: 8),
                Expanded(child: Text('Import failed: $e')),
              ],
            ),
            backgroundColor: Colors.red,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        );
      }
    }
  }

  void _showAddAccountDialog() {
    showDialog(
      context: context,
      builder: (context) => AddAccountDialog(
        onAccountAdded: _addAccount,
      ),
    );
  }



  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    
    return Scaffold(
      backgroundColor: colorScheme.surface,
      appBar: AppBar(
        title: Text(
          'Authenticator',
          style: TextStyle(
            fontWeight: FontWeight.w600,
            color: colorScheme.onSurface,
          ),
        ),
        backgroundColor: colorScheme.surface,
        elevation: 0,
        actions: [
          PopupMenuButton(
            icon: Icon(Icons.more_vert_rounded),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            itemBuilder: (context) => [
              PopupMenuItem(
                value: 'export',
                child: Row(
                  children: [
                    Icon(Icons.upload_rounded, color: colorScheme.primary),
                    SizedBox(width: 12),
                    Text('Export Data'),
                  ],
                ),
              ),
              PopupMenuItem(
                value: 'import',
                child: Row(
                  children: [
                    Icon(Icons.download_rounded, color: colorScheme.primary),
                    SizedBox(width: 12),
                    Text('Import Data'),
                  ],
                ),
              ),
            ],
            onSelected: (value) {
              if (value == 'export') {
                _exportData();
              } else if (value == 'import') {
                _importData();
              }
            },
          ),
          SizedBox(width: 8),
        ],
      ),
      body: accounts.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    padding: EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: colorScheme.primaryContainer,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.security_rounded,
                      size: 64,
                      color: colorScheme.onPrimaryContainer,
                    ),
                  ),
                  SizedBox(height: 24),
                  Text(
                    'No accounts added yet',
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      color: colorScheme.onSurface,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'Add your first account to get started',
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ),
                  SizedBox(height: 32),
                  FilledButton.icon(
                    onPressed: _showAddAccountDialog,
                    icon: Icon(Icons.add_rounded),
                    label: Text('Add Account'),
                    style: FilledButton.styleFrom(
                      padding: EdgeInsets.symmetric(horizontal: 24, vertical: 12),
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
      floatingActionButton: accounts.isNotEmpty
          ? FloatingActionButton.extended(
              onPressed: _showAddAccountDialog,
              icon: Icon(Icons.add_rounded),
              label: Text('Add Account'),
              backgroundColor: colorScheme.primaryContainer,
              foregroundColor: colorScheme.onPrimaryContainer,
            )
          : null,
    );
  }
}

class AccountCard extends StatefulWidget {
  final TOTPAccount account;
  final VoidCallback onDelete;

  const AccountCard({
    Key? key,
    required this.account,
    required this.onDelete,
  }) : super(key: key);

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
      _currentCode = TOTPGenerator.generateTOTP(widget.account.secret, digits: widget.account.digits);
      _remainingSeconds = TOTPGenerator.getRemainingSeconds(period: widget.account.period);
      _progress = _remainingSeconds / widget.account.period;
    });
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      elevation: 0,
      color: colorScheme.surfaceContainerLow,
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
                          padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: colorScheme.secondaryContainer,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            widget.account.issuer,
                            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                              color: colorScheme.onSecondaryContainer,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      SizedBox(height: widget.account.issuer.isNotEmpty ? 8 : 0),
                      Text(
                        widget.account.name,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: colorScheme.onSurface,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
                PopupMenuButton(
                  icon: Icon(
                    Icons.more_vert_rounded,
                    color: colorScheme.onSurfaceVariant,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  itemBuilder: (context) => [
                    PopupMenuItem(
                      value: 'delete',
                      child: Row(
                        children: [
                          Icon(Icons.delete_rounded, color: colorScheme.error),
                          SizedBox(width: 12),
                          Text(
                            'Delete',
                            style: TextStyle(color: colorScheme.error),
                          ),
                        ],
                      ),
                    ),
                  ],
                  onSelected: (value) {
                    if (value == 'delete') {
                      showDialog(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: Text('Delete Account'),
                          content: Text('Are you sure you want to delete this account?'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(context),
                              child: Text('Cancel'),
                            ),
                            FilledButton(
                              onPressed: () {
                                Navigator.pop(context);
                                widget.onDelete();
                              },
                              style: FilledButton.styleFrom(
                                backgroundColor: colorScheme.error,
                                foregroundColor: colorScheme.onError,
                              ),
                              child: Text('Delete'),
                            ),
                          ],
                        ),
                      );
                    }
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
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.check_rounded, color: Colors.white, size: 16),
                              SizedBox(width: 8),
                              Text('Code copied to clipboard'),
                            ],
                          ),
                          duration: Duration(seconds: 2),
                          behavior: SnackBarBehavior.floating,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                      );
                    },
                    child: Container(
                      padding: EdgeInsets.symmetric(vertical: 16, horizontal: 20),
                      decoration: BoxDecoration(
                        color: colorScheme.primaryContainer,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: colorScheme.outline.withValues(alpha: 0.2),
                          width: 1,
                        ),
                      ),
                      child: Text(
                        _formatCode(_currentCode),
                        style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                          color: colorScheme.onPrimaryContainer,
                          fontWeight: FontWeight.w600,
                          letterSpacing: 4,
                          fontFamily: 'monospace',
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                ),
                SizedBox(width: 12),
                IconButton.filled(
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: _currentCode));
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.check_rounded, color: Colors.white, size: 16),
                            SizedBox(width: 8),
                            Text('Code copied to clipboard'),
                          ],
                        ),
                        duration: Duration(seconds: 2),
                        behavior: SnackBarBehavior.floating,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    );
                  },
                  icon: Icon(Icons.copy_rounded),
                  style: IconButton.styleFrom(
                    backgroundColor: colorScheme.secondaryContainer,
                    foregroundColor: colorScheme.onSecondaryContainer,
                  ),
                  tooltip: 'Copy code',
                ),
                SizedBox(width: 12),
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      SizedBox(
                        width: 32,
                        height: 32,
                        child: CircularProgressIndicator(
                          value: _progress,
                          strokeWidth: 3,
                          backgroundColor: colorScheme.outline.withValues(alpha: 0.3),
                          valueColor: AlwaysStoppedAnimation<Color>(
                            _remainingSeconds <= 5 ? colorScheme.error : colorScheme.primary,
                          ),
                        ),
                      ),
                      Text(
                        _remainingSeconds.toString(),
                        style: Theme.of(context).textTheme.labelLarge?.copyWith(
                          color: _remainingSeconds <= 5 ? colorScheme.error : colorScheme.primary,
                          fontWeight: FontWeight.w600,
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
}

class AddAccountDialog extends StatefulWidget {
  final Function(TOTPAccount) onAccountAdded;

  const AddAccountDialog({Key? key, required this.onAccountAdded}) : super(key: key);

  @override
  _AddAccountDialogState createState() => _AddAccountDialogState();
}

class _AddAccountDialogState extends State<AddAccountDialog> {
  final _nameController = TextEditingController();
  final _issuerController = TextEditingController();
  final _secretController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    
    return AlertDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      title: Text(
        'Add Account',
        style: Theme.of(context).textTheme.headlineSmall?.copyWith(
          fontWeight: FontWeight.w600,
        ),
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _nameController,
            decoration: InputDecoration(
              labelText: 'Account Name',
              hintText: 'e.g., john@example.com',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              filled: true,
              fillColor: colorScheme.surfaceContainerHighest,
            ),
          ),
          SizedBox(height: 16),
          TextField(
            controller: _issuerController,
            decoration: InputDecoration(
              labelText: 'Issuer (Optional)',
              hintText: 'e.g., Google, Facebook',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              filled: true,
              fillColor: colorScheme.surfaceContainerHighest,
            ),
          ),
          SizedBox(height: 16),
          TextField(
            controller: _secretController,
            decoration: InputDecoration(
              labelText: 'Secret Key',
              hintText: 'Base32 encoded secret',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              filled: true,
              fillColor: colorScheme.surfaceContainerHighest,
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text('Cancel'),
        ),
        FilledButton(
          onPressed: () {
            if (_nameController.text.isNotEmpty && _secretController.text.isNotEmpty) {
              final account = TOTPAccount(
                name: _nameController.text,
                issuer: _issuerController.text,
                secret: _secretController.text.toUpperCase(),
              );
              widget.onAccountAdded(account);
              Navigator.pop(context);
            }
          },
          child: Text('Add'),
        ),
      ],
    );
  }
}