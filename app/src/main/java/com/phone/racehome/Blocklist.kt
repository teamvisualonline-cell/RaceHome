package com.phone.racehome

object Blocklist {
    /** Exact package names to fight / open App Info for */
    val PACKAGES = listOf(
        "com.motorola.paks",
    )

    /** If package name contains any of these, treat as suspect */
    val KEYWORDS = listOf(
        "telecom",
        "carrier",
        "clm",
        "mdm",
        "motorola.paks",
        "devicepolicy",
        "enterprise",
    )

    fun isBlocked(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName == "com.phone.racehome") return false
        if (PACKAGES.any { it.equals(packageName, ignoreCase = true) }) return true
        val low = packageName.lowercase()
        return KEYWORDS.any { low.contains(it) }
    }
}
