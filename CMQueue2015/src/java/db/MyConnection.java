/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package db;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;

/**
 *
 * @author prego
 */
public class MyConnection {
    
    
    private DB db;
    private DBCollection coll;
    private DBCollection fileColl;
    
    private MongoClient m;
    
    
    
    public MyConnection(String server, String mydb, String mycollection){
        try {
            m = new MongoClient( server , 27017 );
            db = m.getDB(mydb);
            coll = db.getCollection(mycollection);
            fileColl = db.getCollection("Documentos.files");
        } catch (UnknownHostException ex) {
            System.out.println(ex.toString());
        }
    }

    
    
    
    public void setDBCollection(String coll){
        this.coll = db.getCollection(coll);
    }
    
    public DBCollection getDBCollection(){
        return coll;
    }
    
    public void setDB(String db){
        this.db = m.getDB(db);
    }
    
    public DB getDB(){
        return db;
    }
    
    
    
    
    public GridFS getMyGridFS(){
         GridFS myGridFS = new GridFS(db,"Documentos");
         return myGridFS;
    }
    
    
    
    
    
    public GridFSDBFile getFileByEntidade(String referenciaEntidade, String refDoc){
             
        GridFS gfsDB = getGridFS();
        BasicDBObject query = new BasicDBObject();
        query.put("metadata.referenciaEntidade",referenciaEntidade);
        query.put("metadata.referencia",refDoc);
	GridFSDBFile imageForOutput = gfsDB.findOne(query);
        return imageForOutput;
        
    }
    
    
    
