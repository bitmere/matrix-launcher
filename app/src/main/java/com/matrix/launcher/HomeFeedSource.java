package com.matrix.launcher;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Account-gated feed assembly. Selected list IDs remain usable outside the current index page. */
final class HomeFeedSource {
    /** Fetch seam for tests: production uses the real SimklClient. */
    interface FeedFetcher {
        TvFeed.Row library(String type, String status) throws Exception;
        JSONObject list(String id, int page) throws Exception;
    }

    /** Bounded pool for the feed fetches (D-13). Daemon threads, so a
     * stalled feed can never hold the process open; four workers bound
     * the fan-out (sections + pins are a handful of calls). The pool is
     * shared across Home loads; tasks from one load never wait on
     * simklGate, which only the calling thread holds. */
    private static final ExecutorService FEED_POOL = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "simkl-feed");
        thread.setDaemon(true);
        return thread;
    });

    static void append(TvFeed.Result result,android.content.SharedPreferences prefs,SimklClient simkl)throws Exception{
        append(result, prefs, new FeedFetcher() {
            @Override public TvFeed.Row library(String type, String status) throws Exception { return simkl.library(type, status); }
            @Override public JSONObject list(String id, int page) throws Exception { return simkl.list(id, page); }
        });
    }

    /** Fetches sections (LIBRARY order) and pins (sorted) concurrently and
     * assembles them in the same order as the sequential version: sections
     * first, then pins, inserted after the Nuvio rows. Each fetch runs on
     * the pool; assembly runs here, on the calling thread, in submission
     * order — so out-of-order arrivals cannot reorder rows. A failing
     * section or pin is isolated to the status message, exactly as before.
     * Interrupted while waiting: the interrupt flag is restored and the
     * interruption propagates (append declares throws Exception), so a
     * cancelled Home load is not mistaken for a failed section. */
    static void append(TvFeed.Result result,android.content.SharedPreferences prefs,FeedFetcher fetcher)throws Exception{
        List<Callable<TvFeed.Row>> jobs=new ArrayList<>();
        Set<String> enabled=new HashSet<>(prefs.getStringSet("simkl_sections",new HashSet<>(Arrays.asList("shows:watching","movies:plantowatch","shows:plantowatch","anime:watching"))));
        for(String[] section:HomeSections.LIBRARY){
            if(!enabled.contains(section[0]))continue;
            String[] parts=section[0].split(":");
            String title=section[1];
            String key=section[0];
            jobs.add(() -> {
                TvFeed.Row full=fetcher.library(parts[0],parts[1]);
                return HomeSections.preview("SIMKL · "+title,full,"simkl-library:"+key);
            });
        }
        Set<String> pins=new TreeSet<>(prefs.getStringSet("simkl_pins",Collections.emptySet()));
        for(String id:pins){
            jobs.add(() -> {
                JSONObject opened=fetcher.list(id,1);
                TvFeed.Row full=new TvFeed.Row("SIMKL · "+opened.optString("name","Custom list"));JSONArray items=opened.optJSONArray("items");
                full.items.addAll(SimklClient.mediaItems(items,opened.optString("media_type"),HomeSections.PREVIEW_ITEMS));
                return HomeSections.preview(full.title,full,"simkl-list:"+id);
            });
        }
        List<Future<TvFeed.Row>> futures=new ArrayList<>();
        for(Callable<TvFeed.Row> job:jobs)futures.add(FEED_POOL.submit(job));
        List<TvFeed.Row> added=new ArrayList<>();
        for(Future<TvFeed.Row> future:futures){
            try {
                TvFeed.Row row=future.get();
                if(row!=null)added.add(row);
            } catch(ExecutionException | CancellationException failed) {
                simklStatus(result,"Some lists could not be refreshed");
            } catch(InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            }
        }
        int at=0;while(at<result.rows.size() && result.rows.get(at).title.startsWith("Nuvio"))at++;
        result.rows.addAll(at,added);
    }
    private static void simklStatus(TvFeed.Result result,String message){if(!result.status.contains(message))result.status+=" · SIMKL: "+message;}
}
