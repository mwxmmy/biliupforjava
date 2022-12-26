package top.sshh.bililiverecoder.util;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author mwxmmy
 */
public class ChunkUploadRequestBody extends RequestBody {

    InputStream in;

    public ChunkUploadRequestBody(InputStream in) {
        super();
        this.in = in;
    }

    @Override
    public long contentLength() throws IOException {
        return super.contentLength();
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return null;
    }

    @Override
    public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
        Source source = Okio.source(in);
        bufferedSink.writeAll(source);
    }
}