package com.properties.prop.parser.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfigurationLayout;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

public class LayoutWrapper {
    private static String LINE_SEPARATOR=System.getProperty("line.separator");

    private Writer writer;
    private PropertiesConfigurationLayout layout;

    public LayoutWrapper(Writer writer, PropertiesConfigurationLayout layout) throws ConfigurationException {
        this.writer = writer;
        this.layout = layout;
    }
    public void save(String key,String value) throws IOException {
        Iterator<String> iterator=layout.getKeys().iterator();
        if(layout.getHeaderComment()!=null){
            writeln(writer,layout.getCanonicalHeaderComment(false));
            writeln(writer,null);
        }
        while(iterator.hasNext()){
            String currentKey=iterator.next();
            for (int i = 0; i < layout.getBlancLinesBefore(currentKey); i++)
            {
                writeln(writer,null);
            }

            // Output the comment
            if (layout.getComment(currentKey) != null)
            {
                writeln(writer,layout.getCanonicalComment(currentKey, true));
            }

            Object current=layout.getConfiguration().getProperty(currentKey);
            if(current instanceof List){
                for(Object currentValue : (List)current){
                    String val=(String) currentValue;
                    if(!currentKey.equals(key)) {
                        writer.write(currentKey + "=" + val + System.getProperty("line.separator"));
                    }else {
                        writer.write(currentKey + "=" + value + System.getProperty("line.separator"));
                    }
                }
            }else {
                if(!currentKey.equals(key)) {
                    writer.write(currentKey + "=" + current + System.getProperty("line.separator"));
                }else {
                    writer.write(currentKey + "=" + value + System.getProperty("line.separator"));
                }
            }
        }
        writer.flush();
        writer.close();
    }
    private void writeln(Writer writer,String s) throws IOException {
        if (s != null)
        {
            writer.write(s);
        }
        writer.write(LINE_SEPARATOR);
    }

}
