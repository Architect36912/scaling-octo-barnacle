package com.icecream.dessert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.json.JSONObject
import java.net.URLEncoder

data class WebResult(
    val title: String,
    val url: String,
    val description: String,
    val points: String? = null,
    val author: String? = null,
    val comments: String? = null,
    val time: String? = null
)

data class BrowserState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val results: List<WebResult> = emptyList(),
    val error: String? = null,
    val currentUrl: String? = null,
    val isWebViewVisible: Boolean = false,
    val webHistory: List<String> = emptyList()
)

class BrowserViewModel : ViewModel() {
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    init {
        // Load initial front page stories
        searchWeb("")
    }

    fun searchWeb(query: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = true,
                    searchQuery = query,
                    error = null
                )

                val results = withContext(Dispatchers.IO) {
                    if (query.isEmpty()) {
                        // Fetch front page
                        scrapeHackerNews("https://news.ycombinator.com")
                    } else {
                        // Use the direct HN search page
                        val encodedQuery = URLEncoder.encode(query, "UTF-8")
                        scrapeHackerNews("https://news.ycombinator.com/front?q=$encodedQuery")
                    }
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    results = results
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    private fun scrapeHackerNews(url: String): List<WebResult> {
        val results = mutableListOf<WebResult>()
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .followRedirects(true)
                .get()

            // Both front page and search results use the same HTML structure
            val stories = doc.select("tr.athing")
            stories.forEach { story ->
                val titleElement = story.select("span.titleline > a").firstOrNull()
                val title = titleElement?.text() ?: return@forEach
                val storyUrl = titleElement.attr("href")
                
                // Get metadata from the next row
                val metadataRow = story.nextElementSibling()
                val points = metadataRow?.select("span.score")?.firstOrNull()?.text() ?: "0"
                val author = metadataRow?.select("a.hnuser")?.firstOrNull()?.text() ?: ""
                val comments = metadataRow?.select("a")?.lastOrNull()?.text() ?: "0"
                val time = metadataRow?.select("span.age")?.firstOrNull()?.text() ?: ""
                
                results.add(
                    WebResult(
                        title = title,
                        url = if (storyUrl.startsWith("item?")) "https://news.ycombinator.com/$storyUrl" else storyUrl,
                        description = "",
                        points = points,
                        author = author,
                        comments = comments,
                        time = time
                    )
                )
            }
        } catch (e: Exception) {
            throw Exception("Failed to parse page: ${e.message}")
        }
        return results
    }

    fun navigateToUrl(url: String) {
        _state.value = _state.value.copy(
            currentUrl = url,
            isWebViewVisible = true,
            webHistory = _state.value.webHistory + url
        )
    }

    fun goBack() {
        val history = _state.value.webHistory
        if (history.size > 1) {
            val previousUrl = history[history.size - 2]
            _state.value = _state.value.copy(
                currentUrl = previousUrl,
                webHistory = history.dropLast(1)
            )
        } else {
            closeWebView()
        }
    }

    fun closeWebView() {
        _state.value = _state.value.copy(
            isWebViewVisible = false,
            currentUrl = null
        )
    }
}
