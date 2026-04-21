package org.koitharu.kotatsu.parsers.site.madara.pt

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGALIVRE", "Manga Livre", "pt")
internal class MangaLivre(context: MangaLoaderContext) :
    MadaraParser(context, MangaParserSource.MANGALIVRE, "mangalivre.tv") {

    override val datePattern = "MMMM dd, yyyy"
    override val withoutAjax = true

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(ConfigKey.DisableUpdateChecking(defaultValue = true))
        keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
    }
}
