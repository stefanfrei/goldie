/*
 * The MIT License
 * Copyright © 2014-2021 Stefan Frei
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.schlibbuz.goldie;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private static final String FOLLOWUP_URL_SUFFIX = "unicode-utf8-table.pl";
    private static final String DUMP_FILE_NAME = "utf8.data";

    private final Set<Character> charSet;
    private final URL baseUrl;



    public SimpleMiner(final String urlString) throws MalformedURLException {
        charSet = new HashSet<>();
        baseUrl = new URL(urlString);
    }

    @Override
    public void mine() {
        File dumpFile = new File(DUMP_FILE_NAME);
        if(dumpFile.exists()) {
            w.info("Data is already dumped");
        }
        w.trace("Getting data from -> " + baseUrl);
        Document doc = getDocument(baseUrl.toString());
        addUTF8Set(doc); // first request is GET, subsequents are POST
        final String followupUrl = baseUrl + FOLLOWUP_URL_SUFFIX;
        getFollowupIds(doc).stream().forEach(id -> {
            Map<String, String> params = new HashMap<>();
            params.put("start", id);
            addUTF8Set(
                    getDocument(followupUrl, params)
            );

        });
        dump();
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
            charSet.add(s.charAt(0));
        });
    }

    private static final String blacklist = "\"';.()[]"; //chars not usable for tsql

    private boolean isBlacklisted(String s) {
        return blacklist.contains(s);
    }

    private void dump() {

        try(PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(
                    Paths.get(DUMP_FILE_NAME)
                )
        )) {
            var it = charSet.iterator();
            int charsWritten = 0;
            while(it.hasNext()) {
                pw.print(it.next());
                if(++charsWritten % 100 == 0) pw.print("\n");
            }
        } catch(IOException e) {
            w.error(e.getMessage());
        }

    }

}
