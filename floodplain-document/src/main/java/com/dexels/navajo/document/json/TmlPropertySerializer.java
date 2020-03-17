package com.dexels.navajo.document.json;

import com.dexels.navajo.document.Property;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TmlPropertySerializer extends StdSerializer<Property> {

    private static final long serialVersionUID = 6193576754986884043L;

    public TmlPropertySerializer() {
        this(null);
    }

    public TmlPropertySerializer(Class<Property> t) {
        super(t);
    }

    @Override
    public void serialize(Property property, JsonGenerator jg, SerializerProvider provider) throws IOException {
        
        Object value = property.getTypedValue();
        if (value == null) {
            jg.writeNull();
        } else if (property.getType().equals(Property.DATE_PROPERTY)) {
            DateFormat df = new SimpleDateFormat(Property.DATE_FORMAT2);
            try {
                jg.writeString(df.format((Date) value));
            } catch (ClassCastException e) {
                jg.writeString(value.toString());
            }
        } else if (property.getType().equals(Property.TIMESTAMP_PROPERTY)) {
            DateFormat df = new SimpleDateFormat(Property.TIMESTAMP_FORMAT);
            try {
                jg.writeString(df.format((Date) value));
            } catch (ClassCastException e) {
                jg.writeString(value.toString());
            }
        } else if (property.getType().equals(Property.COORDINATE_PROPERTY)) {
            jg.writeString(value.toString());
        } else {
            JsonSerializer<Object> serializer = provider.findValueSerializer(value.getClass());
            serializer.serialize(value, jg, provider);
        }
        
    }

}
