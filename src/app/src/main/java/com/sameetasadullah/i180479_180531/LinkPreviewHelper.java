package com.turkhackteam.org;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkPreviewHelper {
    private static final String TAG = "LinkPreviewHelper";
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://|www\\.)[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*",
            Pattern.CASE_INSENSITIVE);

    public static boolean containsUrl(String text) {
        if (text == null || text.isEmpty()) return false;
        return URL_PATTERN.matcher(text).find();
    }

    public static String extractUrl(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group();
            if (!url.startsWith("http")) url = "https://" + url;
            return url;
        }
        return null;
    }

    public static void fetchPreview(String url, OnPreviewLoadedListener listener) {
        new AsyncTask<String, Void, LinkPreview>() {
            @Override
            protected LinkPreview doInBackground(String... params) {
                try {
                    Document doc = Jsoup.connect(params[0])
                            .userAgent("Mozilla/5.0 (compatible; THT-Chat/3.0)")
                            .timeout(5000)
                            .get();

                    String title = getMetaContent(doc, "og:title");
                    if (title == null || title.isEmpty()) title = doc.title();

                    String description = getMetaContent(doc, "og:description");
                    if (description == null || description.isEmpty())
                        description = getMetaContent(doc, "description");

                    String image = getMetaContent(doc, "og:image");

                    return new LinkPreview(params[0], title, description, image);
                } catch (Exception e) {
                    Log.e(TAG, "fetchPreview error: " + e.getMessage());
                    return new LinkPreview(params[0], params[0], null, null);
                }
            }

            @Override
            protected void onPostExecute(LinkPreview preview) {
                if (listener != null) listener.onLoaded(preview);
            }
        }.execute(url);
    }

    private static String getMetaContent(Document doc, String property) {
        Element el = doc.selectFirst("meta[property=" + property + "]");
        if (el == null) el = doc.selectFirst("meta[name=" + property + "]");
        return el != null ? el.attr("content") : null;
    }

    public static class LinkPreview {
        public final String url, title, description, imageUrl;

        public LinkPreview(String url, String title, String description, String imageUrl) {
            this.url = url;
            this.title = title != null ? title : url;
            this.description = description != null ? description : "";
            this.imageUrl = imageUrl;
        }
    }

    public interface OnPreviewLoadedListener {
        void onLoaded(LinkPreview preview);
    }
}
