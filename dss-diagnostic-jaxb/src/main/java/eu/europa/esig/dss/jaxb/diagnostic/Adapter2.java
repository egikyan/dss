//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.02.08 at 10:36:56 AM CET 
//


package eu.europa.esig.dss.jaxb.diagnostic;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import eu.europa.esig.dss.validation.TimestampedObjectType;

public class Adapter2
    extends XmlAdapter<String, TimestampedObjectType>
{


    public TimestampedObjectType unmarshal(String value) {
        return (eu.europa.esig.dss.jaxb.parsers.TimestampedObjectTypeParser.parse(value));
    }

    public String marshal(TimestampedObjectType value) {
        return (eu.europa.esig.dss.jaxb.parsers.TimestampedObjectTypeParser.print(value));
    }

}