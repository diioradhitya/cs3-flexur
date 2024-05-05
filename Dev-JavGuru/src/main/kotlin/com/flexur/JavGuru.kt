package com.jacekun

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

class JavGuruProvider : MainAPI() {
    private val globalTvType = TvType.NSFW
    override var name = "JavGuru"
    override var mainUrl = "https://jav.guru"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport = false
    override val hasMainPage = true
    override val hasQuickSearch = false

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val html = app.get(mainUrl).text
        val document = Jsoup.parse(html)
        val all = ArrayList<HomePageList>()

        document.getElementsByTag("body").select("div#main-content")
            .select("div.post").forEach { it2 ->
            val title = it2.select("h2.entry-title").text() ?: "Unnamed Row"
            it2.select("div.entry-content").select("article")
                .let { inner ->

                    val elements: List<SearchResponse> = inner.mapNotNull {

                        val aa = it.select("a").firstOrNull()
                        val link = fixUrlNull(aa?.attr("href")) ?: return@mapNotNull null
                        val name = aa?.attr("title") ?: "<No Title>"

                        var image = aa?.select("img").attr("src")
                        if (image.isNullOrBlank()) {
                            image = aa?.select("video").attr("poster")
                        }
                        val year = null

                        MovieSearchResponse(
                            name = name,
                            url = link,
                            apiName = this.name,
                            type = globalTvType,
                            posterUrl = image,
                            year = year
                        )
                    }

                    all.add(
                        HomePageList(
                            name = title,
                            list = elements,
                            isHorizontalImages = true
                        )
                    )
                }
        }
        return HomePageResponse(all)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search/?s=${query}"
        val document = app.get(searchUrl).document
            .select("div.post")

        return document.mapNotNull {
            val aa = it?.select("a") ?: return@mapNotNull null
            val url = fixUrlNull(aa.attr("href")) ?: return@mapNotNull null
            val title = aa.attr("title")
            val year = null
            var image = aa.select("img").attr("src")
            if (image.isNullOrBlank()) {
                image = aa.select("video").attr("poster").toString()
            }

            MovieSearchResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = globalTvType,
                posterUrl = image,
                year = year
            )
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val poster = doc.select("meta[property=og:image]").firstOrNull()?.attr("content")
        val title = doc.select("meta[name=title]").firstOrNull()?.attr("content")?.cleanText() ?: ""
        val descript = doc.select("meta[name=description]").firstOrNull()?.attr("content")?.cleanText()

        var streamUrl = doc.select("video source").firstOrNull()?.attr("src") ?: ""

        return MovieLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            type = globalTvType,
            dataUrl = streamUrl,
            posterUrl = poster,
            year = null,
            plot = descript
        )
    }

    private fun String.cleanText() : String = this.trim().removePrefix("Watch JAV Guru")
        .removeSuffix("HD Free Online on JAV.Guru").trim()
        .removePrefix("Watch JAV").trim()
}