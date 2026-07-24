package com.iicytower.wanderlist.core.util

// Dominujący język urzędowy dla najczęstszych krajów turystycznych.
// Kraje wielojęzyczne dostają jeden praktyczny wybór; nieobsłużone kraje → null
// (pomijamy wtedy dodatkowe wyszukiwanie w lokalnym języku).
private val COUNTRY_TO_LANGUAGE: Map<String, String> = mapOf(
    "PL" to "pl", "DE" to "de", "AT" to "de", "CH" to "de", "LI" to "de",
    "FR" to "fr", "MC" to "fr", "LU" to "fr",
    "ES" to "es", "MX" to "es", "AR" to "es", "CL" to "es", "CO" to "es", "PE" to "es",
    "PT" to "pt", "BR" to "pt",
    "IT" to "it", "SM" to "it", "VA" to "it",
    "GR" to "el", "CY" to "el",
    "CZ" to "cs", "SK" to "sk", "HU" to "hu",
    "HR" to "hr", "SI" to "sl", "RS" to "sr", "BA" to "bs", "ME" to "sr", "MK" to "mk", "AL" to "sq",
    "RO" to "ro", "MD" to "ro", "BG" to "bg",
    "UA" to "uk", "LT" to "lt", "LV" to "lv", "EE" to "et",
    "NL" to "nl", "BE" to "nl",
    "DK" to "da", "SE" to "sv", "NO" to "no", "FI" to "fi", "IS" to "is",
    "GB" to "en", "IE" to "en", "US" to "en", "CA" to "en", "AU" to "en", "NZ" to "en",
    "MT" to "en", "SG" to "en", "PH" to "en",
    "JP" to "ja", "KR" to "ko", "CN" to "zh", "TW" to "zh", "HK" to "zh",
    "TH" to "th", "VN" to "vi", "ID" to "id", "MY" to "ms", "KH" to "km", "LA" to "lo",
    "IN" to "hi", "NP" to "ne", "LK" to "si",
    "TR" to "tr", "IL" to "he", "GE" to "ka", "AM" to "hy", "AZ" to "az",
    "EG" to "ar", "MA" to "ar", "TN" to "ar", "JO" to "ar", "LB" to "ar", "AE" to "ar", "SA" to "ar",
    "RU" to "ru", "BY" to "ru", "KZ" to "kk"
)

fun languageForCountryCode(countryCode: String): String? =
    COUNTRY_TO_LANGUAGE[countryCode.uppercase()]
