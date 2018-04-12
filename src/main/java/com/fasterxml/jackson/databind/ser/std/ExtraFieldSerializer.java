package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;

import java.io.IOException;
import java.util.Set;

public class ExtraFieldSerializer extends BeanSerializerBase {

    BeanSerializerBase source;
    static ThreadLocal<String> typeFieldName = new ThreadLocal<>();
    static ThreadLocal<String> typeValue = new ThreadLocal<>();
    static ThreadLocal<String> joinFieldName = new ThreadLocal<>();
    static ThreadLocal<String> parentId = new ThreadLocal<>();

    public static void setTypeFieldName(String typeFieldName) {
        ExtraFieldSerializer.typeFieldName.set(typeFieldName);
    }

    public static void setTypeValue(String typeValue) {
        ExtraFieldSerializer.typeValue.set(typeValue);
    }

    public static void setJoinFieldName(String joinFieldName) {
        ExtraFieldSerializer.joinFieldName.set(joinFieldName);
    }

    public static void setParentId(String parentId) {
        ExtraFieldSerializer.parentId.set(parentId);
    }

    public ExtraFieldSerializer(BeanSerializerBase source) {
        super(source);
    }

    ExtraFieldSerializer(ExtraFieldSerializer source,
                         ObjectIdWriter objectIdWriter) {
        super(source, objectIdWriter);
    }

    @Override
    protected BeanSerializerBase withIgnorals(Set<String> set) {
        return source.withIgnorals(set);
    }

    @Override
    protected BeanSerializerBase asArraySerializer() {
        return source.asArraySerializer();
    }

    @Override
    public BeanSerializerBase withFilterId(Object o) {
        return source.withFilterId(o);
    }

    public BeanSerializerBase withObjectIdWriter(
            ObjectIdWriter objectIdWriter) {
        return new ExtraFieldSerializer(this, objectIdWriter);
    }


    public void serialize(Object bean, JsonGenerator jgen,
                          SerializerProvider provider) throws IOException,
            JsonGenerationException {
        jgen.writeStartObject();
        if (typeFieldName.get() != null && typeValue.get() != null) {
            jgen.writeStringField(typeFieldName.get(), typeValue.get());
        }
        if (joinFieldName.get() != null) {
            jgen.writeObjectFieldStart(joinFieldName.get());
            jgen.writeStringField("name", typeValue.get());
            if (parentId.get() != null) {
                jgen.writeStringField("parent", parentId.get());
            }
            jgen.writeEndObject();
        }
        typeFieldName.remove();
        typeValue.remove();
        parentId.remove();
        joinFieldName.remove();
        serializeFields(bean, jgen, provider);

        jgen.writeEndObject();
    }
}
