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
    private char delimiter;

    public LayoutWrapper(Writer writer, PropertiesConfigurationLayout layout) throws ConfigurationException {
        this.writer = writer;
        this.layout = layout;
        this.delimiter = layout.getConfiguration().isDelimiterParsingDisabled() ? 0
                : layout.getConfiguration().getListDelimiter();
    }
   /*public static String doMagic(String s) {
       if(s==null) return null;

       char[] data=s.toCharArray();
       int lpos, upos;
        char t;
        int count;
        boolean changed=false;

        for(lpos=upos=0; lpos<data.length; lpos++) {
            if(data[lpos]<0x80) {
                data[upos]=data[lpos];
                upos++;
            } else if(data[lpos]>0xFF) {
                return s;
            } else if(data[lpos]>0xEF) {
                return s;
            } else {
                t=data[lpos];
                if((t|0xE0)==t) count=2;
                else if((t|0xC0)==t) count=1;
                else return s;

                if(lpos+count>=data.length)
                    return s;

                t=(char)(t&(((1<<(6-count))-1)));

                for(int i=1; i<=count; i++) {
                    if(data[lpos+i]>0xBF)
                        return s;
                    t=(char)((t<<6)|(data[lpos+i]&0x3F));
                }

                lpos+=count;
                data[upos]=t;
                upos++;
                changed=true;
            }
        }
        if(changed)
            return new String(data,0,upos);
        else return s;
    }*/
    public void save(String key,String value) throws IOException {

        Iterator<String> iterator=layout.getKeys().iterator();
        if(layout.getHeaderComment()!=null){
            writeln(writer,layout.getCanonicalHeaderComment(true));
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
                List<Object> currents=(List)current;
                if(!currentKey.equals(key)) {
                    writer.write(currentKey + "=" + currents.get(0));
                    for(int i=1;i<currents.size();i++){
                        String val=(String) currents.get(i);
                        writer.write( delimiter  + val);
                    }
                    writeln(writer,null);
                }else {
                    writer.write(currentKey + "=" + value + LINE_SEPARATOR);
                }
            }else {
                if(!currentKey.equals(key)) {
                    writer.write(currentKey + "=" + current + LINE_SEPARATOR);
                }else {
                    writer.write(currentKey + "=" + value + LINE_SEPARATOR);
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
