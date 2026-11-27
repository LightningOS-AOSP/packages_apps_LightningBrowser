package com.lightning.browser.ui.browser

import org.json.JSONObject
import org.json.JSONTokener

data class ReadingTime(val minutes: Int, val words: Int)

object ReadingTimeEstimator {
    const val SCRIPT: String = """(function () {
  try {
    var MIN_WORDS = 200;
    var FALLBACK_MIN_WORDS = 600;
    var WPM = 200;
    var SKIP_SELECTOR = 'nav, header, footer, aside, form, button, [role="navigation"], [role="search"], [role="banner"], [role="contentinfo"], [role="complementary"], [role="menu"]';

    function countWords(str) {
      var cleaned = (str || '').replace(/\s+/g, ' ').trim();
      return cleaned ? cleaned.split(' ').length : 0;
    }

    var host = (window.location.hostname || '').toLowerCase();
    var path = (window.location.pathname || '').toLowerCase();
    var isHome = path === '' || path === '/' || path === '/index.html' || path === '/index.php';
    var isSearchPath = path === '/search' || path.indexOf('/search/') === 0 ||
      path.indexOf('/results') === 0 || path.indexOf('/find') === 0;
    var isNonArticleHost = /(^|\.)(google\.|bing\.com|duckduckgo\.com|search\.brave\.com|yahoo\.com|baidu\.com|yandex\.|youtube\.com|youtu\.be|steampowered\.com|steamcommunity\.com|epicgames\.com|twitch\.tv|tiktok\.com|instagram\.com|facebook\.com|twitter\.com|x\.com|amazon\.|flipkart\.com|ebay\.|netflix\.com|spotify\.com)/.test(host);

    if (isHome || isNonArticleHost || isSearchPath) {
      return { article: false };
    }

    var og = document.querySelector('meta[property="og:type"]');
    var ogType = (og && og.getAttribute('content') ? og.getAttribute('content') : '').trim().toLowerCase();
    var ogArticle = ogType === 'article';

    // Select the largest <article> so feed/search card grids don't trigger on a tiny first card.
    var articles = document.querySelectorAll('article');
    var bestArticle = null;
    var bestArticleWords = 0;
    var secondArticleWords = 0;
    for (var a = 0; a < articles.length; a++) {
      var w = countWords(articles[a].innerText || articles[a].textContent || '');
      if (w > bestArticleWords) {
        secondArticleWords = bestArticleWords;
        bestArticleWords = w;
        bestArticle = articles[a];
      } else if (w > secondArticleWords) {
        secondArticleWords = w;
      }
    }
    var hasSingleArticle = !!(bestArticle && bestArticleWords >= MIN_WORDS &&
      (articles.length <= 2 || (bestArticleWords >= 350 && bestArticleWords >= secondArticleWords * 2.5)));

    var root = null;
    if (hasSingleArticle) {
      root = bestArticle;
    } else {
      var selectors = ['[role="main"]', 'main', '.post-content', '.article-body', '#content'];
      for (var i = 0; i < selectors.length; i++) {
        var el = document.querySelector(selectors[i]);
        if (el && (el.innerText || '').trim().length > 200) {
          root = el;
          break;
        }
      }
      if (!root) root = document.body;
    }
    if (!root) return { article: false };

    if (isSearchPath && !ogArticle && !hasSingleArticle) {
      return { article: false };
    }

    var rawWords = countWords(root.innerText || root.textContent || '');
    if (rawWords < MIN_WORDS) return { article: false, words: rawWords };

    // Count real prose inside <p> elements outside navigation/chrome.
    var paragraphs = root.querySelectorAll('p');
    var pWords = 0;
    var proseParagraphs = 0;
    for (var p = 0; p < paragraphs.length; p++) {
      var pEl = paragraphs[p];
      if (pEl.closest && pEl.closest(SKIP_SELECTOR)) continue;
      var pw = countWords(pEl.innerText || pEl.textContent || '');
      if (pw > 0) {
        pWords += pw;
        if (pw >= 12) proseParagraphs++;
      }
    }

    // Measure link density inside root (excluding chrome).
    var links = root.querySelectorAll('a');
    var linkWords = 0;
    for (var l = 0; l < links.length; l++) {
      var lEl = links[l];
      if (lEl.closest && lEl.closest(SKIP_SELECTOR)) continue;
      linkWords += countWords(lEl.innerText || lEl.textContent || '');
    }
    var linkDensity = rawWords > 0 ? linkWords / rawWords : 1;
    var proseRatio = rawWords > 0 ? pWords / rawWords : 0;

    var schemaMatches = document.querySelectorAll(
      '[itemtype*="Article"], [itemtype*="BlogPosting"], [itemtype*="NewsArticle"], [itemtype*="TechArticle"]'
    );
    var hasSchema = schemaMatches.length > 0 && schemaMatches.length <= 3;

    var isArticle = false;
    if (ogArticle || hasSingleArticle || hasSchema) {
      isArticle = rawWords >= MIN_WORDS &&
        linkDensity <= 0.35 &&
        (pWords >= 80 || proseParagraphs >= 2);
    } else {
      isArticle = rawWords >= FALLBACK_MIN_WORDS &&
        pWords >= 300 &&
        proseParagraphs >= 3 &&
        proseRatio >= 0.35 &&
        linkDensity <= 0.35;
    }

    if (!isArticle) return { article: false, words: rawWords };

    var effectiveWords = (!hasSingleArticle && pWords >= 250 && proseRatio < 0.7) ? pWords : rawWords;
    return {
      article: true,
      words: effectiveWords,
      minutes: Math.max(1, Math.round(effectiveWords / WPM))
    };
  } catch (e) {
    return { article: false };
  }
})();"""

    fun parse(raw: String?): ReadingTime? {
        val value = raw?.takeIf { it.isNotBlank() && it != "null" } ?: return null
        // evaluateJavascript may return a JSON-encoded string if the script
        // ever returns JSON.stringify(...). Handle both shapes.
        val json = if (value.startsWith("\"")) {
            runCatching { JSONTokener(value).nextValue() as? String }.getOrNull()
        } else {
            value
        } ?: return null

        return runCatching {
            val obj = JSONObject(json)
            if (!obj.optBoolean("article", false)) return null
            val words = obj.optInt("words", 0)
            val minutes = obj.optInt("minutes", 0)
            if (words > 0 && minutes > 0) ReadingTime(minutes, words) else null
        }.getOrNull()
    }
}
