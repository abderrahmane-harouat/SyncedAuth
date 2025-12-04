import 'dart:async';
import 'dart:math';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import '../models/totp_account.dart';
import '../services/totp_generator.dart';
import '../utils/cupertino_snackbar.dart';
import '../theme.dart';

class AccountCard extends StatefulWidget {
  final TOTPAccount account;
  final VoidCallback onDelete;

  const AccountCard({super.key, required this.account, required this.onDelete});

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
      margin: EdgeInsets.all(AppSpacing.medium),
      decoration: BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.circular(AppRadius.card),
        boxShadow: AppShadows.card,
      ),
      child: Padding(
        padding: EdgeInsets.all(AppSpacing.large),
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
                            horizontal: AppSpacing.small,
                            vertical: AppSpacing.xs,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.primaryWithOpacity(0.1),
                            borderRadius: BorderRadius.circular(AppRadius.input),
                          ),
                          child: Text(
                            widget.account.issuer,
                            style: AppTextStyles.labelSmall.copyWith(
                              color: AppColors.primary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      SizedBox(
                        height: widget.account.issuer.isNotEmpty ? AppSpacing.small : 0,
                      ),
                      Text(
                        widget.account.name,
                        style: AppTextStyles.titleLarge.copyWith(
                          color: AppColors.textPrimary,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
                CupertinoButton(
                  padding: EdgeInsets.zero,
                  minimumSize: Size(44, 44),
                  child: Icon(
                    CupertinoIcons.ellipsis_vertical,
                    color: AppColors.textSecondary,
                    size: AppIconSizes.medium,
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
                                Text('حذف'),
                                SizedBox(width: 8),
                                Icon(
                                  CupertinoIcons.delete,
                                  color: AppColors.error,
                                  size: AppIconSizes.small,
                                ),
                              ],
                            ),
                            onPressed: () {
                              Navigator.pop(context);
                              showCupertinoDialog(
                                context: context,
                                builder: (dialogContext) => CupertinoAlertDialog(
                                  title: Text("حذف الحساب"),
                                  content: Text(
                                    "هل أنت متأكد من حذف هذا الحساب؟",
                                  ),
                                  actions: [
                                    CupertinoDialogAction(
                                      child: Text("إلغاء"),
                                      onPressed: () =>
                                          Navigator.pop(dialogContext),
                                    ),
                                    CupertinoDialogAction(
                                      isDestructiveAction: true,
                                      child: Text("حذف"),
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
                          child: Text('إلغاء'),
                          onPressed: () => Navigator.pop(context),
                        ),
                      ),
                    );
                  },
                ),
              ],
            ),
            SizedBox(height: AppSpacing.large),
            Row(
              children: [
                Expanded(
                  child: GestureDetector(
                    onTap: () {
                      Clipboard.setData(ClipboardData(text: _currentCode));
                      showCupertinoSnackBar(
                        context,
                        'تم نسخ الكود',
                      );
                    },
                    child: Container(
                      padding: EdgeInsets.symmetric(
                        vertical: AppSpacing.medium,
                        horizontal: AppSpacing.large,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.primaryWithOpacity(0.1),
                        borderRadius: BorderRadius.circular(AppRadius.button),
                        border: Border.all(
                          color: AppColors.primaryWithOpacity(0.3),
                          width: 1,
                        ),
                      ),
                      child: Text(
                        _formatCode(_currentCode),
                        style: AppTextStyles.code.copyWith(
                          color: AppColors.primary,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                ),
                SizedBox(width: AppSpacing.small),
                CupertinoButton(
                  padding: EdgeInsets.all(AppSpacing.small),
                  minimumSize: Size(AppIconSizes.xxl, AppIconSizes.xxl),
                  borderRadius: BorderRadius.circular(AppRadius.button),
                  color: AppColors.border,
                  child: Icon(
                    CupertinoIcons.doc_on_clipboard,
                    color: AppColors.primary,
                    size: AppIconSizes.medium,
                  ),
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: _currentCode));
                    showCupertinoSnackBar(context, 'Code copied to clipboard');
                  },
                ),
                SizedBox(width: AppSpacing.small),
                Container(
                  width: AppIconSizes.xxl,
                  height: AppIconSizes.xxl,
                  decoration: BoxDecoration(
                    color: AppColors.shadow,
                    borderRadius: BorderRadius.circular(AppRadius.button),
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
                            backgroundColor: AppColors.border,
                            progressColor: _remainingSeconds <= 5
                                ? AppColors.error
                                : AppColors.primary,
                            strokeWidth: 3,
                          ),
                        ),
                      ),
                      Text(
                        _remainingSeconds.toString(),
                        style: AppTextStyles.labelLarge.copyWith(
                          fontWeight: FontWeight.w600,
                          color: _remainingSeconds <= 5
                              ? AppColors.error
                              : AppColors.primary,
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