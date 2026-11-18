package com.matrix.launcher;

import com.matrix.launcher.core.MediaLinks;
import com.matrix.launcher.core.SimklCachePolicy;
import com.matrix.launcher.core.SimklRequestPolicy;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Read-only AUTH V2 integration. No Nuvio credentials, scrobbling or account writes.
 *
 * Locking: library-type gates serialize each snapshot transaction; the client
 * monitor serializes shared activities/index work. Neither blocks token refresh. All shared
 * mutable state — token refresh/save, quota-policy state, and the
 * cache-prepared flag — is guarded by {@code stateLock}, held only for
 * brief in-memory or preferences operations. Token refresh is
 * single-flight: at most one refresh HTTP call runs at a time;
 * concurrent callers wait on {@code stateLock} and re-read the token
 * afterwards. Cache-file writes go through SimklDiskCache, whose methods
 * are synchronized and atomic-move based. Lock order: simklGate (Kotlin,
 * held by callers) → stateLock, never the reverse; SimklClient never
 * acquires simklGate, so the order is acyclic by construction. */
public final class SimklClient {
    interface Transport { JSONObject request(String path, JSONObject body, boolean authenticated) throws Exception; }
    private Transport transport;
    private final Object[] libraryLocks = {new Object(), new Object(), new Object()};
    private final SharedPreferences prefs;
    private final File cache;
    private final SharedPreferences home;
    private boolean cachePrepared;
    private final String version;
    private final SimklRequestPolicy policy=new SimklRequestPolicy();
    /** Guards token state, quota-policy state and the cache-prepared flag.
     * Never held across network I/O. Guarded by itself: refreshInFlight. */
    private final Object stateLock=new Object();
    private boolean refreshInFlight;
    public static final class Pin {
        String code, url, device;
        long expires;
        int interval;
    }
    static final class ApiException extends IOException {
        final String code;
        ApiException(String code) { super(code); this.code = code; }
    }
    SimklClient(Context context) {
        prefs = context.getSharedPreferences("simkl_v2", Context.MODE_PRIVATE);
        home = context.getSharedPreferences("personal_home", Context.MODE_PRIVATE);
        cache = new File(context.getFilesDir(), "simkl-cache");
        String installed="unknown";
        try{installed=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;}catch(Exception ignored){}
        version=installed;
        policy.limit=prefs.getInt("quota_limit",-1);policy.remaining=prefs.getInt("quota_remaining",-1);
        policy.reportedAt=prefs.getLong("quota_reported",0);policy.blockedUntil=prefs.getLong("retry_at",0);
        policy.reason=prefs.getString("retry_reason","");
    }
    /** Test transport retains the real snapshot/cache orchestration. */
    SimklClient(SharedPreferences prefs, SharedPreferences home, File cache, Transport transport) {
        this.prefs = prefs; this.home = home; this.cache = cache;
        this.transport = transport; version = "test";
    }
    String clientId() { return prefs.getString("client_id", ""); }
    void clientId(String id) {
        if (!id.equals(clientId())) { clearLocal(); prefs.edit().putString("client_id", id).apply(); }
    }
    boolean connected() { return configured(clientId(),prefs.getString("access_token", "")); }
    static boolean configured(String client,String token){return client!=null && !client.trim().isEmpty() && token!=null && !token.trim().isEmpty();}
    String name() { return prefs.getString("display_name", "SIMKL"); }
    Pin begin() throws Exception {
        if(clientId().trim().isEmpty())throw new ApiException("invalid_client");
        JSONObject request = new JSONObject().put("client_id",clientId()).put("scope","media:read");
        JSONObject response = request("/oauth2/device",request,false);
        Pin pin = new Pin(); pin.code=response.getString("user_code");pin.device=response.getString("device_code");
        pin.url=response.getString("verification_uri_complete");
        Uri uri=Uri.parse(pin.url);
        if(!"https".equals(uri.getScheme()) || !"simkl.com".equals(uri.getHost()))throw new IOException("Unexpected approval URL");
        pin.interval=Math.max(5,response.optInt("interval",5));pin.expires=System.currentTimeMillis()+response.optLong("expires_in",900)*1000;
        return pin;
    }
    boolean poll(Pin pin,java.util.function.BooleanSupplier active) throws Exception {
        if(System.currentTimeMillis()>=pin.expires)throw new ApiException("expired_token");
        try {
            JSONObject response=request("/oauth2/token",new JSONObject().put("client_id",clientId())
                    .put("grant_type","urn:ietf:params:oauth:grant-type:device_code").put("device_code",pin.device),false);
            if(!active.getAsBoolean())return false;saveTokens(response);return true;
        } catch(ApiException e) {
            if(e.code.equals("authorization_pending"))return false;
            if(e.code.equals("slow_down")){pin.interval+=5;return false;}
            throw e;
        }
    }
    private void saveTokens(JSONObject response)throws JSONException {
        synchronized (stateLock) {
            prefs.edit().putString("access_token",response.getString("access_token"))
                    .putString("refresh_token",response.getString("refresh_token"))
                    .putLong("expires",System.currentTimeMillis()+response.optLong("expires_in",604800)*1000).apply();
        }
    }
    /** True when the stored token is usable for at least another minute. Pure
     * preferences reads; no lock needed. */
    private boolean tokenFresh() {
        return System.currentTimeMillis()+60000<prefs.getLong("expires",0);
    }
    /** Single-flight token refresh (D-07). At most one refresh HTTP call runs
     * at a time; concurrent callers wait on stateLock and then re-read the
     * token instead of refreshing again. No lock is held during the HTTP
     * call, so a slow refresh stalls only token waiters — never other
     * SIMKL work. A failed refresh still clears the flag, so the next
     * waiter retries (sequentially, never concurrently). */
    private String access()throws Exception {
        if(!connected())throw new ApiException("not_connected");
        if(tokenFresh())return prefs.getString("access_token","");
        synchronized (stateLock) {
            while (refreshInFlight) {
                try { stateLock.wait(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw interrupted; }
            }
            if(tokenFresh())return prefs.getString("access_token","");
            refreshInFlight=true;
        }
        try {
            JSONObject token=request("/oauth2/token",new JSONObject().put("client_id",clientId()).put("grant_type","refresh_token")
                    .put("refresh_token",prefs.getString("refresh_token","")),false);
            saveTokens(token);
        } catch(Exception error) {
            if(canUseCache(error)) {
                synchronized (stateLock) { policy.networkFailure(System.currentTimeMillis()); savePolicy(); }
            }
            throw error;
        } finally {
            synchronized (stateLock) { refreshInFlight=false; stateLock.notifyAll(); }
        }
        return prefs.getString("access_token","");
    }
    void disconnect() {
        try {request("/oauth2/revoke",new JSONObject().put("client_id",clientId()).put("token",prefs.getString("refresh_token","")),false);}
        catch(Exception ignored){} finally {clearLocal();}
    }
    private void clearLocal() {
        synchronized (stateLock) {
            String id=clientId();prefs.edit().clear().putString("client_id",id).apply();
            policy.limit=policy.remaining=-1;policy.reportedAt=policy.blockedUntil=0;policy.reason="";
            cachePrepared=false;
            File[] files=cache.listFiles();if(files!=null)for(File file:files)file.delete();
        }
    }
    /** The stateLock is not held across the HTTP call. Quota-policy state
     * is touched only inside brief stateLock sections; the token comes
     * from access(), which has its own single-flight gate. */
    private JSONObject request(String path,JSONObject body,boolean authenticated)throws Exception {
        if (transport != null) return transport.request(path, body, authenticated);
        if(authenticated) {
            synchronized (stateLock) {
                if(policy.blocked(System.currentTimeMillis()))throw new ApiException(policy.reason);
            }
        }
        Uri.Builder url=Uri.parse("https://api.simkl.com"+path).buildUpon();
        if(!path.startsWith("/oauth2/"))url.appendQueryParameter("client_id",clientId()).appendQueryParameter("app-name","matrix-launcher").appendQueryParameter("app-version",version);
        HttpURLConnection connection=PersonalImages.connect(url.build().toString(),authenticated?access():null);
        connection.setRequestProperty("User-Agent","MatrixLauncher/"+version);connection.setRequestProperty("Accept","application/json");
        try {
            if(body!=null){connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");
                try(OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int code=connection.getResponseCode();InputStream stream=code>=400?connection.getErrorStream():connection.getInputStream();
            JSONObject response=new JSONObject();
            if(stream!=null)try(InputStream in=stream){String text=PersonalImages.text(in,8*1024*1024);try{response=parseResponse(text);}catch(JSONException invalid){if(code<400)throw invalid;}}
            String error=response.optString("error",code==429?"rate_limit":"http_"+code);
            if(authenticated){
                synchronized (stateLock) {
                    policy.observe(System.currentTimeMillis(),code,error,connection.getHeaderField("X-RateLimit-Limit"),connection.getHeaderField("X-RateLimit-Remaining"),connection.getHeaderField("Retry-After"));
                    if(code>=500)policy.networkFailure(System.currentTimeMillis());savePolicy();
                }
            }
            if(code>=400 || response.has("error") || stream==null)throw new ApiException(error);
            return response;
        }catch(IOException error){
            if(authenticated && !(error instanceof ApiException)){
                synchronized (stateLock) { policy.networkFailure(System.currentTimeMillis()); savePolicy(); }
            }
            throw error;
        }finally{connection.disconnect();}
    }
    /** Search returns an array; existing account/list endpoints return objects. */
    static JSONObject parseResponse(String text)throws JSONException {
        Object value=new JSONTokener(text).nextValue();
        if(value instanceof JSONObject)return (JSONObject)value;
        if(value instanceof JSONArray)return new JSONObject().put("items",value);
        throw new JSONException("Expected JSON object or array");
    }
    TvFeed.Row search(String query)throws Exception {
        return searchPage(request("/search/all?q="+Uri.encode(query)+"&limit=24&extended=full",null,true));
    }
    static TvFeed.Row searchPage(JSONObject response)throws JSONException {
        TvFeed.Row row=new TvFeed.Row("SIMKL search");
        JSONArray items=response.optJSONArray("items");
        if(items!=null)for(int i=0;i<Math.min(24,items.length());i++) {
            JSONObject entry=items.optJSONObject(i);if(entry==null)continue;String type=entry.optString("endpoint_type");
            if(!Arrays.asList("movies","tv","anime").contains(type))continue;
            TvFeed.Item item=media(entry,type);
            String year=entry.isNull("year")?"":entry.optString("year","");
            item.mediaType="movies".equals(type)?"Movie":"anime".equals(type)?("movie".equals(entry.optString("type"))?"Anime movie":"Anime series"):"Series";
            item.metadata=year;
            List<String> detail=new ArrayList<>();
            String status=entry.optString("status");
            if(status.equals("airing"))detail.add("Airing");else if(status.equals("ended"))detail.add("Ended");else if(status.equals("tba"))detail.add("Upcoming");
            int episodes=entry.optInt("ep_count",0);if(!type.equals("movies") && episodes>0)detail.add(episodes+" episode"+(episodes==1?"":"s"));
            item.summary=String.join(" · ",detail);
            JSONObject ratings=entry.optJSONObject("ratings");List<String> scores=new ArrayList<>();
            if(ratings!=null)for(String source:Arrays.asList("simkl","imdb","mal")) {
                JSONObject rating=ratings.optJSONObject(source);if(rating==null || rating.optLong("votes",0)<=0)continue;
                double score=rating.optDouble("rating",Double.NaN);
                if(Double.isFinite(score) && score>=0 && score<=10)scores.add((source.equals("imdb")?"IMDb":source.toUpperCase(Locale.ROOT))+" "+String.format(Locale.ROOT,"%.1f",score));
            }
            if(!scores.isEmpty())item.metadata+=(item.metadata.isEmpty()?"":"\n")+String.join(" · ",scores);
            if(!item.intent.isEmpty())row.items.add(item);
        }
        return row;
    }
    /** Persists quota state. Takes stateLock itself (reentrant: callers
     * already holding it for observe/networkFailure may call it directly). */
    private void savePolicy(){
        synchronized (stateLock) {
            prefs.edit().putInt("quota_limit",policy.limit).putInt("quota_remaining",policy.remaining)
                    .putLong("quota_reported",policy.reportedAt).putLong("retry_at",policy.blockedUntil).putString("retry_reason",policy.reason).apply();
        }
    }
    String usageSummary(){
        long now=System.currentTimeMillis(),reported=prefs.getLong("quota_reported",0);
        String message="AUTH V2 daily requests: Free 500, PRO 1,000, VIP 10,000. Your allowance is shared with other V2 apps.\n\n";
        if(reported>0 && now>=reported && now<SimklRequestPolicy.nextReset(reported))
            message+=prefs.getInt("quota_remaining",-1)+" of "+prefs.getInt("quota_limit",-1)+" requests remained at the last server response. Other apps may have used more since then.\n\n";
        else message+="No current allowance report. This page does not make an API request.\n\n";
        long retry=prefs.getLong("retry_at",0);
        if(now<retry)message+="Live requests paused for about "+Math.max(1,(retry-now+59999)/60000)+" minute(s). Cached lists remain available.\n\n";
        return message+"Requests reset at midnight in New York. Refresh respects server cooldowns. See SIMKL Connected Apps for usage across your account.";
    }
    private JSONObject fallback(JSONObject saved,Exception error)throws Exception{
        if(saved==null || !canUseCache(error))throw error;
        return saved;
    }
    static boolean canUseCache(Exception error){
        if(error instanceof ApiException){String code=((ApiException)error).code;return code.equals("rate_limit") || code.equals("user_limit_exceeded") || code.equals("app_limit_exceeded") || code.equals("connection_unavailable") || code.startsWith("http_5");}
        return error instanceof IOException;
    }
    /** Single preferences write; SharedPreferences edits are thread-safe. */
    void refreshOnNextRead() { prefs.edit().remove("activities_checked").apply(); }
    private synchronized JSONObject activities()throws Exception {
        // Only invoked by visible Home/user actions, never a repeating sync timer.
        long now=System.currentTimeMillis();JSONObject old=read("activities");
        if(old!=null && now-prefs.getLong("activities_checked",0)<300000)return old;
        try{JSONObject response=request("/sync/activities",null,true);write("activities",response);
            prefs.edit().putLong("activities_checked",now).apply();return response;
        }catch(Exception error){return fallback(old,error);}
    }
    synchronized JSONObject index()throws Exception {
        JSONObject index=read("lists");
        try{JSONObject activity=activities();
        JSONObject custom=activity.optJSONObject("custom_lists");String stamp=custom==null?activity.optString("all"):custom.toString();
        if(index!=null && stamp.equals(prefs.getString("lists_stamp","")))return index;
        if(!prefs.contains("user_id")) {
            JSONObject settings=request("/users/settings",null,true);
            prefs.edit().putString("user_id",String.valueOf(settings.getJSONObject("account").getLong("id")))
                    .putString("display_name",settings.getJSONObject("user").optString("name","SIMKL")).apply();
        }
        index=request("/lists/user/"+prefs.getString("user_id","")+"?followed=true&collaborants=true&limit=50",null,true);
        write("lists",index);prefs.edit().putString("lists_stamp",stamp).apply();return index;
        }catch(Exception error){return fallback(index,error);}
    }
    JSONObject list(String id,int page)throws Exception {
        if(!id.matches("[0-9]+"))throw new IOException("Invalid list");
        String key="list-"+id+"-"+Math.max(1,page);JSONObject saved=read(key);
        try{JSONObject activity=activities();String stamp=SimklCachePolicy.listStamp(activity,index(),id);
        // Auto lists can change daily without activity timestamps; cap this cache at one day as well.
        if(saved!=null && stamp.equals(saved.optString("_stamp")) && System.currentTimeMillis()-saved.optLong("_saved")<86400000)return saved;
        JSONObject result=request("/lists/"+id+"?limit=24&page="+Math.max(1,page)+"&extended=full",null,true);
        result.put("_stamp",stamp).put("_saved",System.currentTimeMillis());write(key,result);return result;
        }catch(Exception error){return fallback(saved,error);}
    }
    TvFeed.Row library(String type,String status)throws Exception {
        return library(type,status,1);
    }
    TvFeed.Row library(String type,String status,int page)throws Exception {
        if(!Arrays.asList("shows","movies","anime").contains(type) || !Arrays.asList("watching","plantowatch","hold","completed","dropped").contains(status))throw new IOException("Invalid library");
        synchronized (libraryLocks[Arrays.asList("shows","movies","anime").indexOf(type)]) {
        String key="library-"+type;JSONObject saved=read(key);
        try{JSONObject activity=activities();String stamp=activity.optString("all");
        String typeStamp=SimklCachePolicy.libraryStamp(activity,type);
        boolean changed=saved==null || !typeStamp.equals(saved.optString("_type_stamp"));
        // Lazy per-type initial snapshot, then deltas. Deletions use a small ID-only reconciliation.
        if(saved==null){saved=request("/sync/all-items/"+type,null,true);}
        else if(changed) {
            String since=saved.optString("_stamp");
            JSONObject delta=request("/sync/all-items/"+type+"?date_from="+Uri.encode(since),null,true);
            JSONArray merged=merge(saved.optJSONArray(type),delta.optJSONArray(type),type);
            JSONObject ids=request("/sync/all-items/"+type+"?extended=simkl_ids_only",null,true);
            Set<String> keep=new HashSet<>();JSONArray known=ids.optJSONArray(type);if(known!=null)for(int i=0;i<known.length();i++)keep.add(identity(known.getJSONObject(i),type));
            JSONArray retained=new JSONArray();for(int i=0;i<merged.length();i++)if(keep.contains(identity(merged.getJSONObject(i),type)))retained.put(merged.get(i));
            saved.put(type,retained);
        }
        if(changed){saved.put("_stamp",stamp).put("_type_stamp",typeStamp);write(key,saved);}
        }catch(Exception error){saved=fallback(saved,error);}
        return libraryPage(saved.optJSONArray(type),type,status,page);
        }
    }
    static TvFeed.Row libraryPage(JSONArray values,String type,String status,int page)throws Exception {
        TvFeed.Row row=new TvFeed.Row("SIMKL · "+status+" · "+type);
        int skip=(Math.max(1,page)-1)*24,matched=0;
        if(values!=null)for(int i=0;i<values.length();i++){
            JSONObject entry=values.optJSONObject(i);if(entry==null || !status.equals(entry.optString("status")))continue;
            JSONObject media=entry.optJSONObject(type.equals("movies")?"movie":"show");if(media==null)media=entry.optJSONObject("anime");if(media==null)continue;
            TvFeed.Item item=media(media,type);
            if(item.intent.isEmpty())continue;
            if(matched++<skip)continue;
            if(row.items.size()==24){row.hasMore=true;break;}
            row.items.add(item);
        }
        return row;
    }
    private static JSONArray merge(JSONArray old,JSONArray delta,String type)throws JSONException {
        LinkedHashMap<String,JSONObject> items=new LinkedHashMap<>();
        if(old!=null)for(int i=0;i<old.length();i++)items.put(identity(old.getJSONObject(i),type),old.getJSONObject(i));
        if(delta!=null)for(int i=0;i<delta.length();i++)items.put(identity(delta.getJSONObject(i),type),delta.getJSONObject(i));
        return new JSONArray(items.values());
    }
    private static String identity(JSONObject entry,String type) {
        JSONObject media=entry.optJSONObject(type.equals("movies")?"movie":"show");if(media==null)media=entry.optJSONObject("anime");if(media==null)media=entry;
        JSONObject ids=media.optJSONObject("ids");return ids==null?media.optString("title"):ids.optString("simkl",ids.optString("simkl_id",ids.optString("imdb",media.optString("title"))));
    }
    /** One selected title, not a per-poster batch; reuse auth/quota policy and bounded snapshot cache. */
    String imdb(String reference)throws Exception {
        if(!reference.matches("(?:movies|tv|anime):[0-9]+"))throw new IllegalArgumentException("Unsupported SIMKL title reference.");
        String key="identity-"+reference.replace(':','-');JSONObject saved=read(key);
        String imdb=imdbIdentity(saved);if(!imdb.isEmpty())return imdb;
        JSONObject detail=request("/"+reference.replace(':','/'),null,true);
        imdb=imdbIdentity(detail);
        if(imdb.isEmpty())throw new IOException("This title has no IMDb mapping for Stremio.");
        write(key,new JSONObject().put("ids",new JSONObject().put("imdb",imdb)));
        return imdb;
    }
    static String imdbIdentity(JSONObject detail) {
        JSONObject ids=detail==null?null:detail.optJSONObject("ids");
        String id=ids==null?"":value(ids,"imdb");return id.matches("tt[0-9]+")?id:"";
    }
    static TvFeed.Item media(JSONObject media,String type) {
        TvFeed.Item item=new TvFeed.Item();item.title=media.optString("title","Untitled");item.pkg=MediaLinks.SOURCE;
        String image=media.isNull("poster")?"":media.optString("poster","");item.poster=image.startsWith("https://")?image:(image.isEmpty()?"":"https://simkl.in/posters/"+image+"_m.webp");
        JSONObject ids=media.optJSONObject("ids");String id="";
        if(ids!=null){id=value(ids,"imdb");if(id.isEmpty() && !value(ids,"tmdb").isEmpty())id="tmdb:"+value(ids,"tmdb");
            if(id.isEmpty() && !value(ids,"mal").isEmpty())id="mal:"+value(ids,"mal");if(id.isEmpty())id="simkl:"+value(ids,ids.has("simkl_id")?"simkl_id":"simkl");}
        else if(media.has("id"))id="simkl:"+value(media,"id");
        String kind=type.equals("movie")||type.equals("movies")?"movie":"series";
        if("movie".equals(media.optString("anime_type")) || ("anime".equals(type) && "movie".equals(media.optString("type"))))kind="movie";
        String simkl=ids==null?value(media,"id"):value(ids,ids.has("simkl_id")?"simkl_id":"simkl");
        String endpoint=Arrays.asList("movie","movies").contains(type)?"movies":Arrays.asList("shows","series","tv").contains(type)?"tv":"anime".equals(type)?"anime":"";
        if(!endpoint.isEmpty() && simkl.matches("[0-9]+"))item.simklReference=endpoint+":"+simkl;
        item.intent=MediaLinks.title(kind,id);
        item.portrait=true;
        return item;
    }
    static List<TvFeed.Item> mediaItems(JSONArray values,String type,int limit) {
        List<TvFeed.Item> items=new ArrayList<>();
        if(values!=null)for(int i=0;i<Math.min(limit,values.length());i++) {
            JSONObject value=values.optJSONObject(i);if(value==null)continue;
            TvFeed.Item item=media(value,type);if(!item.intent.isEmpty())items.add(item);
        }
        return items;
    }
    private static String value(JSONObject object,String key){return object.isNull(key)?"":object.optString(key,"").trim();}
    private JSONObject read(String name) {
        // Only the prepare check is guarded: the file read itself is a
        // plain local read, and SimklDiskCache.prepare/write are already
        // synchronized + atomic-move based. A file deleted mid-read (by
        // clearLocal) simply reads as a cache miss and re-fetches.
        synchronized (stateLock) {
            if(!cachePrepared){SimklDiskCache.prepare(cache,retainedCacheKeys());cachePrepared=true;}
        }
        try(InputStream in=new FileInputStream(new File(cache,name+".json"))){return new JSONObject(PersonalImages.text(in,8*1024*1024));}catch(Exception e){return null;}
    }
    private Set<String> retainedCacheKeys() {
        Set<String> keys=new HashSet<>(Arrays.asList("activities","lists","library-shows","library-movies","library-anime"));
        home.getStringSet("simkl_pins",Collections.emptySet()).stream().filter(id->id.matches("[0-9]+")).sorted().limit(4)
                .forEach(id->keys.add("list-"+id+"-1"));
        return keys;
    }
    private void write(String name,JSONObject data)throws IOException {
        SimklDiskCache.write(cache,name,data.toString().getBytes(StandardCharsets.UTF_8),retainedCacheKeys());
    }
}
