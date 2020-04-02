package com.dexels.kafka.streams.xml.parser;

import java.util.Hashtable;

/**
 * @author Frank Lyaruu
 */

public class CaseSensitiveXMLElement extends XMLElement {

    private static final long serialVersionUID = -6830073001073118575L;

    public CaseSensitiveXMLElement(String name) {
        super(new Hashtable<String, char[]>(), false, false);
        setName(name);
    }

    public CaseSensitiveXMLElement() {
        super(new Hashtable<String, char[]>(), false, false);
    }

    public CaseSensitiveXMLElement(boolean skipLeadingWhiteSpace) {
        super(new Hashtable<String, char[]>(), skipLeadingWhiteSpace, false);
    }

    @Override
    protected XMLElement createAnotherElement() {
        return new CaseSensitiveXMLElement();

    }

}