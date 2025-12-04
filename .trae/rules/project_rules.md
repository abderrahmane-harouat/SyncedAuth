1 - never use .withOpacity() as it's deprecated in flutter, use .withValues() instead
2 - always use Cupertino, do not use Material 3 theme
3 - never assign CardTheme to cardTheme but assign CardThemeData
4 - Constructors for public widgets should have a named 'key' parameter.