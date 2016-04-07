package spambot;


import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Crawler {
    
    private static final int MAX_PAGES_TO_SEARCH = 2000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

    private Set<String> pagesVisited = new HashSet<String>();
    private Set<String> pagesToVisit = new LinkedHashSet<String>();
    private Set<String> emails = new LinkedHashSet<String>();
    
    private Object lock1 = new Object();
    private Object lock2 = new Object();
    //private Object email_lock = new Object();
    
    private Parser parser = new Parser();
              
      
    
    public Crawler()
    {
    }
    
    private String nextUrl()
    {
        try
        {
            String nextUrl;
            Iterator<String> iterator = this.pagesToVisit.iterator(); 
            do
            {
                if (!iterator.hasNext())
                    return null;
                
                nextUrl = iterator.next();
                //synchronized (lock1)
                //{
                iterator.remove();
                //}
                
            } while (this.pagesVisited.contains(nextUrl));

            //synchronized (lock2)
            //{
            this.pagesVisited.add(nextUrl);
            //}
            return nextUrl;
        } 
        catch (IllegalStateException ex)
        {
            System.out.println("Error in iterator: " + ex);
        }
        
        return null;
    }
    
    
    private String cleanLink(String s, String baseUrl)
    {

        if (!s.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))
        {
            return null;
        }
        
        
        //If remove external urls
        if (baseUrl != null && !s.contains(baseUrl))
        {
            return null;
        }
        
        
        //Remove last character '/'
        if (s.length() > 0)
        {
            if (s.charAt(s.length() - 1) == '/')
            {

                s = s.substring(0, s.length() - 1);

            }
        }
        
        //Remove all after '#'
        for (int i = 0; i < s.length(); i++)
        {
            if (s.charAt(i) == '#')
            {
                s = s.substring(0, i);
                return s;
            }
        }
        
        return s;
       
    }
    
    
    private void toVisitPrint()
    {
        System.out.println();
        System.out.println("To visit: ");
        
        Iterator<String> iterator = this.pagesToVisit.iterator();
        while (iterator.hasNext())
        {
            System.out.println(iterator.next());
        }
    }
    
    private void visitedPrint()
    {
        System.out.println();
        System.out.println("Visited: ");
        
        Iterator<String> iteratorVisited = this.pagesVisited.iterator();
        while (iteratorVisited.hasNext())
        {
            System.out.println(iteratorVisited.next());
        }
    }
    
    
    private void emailPrint(Set<String> emails)
    {
        System.out.println();
        System.out.println("Emails: ");
        
        Iterator<String> iterator = emails.iterator();
        while (iterator.hasNext())
        {
            System.out.println(iterator.next());
        }
    }
    
    private AtomicInteger page = new AtomicInteger();
    
    public void crawl(String _url, int threads)
    {
        this.pagesToVisit.add(_url);

        //int j = 0;
                
        Runnable myRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                
                String url;
                page.set(0);

                
                //Wait all threads except first for page urls
                if (!Thread.currentThread().getName().equals("0"))
                {
                    while (pagesToVisit.size() < threads * 2)
                    {
                        try
                        {
                            Thread.sleep(500);
                        }
                        catch (InterruptedException ex)
                        {
                        } 
                    }
                }
                
                
                while (!pagesToVisit.isEmpty() && page.get() < MAX_PAGES_TO_SEARCH)
                {
                    page.incrementAndGet();
               
                    synchronized (lock1)
                    {
                        url = nextUrl();
                    }

                    if (url == null) break;

                    System.out.println("Thread " + Thread.currentThread().getName() + " - Page " + page.get() + ": " + url);


                    try
                    {
                        Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);
                        Document htmlDocument = connection.get();


                        //Get emails
                        parser.Parse(htmlDocument.html());
                        //parser.Parse("Hello REDA+SEO@gmail.Com also Rere-r7_7@mail.ru. and what is your main email address reda@sdsds also want to tell you. Is it your email dwarf777@mail. or it is not yours and this is yours ivan.pupkin@mail.com or maybe ivan@ivan.company.com your email or another option peter@ivan.company.");


                        Elements linksOnPage = htmlDocument.select("a[href]");
                        String s;
                        for (Element link : linksOnPage)
                        {
                            //"tuneair.ru"
                            s = cleanLink(link.absUrl("href"), null);

                            if (s != null)
                            {
                                synchronized (lock2)
                                {
                                    pagesToVisit.add(s);
                                }
                            }
                        }
                    }
                    catch(IOException ex)
                    {
                        System.out.println("Error in HTTP request: " + ex);
                    }
                }
/*
                toVisitPrint();
                visitedPrint();

                System.out.println("Visited: " + pagesVisited.size());
                System.out.println("Not visited: " + pagesToVisit.size());

                emailPrint(parser.getEmails());
                */
                
                /*synchronized (email_lock)
                {
                    emails.addAll(parser.getEmails());
                }
                */
                
                System.out.println("Thread finished.");
                

            }
        };

        
        //Create threads
        Thread thread[] = new Thread[threads];//(myRunnable);
        for (int i = 0; i < threads; i++) 
        {
            thread[i] = new Thread(myRunnable);
            thread[i].setName(Integer.toString(i));
            thread[i].start();
        }
        
        
        //Wait for threads
        try
        {
            for (int i = 0; i < threads; i++) 
            {
                thread[i].join();
            }
        }
        catch (InterruptedException ex)
        {
        }
        
        
        //Print parsed email
        emailPrint(parser.getEmails());
        
        
        System.out.println("Program finished.");
        
    }
    
}
