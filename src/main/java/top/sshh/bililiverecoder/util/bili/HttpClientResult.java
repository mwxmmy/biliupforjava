package top.sshh.bililiverecoder.util.bili;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Description: 封装httpClient响应结果
 *
 * @author JourWon
 * @date Created on 2018年4月19日
 */
public class HttpClientResult implements Serializable {

    private static final long serialVersionUID = 2168152194164783950L;

    /**
     * 响应状态码
     */
    private int code;
    private java.net.URI URI;

    private boolean isInputStreamClose = false;

    private String content;
    private byte[] contentByte;

    private CloseableHttpClient httpClient;
    private CloseableHttpResponse response;
    @Setter
    @Getter
    private CookieStore cookie;

    public HttpClientResult() {
    }

    public HttpClientResult(int code) {
        this.code = code;
    }

    public HttpClientResult(int code, CloseableHttpResponse response, CloseableHttpClient httpClient) {
        this.response = response;
        this.httpClient = httpClient;
        this.code = code;
    }

    public byte[] getBytes() throws IOException {
        try {
            contentByte = EntityUtils.toByteArray(response.getEntity());
            return contentByte;
        } finally {
            release();
        }
    }

    public int getCode() {
        return code;
    }

    public String getContent() throws IOException {
        try {
            content = EntityUtils.toString(response.getEntity(), "UTF-8");
            return content;
        } finally {
            release();
        }
    }


    public java.net.URI getURI() {
        return URI;
    }

    public void setURI(java.net.URI URI) {
        this.URI = URI;
    }


    /**
     * 获取网络输入流，注意：InputStream.close() 方法无法关闭输入流或者socket，请使用该工具类的release()方法关闭socket等其他资源
     *
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        if (isInputStreamClose) return null;
        return response.getEntity().getContent();
    }

    /**
     * 获取response header中Content-Disposition中的filename值
     *
     * @return
     */
    public String getFileName() {
        Header contentHeader = response.getFirstHeader("Content-Disposition");
        String filename = null;
        if (contentHeader != null) {
            HeaderElement[] values = contentHeader.getElements();
            if (values.length == 1) {
                NameValuePair param = values[0].getParameterByName("filename");
                if (param != null) {
                    //filename = new String(param.getValue().toString().getBytes(), "utf-8");
                    //filename=URLDecoder.decode(param.getValue(),"utf-8");
                    filename = param.getValue();
                }
            }
        }
        return filename;
    }

    public CloseableHttpResponse getResponse() {
        return response;
    }



    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Description: 释放资源
     *
     * @throws IOException
     */
    public void release() throws IOException {
        // 释放资源
        HttpClientUtils.release(response, httpClient);
        isInputStreamClose = true;
    }


}
