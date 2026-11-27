package com.lightning.browser.ui.browser

import org.json.JSONObject
import org.json.JSONTokener

data class ReadingTime(val minutes: Int, val words: Int)

object ReadingTimeEstimator {
    const val SCRIPT: String = """(function () {
  try {
    var MIN_WORDS = 200;
    var FALLBACK_MIN_WORDS = 800;
    var WPM = 200;
    var selectors = ['article', '[role="main"]', 'main', '.post-content', '.article-body', '#content'];
    var root = null;
    for (var i = 0; i < selectors.length; i++) {
      var el = document.querySelector(selectors[i]);
      if (el && (el.innerText || '').trim().length > 200) { root = el; break; }
    }
    if (!root) root = document.body;
    if (!root) return { article: false };

    var text = (root.innerText || root.textContent || '').replace(/\s+/g, ' ').trim();
    var words = text ? text.split(' ').length : 0;

    var og = document.querySelector('meta[property="og:type"]');
    var ogArticle = !!(og && og.getAttribute('content') &&
      og.getAttribute('content').toLowerCase() === 'article');
    var hasArticleMarkup = !!document.querySelector(
      'article, [itemtype*="Article"], [itemtype*="BlogPosting"]');

    var isArticle = words >= FALLBACK_MIN_WORDS ||
      (words >= MIN_WORDS && (hasArticleMarkup || ogArticle));
    if (!isArticle) return { article: false, words: words };

    return {
      article: true,
      words: words,
      minutes: Math.max(1, Math.round(words / WPM))
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