    public GridFSDBFile getFile(String referenciaEntidade, String filename){
             
        GridFS gfsDB = getGridFS();
        BasicDBObject query = new BasicDBObject();
        query.put("metadata.referencia",referenciaEntidade);
        query.put("metadata.nome",filename);
	GridFSDBFile imageForOutput = gfsDB.findOne(query);
        return imageForOutput;
        
    }
    
    
    public GridFSDBFile getFileCoval(String covalID, String filename){
             
        GridFS gfsDB = getGridFS();
        BasicDBObject query = new BasicDBObject();
        query.put("metadata.covalID",covalID);
        query.put("metadata.nome",filename);
	GridFSDBFile imageForOutput = gfsDB.findOne(query);
        return imageForOutput;
        
    }
    
    
    public GridFSDBFile getFileByID(String id){
             
        GridFS gfsDB = getGridFS();
        BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(id));
	GridFSDBFile imageForOutput = gfsDB.findOne(query);
        return imageForOutput;
        
    }
    
    
    public byte[] getFileByID_ArrayBytes(String id){
        try {
            GridFS gfsDB = getGridFS();
            BasicDBObject query = new BasicDBObject();
            query.put("_id", new ObjectId(id));
            GridFSDBFile imageForOutput = gfsDB.findOne(query);
            InputStream is = imageForOutput.getInputStream();
            byte[] doc = IOUtils.toByteArray(is);
            return doc;
        } catch (IOException ex) {
            System.out.println(ex);
            return null;
        }
        
    }
    
    
    
    public List <GridFSDBFile> getAllFilesByEntidade(String referenciaEntidade){
        GridFS gfsDB = getMyGridFS();
        BasicDBObject query = new BasicDBObject();
        query.put("metadata.referenciaEntidade",referenciaEntidade);
	List <GridFSDBFile> imageForOutput = gfsDB.find(query);
        return imageForOutput;
        
    }
    
    
    
    
    
    
    
    public GridFS getGridFS(){
	return new GridFS(db, "collDocuments");        
    }
    
    
    public void closeConn(){
        m.close();
    }
    
    
    
    private String getMyPrivateID(){
        coll = db.getCollection("sequencialIDs");
        DBObject obj = coll.findOne();
        if(obj!=null){
            String auxx = obj.get("number").toString();
            auxx = auxx.substring(5);
            Long d0 = Long.parseLong(auxx);
            long d1 = d0.longValue()+1;
            
            String aux = "";
            
            if(d1 > 1000000000){ aux = "FILE-" + d1; }
            else if(d1 > 100000000){ aux = "FILE-0" + d1; }
            else if(d1 > 10000000){ aux = "FILE-00" + d1; }
            else if(d1 > 1000000){ aux = "FILE-000" + d1; }
            else if(d1 > 100000){ aux = "FILE-0000" + d1; }
            else if(d1 > 10000){ aux = "FILE-00000" + d1; }
            else if(d1 > 1000){ aux = "FILE-000000" + d1; }
            else if(d1 > 100){ aux = "FILE-0000000" + d1; }
            else if(d1 > 10){ aux = "FILE-00000000" + d1; }
            else { aux = "FILE-000000000" + d1; }
            
            coll.update(obj, new BasicDBObject("number",aux));
                    
            return aux;        
            
        }else{
            coll.insert(new BasicDBObject("number","FILE-0000000000"));
            return "FILE-0000000000";
        }

    }
    
    
    
    
    public String deleteMyFileByID(String id, String user){
        GridFS gfs0 = new GridFS(db, "collDocuments");
        GridFSDBFile gfsf = gfs0.findOne(new ObjectId(id));
        
        if(gfsf!=null){
        if(user!=null && !user.isEmpty()){ 
            
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        
        //obter o ficheiro
        byte[] myfile = this.getFileByID_ArrayBytes(id);
        GridFS gfs1 = new GridFS(db, "collDocuments" + "_REMOVED");
        
        GridFSInputFile gfsFile = gfs1.createFile(myfile);
        String fname = (String) gfsf.getMetaData().get("filename");
        gfsFile.setFilename(fname);
        gfsFile.setMetaData(gfsf.getMetaData());
        DBObject meta = gfsFile.getMetaData();
        meta.put("removeDate", sdf.format(new Date()));
        meta.put("removeUser",user);
        gfsFile.setMetaData(meta);
        gfsFile.save();
        
        gfs0.remove(new ObjectId(id));
        return "ok";
        
        }else{
            return "erro1 : campo username nao preenchido";
        }
        }else{
            return "erro0 : documento nao encontrado";
        }

    }
    
    
    
    public String addMyFile(String filename, File myFile, DBObject metadados){
        try {
            String newFileName = filename;
            File imageFile = myFile;
            GridFS gfsDB = new GridFS(db, "Documentos");
            GridFSInputFile gfsFile = gfsDB.createFile(imageFile);
            gfsFile.setFilename(newFileName);
            metadados.put("myID", this.getMyPrivateID());
            metadados.put("versionNumber","0");
            gfsFile.setMetaData(metadados);
            gfsFile.save();
            return gfsFile.getId().toString();
        } catch (IOException ex) {
            System.out.println(ex.toString());
            return "";
        }
    }
    
    
    public String updateMyFileByID(String id, String filename, File myFile, DBObject metadados, String user){
        
        GridFS gfs0 = new GridFS(db, "collDocuments");
        GridFSDBFile gfsf = gfs0.findOne(new ObjectId(id));
        
        if(gfsf!=null){
        if(user!=null && !user.isEmpty()){ 
            InputStream in = null;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                //obter o ficheiro
                byte[] myfile0 = this.getFileByID_ArrayBytes(id);
                GridFS gfs1 = new GridFS(db, "collDocuments" + "_UPDATED");
                //tratar do documento de seguranca
                GridFSInputFile gfsFile = gfs1.createFile(myfile0);
                String fname = (String) gfsf.getMetaData().get("filename");
                gfsFile.setFilename(fname);
                gfsFile.setMetaData(gfsf.getMetaData());
                DBObject meta = gfsFile.getMetaData();
                gfsFile.setMetaData(meta);
                gfsFile.save();
                //remover o anterior
                gfs0.remove(new ObjectId(id));
                
                //tratar do novo documento
                in = new FileInputStream(myFile);
                byte[] newFile = IOUtils.toByteArray(in);
                GridFSInputFile gfsFileNew = gfs0.createFile(newFile);
                if(!filename.isEmpty()){ fname = filename; }
                gfsFile.setFilename(fname);
                if(metadados!=null){ meta = metadados; }
                else{ gfsFileNew.setMetaData(gfsf.getMetaData()); }
                
                //tratar da versao
                DBObject m = gfsFile.getMetaData();
                
                String myid = (String) gfsf.getMetaData().get("myID");
                meta.put("myID", myid);
                
                String vns = m.get("versionNumber").toString();
                meta.put("versionNumber", Integer.parseInt(vns)+1);
                
                meta.put("updateDate", sdf.format(new Date()));
                meta.put("updateUser",user);
                
                gfsFileNew.setMetaData(meta);
                gfsFileNew.save();
                
                return "ok";
            } catch (FileNotFoundException ex) {
                return "erro2 : " + ex.toString();
            } catch (IOException ex) {
                return "erro3 : " + ex.toString();
            } finally {
                try { in.close(); } 
                catch (IOException ex) { return "erro4 : " + ex.toString(); }
            }
        
        }else{
            return "erro1 : campo username nao preenchido";
        }
        }else{
            return "erro0 : documento nao encontrado";
        }
    }
    
    
    public static void main(String[] args){
    
        File f = new File("C:\\Users\\prego\\Desktop\\temp.txt");
        
        MyConnection conn = new MyConnection("peskix.no-ip.biz","repos","Documentos");
        //conn.registerEntidadeNumber();
        //conn.getEntidadeNumber();
        BasicDBObject doc = new BasicDBObject();
                doc.put("filename", "nomeFicheiro");
                doc.put("realName", "nome");
                doc.put("created", "dataCriacao");
                doc.put("creator", "criadoPor");
                doc.put("source", "origemDoc");
                doc.put("title", "titulo");
                doc.put("description", "descricao");
                doc.put("typology", "tipologia");
                doc.put("validateDate", "dataValidade");
                doc.put("original", "original");
                doc.put("size", "tamanho");
                doc.put("lastAccessDate", "dataUltimoAcesso");
                doc.put("entryRepositoryDate", "10-10-2000");
                doc.put("fileType", "tipoFicheiro");
                doc.put("otherAuthors", "outrosAutores");
                doc.put("uploa0dUser", "uCarregouFicheiro");
                doc.put("pid", "pid");
                doc.put("fid", "fid");
        //conn.addMyFile("temp.txt", f, doc);
        
        conn.updateMyFileByID("5227571ea2844507a848efe7", "temp_teste.txt", f, doc, "prego");
               
                
        //conn.deleteMyFileByID("5224b4db25c0e6087f84a775","prego");
        //String aux = conn.insertMyFile("teste", f, doc);
        //System.out.println(aux);
        conn.closeConn();
    }
    
    
    
    
    
    
    
    /**
     * @return the db
     */
    public DB getDb() {
        return db;
    }

    /**
     * @param db the db to set
     */
    public void setDb(DB db) {
        this.db = db;
    }

    /**
     * @return the coll
     */
    public DBCollection getColl() {
        return coll;
    }

    /**
     * @param coll the coll to set
     */
    public void setColl(DBCollection coll) {
        this.coll = coll;
    }

    /**
     * @return the fileColl
     */
    public DBCollection getFileColl() {
        return fileColl;
    }

    /**
     * @param fileColl the fileColl to set
     */
    public void setFileColl(DBCollection fileColl) {
        this.fileColl = fileColl;
    }
    
    
}
