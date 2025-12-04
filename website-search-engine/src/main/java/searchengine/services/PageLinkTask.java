//package searchengine.services;

import lombok.extern.log4j.Log4j2;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;

//@Log4j2
//public class PageLinkTask extends RecursiveAction {

 //   private static final Set<String> allLinks = new TreeSet<>();
   // private final String URL;

   // public PageLinkTask(String URL) {
   //     this.URL = URL;
   // }

    //@Override
   // protected void compute() {

     //   if (!allLinks.contains(URL)) {

       //     allLinks.add(URL);

         //   try {
 //               Document document = GetWebDocument.getWeb(URL);

 //               Elements linksOnPage = document.select("a[href]");

 //               for (Element page : linksOnPage) {

   //                 String linkHref = page.attr("abs:href");

     //               if (linkHref.startsWith(URL) && !linkHref.contains("#") && !linkHref.equals(URL)) {

       //                 allLinks.add(linkHref);

         //               new PageLinkTask(linkHref).fork();
           //         }
             //   }
      //      } catch (Exception e) {
        //        log.error("Connection error to URL: " + URL, e);
          //  }
       // }
   // }


   // public Set<String> getAllLinks() {
     //   return allLinks;
   // }
//}
