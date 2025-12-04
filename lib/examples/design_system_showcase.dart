/// Design System Showcase
/// This file demonstrates how to use the OneUI Design System components
/// Copy this file to see examples of proper usage

import 'package:flutter/cupertino.dart';
import '../theme.dart';

class DesignSystemShowcase extends StatelessWidget {
  const DesignSystemShowcase({super.key});

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        middle: Text(
          'Design System Showcase',
          style: AppTextStyles.headlineSmall.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
      ),
      child: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.all(AppSpacing.medium),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildTypographySection(),
              SizedBox(height: AppSpacing.large),
              _buildColorSection(),
              SizedBox(height: AppSpacing.large),
              _buildSpacingSection(),
              SizedBox(height: AppSpacing.large),
              _buildCardSection(),
              SizedBox(height: AppSpacing.large),
              _buildButtonSection(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTypographySection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Typography',
          style: AppTextStyles.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        SizedBox(height: AppSpacing.medium),
        Text(
          'Display Large',
          style: AppTextStyles.displayLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        Text(
          'Headline Large',
          style: AppTextStyles.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        Text(
          'Title Large',
          style: AppTextStyles.titleLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        Text(
          'Body Large - This is regular body text that you would use for paragraphs and descriptions.',
          style: AppTextStyles.bodyLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        Text(
          'Label Large',
          style: AppTextStyles.labelLarge.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        Text(
          '123 456',
          style: AppTextStyles.code.copyWith(
            color: AppColors.primary,
          ),
        ),
      ],
    );
  }

  Widget _buildColorSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Colors',
          style: AppTextStyles.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        SizedBox(height: AppSpacing.medium),
        Wrap(
          spacing: AppSpacing.small,
          runSpacing: AppSpacing.small,
          children: [
            _buildColorSwatch('Primary', AppColors.primary),
            _buildColorSwatch('Secondary', AppColors.secondary),
            _buildColorSwatch('Success', AppColors.success),
            _buildColorSwatch('Warning', AppColors.warning),
            _buildColorSwatch('Error', AppColors.error),
            _buildColorSwatch('Info', AppColors.info),
          ],
        ),
      ],
    );
  }

  Widget _buildColorSwatch(String name, Color color) {
    return Column(
      children: [
        Container(
          width: 60,
          height: 60,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(AppRadius.small),
            border: Border.all(
              color: AppColors.border,
              width: 1,
            ),
          ),
        ),
        SizedBox(height: AppSpacing.xs),
        Text(
          name,
          style: AppTextStyles.labelSmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      ],
    );
  }

  Widget _buildSpacingSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Spacing',
          style: AppTextStyles.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        SizedBox(height: AppSpacing.medium),
        _buildSpacingExample('XS', AppSpacing.xs),
        _buildSpacingExample('Small', AppSpacing.small),
        _buildSpacingExample('Medium', AppSpacing.medium),
        _buildSpacingExample('Large', AppSpacing.large),
      ],
    );
  }

  Widget _buildSpacingExample(String name, double spacing) {
    return Padding(
      padding: EdgeInsets.only(bottom: AppSpacing.small),
      child: Row(
        children: [
          SizedBox(
            width: 80,
            child: Text(
              name,
              style: AppTextStyles.labelMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ),
          Container(
            width: spacing,
            height: 20,
            decoration: BoxDecoration(
              color: AppColors.primary,
              borderRadius: BorderRadius.circular(AppRadius.xs),
            ),
          ),
          SizedBox(width: AppSpacing.small),
          Text(
            '${spacing.toInt()}px',
            style: AppTextStyles.labelSmall.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCardSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Cards',
          style: AppTextStyles.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        SizedBox(height: AppSpacing.medium),
        Container(
          padding: EdgeInsets.all(AppSpacing.medium),
          decoration: BoxDecoration(
            color: AppColors.cardBackground,
            borderRadius: BorderRadius.circular(AppRadius.card),
            boxShadow: AppShadows.card,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Card Title',
                style: AppTextStyles.titleLarge.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
              SizedBox(height: AppSpacing.small),
              Text(
                'This is a card with proper spacing, colors, and shadows from the design system.',
                style: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildButtonSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Buttons',
          style: AppTextStyles.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        SizedBox(height: AppSpacing.medium),
        Row(
          children: [
            CupertinoButton(
              color: AppColors.primary,
              borderRadius: BorderRadius.circular(AppRadius.button),
              onPressed: () {},
              child: Text(
                'Primary',
                style: AppTextStyles.button.copyWith(
                  color: CupertinoColors.white,
                ),
              ),
            ),
            SizedBox(width: AppSpacing.small),
            CupertinoButton(
              color: AppColors.border,
              borderRadius: BorderRadius.circular(AppRadius.button),
              onPressed: () {},
              child: Text(
                'Secondary',
                style: AppTextStyles.button.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}