package org.edu_sharing.elasticsearch.edu_sharing.client;

import org.springframework.util.StreamUtils;

import javax.activation.DataSource;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class PreviewDataReader implements MessageBodyReader<PreviewData> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public PreviewData readFrom(Class<PreviewData> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        PreviewData result = new PreviewData();
        result.setMimetype(mediaType.toString());
        result.setData(StreamUtils.copyToByteArray(inputStream));
        return result;
    }
}
