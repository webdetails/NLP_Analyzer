package hitachi.vantara.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONArray;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;

import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;
import opennlp.tools.tokenize.TokenizerME;

public class NaturalLanguageVis 
{

    private static File schema;
    
    
    public static String[] tokenizer(String query){

        // In the meanwhile I'm using a pre-trained tokenizer (available at http://opennlp.sourceforge.net/models-1.5/)
        // In the future I could train a tokenizer for greater accuracy

        String tokens[] = new String[0];

        InputStream inputStream = NaturalLanguageVis.class.getResourceAsStream("/en-token.bin");
        try{
            TokenizerModel model = new TokenizerModel(inputStream);
            TokenizerME tokenizer = new TokenizerME(model);
            tokens = tokenizer.tokenize(query);
            return tokens;
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
        return tokens;
    }

    public static String[] POStagger(String[] tokens){

        // In the meanwhile I'm using a pre-trained POS tagger (available at http://opennlp.sourceforge.net/models-1.5/)
        // In the future I could train a tagger for greater accuracy

        InputStream inputStreamTagger = NaturalLanguageVis.class.getResourceAsStream("/en-pos-maxent.bin");
    
        String[] tags = new String[0];

        try{
            POSModel posModel = new POSModel(inputStreamTagger);
            POSTaggerME posTagger = new POSTaggerME(posModel);
            tags = posTagger.tag(tokens);
        }
        catch(IOException e){
            e.printStackTrace();
        }

        return tags;
    }

    public static String[] lemmatizer(String[] tokens, String[] tags){

        // In the meanwhile I'm using a dictionary lemmatizer
        // A statistical lemmatizer could be used in the future for greater accuracy
      
        InputStream inputStreamLemmatizer = NaturalLanguageVis.class.getResourceAsStream("/en-lemmatizer.dict.txt");
    
        String[] lemmas = new String[0];

        try{
            DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(inputStreamLemmatizer);
            lemmas = lemmatizer.lemmatize(tokens, tags);
            System.out.println("TOKENS" + "     " + "TAGS" + "      " + "LEMMAS");
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
        return lemmas;
        
    }

    public static ArrayList<String> xmlParser (String level){
        
        ArrayList<String> ndNameArray = new ArrayList<String>();

        try{

            InputStream modrianXML = new FileInputStream(schema);
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(modrianXML);
            
            NodeList ndList = doc.getElementsByTagName(level);
    
            
            for(int i = 0; i < ndList.getLength(); i++){
                Node nd = ndList.item(i);
    
                if (nd.getNodeType() == Node.ELEMENT_NODE){
                    Element eElement = (Element) nd;
                    //Replacing white spaces with _
                    //This comes in handy when a Level/Measure has a whitespace
                    String name = eElement.getAttribute("name").replaceAll("\\s", "_");
                    ndNameArray.add(name);
    
                }
    
            }
    
        }
        catch(ParserConfigurationException e){
            e.printStackTrace();
        }
        catch(SAXException saxe){
            saxe.printStackTrace();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }

        return ndNameArray;

    }

    public static String findParentDim (String level) {
        
        level = level.replaceAll("_", " ");

        String output = "";

        try{

            InputStream modrianXML = new FileInputStream(schema);
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(modrianXML);
            
            NodeList ndList = doc.getElementsByTagName("Dimension");
    
            for(int i = 0; i < ndList.getLength(); i++){
                Node nd = ndList.item(i);
    
                if (nd.getNodeType() == Node.ELEMENT_NODE){
                    Element eElement = (Element) nd;

                    String dimname = eElement.getAttribute("name");

                    NodeList levelList = eElement.getElementsByTagName("Level");

                    for(int j = 0; j < levelList.getLength(); j++){
                        Node lvlnd = levelList.item(j);

                        if(lvlnd.getNodeType() == Node.ELEMENT_NODE){
                            Element lvlElement = (Element) lvlnd;

                            String lvlname = lvlElement.getAttribute("name");

                            if(lvlname.equals(level)){
                                output = "[" + dimname + "]" + "." + "[" + lvlname +"]";
                                break;
                            }
                        }

                    }
                }
            }
    
        }
        catch(ParserConfigurationException e){
            e.printStackTrace();
        }
        catch(SAXException saxe){
            saxe.printStackTrace();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }

        return output;

    }

    //This method receives an arraylist of strings and returns those strings stemmed
    public static ArrayList<String> stemmer(ArrayList<String> array){

        //I'm considering we're only tackling English
        //If support for other languages is added in the future, a parameter for language can be added
        SnowballStemmer stemmer = new SnowballStemmer(ALGORITHM.ENGLISH);
        
        ArrayList<String> output = new ArrayList<String>();

        for(int i = 0; i < array.size(); i++){
            output.add(((String) stemmer.stem(array.get(i))).toLowerCase(Locale.ENGLISH)); //lower case added for convenience
        }

        return output;
    }
    
    //This method matches the STEMS in stem_query with the STEMS in stem_lm
    //Returns a HashMap with all the matching STEMS and the equivalent in Natural Language(that's contained in ArrayList lm)
    public static HashMap<String,String> stem_cmp(ArrayList<String> stem_query, ArrayList<String> stem_lm, ArrayList<String> lm){

        HashMap<String,String> output = new HashMap<String,String>();

        for(int i = 0; i < stem_query.size(); i++){
            for(int j = 0; j < stem_lm.size(); j++){
                if(stem_query.get(i).equals(stem_lm.get(j))){
                    output.put(stem_query.get(i),lm.get(j));
                }
            }
        }

        return output;

    }

    public static Span[] span_chunker(String[] tokens, String[] tags){

        Span[] span_chunks = new Span[0];

        try{
            InputStream modelIn = NaturalLanguageVis.class.getResourceAsStream("/en-chunker.bin");
            ChunkerModel model = new ChunkerModel(modelIn);
            ChunkerME chunker = new ChunkerME(model);
            
            span_chunks = chunker.chunkAsSpans(tokens, tags);
            
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
        return span_chunks;
    }

    // Returns a HashMap with the Dimension/Measure in stem form and the filter on it
    public static HashMap<String,Span> filter_finder(Span[] span_chunks, ArrayList<String> stem_query, String[] tags, HashMap<String,String> levels_final, HashMap<String,String> measures_final){

        HashMap<String,Span> filters = new HashMap<String,Span>();
            
        for(int s = 0; s < span_chunks.length; s++){
            int i = span_chunks[s].getStart();
            int j = span_chunks[s].getEnd();
            Boolean its_filter = false;
            Span new_span = span_chunks[s];
            String key = new String();
            String secondary_key = new String();
            if(!(span_chunks[s].length() == 1 && (tags[i].equals("JJR") || tags[i].equals("JJS")))){

                Boolean level_present = false;

                for( ; i < j; i++){
                    if((tags[i].equals("CD") || tags[i].equals("NNP") || tags[i].equals("NNPS") || tags[i].equals("JJ") || tags[i].equals("JJR") || tags[i].equals("JJS")) 
                    && !its_filter){
    
                        //This isn't a perfect solution but aims at solving the problem
                        //where the preposition, adjective comparative/superlative was left out of the filter
                        if((s-1) >= 0 && span_chunks[s-1].length() == 1 && (tags[span_chunks[s-1].getStart()].equals("IN") || tags[span_chunks[s-1].getStart()].equals("JJR") 
                        || tags[span_chunks[s-1].getStart()].equals("JJS"))){
                            new_span = new Span(span_chunks[s-1].getStart(), new_span.getEnd());          
                        }

                        //checking in the vicinity for level/measure
                        if((s-1) >= 0){
                            int k = span_chunks[s-1].getStart();
                            int m = span_chunks[s-1].getEnd();

                            for(; k < m; k++){
                                if((tags[k].equals("NN") || tags[k].equals("NNS")) 
                                //sanity check
                                && (levels_final.get(stem_query.get(k)) != null || measures_final.get(stem_query.get(k)) != null)){
                                    secondary_key = stem_query.get(k);
                                }
                                
                            }
                        }

                        //checking in the vicinity for level/measure
                        if((s-2) >= 0 && secondary_key.length() == 0){
                            int k = span_chunks[s-2].getStart();
                            int m = span_chunks[s-2].getEnd();

                            for(; k < m; k++){
                                if(tags[k].equals("NN") || tags[k].equals("NNS")
                                //sanity check
                                && (levels_final.get(stem_query.get(k)) != null || measures_final.get(stem_query.get(k)) != null)){
                                    secondary_key = stem_query.get(k);
                                }
                                
                            }
                        }

                        //checking in the vicinity for preposition, adjective comparative/superlative
                        if((s-2) >= 0 && span_chunks[s-2].length() == 1 && (tags[span_chunks[s-2].getStart()].equals("IN") || tags[span_chunks[s-2].getStart()].equals("JJR") || tags[span_chunks[s-2].getStart()].equals("JJS"))){
                            new_span = new Span(span_chunks[s-2].getStart(), new_span.getEnd());
                        }

                        //Again this isn't perfect, it aims at solving the problem
                        //where the adverb, adjective comparative/superlative at the end was left out of the filter
                        //TODO: Fine tune this in the future, maybe don't include RBR and RBS
                        if((s+1) < span_chunks.length && span_chunks[s+1].length() == 1 && (tags[span_chunks[s+1].getStart()].equals("RB")
                        || tags[span_chunks[s+1].getStart()].equals("RBR") || tags[span_chunks[s+1].getStart()].equals("RBS")
                        || tags[span_chunks[s+1].getStart()].equals("JJR") || tags[span_chunks[s+1].getStart()].equals("JJS"))){
                            new_span = new Span(new_span.getStart(),span_chunks[s+1].getEnd());
                        }

                        //checking in the vicinity for adjective comparative/superlative
                        if((s+2) < span_chunks.length && span_chunks[s+2].length() == 1 && (tags[span_chunks[s+2].getStart()].equals("JJR") || tags[span_chunks[s+2].getStart()].equals("JJS"))){
                            new_span = new Span(new_span.getStart(),span_chunks[s+2].getEnd());
                        }

                        its_filter = true;
                    }

                    if(!level_present){
                        if(levels_final.get(stem_query.get(i)) != null){
                            level_present = true;
                            key = stem_query.get(i);
                        }
                        else if(measures_final.get(stem_query.get(i)) != null){
                            key = stem_query.get(i);
                        }
                    }
                    
                }

            }
            
            if(its_filter && key.length() != 0){
                if(filters.get(key) != null){
                    new_span = new Span(filters.get(key).getStart(), new_span.getEnd());
                }
                filters.put(key, new_span);
            } //if there wasn't a level/measure explicitly in the original filter, use one in the vicinity!
            else if(its_filter && secondary_key.length() != 0){
                if(filters.get(secondary_key) != null){
                    new_span = new Span(filters.get(secondary_key).getStart(), new_span.getEnd());
                }
                filters.put(secondary_key, new_span);
            } // we didn't find a level/measure inside the filter itself nor did we find two chunks behind
              // let's look back until we find a measure/level
            else if(its_filter){

                String third_key = new String();

                int y = s - 3;

                while(y >= 0 && third_key.length() == 0){
                    
                    int k = span_chunks[y].getStart();
                    int m = span_chunks[y].getEnd();

                    for(; k < m; k++){
                        if((tags[k].equals("NN") || tags[k].equals("NNS")) 
                        //sanity check
                        && (levels_final.get(stem_query.get(k)) != null || measures_final.get(stem_query.get(k)) != null)){
                            third_key = stem_query.get(k);
                        }
                        
                    }

                    y--;
                    
                }

                // this means we already got a filter on this dimension/measure
                // so lets extend the span to fit the new found filter
                
                if(filters.get(third_key) != null){
                    new_span = new Span(filters.get(third_key).getStart(), new_span.getEnd());
                }
                filters.put(third_key, new_span);

            }
        }

        return filters;

    }

    //This method is going to check the filters and set the right measure for the filter
    // as well as a retriciton on that filter for the number of items to show (if restriction not already present)
    // If there is no indication of the measure, the method makes a best guess of what the user wants
    public static HashMap<String, Span> filter_refiner(HashMap<String,Span> filters, HashMap<String,String> levels_final, HashMap<String,String> measures_final, String[] tags, String[] tokens){
        
        HashMap<String,Span> filters_final = new HashMap<String,Span>();
        
        for(String f: filters.keySet()){

            // Saving the original Key and Filter
            ArrayList<String> key = new ArrayList<String>();
            key.add(f);
            Span original_span = filters.get(f);
            String field = levels_final.get(f);

            // Checking if it's a filter on a Level
            // If it's already on a Measure, I'm not interested
            if(field != null){
                field = field.replaceAll("_", " ");

                //If the filter is on a Time Dimension or
                // A numeric dimension we don't want to change it
                Boolean time_dim = false;
                Boolean numeric = false;

                try{
                    InputStream modrianXML = new FileInputStream(schema);
                    
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(modrianXML);
                    
                    NodeList ndList = doc.getElementsByTagName("Level");
            
                    
                    for(int i = 0; i < ndList.getLength(); i++){
                        Node nd = ndList.item(i);
            
                        if (nd.getNodeType() == Node.ELEMENT_NODE){
                            Element eElement = (Element) nd;
                            
                            if(eElement.getAttribute("name").equals(field)){
                                if(eElement.getAttribute("levelType").startsWith("Time")){
                                    time_dim = true;
                                }
                                if(eElement.getAttribute("type").equals("Numeric")){
                                    numeric = true;
                                }
                                break;
                            }
                            
            
                        }
            
                    }
            
                }
                catch(ParserConfigurationException e){
                    e.printStackTrace();
                }
                catch(SAXException saxe){
                    saxe.printStackTrace();
                }
                catch(IOException ioe){
                    ioe.printStackTrace();
                }

                //Now we got a filter that might be on a Measure
                if(!time_dim && !numeric){
                    //If it has proper nouns we're not interested
                    //the filter is probably over that dimension
                    Boolean proper_noun = false;
                    //Checking for a number
                    //If one is not present we have to suggest a restriction
                    Boolean cardinal_number = false;
                    int z = filters.get(f).getStart();
                    int q = filters.get(f).getEnd();

                    for(; z < q; z++){
                        if(tags[z].equals("NNP") || tags[z].equals("NNPS")){
                            proper_noun = true;
                        }
                        if(tags[z].equals("CD")){
                            cardinal_number = true;
                        }
                    }

                    //Now we definitly got a filter that is related to a measure
                    //we just need to check if a measure is already present in the query
                    //as well as number restriction on the filter
                    if(!proper_noun){
                        //Got a measure on the query, set filter to be over that
                        key.clear();
                        if(measures_final.size() != 0){
                            for(String m: measures_final.keySet()){
                                key.add(m);
                            }
                        }
                        //TODO: Fine tune the best guess of the measure, it's very simple now
                        else{
                            //Don't have a measure on the query
                            //We have to take a best guess of what measure the user wants

                            //The best guess is: get the last measure with aggregator "sum"
                            //If there is no measure with the aggregator "sum" simply get the last measure
                            try{
                                InputStream modrianXML = new FileInputStream(schema);
                                
                                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                                Document doc = dBuilder.parse(modrianXML);
                                
                                NodeList ndList = doc.getElementsByTagName("Measure");
                        
                                for(int i = 0; i < ndList.getLength(); i++){
                                    Node nd = ndList.item(i);
                        
                                    if (nd.getNodeType() == Node.ELEMENT_NODE){
                                        Element eElement = (Element) nd;

                                        key.clear();
                                        measures_final.clear();

                                        String measure = eElement.getAttribute("name").replaceAll("\\s", "_");
                                        SnowballStemmer stemmer = new SnowballStemmer(ALGORITHM.ENGLISH);
                                        String measure_stemmed = ((String)stemmer.stem(measure)).toLowerCase(Locale.ENGLISH);
                                        
                                        key.add(measure_stemmed);
                                        measures_final.put(measure_stemmed, measure);

                                        if(eElement.getAttribute("aggregator").equals("sum")){
                                            
                                            key.clear();
                                            measures_final.clear();
                                            key.add(measure_stemmed);
                                            measures_final.put(measure_stemmed, measure);
                                        }

                                    }
                        
                                }
                        
                            }
                            catch(ParserConfigurationException e){
                                e.printStackTrace();
                            }
                            catch(SAXException saxe){
                                saxe.printStackTrace();
                            }
                            catch(IOException ioe){
                                ioe.printStackTrace();
                            }
            

                        }
                    }
                }

            }
            else if((field = measures_final.get(f)) != null){
                Boolean cardinal_number = false;
                int z = filters.get(f).getStart();
                int q = filters.get(f).getEnd();

                for(; z < q; z++){
                    if(tags[z].equals("CD")){
                        cardinal_number = true;
                    }
                }
            }

            for(int i = 0; i < key.size(); i++){
                filters_final.put(key.get(i), original_span);
            }

        }

        return filters_final;
    }
    
    
    public static HashMap<String, HashSet<String>> vis_finder(HashMap<String, Span> filters,  HashMap<String,String> levels, HashMap<String,String> measures, String[] tags, ArrayList<String> stem_query){

        HashSet<String> lv_vis = new HashSet<String>();
        HashSet<String> time_vis = new HashSet<String>();

        HashMap<String, HashSet<String>> output = new HashMap<String, HashSet<String>>();

        Boolean there_is_timedim = false;
        Boolean long_time = false;

        if(filters.size() != 0){
            for(String f: levels.keySet()){
                String lv = levels.get(f).replaceAll("_", " ");

                //If the filter is on a Time Dimension or
                // A numeric dimension it's not going to be used in the visualizaton
                Boolean time_dim = false;
                Boolean numeric = false;

                try{
                    InputStream modrianXML = new FileInputStream(schema);
                    
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(modrianXML);
                    
                    NodeList ndList = doc.getElementsByTagName("Level");
            
                    
                    for(int i = 0; i < ndList.getLength(); i++){
                        Node nd = ndList.item(i);
            
                        if (nd.getNodeType() == Node.ELEMENT_NODE){
                            Element eElement = (Element) nd;
                            
                            if(eElement.getAttribute("name").equals(lv)){
                                if(eElement.getAttribute("levelType").startsWith("Time")){
                                    time_dim = true;
                                    there_is_timedim = true;
                                    time_vis.add(f);

                                    if(filters.get(f) != null){
                                        int z = filters.get(f).getStart();
                                        int q = filters.get(f).getEnd();
                                        for( ; z < q; z++){
                                            if((tags[z].equals("IN") || tags[z].equals("JJ") || tags[z].equals("JJR") || tags[z].equals("JJS")) 
                                            && (stem_query.get(z).equals("last") || stem_query.get(z).equals("after") || stem_query.get(z).equals("sinc") || stem_query.get(z).equals("befor") 
                                            || stem_query.get(z).equals("between"))){
                                                long_time = true;
                                            } 
                                        }
                                    }
                                    else{
                                        long_time = true;
                                    }
                                }
                                if((eElement.getAttribute("type").equals("Numeric") || eElement.getAttribute("type").equals("Integer")) && filters.get(f) != null){
                                    numeric = true;
                                }
                                break;
                            }
                            
            
                        }
            
                    }
            
                }
                catch(ParserConfigurationException e){
                    e.printStackTrace();
                }
                catch(SAXException saxe){
                    saxe.printStackTrace();
                }
                catch(IOException ioe){
                    ioe.printStackTrace();
                }

                if(!time_dim && !numeric){
                    lv_vis.add(f);
                }
            }

            ArrayList<String> key_measures = new ArrayList<String>();
            for(String m: measures.keySet()){
                key_measures.add(m);
            } // FIXME: if there is more than one measure program is going to boom
            
            if(there_is_timedim){

                if(long_time){
                    if(key_measures.size() == 1 && filters.get(key_measures.get(0)) != null){
                        if(lv_vis.size() <= 1){
                            int z = filters.get(key_measures.get(0)).getStart();
                            int q = filters.get(key_measures.get(0)).getEnd();
        
                            int restriction = 0;

                            for(; z < q; z++){
                                if(tags[z].equals("CD")){
                                    restriction = Integer.parseInt(stem_query.get(z));
                                }
                            }

                            if(restriction == 0){
                                restriction = 10;
                            }

                            if(restriction <= 10){
                                HashSet<String> vis = new HashSet<String>();
                                vis.add("Line");
                                HashSet<String> meas = new HashSet<String>();
                                meas.add(key_measures.get(0));
                                output.put("Visualization", vis);
                                output.put("Measure", meas);
                                output.put("X-axis", time_vis);
                                output.put("Series", lv_vis);
                                return output;    
                            }
                            else{
                                HashSet<String> vis = new HashSet<String>();
                                vis.add("Column");
                                HashSet<String> meas = new HashSet<String>();
                                meas.add(key_measures.get(0));
                                output.put("Visualization", vis);
                                output.put("Measure", meas);
                                output.put("X-axis", time_vis);
                                output.put("Series", lv_vis);
                                return output;    
                            }
                        } 
                        else{
                            HashSet<String> vis = new HashSet<String>();
                            vis.add("Table");
                            HashSet<String> meas = new HashSet<String>();
                            meas.add(key_measures.get(0));
                            output.put("Visualization", vis);
                            output.put("Measure", meas);
                            output.put("Columns", time_vis);
                            output.put("Rows", lv_vis);
                            return output;
                        }
                    } 
                    else{

                        if(lv_vis.size() <= 1){
                            HashSet<String> vis = new HashSet<String>();
                            vis.add("Column");
                            HashSet<String> meas = new HashSet<String>();
                            meas.add(key_measures.get(0));
                            output.put("Visualization", vis);
                            output.put("Measure", meas);
                            output.put("X-axis", time_vis);
                            output.put("Series", lv_vis);
                            return output;
                        }
                        else{
                            HashSet<String> vis = new HashSet<String>();
                            vis.add("Table");
                            HashSet<String> meas = new HashSet<String>();
                            meas.add(key_measures.get(0));
                            output.put("Visualization", vis);
                            output.put("Measure", meas);
                            output.put("Columns", time_vis);
                            output.put("Rows", lv_vis);
                            return output;
                        }
                    } 
                }
                else{
                    if(key_measures.size() == 1 && filters.get(key_measures.get(0)) != null){
                        if(lv_vis.size() <= 1){
                            int z = filters.get(key_measures.get(0)).getStart();
                            int q = filters.get(key_measures.get(0)).getEnd();
        
                            int restriction = 0;

                            for(; z < q; z++){
                                if(tags[z].equals("CD")){
                                    restriction = Integer.parseInt(stem_query.get(z));
                                }
                            }

                            if(restriction == 0){
                                restriction = 10;
                            }

                            if(restriction <= 25){
                                HashSet<String> vis = new HashSet<String>();
                                vis.add("Pie");
                                HashSet<String> meas = new HashSet<String>();
                                meas.add(key_measures.get(0));
                                output.put("Visualization", vis);
                                output.put("Measure", meas);
                                output.put("Multi-Pie", time_vis);
                                output.put("Slices", lv_vis);
                                return output;
                            }
                            else{
                                HashSet<String> vis = new HashSet<String>();
                                vis.add("Column");
                                HashSet<String> meas = new HashSet<String>();
                                meas.add(key_measures.get(0));
                                output.put("Visualization", vis);
                                output.put("Measure", meas);
                                output.put("X-axis", time_vis);
                                output.put("Series", lv_vis);
                                return output;    
                            }
                        } 
                        else{
                            HashSet<String> vis = new HashSet<String>();
                            vis.add("Table");
                            HashSet<String> meas = new HashSet<String>();
                            meas.add(key_measures.get(0));
                            output.put("Visualization", vis);
                            output.put("Measure", meas);
                            output.put("Columns", time_vis);
                            output.put("Rows", lv_vis);
                            return output;
                        }
                    } 
                    else{
                        if(lv_vis.size() <= 1){
                            HashSet<String> vis = new HashSet<String>();
                            vis.add("Column");
                            HashSet<String> meas = new HashSet<String>();
                            meas.add(key_measures.get(0));
                            output.put("Visualization", vis);
                            output.put("Measure", meas);
                            output.put("X-axis", time_vis);
                            output.put("Series", lv_vis);
                            return output;
                        }
                        else{                
                            HashSet<String> vis = new HashSet<String>();
                            vis.add("Table");
                            HashSet<String> meas = new HashSet<String>();
                            meas.add(key_measures.get(0));
                            output.put("Visualization", vis);
                            output.put("Measure", meas);
                            output.put("Columns", time_vis);
                            output.put("Rows", lv_vis);
                            return output;
                        }

                    }
                }
            }
            else{
                if(key_measures.size() == 1 && filters.get(key_measures.get(0)) != null){
                    if(lv_vis.size() <= 1){
                        int z = filters.get(key_measures.get(0)).getStart();
                        int q = filters.get(key_measures.get(0)).getEnd();
    
                        int restriction = 0;

                        for(; z < q; z++){
                            if(tags[z].equals("CD")){
                                restriction = Integer.parseInt(stem_query.get(z));
                            }
                        }

                        if(restriction == 0){
                            restriction = 10;
                        }

                        if(restriction <= 25){
                            HashSet<String> vis = new HashSet<String>();
                            vis.add("Pie");
                            HashSet<String> meas = new HashSet<String>();
                            meas.add(key_measures.get(0));
                            HashSet<String> multip = new HashSet<String>();
                            multip.add("");

                            output.put("Visualization", vis);
                            output.put("Measure", meas);
                            output.put("Multi-Pie", multip);
                            output.put("Slices", lv_vis);
                            return output;
                        }
                        else{

                            HashSet<String> vis = new HashSet<String>();
                            vis.add("Column");
                            HashSet<String> meas = new HashSet<String>();
                            meas.add(key_measures.get(0));
                            HashSet<String> series = new HashSet<String>();
                            series.add("");
                            
                            output.put("Visualization", vis);
                            output.put("Measure", meas);
                            output.put("X-axis", lv_vis);
                            output.put("Series", series);
                            return output;    
                        }

                    }
                    else{
                        int a = filters.get(key_measures.get(0)).getStart();
                        int b = filters.get(key_measures.get(0)).getEnd();
    
                        String series_dim = new String();

                        for(; a < b; a++){
                            if(tags[a].equals("NN") || tags[a].equals("NNS")){
                                series_dim = stem_query.get(a);
                                break;
                            }
                        }

                        HashSet<String> vis = new HashSet<String>();
                        vis.add("Column");
                        HashSet<String> meas = new HashSet<String>();
                        meas.add(key_measures.get(0));
                        lv_vis.remove(series_dim);
                        HashSet<String> series = new HashSet<String>();
                        series.add(series_dim);
                        
                        output.put("Visualization", vis);
                        output.put("Measure", meas);
                        output.put("X-axis", lv_vis);
                        output.put("Series", series);
                        return output;      
                    }
                } 
                else{
                    HashSet<String> vis = new HashSet<String>();
                    vis.add("Table");
                    HashSet<String> meas = new HashSet<String>();
                    meas.add(key_measures.get(0));
                    HashSet<String> columns = new HashSet<String>();
                    columns.add("");
                    output.put("Visualization", vis);
                    output.put("Measure", meas);
                    output.put("Columns", columns);
                    output.put("Rows", lv_vis);
                    return output;
                }
            }
        }
        else{
            for(String f: levels.keySet()){
                String lv = levels.get(f).replaceAll("_", " ");

                //If the filter is on a Time Dimension or
                // A numeric dimension it's not going to be used in the visualizaton
                Boolean time_dim = false;
                Boolean numeric = false;

                try{
                    InputStream modrianXML = new FileInputStream(schema);
                    
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(modrianXML);
                    
                    NodeList ndList = doc.getElementsByTagName("Level");
            
                    
                    for(int i = 0; i < ndList.getLength(); i++){
                        Node nd = ndList.item(i);
            
                        if (nd.getNodeType() == Node.ELEMENT_NODE){
                            Element eElement = (Element) nd;
                            
                            if(eElement.getAttribute("name").equals(lv)){
                                if(eElement.getAttribute("levelType").startsWith("Time")){
                                    time_dim = true;
                                    time_vis.add(f);
                                }
                                if((eElement.getAttribute("type").equals("Numeric") || eElement.getAttribute("type").equals("Integer")) && filters.get(f) != null){
                                    numeric = true;
                                }
                                break;
                            }
                            
            
                        }
            
                    }
            
                }
                catch(ParserConfigurationException e){
                    e.printStackTrace();
                }
                catch(SAXException saxe){
                    saxe.printStackTrace();
                }
                catch(IOException ioe){
                    ioe.printStackTrace();
                }

                if(!time_dim && !numeric){
                    lv_vis.add(f);
                }
            }

            
            if(time_vis.size() != 0){
                HashSet<String> vis = new HashSet<String>();
                vis.add("Table");
    
                HashSet<String> meas = new HashSet<String>();
                
                for(String m: measures.keySet()){
                    meas.add(m);
                } // FIXME: if there is more than one measure program is going to boom
                

                output.put("Visualization", vis);
                output.put("Measure", meas);
                output.put("Columns", time_vis);
                output.put("Rows", lv_vis);
                return output;
            }
            
            else{
                HashSet<String> vis = new HashSet<String>();
                vis.add("Table");

                HashSet<String> meas = new HashSet<String>();
                
                for(String m: measures.keySet()){
                    meas.add(m);
                } // FIXME: if there is more than one measure program is going to boom

                HashSet<String> columns = new HashSet<String>();
                columns.add("");
                output.put("Visualization", vis);
                output.put("Measure", meas);
                output.put("Columns", columns);
                output.put("Rows", lv_vis);
                return output;
            }
        }
    }

    public static JSONObject run(String query, File sc){


        /*-------------------------------------------------------------------------------
        *----------------------Setting up Dictionary Maps for operators------------------
        ---------------------------------------------------------------------------------*/

        HashMap<String, String> measopdictionary = new HashMap<String, String>();

        InputStream is = NaturalLanguageVis.class.getResourceAsStream("/measureoperator.dict.txt");
        InputStreamReader r = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(r);

        try{
                    
            String line = null;
            
            //read file line by line
            while ( (line = br.readLine()) != null ){
                
                //split the line by :
                String[] parts = line.split(":");
                
                String key = parts[0].trim();
                String value = parts[1].trim();
                
                if( !key.equals("") && !value.equals("") )
                    measopdictionary.put(key, value);
            }
                        
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            
            //closing the buffered reader
            if(br != null){
                try { 
                    br.close(); 
                }catch(Exception e){};
            }
        }
        
        HashMap<String, String> numopdictionary = new HashMap<String, String>();

        is = NaturalLanguageVis.class.getResourceAsStream("/numericoperator.dict.txt");
        r = new InputStreamReader(is);
        br = new BufferedReader(r);

        try{
                    
            String line = null;
            
            //read file line by line
            while ( (line = br.readLine()) != null ){
                
                //split the line by :
                String[] parts = line.split(":");
                
                String key = parts[0].trim();
                String value = parts[1].trim();
                
                if( !key.equals("") && !value.equals("") )
                    numopdictionary.put(key, value);
            }
                        
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            
            //closing the buffered reader
            if(br != null){
                try { 
                    br.close(); 
                }catch(Exception e){};
            }
        }


        HashMap<String, String> timeopdictionary = new HashMap<String, String>();

        is = NaturalLanguageVis.class.getResourceAsStream("/timecoperator.dict.txt");
        r = new InputStreamReader(is);
        br = new BufferedReader(r);

        try{
                    
            String line = null;
            
            //read file line by line
            while ( (line = br.readLine()) != null ){
                
                //split the line by :
                String[] parts = line.split(":");
                
                String key = parts[0].trim();
                String value = parts[1].trim();
                
                if( !key.equals("") && !value.equals("") )
                    timeopdictionary.put(key, value);
            }
                        
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            
            //closing the buffered reader
            if(br != null){
                try { 
                    br.close(); 
                }catch(Exception e){};
            }
        }

        LinkedHashMap<String, String> monthsdictionary = new LinkedHashMap<String, String>();

        is = NaturalLanguageVis.class.getResourceAsStream("/months.dict.txt");
        r = new InputStreamReader(is);
        br = new BufferedReader(r);

        try{
                    
            String line = null;
            
            //read file line by line
            while ( (line = br.readLine()) != null ){
                
                //split the line by :
                String[] parts = line.split(":");
                
                String key = parts[0].trim();
                String value = parts[1].trim();
                
                if( !key.equals("") && !value.equals("") )
                    monthsdictionary.put(key, value);
            }
                        
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            
            //closing the buffered reader
            if(br != null){
                try { 
                    br.close(); 
                }catch(Exception e){};
            }
        }

        LinkedHashMap<String, String> quartersdictionary = new LinkedHashMap<String, String>();

        is = NaturalLanguageVis.class.getResourceAsStream("/quarters.dict.txt");
        r = new InputStreamReader(is);
        br = new BufferedReader(r);

        try{
                    
            String line = null;
            
            //read file line by line
            while ( (line = br.readLine()) != null ){
                
                //split the line by :
                String[] parts = line.split(":");
                
                String key = parts[0].trim();
                String value = parts[1].trim();
                
                if( !key.equals("") && !value.equals("") )
                    quartersdictionary.put(key, value);
            }
                        
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            
            //closing the buffered reader
            if(br != null){
                try { 
                    br.close(); 
                }catch(Exception e){};
            }
        }
        

        /*-------------------------------------------------------------------------------
        *----------------------Getting the SCHEMA----------------------------------------
        ---------------------------------------------------------------------------------*/

        schema = sc;
        
        /*------------------------------------------------------------------------------
        --------------------------------------------------------------------------------*/

        /*-------------------------------------------------------------------------------
        *----------------------Getting the RAW TOKENS out of the QUERY-------------------
        --------------------------------------------------------------------------------*/
        
        String[] tmp_tokens = tokenizer(query);
        
        if(tmp_tokens.length == 0){
            System.exit(1);
        }    
        
        /*-----------------------------------------------------------------------------
        -------------------------------------------------------------------------------*/

        /*-----------------------------------------------------------------------------
        *----------------------Getting the RAW TAGS out of the QUERY-------------------
        -------------------------------------------------------------------------------*/

        String[] tmp_tags = POStagger(tmp_tokens);
        
        if(tmp_tags.length == 0){
            System.exit(1);
        }

        /*-----------------------------------------------------------------------------
        -------------------------------------------------------------------------------*/


        /*-----------------------------------------------------------------------------
        *------------------------Parsing the XML SCHEMA file---------------------------
        *----------------------Getting the LEVELS and MEASURES-------------------------
        -------------------------------------------------------------------------------*/

        
        ArrayList<String> levels = xmlParser("Level");
        
        if(levels.size() == 0){
            System.out.println("It appears the Mondrain XML file you specified has no Levels");
            System.exit(1);
        }
        
        ArrayList<String> measures = xmlParser("Measure");
        
        if(measures.size() == 0){
            System.out.println("It appears the Mondrain XML file you specified has no Measures");
            System.exit(1);
        }

        /*------------------------------------------------------------------------------
        --------------------------------------------------------------------------------*/
        
        /*-----------------------------------------------------------------------------
        *----------------------STEMMING the RAW QUERY, LEVELS and MEASURES-----------------
        -------------------------------------------------------------------------------*/
        
        //Turning this into an arraylist because the method stemmer only receives arraylist
        ArrayList<String> tmparray = new ArrayList<String>(Arrays.asList(tmp_tokens));

        ArrayList<String> stem_query = stemmer(tmparray);

        ArrayList<String> stem_levels = stemmer(levels);
        
        ArrayList<String> stem_measures = stemmer(measures);
                
        /*-----------------------------------------------------------------------------
        -------------------------------------------------------------------------------*/

        /*-----------------------------------------------------------------------------
        *---Checking if there is any Measures/Levels with white spaces in the query----
        *-----------------------and substituting it with a "_" ------------------------
        -------------------------------------------------------------------------------*/

        ArrayList<String> clean_tokens = new ArrayList<String>();
        ArrayList<String> clean_tags = new ArrayList<String>();
        for(int i = 0; i < tmp_tags.length; i++){
            if(i+1 < tmp_tags.length && (tmp_tags[i].equals("NN") || tmp_tags[i].equals("NNS")) && (tmp_tags[i+1].equals("NN") || tmp_tags[i+1].equals("NNS"))
            && !stem_levels.contains(stem_query.get(i)) && !stem_measures.contains(stem_query.get(i)) && !stem_levels.contains(stem_query.get(i+1)) && !stem_measures.contains(stem_query.get(i+1))){
                clean_tags.add("NN");
                clean_tokens.add(tmp_tokens[i] + "_" + tmp_tokens[i+1]);
                i++;
            }
            else{
                clean_tags.add(tmp_tags[i]);
                clean_tokens.add(tmp_tokens[i]);
            }
        }

        String[] tokens = clean_tokens.toArray(new String[clean_tokens.size()]);
        String[] tags = clean_tags.toArray(new String[clean_tags.size()]);


        /*-----------------------------------------------------------------------------
        -------------------------------------------------------------------------------*/

        /*-----------------------------------------------------------------------------
        *--------Stemming the QUERY AGAIN in case we altered the TOKENS & TAGS---------
        -------------------------------------------------------------------------------*/

        //Turning this into an arraylist because the method stemmer only receives arraylist
        tmparray = new ArrayList<String>(Arrays.asList(tokens));
        
        stem_query = stemmer(tmparray);
        

        /*-----------------------------------------------------------------------------
        -------------------------------------------------------------------------------*/


        /*-----------------------------------------------------------------------------
        *---------------------Getting the LEVELS & MEASURES----------------------------
        ---------------------------present in the QUERY--------------------------------
        ------------------------------------------------------------------------------*/


        HashMap<String,String> levels_final = stem_cmp(stem_query, stem_levels, levels);

        HashMap<String,String> measures_final = stem_cmp(stem_query, stem_measures, measures);

        /*---------------------------------------------------------------------------
        ----------------------------------------------------------------------------*/


        /*---------------------------------------------------------------------------
        *---------------------Getting the SPANS of the QUERY-------------------------
        ----------------------------------------------------------------------------*/

        Span[] span_chunks = span_chunker(tokens, tags);

        if(span_chunks.length == 0){
            System.exit(1);
        }

        /*---------------------------------------------------------------------------
        -----------------------------------------------------------------------------*/

        /*----------------------------------------------------------------------------
        *----------Taking the SPANS and finding which ones are FILTERS----------------
        -----------------------------------------------------------------------------*/
        
        HashMap<String, Span> filters = filter_finder(span_chunks, stem_query, tags, levels_final, measures_final);

        HashMap<String,Span> filter_final = filter_refiner(filters, levels_final, measures_final, tags, tokens);
        
        /*---------------------------------------------------------------------------
        -----------------------------------------------------------------------------*/



        
        /*---------------------------------------------------------------------------
        *-------------Finding which VIS is better based on the filters---------------
        -----------------------------------------------------------------------------*/

        HashMap<String, HashSet<String>> vis = vis_finder(filter_final, levels_final, measures_final, tags, stem_query);

        /*---------------------------------------------------------------------------
        -----------------------------------------------------------------------------*/


        String graph = new String();

        Iterator<String> i = vis.get("Visualization").iterator(); 
        while (i.hasNext()){ 
            graph = i.next();
        }

        JSONObject json = new JSONObject();
        ArrayList<String> tmpa = new ArrayList<String>();
        JSONArray ja = new JSONArray();

        switch(graph){
            case "Pie":

                json.put("viz", "ccc_pie");
                i = vis.get("Slices").iterator(); 
                tmpa = new ArrayList<String>();
                while (i.hasNext()){
                    tmpa.add(findParentDim(levels_final.get(i.next())));
                }
                
                Collections.reverse(tmpa);
                ja = new JSONArray(tmpa);
                json.put("rows", ja);
                
                i = vis.get("Multi-Pie").iterator();
                tmpa = new ArrayList<String>(); 
                while (i.hasNext()){ 
                    String tmp = i.next(); 
                    if(levels_final.get(tmp) != null){
                        tmpa.add(findParentDim(levels_final.get(tmp)));
                    }
                }

                Collections.reverse(tmpa);
                ja = new JSONArray(tmpa);
                json.put("columns", ja);

                ja = new JSONArray();
                i = vis.get("Measure").iterator(); 
                while (i.hasNext()){
                    // don't know why but the api needs two of these
                    String tmp = i.next();
                    ja.put("[Measures]." + "[" + measures_final.get(tmp) + "]");
                    ja.put("[Measures]." + "[" + measures_final.get(tmp) + "]");
                }

                json.put("measures", ja);

                break;
            case "Line":

                json.put("viz", "ccc_line");

                i = vis.get("X-axis").iterator();
                tmpa = new ArrayList<String>();
                while (i.hasNext()){ 
                    tmpa.add(findParentDim(levels_final.get(i.next())));
                }
                
                Collections.reverse(tmpa);
                ja = new JSONArray(tmpa);
                json.put("rows", ja);
                
                i = vis.get("Series").iterator(); 
                tmpa = new ArrayList<String>();
                while (i.hasNext()){ 
                    String tmp = i.next();
                    if(levels_final.get(tmp) != null){
                        tmpa.add(findParentDim(levels_final.get(tmp)));
                    }
                }
                
                Collections.reverse(tmpa);
                ja = new JSONArray(tmpa);
                json.put("columns", ja);

                ja = new JSONArray();
                i = vis.get("Measure").iterator(); 
                while (i.hasNext()){
                    // don't know why but the api needs two of these
                    String tmp = i.next();
                    ja.put("[Measures]." + "[" + measures_final.get(tmp) + "]");
                    ja.put("[Measures]." + "[" + measures_final.get(tmp) + "]");
                }

                json.put("measures", ja);

                break;
            case "Column":

                json.put("viz", "ccc_bar");

                i = vis.get("X-axis").iterator();
                tmpa = new ArrayList<String>();
                while (i.hasNext()){ 
                    tmpa.add(findParentDim(levels_final.get(i.next())));
                }

                Collections.reverse(tmpa);
                ja = new JSONArray(tmpa);
                json.put("rows", ja);


                i = vis.get("Series").iterator();
                tmpa = new ArrayList<String>();
                while (i.hasNext()){ 
                    String tmp = i.next();
                    if(levels_final.get(tmp) != null){
                        tmpa.add(findParentDim(levels_final.get(tmp)));
                    }
                }
                
                Collections.reverse(tmpa);
                ja = new JSONArray(tmpa);
                json.put("columns", ja);


                ja = new JSONArray();
                i = vis.get("Measure").iterator(); 
                while (i.hasNext()){
                    // don't know why but the api needs two of these
                    String tmp = i.next();
                    ja.put("[Measures]." + "[" + measures_final.get(tmp) + "]");
                    ja.put("[Measures]." + "[" + measures_final.get(tmp) + "]");
                }

                json.put("measures", ja);

                break;
            case "Table":

                json.put("viz", "pivot");
                
                i = vis.get("Rows").iterator();
                tmpa = new ArrayList<String>();
                while (i.hasNext()){ 
                    tmpa.add(findParentDim(levels_final.get(i.next())));
                }
                
                Collections.reverse(tmpa);
                ja = new JSONArray(tmpa);
                json.put("rows", ja);

                i = vis.get("Columns").iterator();
                tmpa = new ArrayList<String>();
                while (i.hasNext()){ 
                    String tmp = i.next();
                    if(levels_final.get(tmp) != null){
                        tmpa.add(findParentDim(levels_final.get(tmp)));
                    }                                   
                }
                
                Collections.reverse(tmpa);
                ja = new JSONArray(tmpa);
                json.put("columns", ja);

                ja = new JSONArray();
                i = vis.get("Measure").iterator(); 
                while (i.hasNext()){
                    // don't know why but the api needs two of these
                    String tmp = i.next();
                    ja.put("[Measures]." + "[" + measures_final.get(tmp) + "]" );
                    ja.put("[Measures]." + "[" + measures_final.get(tmp) + "]");
                }

                json.put("measures", ja);


                break;
        }

        if(filters.size() == 0){
            //There's no filters, put empty array
            ja = new JSONArray();
            json.put("filters",ja);
            json.put("numfilters",ja);
        }
        else{

            JSONArray jafilter = new JSONArray();
            JSONArray janumfilter = new JSONArray();

            for(String f: filter_final.keySet()){
                if(levels_final.get(f) == null){
                    //it's a measure

                    JSONObject jsoninsideja = new JSONObject();

                    int j = filter_final.get(f).getStart();
                    int k = filter_final.get(f).getEnd();

                    String dim = "";

                    //find the dim to apply the measure filter on
                    for( ; j < k; j++){
                        if(levels_final.get(stem_query.get(j)) != null){
                            dim = findParentDim(levels_final.get(stem_query.get(j)));
                        }
                    }

                    //dim not present in query, take first in rows
                    if(dim.equals("")){
                        JSONArray tmpja = (JSONArray) json.get("rows");
                        dim = tmpja.getString(0);
                    }

                    //The dim is now set in the json
                    jsoninsideja.put("dim", dim);


                    //----------------------------------------------------------------------
                    //----------------------------------------------------------------------


                    JSONArray jasinsidefilter = new JSONArray();


                    //going to iterate over the span again to find the operators and numbers
                    j = filter_final.get(f).getStart();
                    k = filter_final.get(f).getEnd();

                    String operator1 = "";
                    String operator2 = "";

                    Integer op1 = null;

                    Integer op2 = null;

                    String formula = "[Measures].[" + measures_final.get(f).replaceAll("_", " ") + "]";

                    for( ; j < k; j++){

                        if(stem_query.get(j).equals("top") || stem_query.get(j).equals("bottom") || stem_query.get(j).equals("best")
                        || stem_query.get(j).equals("worst") || stem_query.get(j).equals("greater") || stem_query.get(j).equals("less") || stem_query.get(j).equals("between")
                        || stem_query.get(j).equals("equal")){
                            if(!operator1.equals("")){
                                operator2 = measopdictionary.get(stem_query.get(j));
                            }
                            else{
                                operator1 = measopdictionary.get(stem_query.get(j));
                            }
                        }

                        if(tags[j].equals("CD")){

                            if(op1 != null){
                                op2 = Integer.parseInt(stem_query.get(j));
                            }
                            else{
                                op1 = Integer.parseInt(stem_query.get(j));
                            }

                        }
                    }

                    
                    if(operator1.equals("") && operator2.equals("")){
                        //do nothing here
                    }
                    else{
                        if(op1 == null){
                            op1 = 10;
                        }

                        if(operator2.equals("")){
                            if(operator1.equals("TOP") || operator1.equals("BOTTOM")){
                                JSONObject tmpjson = new JSONObject();
                                tmpjson.put("count", Integer.toString(op1));
                                tmpjson.put("formula", formula);
                                tmpjson.put("operator", operator1);
                                jasinsidefilter.put(tmpjson);
                            }
                            else{
                                if(op2 == null){
                                    //probably a "greater", "less" or "equals" filter
                                    JSONObject tmpjson = new JSONObject();
                                    tmpjson.put("op1", Integer.toString(op1));
                                    tmpjson.put("formula", formula);
                                    tmpjson.put("operator", operator1);
                                    jasinsidefilter.put(tmpjson);


                                }
                                else{

                                    if(operator1.equals("EQUAL")){

                                        j = filter_final.get(f).getStart();
                                        k = filter_final.get(f).getEnd();

                                        op1 = null;

                                        formula = "[Measures].[" + measures_final.get(f).replaceAll("_", " ") + "]";

                                        for( ; j < k; j++){

                                            if(tags[j].equals("CD")){

                                                op1 = Integer.parseInt(stem_query.get(j));

                                                JSONObject tmpjson = new JSONObject();
                                                tmpjson.put("op1", Integer.toString(op1));
                                                tmpjson.put("formula", formula);
                                                tmpjson.put("operator", operator1);
                                                jasinsidefilter.put(tmpjson);

                                            }
                                        }

                                    }
                                    else{
                                        //means it's a "between" filter
        
                                        if(op1 <= op2){
                                            JSONObject tmpjson = new JSONObject();
                                            tmpjson.put("op1", Integer.toString(op1));
                                            tmpjson.put("op2", Integer.toString(op2));
                                            tmpjson.put("formula", formula);
                                            tmpjson.put("operator", operator1);
                                            jasinsidefilter.put(tmpjson);
                                        }
                                        else{
                                            JSONObject tmpjson = new JSONObject();
                                            tmpjson.put("op1", Integer.toString(op2));
                                            tmpjson.put("op2", Integer.toString(op1));
                                            tmpjson.put("formula", formula);
                                            tmpjson.put("operator", operator1);
                                            jasinsidefilter.put(tmpjson);
                                        }
                                    }
                                }
                            }
                        }
                        else{
                            //it's probably a greater than and less than filter
    
                            if(op2 != null){
                                //just a sanity check
                                if(op1 <= op2){
                                    if(operator1.equals("GREATER_THAN_EQUAL")){
                                        JSONObject tmpjson1 = new JSONObject();
                                        tmpjson1.put("op1", Integer.toString(op1));
                                        tmpjson1.put("formula", formula);
                                        tmpjson1.put("operator", operator1);
                                        jasinsidefilter.put(tmpjson1);
    
                                        JSONObject tmpjson2 = new JSONObject();
                                        tmpjson2.put("op1", Integer.toString(op2));
                                        tmpjson2.put("formula", formula);
                                        tmpjson2.put("operator", operator2);
                                        jasinsidefilter.put(tmpjson2);
    
                                    }
                                    else{
    
                                        JSONObject tmpjson1 = new JSONObject();
                                        tmpjson1.put("op1", Integer.toString(op1));
                                        tmpjson1.put("formula", formula);
                                        tmpjson1.put("operator", operator2);
                                        jasinsidefilter.put(tmpjson1);
    
                                        JSONObject tmpjson2 = new JSONObject();
                                        tmpjson2.put("op1", Integer.toString(op2));
                                        tmpjson2.put("formula", formula);
                                        tmpjson2.put("operator", operator1);
                                        jasinsidefilter.put(tmpjson2);
    
                                    }
    
                                }
                                else{
    
                                    if(operator1.equals("LESS_THAN_EQUAL")){
    
                                        JSONObject tmpjson1 = new JSONObject();
                                        tmpjson1.put("op1", Integer.toString(op2));
                                        tmpjson1.put("formula", formula);
                                        tmpjson1.put("operator", operator2);
                                        jasinsidefilter.put(tmpjson1);
    
                                        JSONObject tmpjson2 = new JSONObject();
                                        tmpjson2.put("op1", Integer.toString(op1));
                                        tmpjson2.put("formula", formula);
                                        tmpjson2.put("operator", operator1);
                                        jasinsidefilter.put(tmpjson2);
    
                                    }
                                    else{
                                        JSONObject tmpjson1 = new JSONObject();
                                        tmpjson1.put("op1", Integer.toString(op2));
                                        tmpjson1.put("formula", formula);
                                        tmpjson1.put("operator", operator1);
                                        jasinsidefilter.put(tmpjson1);
    
                                        JSONObject tmpjson2 = new JSONObject();
                                        tmpjson2.put("op1", Integer.toString(op1));
                                        tmpjson2.put("formula", formula);
                                        tmpjson2.put("operator", operator2);
                                        jasinsidefilter.put(tmpjson2);
                                    }
                                }
                            }
    
                        }

                    }


                    jsoninsideja.put("filter", jasinsidefilter);
                    janumfilter.put(jsoninsideja);

                }
                else{
                    //it's a dimension

                    Boolean time_dim = false;
                    Boolean numeric = false;

                    String field = levels_final.get(f).replaceAll("_", " ");

                    try{
                        InputStream modrianXML = new FileInputStream(schema);
                        
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(modrianXML);
                        
                        NodeList ndList = doc.getElementsByTagName("Level");
                
                        
                        for(int z = 0; z < ndList.getLength(); z++){
                            Node nd = ndList.item(z);
                
                            if (nd.getNodeType() == Node.ELEMENT_NODE){
                                Element eElement = (Element) nd;
                                
                                if(eElement.getAttribute("name").equals(field)){
                                    if(eElement.getAttribute("levelType").startsWith("Time")){
                                        time_dim = true;
                                    }
                                    if(eElement.getAttribute("type").equals("Numeric")){
                                        numeric = true;
                                    }
                                    break;
                                }
                                
                
                            }
                
                        }
                
                    }
                    catch(ParserConfigurationException e){
                        e.printStackTrace();
                    }
                    catch(SAXException saxe){
                        saxe.printStackTrace();
                    }
                    catch(IOException ioe){
                        ioe.printStackTrace();
                    }


                    //it's a filter on a time dimension
                    if(time_dim){

                        JSONObject jsoninsideja = new JSONObject();

                        String dim = findParentDim(levels_final.get(f));

                        //The dim is now set in the json
                        jsoninsideja.put("dim", dim);
                        
                        //--------------------------------------------------------
                        //--------------------------------------------------------

                        JSONArray jasinsidefilter = new JSONArray();

                        switch(f){
                            case "year":
                                //going to iterate over the span again to find the operators
                                int j = filter_final.get(f).getStart();
                                int k = filter_final.get(f).getEnd();

                                String operator1 = "";

                                for( ; j < k; j++){

                                    if(stem_query.get(j).equals("ago") || stem_query.get(j).equals("previous") || stem_query.get(j).equals("last")
                                    || stem_query.get(j).equals("between") || stem_query.get(j).equals("equal")){
                                        
                                        operator1 = timeopdictionary.get(stem_query.get(j));
                                        
                                    }
                                }


                                if(operator1.equals("TIME_AGO") || operator1.equals("TIME_RANGE_PREV")){

                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    Integer op1 = null;

                                    for( ; j < k; j++){
                                        if(tags[j].equals("CD")){
                                            op1 = Integer.parseInt(stem_query.get(j));                                                                    
                                        }
                                    }

                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", operator1);
                                    JSONArray tmpja1 = new JSONArray();
                                    tmpja1.put(op1);
                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);
                                    if(op1 == null || tmpja1.length() == 0){
                                        break;
                                    }
                                }

                                if(operator1.equals("") || operator1.equals("CONTAIN")){
                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    String op1 = "";
                                    
                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", "CONTAIN");
                                    JSONArray tmpja1 = new JSONArray();
                                    
                                    for( ; j < k; j++){
                                        if(tags[j].equals("CD")){
                                            op1 = stem_query.get(j);
                                            tmpja1.put(op1);
                                        }
                                    }

                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);
                                    if(op1 == null || tmpja1.length() == 0){
                                        break;
                                    }
                                }


                                if(operator1.equals("BETWEEN")){
                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    Integer op1 = null;

                                    Integer op2 = null;
                                    
                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", "CONTAIN");
                                    JSONArray tmpja1 = new JSONArray();
                                    
                                    for( ; j < k; j++){
                                        if(tags[j].equals("CD")){
                                            if(op1 == null){
                                                op1 = Integer.parseInt(stem_query.get(j));
                                            }
                                            else{
                                                op2 = Integer.parseInt(stem_query.get(j));
                                            }
                                        }
                                    }

                                    if(op1 != null && op2 != null){
                                        if(op2 <= op1){
                                            Integer tmp = op2;
                                            op2 = op1;
                                            op1 = tmp;
                                        }

                                        while(op1 != (op2 + 1)){
                                            tmpja1.put(String.valueOf(op1));
                                            op1++;
                                        }
                                    }

                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);
                                    if(tmpja1.length() == 0){
                                        break;
                                    }
                                }


                                jsoninsideja.put("filter", jasinsidefilter);
                                jafilter.put(jsoninsideja);

                                break;
                            case "quarter":

                                //going to iterate over the span again to find the operators
                                j = filter_final.get(f).getStart();
                                k = filter_final.get(f).getEnd();

                                operator1 = "";

                                for( ; j < k; j++){

                                    if(stem_query.get(j).equals("ago") || stem_query.get(j).equals("previous") || stem_query.get(j).equals("last")
                                    || stem_query.get(j).equals("between") || stem_query.get(j).equals("equal")){
                                        
                                        operator1 = timeopdictionary.get(stem_query.get(j));
                                        
                                    }
                                }


                                if(operator1.equals("TIME_AGO") || operator1.equals("TIME_RANGE_PREV")){

                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    Integer op1 = null;

                                    for( ; j < k; j++){
                                        if(tags[j].equals("CD")){
                                            op1 = Integer.parseInt(stem_query.get(j));                                                                    
                                        }
                                    }

                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", operator1);
                                    JSONArray tmpja1 = new JSONArray();
                                    tmpja1.put(op1);
                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);
                                    if(op1 == null || tmpja1.length() == 0){
                                        break;
                                    }
                                }


                                if(operator1.equals("") || operator1.equals("CONTAIN")){
                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    String op1 = "";
                                    
                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", "CONTAIN");
                                    JSONArray tmpja1 = new JSONArray();
                                    
                                    for( ; j < k; j++){
                                        if(tags[j].equals("JJ") || tags[j].equals("CD")){
                                            op1 = quartersdictionary.get(stem_query.get(j));
                                            if(op1 != null){
                                                tmpja1.put(op1);
                                            }
                                        }
                                    }

                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);
                                    if(tmpja1.length() == 0){
                                        break;
                                    }
                                }


                                if(operator1.equals("BETWEEN")){
                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    String op1 = "";

                                    String op2 = "";
                                    
                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", "CONTAIN");
                                    JSONArray tmpja1 = new JSONArray();
                                    
                                    for( ; j < k; j++){
                                        if(tags[j].equals("JJ") || tags[j].equals("CD")){
                                            if(op1.equals("")){
                                                if(quartersdictionary.get(stem_query.get(j)) != null){
                                                    op1 = quartersdictionary.get(stem_query.get(j));
                                                }
                                            }
                                            else{
                                                if(quartersdictionary.get(stem_query.get(j)) != null){
                                                    op2 = quartersdictionary.get(stem_query.get(j));
                                                }
                                            }
                                        }
                                    }

                                    if(!op1.equals("") && !op2.equals("")){

                                        Boolean insert = false;

                                        for(Map.Entry<String, String> entry :  quartersdictionary.entrySet()){
                                            if(entry.getValue().equals(op1) || insert){
                                                tmpja1.put(entry.getValue());
                                                insert = true;
                                            }
                                            if(entry.getValue().equals(op2)){
                                                break;
                                            }
                                        }
    
                                    }

                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);
                                    if(tmpja1.length() == 0){
                                        break;
                                    }
                                }



                                jsoninsideja.put("filter", jasinsidefilter);
                                jafilter.put(jsoninsideja);
                                    

                                break;
                            case "month":

                                //going to iterate over the span again to find the operators
                                j = filter_final.get(f).getStart();
                                k = filter_final.get(f).getEnd();

                                operator1 = "";

                                for( ; j < k; j++){

                                    if(stem_query.get(j).equals("ago") || stem_query.get(j).equals("previous") || stem_query.get(j).equals("last")
                                    || stem_query.get(j).equals("between") || stem_query.get(j).equals("equal")){
                                        
                                        operator1 = timeopdictionary.get(stem_query.get(j));
                                        
                                    }
                                }


                                if(operator1.equals("TIME_AGO") || operator1.equals("TIME_RANGE_PREV")){

                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    Integer op1 = null;

                                    for( ; j < k; j++){
                                        if(tags[j].equals("CD")){
                                            op1 = Integer.parseInt(stem_query.get(j));                                                                    
                                        }
                                    }

                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", operator1);
                                    JSONArray tmpja1 = new JSONArray();
                                    tmpja1.put(op1);
                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);    
                                    if(op1 == null || tmpja1.length() == 0){
                                        break;
                                    }
                                }


                                if(operator1.equals("") || operator1.equals("CONTAIN")){
                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    String op1 = "";
                                    
                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", "CONTAIN");
                                    JSONArray tmpja1 = new JSONArray();
                                    
                                    for( ; j < k; j++){
                                        if(tags[j].equals("NNP") || tags[j].equals("NNPS")){
                                            op1 = monthsdictionary.get(stem_query.get(j));
                                            if(op1 != null){
                                                tmpja1.put(op1);
                                            }
                                        }
                                    }

                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);
                                    if(tmpja1.length() == 0){
                                        break;
                                    }
                                }


                                if(operator1.equals("BETWEEN")){
                                    //going to iterate over the span again to find the operators
                                    j = filter_final.get(f).getStart();
                                    k = filter_final.get(f).getEnd();

                                    String op1 = "";

                                    String op2 = "";
                                    
                                    JSONObject tmpjson1 = new JSONObject();
                                    tmpjson1.put("operator", "CONTAIN");
                                    JSONArray tmpja1 = new JSONArray();
                                    
                                    for( ; j < k; j++){
                                        if(tags[j].equals("NNP") || tags[j].equals("NNPS")){
                                            if(op1.equals("")){
                                                if(monthsdictionary.get(stem_query.get(j)) != null){
                                                    op1 = monthsdictionary.get(stem_query.get(j));
                                                }
                                            }
                                            else{
                                                if(monthsdictionary.get(stem_query.get(j)) != null){
                                                    op2 = monthsdictionary.get(stem_query.get(j));
                                                }
                                            }
                                        }
                                    }

                                    if(!op1.equals("") && !op2.equals("")){

                                        Boolean insert = false;

                                        for(Map.Entry<String, String> entry :  monthsdictionary.entrySet()){
                                            if(entry.getValue().equals(op1) || insert){
                                                tmpja1.put(entry.getValue());
                                                insert = true;
                                            }
                                            if(entry.getValue().equals(op2)){
                                                break;
                                            }
                                        }
    
                                    }

                                    tmpjson1.put("members",tmpja1);
                                    jasinsidefilter.put(tmpjson1);

                                    if(tmpja1.length() == 0){
                                        break;
                                    }
                                }

                                
                                jsoninsideja.put("filter", jasinsidefilter);
                                jafilter.put(jsoninsideja);

                                break;
                        }

                    }
                    else{
                        //it's a filter on numeric dimension
                        if(numeric){
                            JSONObject jsoninsideja = new JSONObject();

                            String dim = findParentDim(levels_final.get(f));

                            //The dim is now set in the json
                            jsoninsideja.put("dim", dim);

                            //--------------------------------------------------------
                            //--------------------------------------------------------

                            JSONArray jasinsidefilter = new JSONArray();


                            //going to iterate over the span again to find the operators
                            int j = filter_final.get(f).getStart();
                            int k = filter_final.get(f).getEnd();

                            String operator1 = "";
                            String operator2 = "";

                            for( ; j < k; j++){

                                if(stem_query.get(j).equals("greater") || stem_query.get(j).equals("less") || stem_query.get(j).equals("between")
                                || stem_query.get(j).equals("equal") || stem_query.get(j).equals("bigger") || stem_query.get(j).equals("lower")){
                                    if(!operator1.equals("")){
                                        operator2 = numopdictionary.get(stem_query.get(j));
                                    }
                                    else{
                                        operator1 = numopdictionary.get(stem_query.get(j));
                                    }
                                }
                            }

                            if(!operator1.equals("") && !operator2.equals("")){
                                //we have a "greater" and "less" filter

                                //going to iterate over the span again to find the operators
                                j = filter_final.get(f).getStart();
                                k = filter_final.get(f).getEnd();

                                Integer op1 = null;

                                Integer op2 = null;

                                for( ; j < k; j++){
                                    if(tags[j].equals("CD")){

                                        if(op1 != null){
                                            op2 = Integer.parseInt(stem_query.get(j));
                                        }
                                        else{
                                            op1 = Integer.parseInt(stem_query.get(j));
                                        }
            
                                    }
                                }


                                if(op1 <= op2){
                                    if(operator1.equals("NUMERIC_GREATER_THAN_EQUAL")){
                                        JSONObject tmpjson1 = new JSONObject();
                                        tmpjson1.put("op1", op1);
                                        tmpjson1.put("operator", operator1);
                                        jasinsidefilter.put(tmpjson1);
    
                                        JSONObject tmpjson2 = new JSONObject();
                                        tmpjson2.put("op1", op2);
                                        tmpjson2.put("operator", operator2);
                                        jasinsidefilter.put(tmpjson2);
    
                                    }
                                    else{
    
                                        JSONObject tmpjson1 = new JSONObject();
                                        tmpjson1.put("op1", op1);
                                        tmpjson1.put("operator", operator2);
                                        jasinsidefilter.put(tmpjson1);
    
                                        JSONObject tmpjson2 = new JSONObject();
                                        tmpjson2.put("op1", op2);
                                        tmpjson2.put("operator", operator1);
                                        jasinsidefilter.put(tmpjson2);
    
                                    }
    
                                }
                                else{
    
                                    if(operator1.equals("NUMERIC_LESS_THAN_EQUAL")){
    
                                        JSONObject tmpjson1 = new JSONObject();
                                        tmpjson1.put("op1", op2);
                                        tmpjson1.put("operator", operator2);
                                        jasinsidefilter.put(tmpjson1);
    
                                        JSONObject tmpjson2 = new JSONObject();
                                        tmpjson2.put("op1", op1);
                                        tmpjson2.put("operator", operator1);
                                        jasinsidefilter.put(tmpjson2);
    
                                    }
                                    else{
                                        JSONObject tmpjson1 = new JSONObject();
                                        tmpjson1.put("op1", op2);
                                        tmpjson1.put("operator", operator1);
                                        jasinsidefilter.put(tmpjson1);
    
                                        JSONObject tmpjson2 = new JSONObject();
                                        tmpjson2.put("op1", op1);
                                        tmpjson2.put("operator", operator2);
                                        jasinsidefilter.put(tmpjson2);
                                    }
                                }

                                jsoninsideja.put("filter", jasinsidefilter);
                                jafilter.put(jsoninsideja);

                            }
                            else{
                                //we only have one filter i.e one operator

                                //just checking, if no operators nothing is added
                                if(!operator1.equals("")){
                                    if(operator1.equals("NUMERIC_BETWEEN")){

                                        //going to iterate over the span again to find the operators
                                        j = filter_final.get(f).getStart();
                                        k = filter_final.get(f).getEnd();

                                        Integer op1 = null;

                                        Integer op2 = null;

                                        for( ; j < k; j++){
                                            if(tags[j].equals("CD")){

                                                if(op1 != null){
                                                    op2 = Integer.parseInt(stem_query.get(j));
                                                }
                                                else{
                                                    op1 = Integer.parseInt(stem_query.get(j));
                                                }
                    
                                            }
                                        }

                                        if(op1 <= op2){
                                            JSONObject tmpjson1 = new JSONObject();
                                            tmpjson1.put("op1", op1);
                                            tmpjson1.put("op2", op2);
                                            tmpjson1.put("operator", operator1);
                                            jasinsidefilter.put(tmpjson1);
                                        }
                                        else{
                                            JSONObject tmpjson1 = new JSONObject();
                                            tmpjson1.put("op1", op2);
                                            tmpjson1.put("op2", op1);
                                            tmpjson1.put("operator", operator1);
                                            jasinsidefilter.put(tmpjson1);
                                        }

                                    }
                                    else{
                                        if(operator1.equals("EQUAL")){

                                            JSONObject jsoninsidev2 = new JSONObject();

                                            jsoninsidev2.put("operator", operator1);

                                            //-------------------------------------------------
                                            //-------------------------------------------------

                                            JSONArray jasinsidev2 = new JSONArray();

                                            j = filter_final.get(f).getStart();
                                            k = filter_final.get(f).getEnd();

                                            Integer op1 = null;

                                            for( ; j < k; j++){
                                                if(tags[j].equals("CD")){

                                                    op1 = Integer.parseInt(stem_query.get(j));

                                                    JSONObject jsontmp1 = new JSONObject();
                                                    jsontmp1.put("formula", "[" + levels_final.get(f).replaceAll("_", " ") + "]." + "[" + op1 + "]");
                                                    jsontmp1.put("caption", Integer.toString(op1));

                                                    jasinsidev2.put(jsontmp1);

                                                }
                                            }

                                            jsoninsidev2.put("members", jasinsidev2);

                                            jasinsidefilter.put(jsoninsidev2);

                                        }
                                        else{

                                            //going to iterate over the span again to find the operators
                                            j = filter_final.get(f).getStart();
                                            k = filter_final.get(f).getEnd();

                                            Integer op1 = null;

                                            for( ; j < k; j++){
                                                if(tags[j].equals("CD")){
                                                    op1 = Integer.parseInt(stem_query.get(j));
                                                }
                                            }

                                            //it's a "greater" or "less" operator
                                            JSONObject tmpjson1 = new JSONObject();
                                            tmpjson1.put("op1", op1);
                                            tmpjson1.put("operator", operator1);
                                            jasinsidefilter.put(tmpjson1);
                                        }
                                    }

                                    jsoninsideja.put("filter", jasinsidefilter);
                                    jafilter.put(jsoninsideja);

                                }
                            }


                        }
                        //it's a filter a non-numeric dimension (string, boolean, etc)
                        else{
                            JSONObject jsoninsideja = new JSONObject();

                            String dim = findParentDim(levels_final.get(f));

                            //The dim is now set in the json
                            jsoninsideja.put("dim", dim);

                            //--------------------------------------------------------
                            //--------------------------------------------------------

                            JSONArray jasinsidefilter = new JSONArray();

                            JSONObject jsoninsidejav2 = new JSONObject();

                            jsoninsidejav2.put("operator", "EQUAL");

                            JSONArray jasinsidefilterv2 = new JSONArray();

                            int j = filter_final.get(f).getStart();
                            int k = filter_final.get(f).getEnd();

                            for( ; j < k; j++){
                                if(tags[j].equals("NNP") || tags[j].equals("NNPS")){

                                    String subfield = tokens[j];

                                    int aux = j+1;

                                    for( ; aux < k; aux++){
                                        if(tags[aux].equals("NNP") || tags[aux].equals("NNPS")){
                                            subfield = subfield + " " + tokens[aux];
                                            j = aux;
                                        }
                                        else{
                                            break;
                                        }

                                    }

                                    JSONObject jsontmp = new JSONObject();
                                    jsontmp.put("formula", "[" + field +"]." + "[" + subfield + "]");
                                    jsontmp.put("caption", subfield);
                                    jasinsidefilterv2.put(jsontmp);
                                }
                            }

                            jsoninsidejav2.put("members", jasinsidefilterv2);

                            jasinsidefilter.put(jsoninsidejav2);

                            jsoninsideja.put("filter", jasinsidefilter);

                            jafilter.put(jsoninsideja);


                        }
                    }    
                }
            }

            json.put("numfilters", janumfilter);
            
            json.put("filters", jafilter);

        }




        return json;
        
    }
}
