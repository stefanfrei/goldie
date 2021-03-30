/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.schlibbuz.goldie;

import java.net.MalformedURLException;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Stefan Frei <stefan.a.frei@gmail.com>
 */
public class Main {

    private static final Logger w = LogManager.getLogger(Main.class);


    public static void main(String[] args) {

        final String urlString = "https://www.utf8-chartable.de/";

        try {
            new SimpleMiner(urlString).mine();
        } catch (MalformedURLException e) {
            w.error(e.getMessage());
        }


    }

    private static void trash() {
        IntStream.range(1, 20).forEach(i -> {
            System.out.println(UUID.randomUUID());
        });
    }

}
