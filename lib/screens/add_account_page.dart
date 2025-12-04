import 'package:flutter/cupertino.dart';
import '../models/totp_account.dart';
import '../theme.dart';

class AddAccountPage extends StatefulWidget {
  final Function(TOTPAccount) onAccountAdded;

  const AddAccountPage({super.key, required this.onAccountAdded});

  @override
  State<AddAccountPage> createState() => _AddAccountPageState();
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
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        middle: const Text('إضافة حساب'),
        leading: CupertinoButton(
          padding: EdgeInsets.zero,
          child: const Text('إلغاء'),
          onPressed: () => Navigator.of(context).pop(),
        ),
        trailing: CupertinoButton(
          padding: EdgeInsets.zero,
          onPressed: _addAccount,
          child: Text(
            'تم',
            style: AppTextStyles.titleMedium.copyWith(
              fontWeight: FontWeight.bold,
              color: AppColors.primary,
            ),
          ),
        ),
      ),
      child: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.all(AppSpacing.medium),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                "إضافة حساب يدويا",
                style: AppTextStyles.headlineSmall.copyWith(
                  color: AppColors.textPrimary,
                ),
                textDirection: TextDirection.rtl,
              ),
              SizedBox(height: AppSpacing.medium),
              CupertinoTextField(
                placeholder: "eg : username@email.com",
                controller: _nameController,
                padding: EdgeInsets.all(AppSpacing.small),
                style: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
                placeholderStyle: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              SizedBox(height: AppSpacing.small),
              CupertinoTextField(
                placeholder: "eg : Binance",
                controller: _issuerController,
                padding: EdgeInsets.all(AppSpacing.small),
                style: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
                placeholderStyle: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              SizedBox(height: AppSpacing.small),
              CupertinoTextField(
                placeholder: "Secret Key",
                controller: _secretController,
                padding: EdgeInsets.all(AppSpacing.small),
                style: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
                placeholderStyle: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              SizedBox(height: AppSpacing.small),
              Text(
                'أدخل المفتاح السري لحسابك. وهو عبارة عن سلسلة من الأحرف والأرقام.',
                style: AppTextStyles.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
                textDirection: TextDirection.rtl,
              ),
            ],
          ),
        ),
      ),
    );
  }
}