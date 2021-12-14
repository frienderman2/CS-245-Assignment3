// Callaghan Donnelly
// 12/13/2021
// Program to take all the last 3 decades of top 40 hits (per week) and then get reccomendations based off of collaborations
// biggest speed improvement would be multi threading the Jsoup calls and the list parsing becasue it is unbearably slow

import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.*;

import java.io.IOException;

// In 2012 there is a song Clique, but JayZ, Kanye, and Big Sean that has no delimiter or markers for separate artists, not really feasible to parse
// The largest pervasive problem is that the data is so poorly formatted (on the website's end) for rappers and hip hop artists, that many of those will have issues

public class Assignmnet3 {

    public Assignmnet3() throws IOException {
    }

    public ArrayList<String> getArtists() throws IOException {
        ArrayList<String> allArtists = new ArrayList<>();
        String url = "http://top40weekly.com/";
        //String[] sub_urls = {"1990", "1991", "1992", "1993", "1994", "1995", "1996", "1997", "1998", "1999", "2000", "2001", "2002", "2003", "2004", "2005", "2006", "2007", "2008", "2009", "2010", "2011", "2012", "2013", "2014", "2015", "2016", "2017", "2018", "2019"};
        String[] sub_urls = {"1990", "2000", "2010", "2019"};
        for(String part: sub_urls){
            url = "http://top40weekly.com/" + part + "-all-charts";
            Document doc = Jsoup.connect(url).get();
            String page_content = doc.outerHtml();
            BufferedReader reader = new BufferedReader(new StringReader(page_content));
            ArrayList<String> rawHtml = new ArrayList<>();
            String hold = reader.readLine();
            while (hold != null){
                rawHtml.add(hold);
                hold = reader.readLine();
            }

            // setup the regex to carve out just the songs from the html
            String pattern = "#W[p]#W#d";
            String newPat = pattern.replace('#', '\\');
            Pattern finPat = Pattern.compile(newPat);

            ArrayList<String> cleanHtml = new ArrayList<>();
            // if the regex pattern is in that line, then add it to the list of lines with music in it and keep looking
            for(String line: rawHtml){
                Matcher match = finPat.matcher(line);
                if (match.find()){
                    cleanHtml.add(line);
                }
            }

            ArrayList<String> cleanLines = new ArrayList<>();
            for(String item: cleanHtml){
                // remove the annoying html markers and reformats lines so that they each have exactly one song per line
                String newInd = item.replace("<br>", "\n");
                String cleaned = newInd.replace("</p>", "\n");
                String finished = cleaned.replace("<p>", " ");
                // replace all the - with a ( so ( can be used for lookup later instead of - or (
                String clipped = finished.replace("-", "(");
                String[] holder = clipped.split("\n");
                // get the new lines and shift them into the arraylist for the next step
                for(String subline: holder){
                    cleanLines.add(subline);
                }
            }

            ArrayList<String> artistNames = new ArrayList<>();
            // parse just the names out of the lines of song info
            for(String line: cleanLines){
                // look at the bits that appear in every line before the artist's name
                int indexStart = line.indexOf('•');
                // TODO: replace - with (, then loop while the index value returned by the ( is greater than the index of the dot
                int indexEnd = line.indexOf('(');
                // if it's an empty line then skip it and go to the next
                if (indexStart == -1 && indexEnd == -1){
                    continue;
                }
                // remove the ugly extra markers at the start
                indexStart = indexStart + 2;
                // make sure it didn't encounter an earlier '(' in the string
                while(indexEnd < indexStart){
                    indexEnd = line.indexOf('(', indexEnd + 1);
                }
                // grab the name I want and add it to the array list
                String artistName = line.substring(indexStart, indexEnd);
                artistNames.add(artistName);
            }

            ArrayList<String> finList = new ArrayList<>();
            // trim off all of the leading and trailing whitespace that may be on the names
            for(String name: artistNames){
                String finName = name.trim();
                finList.add(finName);
            }

            // add each artist from this decade to the total list of artists
            allArtists.addAll(finList);
        }
        return allArtists;
    }


