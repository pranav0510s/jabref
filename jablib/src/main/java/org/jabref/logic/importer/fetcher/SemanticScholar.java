package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemanticScholar implements FulltextFetcher, PagedSearchBasedParserFetcher, EntryBasedFetcher, CustomizableKeyFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticScholar.class);

    private static final String SOURCE_ID_SEARCH = "https://api.semanticscholar.org/v1/paper/";
    private static final String SOURCE_WEB_SEARCH = "https://api.semanticscholar.org/graph/v1/paper/search?";
    private final ImporterPreferences importerPreferences;

    public SemanticScholar(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     * <p>
     * Uses the DOI if present, otherwise the arXiv identifier.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws IOException      if a page could not be fetched correctly
     * @throws FetcherException if the received page differs from what was expected
     */
    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        Optional<ArXivIdentifier> arXiv = entry.getField(StandardField.EPRINT).flatMap(ArXivIdentifier::parse);

        Document html = null;
        if (doi.isPresent()) {
            try {
                // Retrieve PDF link
                String source = SOURCE_ID_SEARCH + doi.get().asString();
                Connection jsoupRequest = Jsoup.connect(getURLBySource(source))
                                               .userAgent(URLDownload.USER_AGENT)
                                               .header("Accept", "text/html; charset=utf-8")
                                               .referrer("https://www.google.com")
                                               .ignoreHttpErrors(true);
                importerPreferences.getApiKey(getName()).ifPresent(
                        key -> jsoupRequest.header("x-api-key", key));
                html = jsoupRequest.get();
            } catch (IOException e) {
                LOGGER.info("Error for pdf lookup with DOI");
            }
        }
        if (arXiv.isPresent() && entry.getField(StandardField.EPRINT).isPresent()) {
            // Check if entry is a match
            String arXivString = entry.getField(StandardField.EPRINT).get();
            if (!arXivString.startsWith("arXiv:")) {
                arXivString = "arXiv:" + arXivString;
            }
            String source = SOURCE_ID_SEARCH + arXivString;
            Connection jsoupRequest = Jsoup.connect(getURLBySource(source))
                                    .userAgent(URLDownload.USER_AGENT)
                                    .referrer("https://www.google.com")
                                    .header("Accept", "text/html; charset=utf-8")
                                    .ignoreHttpErrors(true);
            importerPreferences.getApiKey(getName()).ifPresent(
                    key -> jsoupRequest.header("x-api-key", key));
            html = jsoupRequest.get();
        }
        if (html == null) {
            return Optional.empty();
        }

        Element metaTag = html.selectFirst("meta[name=citation_pdf_url]");
        if (metaTag == null) {
            return Optional.empty();
        }
        String link = metaTag.attr("content");
        if (StringUtil.isNullOrEmpty(link)) {
            return Optional.empty();
        }
        LOGGER.info("Fulltext PDF found @ SemanticScholar. Link: {}", link);
        return Optional.of(URLUtil.create(link));
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    String getURLBySource(String source) throws IOException, FetcherException {
        URLDownload download = new URLDownload(source);
        JSONObject json = new JSONObject(download.asString());
        LOGGER.debug("URL for source: {}", json.get("url").toString());
        if (!json.has("url")) {
            throw new FetcherException("Page does not contain field \"url\"");
        }
        return json.get("url").toString();
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery, int pageNumber) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(SOURCE_WEB_SEARCH);
        uriBuilder.addParameter("query", new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        uriBuilder.addParameter("offset", String.valueOf(pageNumber * getPageSize()));
        uriBuilder.addParameter("limit", String.valueOf(Math.min(getPageSize(), 10000 - pageNumber * getPageSize())));
        // All fields need to be specified
        uriBuilder.addParameter("fields", "paperId,externalIds,url,title,abstract,venue,year,authors");
        URL result = uriBuilder.build().toURL();
        LOGGER.debug("URL for query: {}", result);
        return result;
    }

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    @Override
    public Parser getParser() {
        return inputStream -> {

            JSONObject response = JsonReader.toJsonObject(inputStream);
            LOGGER.debug("Response for Parser: {}", response);
            if (response.isEmpty()) {
                return List.of();
            }

            int total = response.getInt("total");
            if (total == 0) {
                return List.of();
            } else if (response.has("next")) {
                total = Math.min(total, response.getInt("next") - response.getInt("offset"));
            }

            // Response contains a list
            JSONArray items = response.getJSONArray("data");
            List<BibEntry> entries = new ArrayList<>(items.length());
            for (int i = 0; i < total; i++) {
                JSONObject item = items.getJSONObject(i);
                BibEntry entry = jsonItemToBibEntry(item);
                entries.add(entry);
            }

            return entries;
        };
    }

    /**
     * This is copy-paste from CrossRef, need to be checked.
     *
     * @param item an entry received, needs to be parsed into a BibEntry
     * @return The BibEntry that corresponds to the received object
     * @throws ParseException if the JSONObject could not be parsed
     */
    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            BibEntry entry = new BibEntry(StandardEntryType.Article);
            entry.setField(StandardField.URL, item.optString("url"));
            entry.setField(StandardField.TITLE, item.optString("title"));
            entry.setField(StandardField.ABSTRACT, item.optString("abstract"));
            entry.setField(StandardField.VENUE, item.optString("venue"));
            entry.setField(StandardField.YEAR, item.optString("year"));

            entry.setField(StandardField.AUTHOR,
                    IntStream.range(0, item.optJSONArray("authors").length())
                             .mapToObj(item.optJSONArray("authors")::getJSONObject)
                             .map(author -> author.has("name") ? author.getString("name") : "")
                             .collect(Collectors.joining(" and ")));

            JSONObject externalIds = item.optJSONObject("externalIds");
            entry.setField(StandardField.DOI, externalIds.optString("DOI"));
            if (externalIds.has("ArXiv")) {
                entry.setField(StandardField.EPRINT, externalIds.getString("ArXiv"));
                entry.setField(StandardField.EPRINTTYPE, "arXiv");
            }
            entry.setField(StandardField.PMID, externalIds.optString("PubMed"));
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("SemanticScholar API JSON format has changed", exception);
        }
    }

    /**
     * Returns the localized name of this fetcher. The title can be used to display the fetcher in the menu and in the side pane.
     *
     * @return the localized name
     */
    @Override
    public String getName() {
        return "SemanticScholar";
    }

    /**
     * Looks for hits which are matched by the given {@link BibEntry}.
     *
     * @param entry entry to search bibliographic information for
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     * @throws FetcherException if an error linked to the Fetcher applies
     */
    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> title = entry.getTitle();
        if (title.isEmpty()) {
            return new ArrayList<>();
        }
        return performSearch(title.get());
    }
}
