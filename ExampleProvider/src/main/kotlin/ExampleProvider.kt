import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://archive.org"
    override var name = "Internet Archive Movies"
    override var supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

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

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val filesUrl = "$mainUrl/metadata/$data/files"

        val response = app.get(filesUrl).parsedSafe<ArchiveFilesResponse>()

        response?.result?.filter { it.name?.endsWith(".mp4") == true }?.forEach { file ->
            val videoUrl = "$mainUrl/download/$data/${file.name}"
            val fileName = file.name ?: "MP4 Video"

            callback(
                newExtractorLink(
                    source = name,
                    name = fileName,
                    url = videoUrl,
                    type = ExtractorLinkType.VIDEO
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