    // parse all of the names and features into a hashtable with LinkedLists (of the people they collaborated with) as the values
    public Hashtable<String, LinkedList<String>> parseToHashTable(ArrayList<String> artistList){
        // hashtable to represent all artists (with the LL of who is associated to them)
        // using a hashtable in case it becomes necessary to multithread later due to speed issues
        Hashtable<String, LinkedList<String>> lookUpTable = new Hashtable<>();
        for(String name: artistList){
            // there are 4 special conditions:
            // 1 - the ampersand for featuring
            // 2 - the ampersand for Mumford & Sons (my favorite band)
            // 3 - featuring
            // 4 - Kanye West...seems to make trouble wherever he goes, and never has a marker when he is collaborating
            if(name.contains("&amp;") || name.contains("featuring") || name.contains("Featuring")){
                // the only possible top 100 collaboration they had was The Boxer (on Babel) but I don't think that broke top 150
                if (name.equals("Mumford &amp; Sons")){
                    if (!lookUpTable.containsKey(name)){
                        lookUpTable.put(name, new LinkedList<String>());
                    }
                }

                // some have both
                else if (name.contains("&amp;") && name.contains("featuring")){
                    // do the first split
                    String[] hold = name.split("&amp;");
                    // create a temp list to get all 3 names (with whitespace) into
                    ArrayList<String> temp = new ArrayList<>();
                    for (String toParse: hold){
                        // if the item has the other 2 names, then separate them and dump them both into the temp list
                        if (toParse.contains("featuring")){
                            String[] innerList = name.split("featuring");
                            temp.addAll(Arrays.asList(innerList));
                        }
                        else {
                            // if it is the single name, add it alone to the temp list
                            temp.add(toParse);
                        }
                    }

                    // the array list that will be used to store the actual names and indicies to be retrieved
                    ArrayList<String> store = new ArrayList<>();
                    // remove whitespace and place into clean list
                    for (String names: temp){
                        String cleaned = names.strip();
                        store.add(cleaned);
                    }

                    // if the person isn't already in the table then add them to it and add the two collabs to their LL
                    for (String person: store) {
                        if (!lookUpTable.containsKey(person)) {
                            // rappers are an edge case and are breaking things
                            try{
                                if (person.equals(store.get(0))){
                                    lookUpTable.put(person, new LinkedList<String>(Arrays.asList(store.get(1), store.get(2))));
                                }
                                else if (person.equals(store.get(1))){
                                    lookUpTable.put(person, new LinkedList<String>(Arrays.asList(store.get(0), store.get(2))));
                                }
                                else if (person.equals(store.get(2))){
                                    lookUpTable.put(person, new LinkedList<String>(Arrays.asList(store.get(0), store.get(1))));
                                }
                            }catch (IndexOutOfBoundsException e){
                                String noting = "Do nothing.";
                            }
                        }
                        else {
                            // if the person exists, check if they already have the collab with each one on record (add it if not)
                            if (person.equals(store.get(0))){
                                if (!lookUpTable.get(person).contains(store.get(1))){
                                    LinkedList<String> holdList = lookUpTable.get(person);
                                    holdList.add(store.get(1));
                                    lookUpTable.put(person, holdList);
                                }
                                if (!lookUpTable.get(person).contains(store.get(2))){
                                    LinkedList<String> holdList = lookUpTable.get(person);
                                    holdList.add(store.get(2));
                                    lookUpTable.put(person, holdList);
                                }
                            }
                            else if (person.equals(store.get(1))){
                                if (!lookUpTable.get(person).contains(store.get(0))){
                                    LinkedList<String> holdList = lookUpTable.get(person);
                                    holdList.add(store.get(0));
                                    lookUpTable.put(person, holdList);
                                }
                                // rappers are an edge case and are breaking things
                                try {
                                    if (!lookUpTable.get(person).contains(store.get(2))){
                                        LinkedList<String> holdList = lookUpTable.get(person);
                                        holdList.add(store.get(2));
                                        lookUpTable.put(person, holdList);
                                    }
                                }catch (IndexOutOfBoundsException e){
                                    String nothing = "Do nothing";
                                }
                            }

                            // rappers are and edge case and breaking things
                            try {
                                if (person.equals(store.get(2))) {
                                    if (!lookUpTable.get(person).contains(store.get(0))) {
                                        LinkedList<String> holdList = lookUpTable.get(person);
                                        holdList.add(store.get(0));
                                        lookUpTable.put(person, holdList);
                                    }
                                    if (!lookUpTable.get(person).contains(store.get(1))) {
                                        LinkedList<String> holdList = lookUpTable.get(person);
                                        holdList.add(store.get(1));
                                        lookUpTable.put(person, holdList);
                                    }
                                }
                            } catch (IndexOutOfBoundsException e){
                                String nothing = "Do noting";
                            }
                        }
                    }
                }

                // if it has Kanye
                else if (name.contains("Kanye West")){
                    // check for Kanye in table (add him if need be)
                    // add the Kanye clause (NVMD Prof said this was an edge case I can ignore)
                }

                // if it has just ampersand
                else if (name.contains("&amp;")){
                    // split the two names into their own strings
                    String[] hold = name.split("&amp;");
                    ArrayList<String> store = new ArrayList<>();
                    // remove whitespace
                    for (String names: hold){
                        String cleaned = names.strip();
                        store.add(cleaned);
                    }
                    // FIXME: This implementation is disgusting. There is definitely a better way to do this. But it's 4:30am...
                    // do the needed operations on each name
                    for (String artist: store){
                        // if it's a new artist then create a new entry for them
                        if (!lookUpTable.containsKey(artist)){
                            // grab the value of the other artist
                            String otherArtist = "";
                            for (int i = 0; i < store.size(); i++){
                                if (!store.get(i).equals(artist)){
                                    otherArtist = store.get(i);
                                }
                            }
                            // add the other artist to their linked list
                            lookUpTable.put(artist, new LinkedList<String>(Arrays.asList(otherArtist)));

                        }
                        // if they already exist in the table
                        else {
                            // grab the value of the other artist
                            String otherArtist = "";
                            for (int i = 0; i < store.size(); i++){
                                if (!store.get(i).equals(artist)){
                                    otherArtist = store.get(i);
                                }
                            }
                            // check if they already have this collab archived, and if not, get a copy of their current list, add this collab, and put the new LL back in the Table
                            if (!lookUpTable.get(artist).contains(otherArtist)){
                                LinkedList<String> holdList = lookUpTable.get(artist);
                                holdList.add(otherArtist);
                                lookUpTable.put(artist, holdList);
                            }
                        }
                    }
                }

                // Yes this and the previous condition are identical except for the one word being parsed out. Oh well.
                // it has just featuring
                else if (name.contains("featuring")){
                    // split the two names into their own strings
                    String[] hold = name.split("featuring");
                    ArrayList<String> store = new ArrayList<>();
                    // remove whitespace
                    for (String names: hold){
                        String cleaned = names.strip();
                        store.add(cleaned);
                    }
                    // FIXME: This implementation is disgusting. There is definitely a better way to do this. But it's 4:30am...
                    // do the needed operations on each name
                    for (String artist: store){
                        // if it's a new artist then create a new entry for them
                        if (!lookUpTable.containsKey(artist)){
                            // grab the value of the other artist
                            String otherArtist = "";
                            for (int i = 0; i < store.size(); i++){
                                if (!store.get(i).equals(artist)){
                                    otherArtist = store.get(i);
                                }
                            }
                            // add the other artist to their linked list
                            lookUpTable.put(artist, new LinkedList<String>(Arrays.asList(otherArtist)));

                        }
                        // if they already exist in the table
                        else {
                            // grab the value of the other artist
                            String otherArtist = "";
                            for (int i = 0; i < store.size(); i++){
                                if (!store.get(i).equals(artist)){
                                    otherArtist = store.get(i);
                                }
                            }
                            // check if they already have this collab archived, and if not, get a copy of their current list, add this collab, and put the new LL back in the Table
                            if (!lookUpTable.get(artist).contains(otherArtist)){
                                LinkedList<String> holdList = lookUpTable.get(artist);
                                holdList.add(otherArtist);
                                lookUpTable.put(artist, holdList);
                            }
                        }
                    }
                }

                // there are some years that use uppercase F
                else if (name.contains("Featuring")){
                    // split the two names into their own strings
                    String[] hold = name.split("Featuring");
                    ArrayList<String> store = new ArrayList<>();
                    // remove whitespace
                    for (String names: hold){
                        String cleaned = names.strip();
                        store.add(cleaned);
                    }
                    // FIXME: This implementation is disgusting. There is definitely a better way to do this. But it's 4:30am...
                    // do the needed operations on each name
                    for (String artist: store){
                        // if it's a new artist then create a new entry for them
                        if (!lookUpTable.containsKey(artist)){
                            // grab the value of the other artist
                            String otherArtist = "";
                            for (int i = 0; i < store.size(); i++){
                                if (!store.get(i).equals(artist)){
                                    otherArtist = store.get(i);
                                }
                            }
                            // add the other artist to their linked list
                            lookUpTable.put(artist, new LinkedList<String>(Arrays.asList(otherArtist)));

                        }
                        // if they already exist in the table
                        else {
                            // grab the value of the other artist
                            String otherArtist = "";
                            for (int i = 0; i < store.size(); i++){
                                if (!store.get(i).equals(artist)){
                                    otherArtist = store.get(i);
                                }
                            }
                            // check if they already have this collab archived, and if not, get a copy of their current list, add this collab, and put the new LL back in the Table
                            if (!lookUpTable.get(artist).contains(otherArtist)){
                                LinkedList<String> holdList = lookUpTable.get(artist);
                                holdList.add(otherArtist);
                                lookUpTable.put(artist, holdList);
                            }
                        }
                    }
                }
            }

            else {
                // if the person isn't already in the table then add them to it with an empty LL
                if (!lookUpTable.containsKey(name)){
                    lookUpTable.put(name, new LinkedList<String>());
                }
            }
        }

        return lookUpTable;
    }


