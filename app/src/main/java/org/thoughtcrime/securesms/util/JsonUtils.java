package org.thoughtcrime.securesms.ryan.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import javax.annotation.Nullable;

public class JsonUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    com.fasterxml.jackson.module.kotlin.ExtensionsKt.registerKotlinModule(objectMapper);
  }

  public static <T> T fromJson(byte[] serialized, Class<T> clazz) throws IOException {
    return fromJson(new String(serialized), clazz);
  }

  public static <T> T fromJson(String serialized, Class<T> clazz) throws IOException {
    return objectMapper.readValue(serialized, clazz);
  }

  public static <T> T fromJson(InputStream serialized, Class<T> clazz) throws IOException {
    return objectMapper.readValue(serialized, clazz);
  }

  public static <T> T fromJson(Reader serialized, Class<T> clazz) throws IOException {
    return objectMapper.readValue(serialized, clazz);
  }

  public static <T> List<T> fromJsonArray(String serialized, Class<T> clazz) throws IOException {
    TypeFactory typeFactory = objectMapper.getTypeFactory();
    return objectMapper.readValue(serialized, typeFactory.constructCollectionType(List.class, clazz));
  }

  public static String toJson(Object object) throws IOException {
    return objectMapper.writeValueAsString(object);
  }

  public static ObjectMapper getMapper() {
    return objectMapper;
  }

  public static class SaneJSONObject {

    private final JSONObject delegate;

    public SaneJSONObject(JSONObject delegate) {
      this.delegate = delegate;
    }

    public @Nullable String getString(String name) throws JSONException {
      if (delegate.isNull(name)) return null;
      else                       return delegate.getString(name);
    }

    public long getLong(String name) throws JSONException {
      return delegate.getLong(name);
    }

    public boolean getBoolean(String name) throws JSONException {
      return delegate.getBoolean(name);
    }

    public boolean isNull(String name) {
      return delegate.isNull(name);
    }

    public int getInt(String name) throws JSONException {
      return delegate.getInt(name);
    }
  }
}
