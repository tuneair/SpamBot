/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spambot;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
//import ;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author owner
 */
public class GateController 
{

    private static boolean isGateInitilised = false;
    private static SerialAnalyserController annotationPipeline = null;
    
    
    public GateController ()
    {
        if (!isGateInitilised)
        {
            org.apache.log4j.BasicConfigurator.configure();
            this.initialiseGate();
        }
        
        try
        {
            this.prepareFilters();
        }
        catch (GateException ex) 
        {
            System.out.println("Error: " + ex.toString());
        } 
    }
    
    
    private void initialiseGate() 
    {
        try 
        {
            Gate.setGateHome(new File("C:\\gate"));
        }
        catch (IllegalStateException ex)
        {
        }

        File pluginsHome = new File("C:\\gate\\plugins");
        try 
        {
            Gate.setPluginsHome(pluginsHome);            
        }
        catch (IllegalStateException ex)
        {
        }
            
            
        try 
        {
            Gate.setUserConfigFile(new File("C:\\gate", "user.xml"));            
            //Gate.setUserSessionFile(new File("C:\\gate", "gate.session"));
        }
        catch (IllegalStateException ex)
        {
        }
            
        
        try 
        {    
            // initialise the GATE library
            Gate.init();
    
            // load ANNIE plugin
            CreoleRegister register = Gate.getCreoleRegister();
            
            URL annieHome = new File(pluginsHome, "ANNIE").toURL();
            register.registerDirectories(annieHome);
            
            // flag that GATE was successfuly initialised
            isGateInitilised = true;
        } 
        catch (MalformedURLException ex) 
        {
            //Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GateException ex) 
        {
            //Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    
    private void prepareFilters() throws ResourceInstantiationException
    {
         
            ProcessingResource documentResetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");

            //ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter");
            //ProcessingResource gazeet = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");

            // locate the JAPE grammar file
            File japeFile = new File("email.jape");
         
            // create feature map for the transducer
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try 
            {
                transducerFeatureMap.put("grammarURL", japeFile.toURI().toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } 
            catch (MalformedURLException e) 
            {
                System.out.println("Malformed URL of JAPE grammar");
                System.out.println(e.toString());
            }

            // create an instance of a JAPE Transducer processing resource
            ProcessingResource japeTransducerPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);
    
            // create corpus pipeline
            annotationPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");
            annotationPipeline.add(documentResetPR);
            annotationPipeline.add(tokenizerPR);
            annotationPipeline.add(japeTransducerPR);

    }
    
    
    private Corpus createCorpus(String html) throws ResourceInstantiationException, MalformedURLException
    {
        // create a document
        //Document document = Factory.newDocument(new URL(url), "UTF-8");
        Document document = Factory.newDocument(html);
        // create a corpus and add the document
        Corpus corpus = Factory.newCorpus("");
        corpus.add(document);
        return corpus;
    }
        
        
    public Set<String> getEmails(String html)
    {
        Set<String> emails = new LinkedHashSet<String>();

        try
        {
            Corpus corpus = createCorpus(html);
            
            annotationPipeline.setCorpus(corpus);
            annotationPipeline.execute();

            // loop through the documents in the corpus
            for (int i = 0; i < corpus.size(); i++)
            {

                Document doc = corpus.get(i);

                // get the default annotation set
                AnnotationSet as_default = doc.getAnnotations();

                FeatureMap futureMap = null;
                // get all Token annotations
                AnnotationSet annSetTokens = as_default.get("EMail", futureMap);
                //System.out.println("Number of found emails: " + annSetTokens.size());


                ArrayList tokenAnnotations = new ArrayList(annSetTokens);

                // looop through the Token annotations
                for(int j = 0; j < tokenAnnotations.size(); ++j) 
                {
                    // get a token annotation
                    Annotation token = (Annotation)tokenAnnotations.get(j);

                    // get the underlying string for the Token
                    Node isaStart = token.getStartNode();
                    Node isaEnd = token.getEndNode();
                    String email = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    emails.add(email);
                    
                    //System.out.println("Email " + (j + 1) + ": " + email);
                }

            }
        
        } 
        catch (MalformedURLException ex) 
        {
            System.out.println("Error: " + ex.toString());
            
            //Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (GateException ex) 
        {
            System.out.println("Error: " + ex.toString());

            //Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        } 
            
        
        return emails;
    }
    
    
}
