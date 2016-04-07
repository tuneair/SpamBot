package spambot;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;


public class Parser 
{
    
    private Set<String> emails = new LinkedHashSet<String>();
    private GateController gate = new GateController(); 
    
    private Object email_lock = new Object();
    
    
    public Parser()
    {   
    }
    
    
    public Set<String> getEmails()
    {
        return this.emails;
    }
    
    
    private void saveEmails(Set<String> toSave)
    {
        Iterator<String> iterator = toSave.iterator();
        while (iterator.hasNext())
        {
            this.emails.add(iterator.next());
        }
    }
   
    
    public void Parse(String url)
    {
        synchronized (email_lock)
        {
            this.saveEmails(gate.getEmails(url));
        }
    }
    
    
    public void clearEmails()
    {
        synchronized (email_lock)
        {
            this.emails.clear();
        }
    }
    
    
    public int getEmailsSize()
    {
        return this.emails.size();
    }
    
    
}
