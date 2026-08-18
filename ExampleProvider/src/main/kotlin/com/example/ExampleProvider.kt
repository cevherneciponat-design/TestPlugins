package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://archive.org"
    override var name = "Internet Archive Movies"
    override var supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    // Arama fonksiyonu: Sitede kelime aratıldığında çalışır
    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/advancedsearch.php?q=$query+AND+mediatype%3Adownloads&fl[]=identifier,title&sort[]=&sort[]=&sort[]=&rows=50&page=1&output=json"

        val response = app.get(searchUrl).parsedSafe<ArchiveSearchResponse>()

        return response?.response?.docs?.mapNotNull { doc ->
            val title = doc.title ?: return@mapNotNull null
            val id = doc.identifier ?: return@mapNotNull null

            newMovieSearchResponse(name = title, url = "$mainUrl/details/$id", type = TvType.Movie) {
                this.posterUrl = "$mainUrl/services/img/$id"
            }
        } ?: emptyList()
    }

    // Detay sayfası fonksiyonu: Bir filme tıklandığında çalışır
    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfter("/details/")
        val metaUrl = "$mainUrl/metadata/$id"

        val response = app.get(metaUrl).parsedSafe<ArchiveMetadataResponse>()
        val title = response?.metadata?.title ?: "Bilinmeyen Film"
        val description = response?.metadata?.description ?: ""

        return newMovieLoadResponse(name = title, url = url, type = TvType.Movie, dataUrl = id) {
            this.posterUrl = "$mainUrl/services/img/$id"
            this.plot = description
        }
    }

    // Oynatma bağlantısı fonksiyonu: Video oynatılmak istendiğinde MP4 linkini çözer
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        offsetCallback: (ExtractorLink) -> Unit
    ): Boolean {
        val id = data
        val filesUrl = "$mainUrl/metadata/$id/files"

        val response = app.get(filesUrl).parsedSafe<ArchiveFilesResponse>()

        response?.result?.filter { it.name?.endsWith(".mp4") == true }?.forEach { file ->
            val videoUrl = "$mainUrl/download/$id/${file.name}"

            offsetCallback(
                newExtractorLink(
                    source = name,
                    name = file.name ?: "MP4 Video",
                    url = videoUrl
                )
            )
        }
        return true
    }
}

// JSON Veri Modelleri
data class ArchiveSearchResponse(val response: ArchiveSearchInner?)
data class ArchiveSearchInner(val docs: List<ArchiveDoc>?)
data class ArchiveDoc(val identifier: String?, val title: String?)

data class ArchiveMetadataResponse(val metadata: ArchiveMetaDetail?)
data class ArchiveMetaDetail(val title: String?, val description: String?)

data class ArchiveFilesResponse(val result: List<ArchiveFile>?)
data class ArchiveFile(val name: String?)