    public ArrayList<String> findRecomendations(String artistIn, Hashtable<String, LinkedList<String>> looUpTable){
        // I was gonna use a queue but I can't be bothered to write all that
        ArrayList<String> fakeQue = new ArrayList<>();
        boolean modified = true;
        int count = 0;

        // make sure the artist exists
        if (looUpTable.containsKey(artistIn)){
            fakeQue.addAll(looUpTable.get(artistIn));
            System.out.println("Artist found.");

            // for every artist in the queue (both from the original artist, as well as the many levels of collaborators)
            // while there are changes being made to the queue
            while (modified){
                modified = false;
                String collab = " ";
                // if no changes were made and I've cycled the whole queue, it will sometimes go out of range, so this fixes that
                try{
                    collab = fakeQue.get(count);
                } catch (IndexOutOfBoundsException e){
                    modified = false;
                    break;
                }

                // check the table's entry for this collaborator
                if(looUpTable.containsKey(collab)){
                    // get a list of all their collaborators
                    ArrayList<String> hold = new ArrayList<>(looUpTable.get(collab));
                    // for each of the collaborator's collaborators
                    modified = true;
                    for (String collItem: hold){
                        // if the collaborators are not already in the queue, then add them to the que
                        if (!fakeQue.contains(collItem) && !collItem.equals(artistIn)){
                            fakeQue.add(collItem);
                            modified = true;
                        }
                    }
                }
                // used for indexing in the while loop
                count++;
            }
        }
        return fakeQue;
    }


    public static void main(String[] args) throws IOException {
        Assignmnet3 newSet = new Assignmnet3();
        System.out.println("Retrieving data...");
        ArrayList<String> listOfArtists = newSet.getArtists();
        System.out.println("List of artists acquired.");
        System.out.println("Parsing for lookup...");
        Hashtable<String, LinkedList<String>> lookUpTable = newSet.parseToHashTable(listOfArtists);
        System.out.println("Lookup ready!");
        // take in the user's artist to lookup
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter an artist to search for: ");
        String userArtist = scan.nextLine();
        ArrayList<String> recommendations = newSet.findRecomendations(userArtist, lookUpTable);
        System.out.println("You might also like: ");
        for (String recs: recommendations){
            System.out.println(recs);
        }


    }
}
