import 'dart:convert';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as path;
import '../models/totp_account.dart';

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