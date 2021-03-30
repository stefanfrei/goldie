/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.schlibbuz.goldie;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author Stefan Frei <stefan.a.frei@gmail.com>
 */
public class SimpleMiner implements Miner {

    private static final Logger w = LogManager.getLogger(SimpleMiner.class);
    private static final String followupUrlSuffix = "unicode-utf8-table.pl";

    private final Set<Byte> charSet;
    private final URL baseUrl;



    public SimpleMiner(final String urlString) throws MalformedURLException {
        charSet = new HashSet<>();
        baseUrl = new URL(urlString);
    }

    @Override
    public void mine() {
        w.trace("Getting data from -> " + baseUrl);
        Document doc = getDocument(baseUrl.toString());
        addUTF8Set(doc); // first request is GET, subsequents are POST
        final String followupUrl = baseUrl + followupUrlSuffix;
        getFollowupIds(doc).stream().forEach(id -> {
            Map<String, String> params = new HashMap<>();
            params.put("start", id);
            getDocument(followupUrl, params);

        });
    }

    private Document getDocument(final String url) {
        try {
            w.trace("Getting data from -> " + url);
            return Jsoup.connect(url).get();
        } catch(IOException e) {
            w.error(e.getMessage());
        }
        return  null;
    }

    private Document getDocument(final String url, Map<String, String> params) {

        try {
            w.trace("Getting data from -> " + url + " with params [ \"start\":\"" + params.get("start") + "\" ]");
            return Jsoup.connect(url).data(params).post();
        } catch(IOException e) {
            w.error(e.getMessage());
        }
        return  null;
    }

    private List<String> getFollowupIds(Document doc) {
        Set<String> vals = new HashSet<>();
        doc.select("table.params option").forEach(e -> {
            vals.add(e.val());
            w.trace(e.val());
        });
        return vals.stream().collect(Collectors.toList());
    }

    private void addUTF8Set(Document doc) {
        Elements elems = doc.select("table.codetable td.char");

        elems.forEach(elem -> {
            String s = elem.text().trim();
            //illegal parse or blacklisted
            if (s.length() != 1 || isBlacklisted(s)) return;
            charSet.add(s.getBytes()[0]);
        });
    }

    private static final String blacklist = "\"';.()[]"; //chars not usable for tsql

    private boolean isBlacklisted(String s) {
        return blacklist.contains(s);
    }

}
