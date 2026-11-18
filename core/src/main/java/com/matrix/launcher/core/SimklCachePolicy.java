package com.matrix.launcher.core;

import org.json.JSONArray;
import org.json.JSONObject;

/** Invalidate only the library type/list whose server timestamp changed. */
public final class SimklCachePolicy {
    public static String libraryStamp(JSONObject activities,String type){
        JSONObject bucket=activities.optJSONObject(type.equals("shows")?"tv_shows":type);
        return bucket==null?activities.optString("all"):bucket.toString();
    }
    public static String listStamp(JSONObject activities,JSONObject index,String id){
        JSONArray lists=index.optJSONArray("lists");
        if(lists!=null)for(int i=0;i<lists.length();i++){
            JSONObject list=lists.optJSONObject(i);
            if(list!=null && id.equals(list.optString("id")) && !list.isNull("updated_at") && !list.optString("updated_at").isEmpty())return list.optString("updated_at");
        }
        JSONObject custom=activities.optJSONObject("custom_lists");
        return custom==null?activities.optString("all"):custom.toString();
    }
}
