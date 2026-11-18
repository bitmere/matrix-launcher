package com.matrix.launcher.core;

import org.junit.Test;
import static org.junit.Assert.*;
public class MediaLinksTest {
    @Test public void titleHandoffKeepsTypedDecodedIdentity() {
        for(String type:new String[]{"movie","series"})for(String id:new String[]{"tt0133093","tmdb:1399","mal:5114","simkl:42"}) {
            MediaLinks.Title title=MediaLinks.parseTitle(MediaLinks.title(type,id));
            assertNotNull(title);assertEquals(type,title.type);assertEquals(id,title.id);
        }
    }
    @Test public void titleHandoffRejectsForeignActionsAndMalformedPaths() {
        for(String link:new String[]{"nuvio://auth/code","nuvio://addon.example/manifest.json","https://meta/movie/tt1",
                "nuvio://meta/movie/tt1/extra","nuvio://meta/movie/tt1?play=true","nuvio://meta/movie/tt1#extra",
                "nuvio://foreign@meta/movie/tt1","nuvio://meta/other/tt1","nuvio://meta/movie/tt1%2Fextra","nuvio://meta/movie/", "nuvio://meta"})
            { assertNull(link,MediaLinks.parseTitle(link)); assertNull(link,MediaLinks.parseTitle(link.replace("nuvio://", "matrix-title://"))); }
        assertNull(MediaLinks.parseTitle(null));
    }
    @Test public void catalogLinksAreAppIndependent(){
        assertEquals("matrix-title://meta/movie/tt0133093",MediaLinks.title("movies","tt0133093"));
        assertEquals("matrix-title://meta/series/tmdb%3A1399",MediaLinks.title("shows","tmdb:1399"));
        assertEquals("matrix-title://meta/series/mal%3A5114",MediaLinks.title("anime","mal:5114"));
    }
    @Test public void legacyNuvioLinksRemainParseableButAreNotGenerated() {
        assertEquals("tt42",MediaLinks.parseTitle("nuvio://meta/movie/tt42").id);
        assertEquals("series",MediaLinks.parseTitle("matrix-title://meta/series/tt43").type);
    }
    @Test public void stremioDetailsUseImdbAndNeverAutoPlay() {
        assertEquals("stremio:///detail/movie/tt42/tt42?autoPlay=false",MediaLinks.stremio("movie","tt42"));
        assertEquals("stremio:///detail/series/tt43?autoPlay=false",MediaLinks.stremio("series","tt43"));
        for(String id:new String[]{"tmdb:42","mal:42","simkl:42","tt42/extra","tt42?autoPlay=true",""})
            assertEquals("",MediaLinks.stremio("movie",id));
        assertEquals("",MediaLinks.stremio("other","tt42"));
    }
    @Test public void missingOrInjectedIdentifiersNeverLaunch(){
        assertEquals("",MediaLinks.title("movie","simkl:"));
        assertEquals("",MediaLinks.title("movie","tt1/../../auth"));
        assertEquals("",MediaLinks.title("movie",null));
    }
}
