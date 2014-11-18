/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author turtlepool
 */
public class tags {

    public class tag {

        private String name = "";
        private String content = "";
        private Map<String, String> attributes = new HashMap<String, String>();

        public tag(String name, String attribute, String content) {
            this.name = name;
            this.content = content;

            String attr[] = attribute.split(" ");
            for (int i = 0; i < attr.length; i++) {
                int e = attr[i].indexOf("=");
                String n = "", v = "";
                String s[] = new String[2];
                s[0] = attr[i].substring(0, (e < 0 ? attr[i].length() : e));
                s[1] = attr[i].substring((e < 0 ? attr[i].length() : e + 1), attr[i].length());
                if (s.length >= 1) {
                    n = s[0];
                }
                if (s.length >= 2) {
                    v = s[1].replace("\"", "").replace("'", "");
                }
                attributes.put(n, v);
            }
        }

        public String getName() {
            return name;
        }

        public String getContent() {
            return content;
        }

        public String attribute(String s) {
            String v = attributes.get(s);
            if (v == null) {
                v = "";
            }
            return v;
        }

        public String getText() {
            return filter("&", ";", filter("<", ">", content));
        }

        public String filter(String s, String e, String input) {
            String output = input;
            int start = output.indexOf(s);
            int end = output.indexOf(e, start);
            while (start >= 0 && end >= 0) {
                output = output.substring(0, start) + output.substring(end + 1, output.length());
                start = output.indexOf(s);
                end = output.indexOf(e, start);
            }
            return output.replace("\t", "").replace(" +", " ").replace("\r", "");
        }

        public String filter() {
            return filter("img a", "iframe form input button textarea font", "style script noscript", content);
        }

        public String filter(String preserveTags, String removeTags, String removeTagsAndContent, String input) {
            List pt = Arrays.asList(preserveTags.split(" "));
            List rt = Arrays.asList(removeTags.split(" "));
            List rtc = Arrays.asList(removeTagsAndContent.split(" "));
            String output = "";
            int lastEnd = -1;
            int start = input.indexOf("<");
            int end = input.indexOf(">", start);
            while (start >= 0 && end >= 0) {
                String s[] = input.substring(start + 1, end).split(" ");
                if (s.length > 0) {
                    if (rt.contains(s[0].replace("/", ""))) {
                        output += input.substring(lastEnd + 1, start);
                        lastEnd = end;
                    } else if (rtc.contains(s[0]) && !s[0].startsWith("/")) {
                        output += input.substring(lastEnd + 1, start);
                        start = input.indexOf("</" + s[0], end);
                        end = input.indexOf(">", start);
                        if (start >= 0 && end >= 0) {
                            lastEnd = end;
                        }
                    } else if (s[0].startsWith("!")) {
                        end = input.indexOf("->", start) + 1;
                        output += input.substring(lastEnd + 1, start);
                        lastEnd = end;
                    } else {
                        output += input.substring(lastEnd + 1, start) + "<" + (!pt.contains(s[0].replace("/", "")) ? s[0] : input.substring(start + 1, end)) + ">";
                        lastEnd = end;
                    }
                }
                if (start >= 0 && end >= 0) {
                    start = input.indexOf("<", end);
                    end = input.indexOf(">", start);
                }
            }
            output += input.substring(lastEnd + 1, input.length());
            return output;
        }
    }

    public List<tag> list = new ArrayList<tag>();

    public tags(String name, String content) {
        int start = content.indexOf("<" + name);
        int end = content.indexOf(">", start);

        while (start >= 0 && end >= 0) {
            String attribute = content.substring(start + 1, end);

            //compare the tags name
            String s[] = attribute.split(" ");
            if (s[0].equalsIgnoreCase(name)) {
                //add the tags to list
                int endtag = content.indexOf("</" + name + ">", end);
                list.add(new tag(name, attribute, content.substring(end + 1, endtag < 0 ? end + 1 : endtag)));
            }

            start = content.indexOf("<" + name, end);
            end = content.indexOf(">", start);
        }
    }

    public tag getFirstTag() {
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}
