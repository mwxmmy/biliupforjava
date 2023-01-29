package top.sshh.bililiverecoder.util.bili.upload.pojo;

import lombok.Data;

import java.util.Map;

@Data
public class PreUploadBean {


    private int OK;

    private String bili_filename;

    private String auth;

    private String uptoken;

    /**
     * X-Upos-Auth
     * X-Upos-Fetch-Source
     * Fetch-Header-Authorization
     */
    private Map<String, String> fetch_headers;

    private String fetch_url;

    private String key;

    private String[] fetch_urls;

    private long biz_id;

    private int chunk_retry;

    private int chunk_retry_delay;

    private long chunk_size;

    private String endpoint;

    private String[] endpoints;

    private String expose_params;

    private String put_query;

    private String threads;

    private String timeout;

    private String uip;

    private String upos_uri;

    public String getUpUrl() {
        if (this.upos_uri == null || this.upos_uri.equals("")) {
            return "";
        }
        return this.upos_uri.substring(this.upos_uri.indexOf("/") + 1);
    }

    public String getUptoken() {
        return "UpToken " + uptoken;
    }
}
