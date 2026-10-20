package com.harouat.syncedauth.ui.components

import com.harouat.syncedauth.R

/**
 * Locally bundled brand logos for known services. Two flavors:
 *  - [BrandIcon.Vector]: official brand-colored vector mark (Simple Icons CC0 or
 *    real multicolor logos) — drawn as-is on a white tile, like an app icon.
 *  - [BrandIcon.Bitmap]: bundled full-color brand bitmap — drawn full-bleed.
 *
 * Nothing touches the network: every asset ships inside the APK for privacy.
 */
sealed interface BrandIcon {
    data class Vector(val res: Int) : BrandIcon
    data class Bitmap(val res: Int) : BrandIcon
}

object BrandIcons {

    /** First rule whose keyword appears (issuer first, then name) wins. */
    private val vectorRules: List<Pair<Int, List<String>>> = listOf(
        R.drawable.ic_brand_binance to listOf("binance"),
        R.drawable.ic_brand_coinbase to listOf("coinbase"),
        R.drawable.ic_brand_okx to listOf("okx"),
        R.drawable.ic_brand_kucoin to listOf("kucoin"),
        R.drawable.ic_brand_wise to listOf("wise"),
        R.drawable.ic_brand_lemonsqueezy to listOf("lemon squeezy", "lemonsqueezy"),
        R.drawable.ic_brand_stripe to listOf("stripe"),
        R.drawable.ic_brand_ngrok to listOf("ngrok"),
        R.drawable.ic_brand_qwant to listOf("qwant"),
        R.drawable.ic_brand_bugcrowd to listOf("bugcrowd"),
        R.drawable.ic_brand_hackerone to listOf("hackerone"),
        R.drawable.ic_brand_google to listOf("google"),
        R.drawable.ic_brand_protonmail to listOf("protonmail", "proton mail"),
        R.drawable.ic_brand_protonvpn to listOf("protonvpn", "proton vpn"),
        R.drawable.ic_brand_discord to listOf("discord"),
        R.drawable.ic_brand_slack to listOf("slack"),
        R.drawable.ic_brand_github to listOf("github"),
        R.drawable.ic_brand_gitlab to listOf("gitlab"),
        R.drawable.ic_brand_dropbox to listOf("dropbox"),
        R.drawable.ic_brand_cloudflare to listOf("cloudflare"),
        R.drawable.ic_brand_digitalocean to listOf("digitalocean", "digital ocean"),
        R.drawable.ic_brand_shopify to listOf("shopify"),
        R.drawable.ic_brand_paypal to listOf("paypal"),
        R.drawable.ic_brand_notion to listOf("notion"),
        R.drawable.ic_brand_figma to listOf("figma"),
        R.drawable.ic_brand_reddit to listOf("reddit"),
        R.drawable.ic_brand_linkedin to listOf("linkedin"),
        R.drawable.ic_brand_twitch to listOf("twitch"),
        R.drawable.ic_brand_steam to listOf("steam"),
        R.drawable.ic_brand_apple to listOf("apple", "icloud"),
        R.drawable.ic_brand_facebook to listOf("facebook"),
    )

    /** Full-color bundled bitmaps (marks that are inherently multicolor photos/gradients). */
    private val bitmapRules: List<Pair<Int, List<String>>> = listOf(
        R.drawable.brand_aws to listOf("amazonaws", "amazon web services", "amazonaws.com", "aws"),
        R.drawable.brand_gmail to listOf("gmail"),
        R.drawable.brand_telegram to listOf("telegram"),
        R.drawable.brand_whatsapp to listOf("whatsapp"),
        R.drawable.brand_instagram to listOf("instagram"),
        R.drawable.brand_bitbucket to listOf("bitbucket"),
        R.drawable.brand_bybit to listOf("bybit"),
        R.drawable.brand_blofin to listOf("blofin"),
        R.drawable.brand_mexc to listOf("mexc"),
        R.drawable.brand_bitget to listOf("bitget"),
        R.drawable.brand_pionex to listOf("pionex"),
        R.drawable.brand_kraken to listOf("kraken"),
        R.drawable.brand_yeswehack to listOf("yeswehack"),
    )

    fun forAccount(issuer: String, name: String): BrandIcon? {
        val hayIssuer = issuer.lowercase()
        val hayName = name.lowercase()

        // Gmail is DEFERRED everywhere (see below): an embedded email address
        // (e.g. "Pionex(me@gmail.com)") must never hijack the row.
        val gmail = R.drawable.brand_gmail

        fun ruleHit(
            rules: List<Pair<Int, List<String>>>,
            hay: String,
            skipRes: Int = -1,
        ): Int? =
            rules.asSequence()
                .filter { it.first != skipRes }
                .flatMap { (res, keys) -> keys.asSequence().map { key -> res to key } }
                .firstOrNull { hay.contains(it.second) }
                ?.first

        // 1. Brand-specific bitmaps by issuer — the only kind some services have.
        ruleHit(bitmapRules, hayIssuer, gmail)?.let { return BrandIcon.Bitmap(it) }
        // 2. Vector marks by issuer.
        ruleHit(vectorRules, hayIssuer)?.let { return BrandIcon.Vector(it) }
        // 3. Name fallbacks.
        ruleHit(bitmapRules, hayName, gmail)?.let { return BrandIcon.Bitmap(it) }
        ruleHit(vectorRules, hayName)?.let { return BrandIcon.Vector(it) }
        // 4. Last resort: anything tied to gmail itself gets the envelope mark.
        if (hayIssuer.contains("gmail") || hayName.contains("gmail")) {
            return BrandIcon.Bitmap(gmail)
        }
        return null
    }
}
