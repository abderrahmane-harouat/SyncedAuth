import 'dart:convert';
import 'dart:io';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart' show Colors;
import 'package:file_picker/file_picker.dart';
import '../models/totp_account.dart';
import '../services/database_helper.dart';
import '../widgets/account_card.dart';
import 'add_account_page.dart';

class AuthenticatorHome extends StatefulWidget {
  const AuthenticatorHome({super.key});

  @override
  _AuthenticatorHomeState createState() => _AuthenticatorHomeState();
}

class _AuthenticatorHomeState extends State<AuthenticatorHome>
    with TickerProviderStateMixin {
  List<TOTPAccount> accounts = [];
  List<TOTPAccount> filteredAccounts = [];
  final DatabaseHelper _dbHelper = DatabaseHelper();
  final TextEditingController _searchController = TextEditingController();
  final FocusNode _searchFocusNode = FocusNode();
  late AnimationController _searchAnimationController;
  late Animation<double> _searchAnimation;
  bool _isSearchVisible = false;

  @override
  void initState() {
    super.initState();
    _loadAccounts();
    _searchAnimationController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _searchAnimation = CurvedAnimation(
      parent: _searchAnimationController,
      curve: Curves.easeInOut,
    );
    _searchController.addListener(_onSearchChanged);
  }

  @override
  void dispose() {
    _searchAnimationController.dispose();
    _searchController.dispose();
    _searchFocusNode.dispose();
    super.dispose();
  }

  void _onSearchChanged() {
    setState(() {
      if (_searchController.text.isEmpty) {
        filteredAccounts = List.from(accounts);
      } else {
        filteredAccounts = accounts.where((account) {
          final query = _searchController.text.toLowerCase();
          return account.name.toLowerCase().contains(query) ||
              account.issuer.toLowerCase().contains(query);
        }).toList();
      }
    });
  }

  void _toggleSearch() {
    setState(() {
      _isSearchVisible = !_isSearchVisible;
      if (_isSearchVisible) {
        _searchAnimationController.forward();
        // Auto-focus the search field after animation
        Future.delayed(Duration(milliseconds: 300), () {
          _searchFocusNode.requestFocus();
        });
      } else {
        _searchAnimationController.reverse();
        _searchController.clear();
        _searchFocusNode.unfocus();
        filteredAccounts = List.from(accounts);
      }
    });
  }

  Future<void> _loadAccounts() async {
    try {
      final loadedAccounts = await _dbHelper.getAccounts();
      setState(() {
        accounts = loadedAccounts;
        filteredAccounts = List.from(loadedAccounts);
      });
    } catch (e) {
      // Handle error gracefully
      setState(() {
        accounts = [];
        filteredAccounts = [];
      });
    }
  }

  Future<void> _addAccount(TOTPAccount account) async {
    try {
      await _dbHelper.insertAccount(account);
      await _loadAccounts();
    } catch (e) {
      // Handle error gracefully
    }
  }

  Future<void> _deleteAccount(int index) async {
    try {
      // Find the actual account from the original list
      final accountToDelete = filteredAccounts[index];
      await _dbHelper.deleteAccount(accountToDelete);
      await _loadAccounts();
    } catch (e) {
      // Handle error gracefully
    }
  }

  Future<void> _exportData() async {
    try {
      final jsonData = await _dbHelper.exportAccountsToJson();

      String? outputFile = await FilePicker.platform.saveFile(
        dialogTitle: 'حفظ البيانات في الجهاز',
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
            title: Text("ممتاز!"),
            content: Text("تم حفظ البيانات بنجاح"),
            actions: [
              CupertinoDialogAction(
                isDefaultAction: true,
                onPressed: () => Navigator.pop(ctx),
                child: Text("تم"),
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
            title: Text("خطأ"),
            content: Text("فشل حفظ البيانات هناك خطأ"),
            actions: [
              CupertinoDialogAction(
                isDestructiveAction: true,
                onPressed: () => Navigator.pop(ctx),
                child: Text("تم"),
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
        dialogTitle: 'استيراد البيانات',
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
              title: Text('استيراد البيانات'),
              content: accounts.isNotEmpty
                  ? Text(
                      'تم ايجاد ${importedAccounts.length} الحسابات. سيتم استبدال جميع الحسابات الحالية. هل أنت متأكد؟',
                    )
                  : null,
              actions: [
                CupertinoDialogAction(
                  onPressed: () => Navigator.pop(ctx),
                  child: Text('إلغاء'),
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
                          title: Text("ممتاز!"),
                          content: Text("تم استيراد البيانات بنجاح"),
                          actions: [
                            CupertinoDialogAction(
                              isDefaultAction: true,
                              onPressed: () => Navigator.pop(ctx2),
                              child: Text("تم"),
                            ),
                          ],
                        ),
                      );
                    }
                  },
                  child: Text('إستيراد'),
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
            title: Text("خطأ"),
            content: Text("فشل استيراد البيانات هناك خطأ"),
            actions: [
              CupertinoDialogAction(
                isDestructiveAction: true,
                onPressed: () => Navigator.pop(ctx),
                child: Text("تم"),
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
        fullscreenDialog: true,
        builder: (context) => AddAccountPage(onAccountAdded: _addAccount),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        trailing: Text(
          'تطبيق التحقق',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 19.0),
        ),
        leading: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (accounts.isNotEmpty)
              CupertinoButton(
                onPressed: _showAddAccountDialog,
                padding: EdgeInsets.zero,
                child: Icon(
                  CupertinoIcons.add,
                  size: 28,
                ),
              ),
            if (accounts.isNotEmpty)
              CupertinoButton(
                onPressed: _toggleSearch,
                padding: EdgeInsets.zero,
                child: Icon(
                  _isSearchVisible ? CupertinoIcons.xmark : CupertinoIcons.search,
                  size: 28,
                ),
              ),
            CupertinoButton(
              padding: EdgeInsets.zero,
              child: Icon(CupertinoIcons.ellipsis, size: 28),
              onPressed: () {
                showCupertinoModalPopup(
                  context: context,
                  builder: (BuildContext context) => CupertinoActionSheet(
                    title: Text(
                      "البيانات تكون على شكل JSON",
                      textDirection: TextDirection.rtl,
                      style: TextStyle(
                          fontSize: 18, fontWeight: FontWeight.w500),
                    ),
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
                              CupertinoIcons.device_phone_portrait,
                              color: CupertinoColors.activeBlue,
                            ),
                            SizedBox(width: 8),
                            Text("حفظ البيانات"),
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
                              CupertinoIcons.square_arrow_down,
                              color: CupertinoColors.activeBlue,
                            ),
                            SizedBox(width: 8),
                            Text("إستيراد البيانات"),
                          ],
                        ),
                      ),
                    ],
                    cancelButton: CupertinoActionSheetAction(
                      onPressed: () {
                        Navigator.pop(context);
                      },
                      isDefaultAction: true,
                      child: Text("إلغاء"),
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
                      CupertinoIcons.lock_shield,
                      size: 64,
                      color: CupertinoColors.white,
                    ),
                  ),
                  SizedBox(height: 24),
                  Text(
                    "لا يوجد أي حساب بعد",
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w500,
                      color: CupertinoColors.label,
                    ),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'أضف أول حساب للبدأ',
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
                        Text('إضافة حساب'),
                        SizedBox(width: 8),
                        Icon(CupertinoIcons.add, size: 20),
                      ],
                    ),
                  ),
                ],
              ),
            )
          : Column(
              children: [
                // Animated Search Bar
                AnimatedBuilder(
                  animation: _searchAnimation,
                  builder: (context, child) {
                    return Container(
                      height: _searchAnimation.value * 80,
                      padding: EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: _searchAnimation.value * 12,
                      ),
                      child: _searchAnimation.value > 0
                          ? Container(
                              height: 56,
                              decoration: BoxDecoration(
                                color: CupertinoColors.systemBackground,
                                borderRadius: BorderRadius.circular(16),
                                border: Border.all(
                                  color: CupertinoColors.systemGrey4,
                                  width: 1,
                                ),
                                boxShadow: [
                                  BoxShadow(
                                    color: CupertinoColors.black.withValues(alpha: 0.05),
                                    blurRadius: 8,
                                    offset: Offset(0, 2),
                                  ),
                                ],
                              ),
                              child: Directionality(
                                textDirection: TextDirection.rtl,
                                child: StatefulBuilder(
                                  builder: (context, setSearchState) {
                                    return CupertinoTextField(
                                      controller: _searchController,
                                      focusNode: _searchFocusNode,
                                      placeholder: 'البحث في الحسابات...',
                                      placeholderStyle: TextStyle(
                                        color: CupertinoColors.systemGrey2,
                                        fontSize: 16,
                                      ),
                                      style: TextStyle(
                                        fontSize: 16,
                                        color: CupertinoColors.label,
                                      ),
                                      padding: EdgeInsets.symmetric(
                                        horizontal: 20,
                                        vertical: 16,
                                      ),
                                      decoration: BoxDecoration(
                                        color: Colors.transparent,
                                        borderRadius: BorderRadius.circular(16),
                                      ),
                                      onChanged: (value) {
                                        setSearchState(() {});
                                        _onSearchChanged();
                                      },
                                      prefix: Padding(
                                        padding: EdgeInsets.only(right: 12),
                                        child: Icon(
                                          CupertinoIcons.search,
                                          color: CupertinoColors.systemGrey2,
                                          size: 22,
                                        ),
                                      ),
                                      suffix: _searchController.text.isNotEmpty
                                          ? CupertinoButton(
                                              padding: EdgeInsets.zero,
                                              minimumSize: Size(24, 24),
                                              onPressed: () {
                                                _searchController.clear();
                                                setSearchState(() {});
                                                _onSearchChanged();
                                              },
                                              child: Icon(
                                                CupertinoIcons.xmark_circle_fill,
                                                color: CupertinoColors.systemGrey2,
                                                size: 20,
                                              ),
                                            )
                                          : null,
                                    );
                                  },
                                ),
                              ),
                            )
                          : null,
                    );
                  },
                ),
                // Accounts List
                Expanded(
                  child: filteredAccounts.isEmpty && _searchController.text.isNotEmpty
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                CupertinoIcons.search,
                                size: 64,
                                color: CupertinoColors.systemGrey,
                              ),
                              SizedBox(height: 16),
                              Text(
                                'لا توجد نتائج',
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w500,
                                  color: CupertinoColors.systemGrey,
                                ),
                              ),
                              SizedBox(height: 8),
                              Text(
                                'جرب البحث بكلمات مختلفة',
                                style: TextStyle(
                                  fontSize: 14,
                                  color: CupertinoColors.systemGrey2,
                                ),
                              ),
                            ],
                          ),
                        )
                      : ListView.builder(
                          padding: EdgeInsets.all(16),
                          itemCount: filteredAccounts.length,
                          itemBuilder: (context, index) {
                            return Padding(
                              padding: EdgeInsets.only(bottom: 12),
                              child: AccountCard(
                                key: ValueKey(filteredAccounts[index].secret),
                                account: filteredAccounts[index],
                                onDelete: () => _deleteAccount(index),
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),
    );
  }
}