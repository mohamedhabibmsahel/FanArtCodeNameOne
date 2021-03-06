/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.myapp.service;

import com.codename1.io.CharArrayReader;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.Resources;
import com.mycompany.myapp.entity.Evenement;
import com.mycompany.myapp.utils.MailSender;
import com.mycompany.myapp.utils.Statics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Ben Gouta Monam
 */
public class ServiceEvents extends Form {
     public ArrayList<Evenement> events;
    
    public static ServiceEvents instance=null;
    public boolean resultOK;
    private ConnectionRequest req;

    public ServiceEvents() {
         req = new ConnectionRequest();
    }

    public static ServiceEvents getInstance() {
        if (instance == null) {
            instance = new ServiceEvents();
        }
        return instance;
    }
    public ArrayList<Evenement> getAllTasks(){
        String url = Statics.BASE_URL+"/geteventsmobile/";
        req.setUrl(url);
        req.setPost(false);
        req.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent evt) {
                events = parseTasks(new String(req.getResponseData()));
                req.removeResponseListener(this);
            }
        });
        NetworkManager.getInstance().addToQueueAndWait(req);
        return events;
    }
    public ArrayList<Evenement> parseTasks(String jsonText){
        try {
            events=new ArrayList<>();
            JSONParser j = new JSONParser();// Instanciation d'un objet JSONParser permettant le parsing du r??sultat json

            /*
                On doit convertir notre r??ponse texte en CharArray ?? fin de
            permettre au JSONParser de la lire et la manipuler d'ou vient 
            l'utilit?? de new CharArrayReader(json.toCharArray())
            
            La m??thode parse json retourne une MAP<String,Object> ou String est 
            la cl?? principale de notre r??sultat.
            Dans notre cas la cl?? principale n'est pas d??finie cela ne veux pas
            dire qu'elle est manquante mais plut??t gard??e ?? la valeur par defaut
            qui est root.
            En fait c'est la cl?? de l'objet qui englobe la totalit?? des objets 
                    c'est la cl?? d??finissant le tableau de t??ches.
            */
            Map<String,Object> tasksListJson = j.parseJSON(new CharArrayReader(jsonText.toCharArray()));
            
              /* Ici on r??cup??re l'objet contenant notre liste dans une liste 
            d'objets json List<MAP<String,Object>> ou chaque Map est une t??che.               
            
            Le format Json impose que l'objet soit d??finit sous forme
            de cl?? valeur avec la valeur elle m??me peut ??tre un objet Json.
            Pour cela on utilise la structure Map comme elle est la structure la
            plus ad??quate en Java pour stocker des couples Key/Value.
            
            Pour le cas d'un tableau (Json Array) contenant plusieurs objets
            sa valeur est une liste d'objets Json, donc une liste de Map
            */
            List<Map<String,Object>> list = (List<Map<String,Object>>)tasksListJson.get("root");
            
            //Parcourir la liste des t??ches Json
            for(Map<String,Object> obj : list){
                //Cr??ation des t??ches et r??cup??ration de leurs donn??es
                Evenement t = new Evenement();
               
                t.setId_evenement((int)Float.parseFloat(obj.get("idEvenement").toString()));
                t.setTitre(obj.get("titre").toString());
                t.setDescription(obj.get("description").toString());
                t.setDate_debut(obj.get("dateDebut").toString());
                t.setDate_Fin(obj.get("dateFin").toString());
                t.setLocall(obj.get("locall").toString());
                t.setNombre_place((int)Float.parseFloat(obj.get("nombrePlace").toString()));
                t.setPrix((int)Float.parseFloat(obj.get("prix").toString()));
                t.setImage(obj.get("image").toString());
               
                //Ajouter la t??che extraite de la r??ponse Json ?? la liste
                events.add(t);
            }
            
            
        } catch (IOException ex) {
            
        }
         /*
            A ce niveau on a pu r??cup??rer une liste des t??ches ?? partir
        de la base de donn??es ?? travers un service web
        
        */
        return events;
    }
    public void addEvent(Evenement evenement) {

        String url = Statics.BASE_URL + "/addeventmobile?titre=" + evenement.getTitre()
                + "&description=" + evenement.getDescription() + "&salle=" +evenement.getLocall()
                + "&datedebut=" + evenement.getDate_debut() + "&datefin=" + evenement.getDate_Fin() + "&nbplace=" +
                evenement.getNombre_place()+"&prix="+evenement.getPrix()+"&image="+evenement.getImage()+"&iduser"+Statics.session;
        req.setUrl(url);

        req.addResponseListener((evt) -> {

            try {
               
                System.out.println("add validated !");
            } catch (Exception ex) {
                
                ex.printStackTrace();

            }

        });
        NetworkManager.getInstance().addToQueueAndWait(req);
    }
    
    public void sendphp(String che) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httppost = new HttpPost("http://localhost:80/img/upload2.php");

        File file = new File(che.substring(6));

        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new FileBody(file, "image/jpeg");
        //System.out.println(cbFile.getFilename());
        mpEntity.addPart("userfile", cbFile);

        httppost.setEntity(mpEntity);
        //System.out.println("executing request " + httppost.getRequestLine());
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();

        //System.out.println(response.getStatusLine());
        if (resEntity != null) {
            System.out.println(EntityUtils.toString(resEntity));
        }
        if (resEntity != null) {
            resEntity.consumeContent();
        }

        httpclient.getConnectionManager().shutdown();
        revalidate();
    }
}
