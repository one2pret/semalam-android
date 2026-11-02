package dev.one2pret.semalam

/** Only user-selected labels are counted. No automatic sleep disorder diagnosis. */
internal data class NightSummary(
    val snoring: Int,
    val sleepTalking: Int,
    val environment: Int,
    val bodySound: Int,
    val unreviewed: Int,
    val other: Int,
    val total: Int
)

internal object SoundDashboard {
    private val knownLabels = setOf(
        "Mirip dengkuran", "Mirip berbicara", "Suara sekitar",
        "Suara keras dari luar", "Batuk / suara tubuh",
        "Belum dilabeli", "Tidak yakin", "Lainnya"
    )
    fun summarize(labels: List<String>): NightSummary = NightSummary(
        snoring = labels.count { it == "Mirip dengkuran" },
        sleepTalking = labels.count { it == "Mirip berbicara" },
        environment = labels.count { it == "Suara sekitar" || it == "Suara keras dari luar" },
        bodySound = labels.count { it == "Batuk / suara tubuh" },
        unreviewed = labels.count { it == "Belum dilabeli" || it == "Tidak yakin" },
        other = labels.count { it == "Lainnya" || it !in knownLabels },
        total = labels.size
    )
    fun matchesFilter(label: String, filter: String): Boolean = when (filter) {
        "Dengkuran" -> label == "Mirip dengkuran"
        "Berbicara" -> label == "Mirip berbicara"
        "Lingkungan" -> label == "Suara sekitar" || label == "Suara keras dari luar"
        "Suara tubuh" -> label == "Batuk / suara tubuh"
        "Belum ditinjau" -> label == "Belum dilabeli" || label == "Tidak yakin"
        else -> true
    }
}
