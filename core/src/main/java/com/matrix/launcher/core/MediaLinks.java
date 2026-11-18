package com.matrix.launcher.core;
/** Typed catalog identities are independent of the app selected to open them. */
public final class MediaLinks {
    public static final String SOURCE = "matrix.catalog";
    private static final java.util.regex.Pattern ID = java.util.regex.Pattern.compile("(?:tt[0-9]+|(?:tmdb|mal|simkl):[0-9]+)");
    public static final class Title {
        public final String type, id;
        private Title(String type, String id) { this.type = type; this.id = id; }
    }
    /** Parse only Matrix's title route, never addon installation/auth or a foreign component. */
    public static Title parseTitle(String link) {
        if(link == null)return null;
        try {
            java.net.URI uri = new java.net.URI(link);
            if(!("matrix-title".equals(uri.getScheme()) || "nuvio".equals(uri.getScheme())) || !"meta".equals(uri.getHost()) || uri.getUserInfo()!=null ||
                    uri.getPort()!=-1 || uri.getRawQuery()!=null || uri.getRawFragment()!=null)return null;
            String[] parts = uri.getRawPath().split("/",-1);
            if(parts.length!=3 || !(parts[1].equals("movie") || parts[1].equals("series")))return null;
            String id = java.net.URLDecoder.decode(parts[2],"UTF-8");
            return ID.matcher(id).matches() ? new Title(parts[1],id) : null;
        } catch(Exception invalid) { return null; }
    }
    /** Stremio Cinemeta uses IMDb IDs; never pretend MAL/SIMKL/TMDB IDs are equivalent. */
    public static String stremio(String type, String id) {
        if (!("movie".equals(type) || "series".equals(type)) || id == null || !id.matches("tt[0-9]+")) return "";
        return "stremio:///detail/" + type + "/" + id + ("movie".equals(type) ? "/" + id : "") + "?autoPlay=false";
    }
    public static String title(String type,String id) {
        if(id==null || !ID.matcher(id).matches())return "";
        String kind=("movie".equals(type)||"movies".equals(type))?"movie":"series";
        return "matrix-title://meta/"+kind+"/"+id.replace(":","%3A");
    }
}
