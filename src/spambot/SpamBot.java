package spambot;


public class SpamBot 
{

    public static void main(String[] args) 
    {
        
        Crawler crawler = new Crawler();
        //crawler.crawl("http://www.ebay.com/", 3);
        crawler.crawl("http://tuneair.ru", 5);
        
    }
    
}
