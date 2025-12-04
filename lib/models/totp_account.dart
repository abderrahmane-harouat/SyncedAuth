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