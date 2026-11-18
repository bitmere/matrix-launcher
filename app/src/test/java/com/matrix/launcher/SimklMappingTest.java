package com.matrix.launcher;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class SimklMappingTest {
    @org.junit.Test public void stremioLookupRetainsSimklIdentityAndAcceptsOnlyImdbDetails()throws Exception {
        org.json.JSONObject media=new org.json.JSONObject("{\"ids\":{\"simkl_id\":42,\"tmdb\":43}}");
        assertEquals("movies:42",SimklClient.media(media,"movies").simklReference);
        assertEquals("tv:42",SimklClient.media(media,"tv").simklReference);
        assertEquals("anime:42",SimklClient.media(media,"anime").simklReference);
        assertEquals("",SimklClient.media(media,"unknown").simklReference);
        assertEquals("tt42",SimklClient.imdbIdentity(new org.json.JSONObject("{\"ids\":{\"imdb\":\"tt42\"}}")));
        assertEquals("",SimklClient.imdbIdentity(new org.json.JSONObject("{\"ids\":{\"imdb\":\"tmdb:42\"}}")));
        assertEquals("",SimklClient.imdbIdentity(null));
    }
    @Test public void nullImdbFallsBackToTmdbAndKeepsPoster()throws Exception{
        JSONObject json=new JSONObject("{\"title\":\"Example\",\"year\":2025,\"poster\":\"12/abc\",\"ids\":{\"imdb\":null,\"tmdb\":42},\"ratings\":{\"imdb\":{\"rating\":8.1}}}");
        TvFeed.Item item=SimklClient.media(json,"movies");
        assertEquals("matrix-title://meta/movie/tmdb%3A42",item.intent);
        assertEquals("https://simkl.in/posters/12/abc_m.webp",item.poster);
        assertTrue(item.portrait);
    }
    @Test public void posterOnlyListPreviewUsesItsSimklId()throws Exception{
        TvFeed.Item item=SimklClient.media(new JSONObject("{\"id\":50250,\"type\":\"anime\",\"poster\":\"83/x\"}"),"anime");
        assertEquals("matrix-title://meta/series/simkl%3A50250",item.intent);
    }
    @Test public void animeMoviesStayMoviesAndMissingIdsNeverBecomeFakeLinks()throws Exception{
        assertEquals("matrix-title://meta/movie/tt123",SimklClient.media(new JSONObject("{\"anime_type\":\"movie\",\"ids\":{\"imdb\":\"tt123\"}}"),"anime").intent);
        assertEquals("",SimklClient.media(new JSONObject("{\"title\":\"No identifier\"}"),"tv").intent);
    }
}
