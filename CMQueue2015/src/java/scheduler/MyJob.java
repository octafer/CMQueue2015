/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scheduler;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import dataMap.ObjIndexacao;
import db.MyConnection;
import db.MySQLConnIFlow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.faces.context.FacesContext;
import models.entidade.Entidade;
import org.joda.time.DateTime;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import structures.Acao;
import structures.AreaFuncional;
import structures.AreaIntervencao;
import structures.ClassificacaoAMEC;
import structures.ClassificacaoMEF;
import structures.Documento;
import structures.EntradaExpediente;
import structures.Indexacao;
import structures.Intervencao;
import structures.MacroArea;
import structures.MacroProcesso;
import structures.ObjetivoEstrategico;
import structures.ObjetivoOperacional;
import structures.Processo;
import structures.ProcessoGenerico;
import structures.ProcessoRH;
import structures.Produto;
import structures.ReqIntViatura;
import structures.RequisicaoArquivo;
import structures.RequisicaoInterna;
import structures.TarefaColaborativa;
import structures.Titulo;
import utils.MyParsers;
import utils.Parsers;

/**
 *
 * @author prego
 */

@DisallowConcurrentExecution
public class MyJob implements Job {

    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        
        MyConnection conn = null; MySQLConnIFlow sqlconn = null; ResultSet rs = null;
        
        try {
            
            System.gc();
            
            DateTime dt = new DateTime();
            
            //if(dt.getHourOfDay()< 24 && dt.getHourOfDay() > 7){
          
            //conn = (MyConnection) context.getJobDetail().getJobDataMap().get("serverNoSQL");
            conn = new MyConnection("192.168.1.46", "dbProcessos", "gestaoProcessos");
            //conn = new MyConnection("192.168.1.46", "dbProcessosTESTES", "gestaoProcessosTESTES");
            sqlconn = new MySQLConnIFlow();
            
            
            //ResultSet rs = sqlconn.search("SELECT * FROM cma.cma_acoes_processo where pid = 212566");
            rs = sqlconn.search("select * from cma.cma_acoes_processo order by id limit 1");
            
            boolean processado = false;
            
            
            //for(int x=0;rs.next();x++){
            if(rs.next()){
            
                long mid = rs.getLong("id"); long mpid = rs.getLong("pid");
                System.out.println(mid + " - " + mpid);
                
                ResultSet rsi = sqlconn.search("select procdata, extractvalue(procdata, '//a[@n=\\\"numero_processo\\\"]') as num_processo,"
                                         + "extractvalue(procdata, '//a[@n=\\\"titulo_processo\\\"]') as tit_macroProc"
                                         + " from iflow.process where pid="+rs.getLong("pid") +" and flowid=" + rs.getInt("fid"));
                
                rsi.next();
                
                
                //AUTORIZACAO TRABALHO SUPLEMENTAR criacao
                if(rs.getString("acao").equals("Autorização trabalho suplementar criação")){ 
                    criacao_AutTrabSuplementar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }

                //AUTORIZACAO TRABALHO SUPLEMENTAR associar a macro
                else if(rs.getString("acao").equals("Autorização trabalho suplementar macro")){
                    assocMacro_AutTrabSuplementar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ASSIDUIDADE IRREGULAR associar a macro
                else if(rs.getString("acao").equals("Assiduidade irregular macro")){
                    assocMacroAssidIrregular(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //SERVICO EXTERNO associar a macro
                else if(rs.getString("acao").equals("Serviço externo macro")){
                    assocMacroServExterno(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //AUTORIZACAO TRABALHO SUPLEMENTAR encaminhamento
                else if(rs.getString("acao").equals("Autorização trabalho suplementar tramitação")){
                    try{
                    encaminhamento_AutTrabSuplementar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                    }catch(Exception e){}
                }
                
                //EXECUCAO TRABALHO SUPLEMENTAR fim
                else if(rs.getString("acao").equals("Execução trabalho suplementar fim")){
                    fim_ExeTrabSuplementar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //AUTORIZACAO TRABALHO SUPLEMENTAR fim
                else if(rs.getString("acao").equals("Autorização trabalho suplementar fim")){
                    fim_AutTrabSuplementar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //EXECUCAO TRABALHO SUPLEMENTAR criacao
                else if(rs.getString("acao").equals("Execução trabalho suplementar criação")){
                    criacao_ExeTrabSuplementar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //EXECUCAO TRABALHO SUPLEMENTAR associar a macro
                else if(rs.getString("acao").equals("Execução trabalho suplementar macro")){
                    assocMacro_ExeTrabSuplementar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //EXECUCAO TRABALHO SUPLEMENTAR encaminhamento
                else if(rs.getString("acao").equals("Execução trabalho suplementar tramitação") || 
                        rs.getString("acao").equals("Execução trabalho suplementar tramitação justificação") ||
                        rs.getString("acao").equals("Execução trabalho suplementar tramitação decisão")){
                    encaminhamento_ExeTrabSuplementar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                
                //ESCLARECIMENTO DE DUVIDAS - associacao à macro
                else if(rs.getString("acao").equals("Esclarecimento dúvidas macro")){
                    assocMacro_EsclarecimentoDuvidas(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //PLATAFORMA - associacao à macro
                else if(rs.getString("acao").equals("Faltas macro")){
                    assocMacro_Falta(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //AUSENCIA DIA ANIVERSARIO - associacao à macro
                else if(rs.getString("acao").equals("Ausência dia aniversário macro")){
                    assocMacro_AusenciaDiaAniversario(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //FÉRIAS - associacao à macro
                else if(rs.getString("acao").equals("Férias macro")){
                    assocMacro_Ferias(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                
                //PLATAFORMA - associacao à macro
                else if(rs.getString("acao").equals("Plataforma macro")){
                    assocMacro_Plataforma(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ASSIDUIDADE IRREGULAR - criacao
                else if(rs.getString("acao").equals("Assiduidade irregular criação")){
                    criacao_AssiduidadeIrregular(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ASSIDUIDADE IRREGULAR -tramitacao 
                else if(rs.getString("acao").equals("Assiduidade irregular tramitação")){
                    tramitacao_AssiduidadeIrregular(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ASSIDUIDADE IRREGULAR - devolucao 
                else if(rs.getString("acao").equals("Assiduidade irregular devolução")){
                    devolucao_AssiduidadeIrregular(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ASSIDUIDADE IRREGULAR - fim 
                else if(rs.getString("acao").equals("Assiduidade irregular fim")){
                    fim_AssiduidadeIrregular(conn, sqlconn, rs, rsi, mpid);
                    processado=true;
                }
                
                //PG - associado a processo
                else if(rs.getString("acao").equals("associado a processo") || rs.getString("acao").equals("associação a processo")){
                    associadoAProcesso(conn, sqlconn, rs, rsi, mpid);
                    processado=true;
                }
                
                //PG - tipo de permissao
                else if(rs.getString("acao").equals("PG tipo permissão")){
                    processado=true;
                }
                
                //PG - data reuniao camara
                else if(rs.getString("acao").equals("TC interna junção de documento ") || rs.getString("acao").equals("TC interna junção de documento")){
                    processado = true;
                }

                //PG - junção de documento - encaminhar
                else if(rs.getString("acao").equals("junção de documento - encaminhar") || rs.getString("acao").equals("TC interna junção de documento")){
                    juncaoDocEncaminhar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                
                //TC - externa anexos
                else if(rs.getString("acao").equals("TC externa anexos")){
                    tcExternaAnexos(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ABUSA - criacao
                else if(rs.getString("acao").equals("Abusa criação")){
                    abusaCriacao(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ABUSA - encaminhar
                else if(rs.getString("acao").equals("Abusa encaminhar")){
                    abusaEncaminhar(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //CRIACAO FICHA DE PROCESSO
                else if(rs.getString("acao").equals("criação ficha processo")){
                    urbCriacaoFichaProcesso(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //Hortas
                else if(rs.getString("acao").equals("hortas")){
                    urbCriacaoFichaProcesso(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //CRIACAO Requisicao interna de viaturas
                else if(rs.getString("acao").equals("criação rv")){
                    criacaoRequisicaoViaturas(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                
                //TRAMITAR Requisicao interna de viaturas
                else if(rs.getString("acao").equals("tramitar rv")){
                    tramitarRequisicaoViaturas(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //FIM Requisicao interna de viaturas
                else if(rs.getString("acao").equals("fim rv")){
                    fimRequisicaoViaturas(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                
                //ASSOCIAR Requisicao interna de viaturas
                else if(rs.getString("acao").equals("associar rv")){
                    associarRequisicaoViaturas(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                
                //CRIACAO Requisicao Arquivo
                else if(rs.getString("acao").equals("RA criação")){
                    criacaoRequisicaoArquivo(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ENCAMINHAR Requisicao Arquivo
                else if(rs.getString("acao").equals("RA encaminhar") || rs.getString("acao").equals("RA fim")){
                    encaminharRequisicaoArquivo(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //ENCAMINHAR Requisicao Arquivo
                else if(rs.getString("acao").equals("TC externa junção de documento ")){
                    //nao e necessario registar esta acao
                    processado = true;
                }
                
                //AJUDAS DE CUSTO - criacao
                else if(rs.getString("acao").equals("Ajudas de custo criação")){
                    criacao_AjudasCusto(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
               //AJUDAS DE CUSTO -tramitacao 
               else if(rs.getString("acao").equals("Ajudas de custo tramitação")){
                    tramitacao_AjudasCusto(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //AJUDAS DE CUSTO - devolucao 
                else if(rs.getString("acao").equals("Ajudas de custo devolução")){
                    devolucao_AjudasCusto(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }
                
                //AJUDAS DE CUSTO - fim 
                else if(rs.getString("acao").equals("Ajudas de custo fim")){
                    fim_AjudasCusto(conn, sqlconn, rs, rsi, mpid);
                    processado=true;
                }
                 //AJUDAS DE CUSTO associar a macro
                else if(rs.getString("acao").equals("Ajudas de custo macro")){
                    assocMacroAjudasCusto(conn, sqlconn, rs, rsi, mpid);
                    processado = true;
                }   
                
                
                              
                else{
                    processado = validarOpcoes(conn, sqlconn, rs, rsi, mpid);
                }
                                
                if(processado){  
                    sqlconn.delete("delete from cma.cma_acoes_processo where id="+rs.getLong("id"));
                }else{
                    sqlconn.insert("insert into cma.cma_acoes_processo(pid, fid, acao, local1, local2, data, c1,c2,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13, n1, c14, n2, comentario, c15, c16, c17, c18, "
                                + "c19, c20, c21, c22, c23, c24, c25, c26, c27, servicoOrigem, servicoDestino, n3) "
                                    + "values ("+rs.getLong("pid")+","+rs.getLong("fid")+",'"+rs.getString("acao")+"','"+rs.getString("local1")+"','"+rs.getString("local2")+"','"+rs.getString("data")+"','"+rs.getString("c1")+"','"+
                                rs.getString("c2")+"','"+rs.getString("c3")+"','"+rs.getString("c4")+"','"+rs.getString("c5")+"','"+rs.getString("c6")+"','"+rs.getString("c7")+"','"+rs.getString("c8")+"','"+
                                rs.getString("c10")+"','"+rs.getString("c9")+"','"+rs.getString("c11")+"','"+rs.getString("c12")+"',"+rs.getInt("n1")+",'"+rs.getString("c14")+"',"+rs.getInt("n2")+
                                ",'"+rs.getString("comentario")+"','"+rs.getString("c15")+"','"+rs.getString("c16")+"','"+rs.getString("c17")+"','"+rs.getString("c18")+"','"+rs.getString("c19")+"','"+rs.getString("c20")+
                                "','"+rs.getString("c21")+"','"+rs.getString("c22")+"','"+rs.getString("c23")+"','"+rs.getString("c24")+"','"+rs.getString("c25")+"','"+rs.getString("c26")+"','"+rs.getString("c27")+
                                    "','"+rs.getString("servicoOrigem")+"','"+rs.getString("servicoDestino")+"',"+rs.getInt("n3")+")");
                    
                    sqlconn.delete("delete from cma.cma_acoes_processo where id="+rs.getLong("id"));
                }
                
            }    
                
            
            sqlconn.closeConnection();
            conn.closeConn();
            
            // E AQUI }
            
                
        } catch (SQLException ex) {
                Logger.getLogger(MyJob.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(MyJob.class.getName()).log(Level.SEVERE, null, ex);
        }
    
        
    }
    
    
    
    
    
    //********* DEFINICAO DE ROTINAS ********************************************************************************************************************************************
   
    private void assocMacroAjudasCusto(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
        sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
           
    }
    
    
    private void devolucao_AjudasCusto(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
                
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pRH.setEstado("Em processamento");
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Ajudas  de custo",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        
                    }
                
        
        
    }
    
 
        private void fim_AjudasCusto(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
        
        
                    try{
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Concluído"; String descricao="O processo está concluído";
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Ajudas de custo",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Concluído");
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && rs.getString("local1").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        

                    }
                    }catch(Exception e){}
                
    
    
    }

       private void criacao_AjudasCusto(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
        //AJUDAS DE CUSTO
 
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                    Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar processo de ajudas de custo", "");
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Ajudas de custo", rs.getString("servicoOrigem"));
                    if(oi==null){
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                    } 
                    
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    Entidade e=null;
                    if(oe!=null){
                        e = MyParsers.parseEntidadeJSONtoObj(oe);
                    }
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    //fim entidade
                    
                    SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy");
                    Date dtIncid = sdf2.parse(rs.getString("c10"));
                    
                    
                    ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <MacroProcesso> (),
                                                    "Reposição e reembolso de valores", rs.getString("c5"), " | " + rs.getString("c12")+ " | " + rs.getString("c13") 
                                                     ,dtIncid, rs.getString("c1"), e); 
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
         }
       
   private void tramitacao_AjudasCusto(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
                
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pRH.setEstado("Em processamento");
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Ajudas de custo",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                }
                
        
        
    }
           
    
    private void encaminharRequisicaoArquivo(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                        
                RequisicaoArquivo ra = Parsers.jsonToReqARQJava(o);
                        
                //Obter a acao
                Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "RArq encaminhada"; 
                String descricao=rs.getString("local2");
                Acao acao = new Acao(data, user, designacao, descricao);
                ra.getLstAcoes().add(acao);
                        
                if(!rs.getString("local2").isEmpty()){
                    ra.setLocalAnterior(rs.getString("local1"));
                    ra.setLocalAtual(rs.getString("local2"));
                }    
                
                ra.setDados(rsi.getString("procdata"));
                ra.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                if(ra.getLstColabs().indexOf(rs.getString("local1"))==-1){ ra.getLstColabs().add(rs.getString("local1")); }
                if(ra.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ ra.getLstColabs().add(rs.getString("local2")); }
                        
                if(ra.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ ra.getLstServicos().add(rs.getString("servicoOrigem")); }
                if(ra.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ ra.getLstServicos().add(rs.getString("servicoDestino")); }

                
                if(rs.getString("local2")!=null){ ra.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                else if(rs.getString("local1")!=null){ ra.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ ra.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ ra.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                ra.setEstado("Em processamento.");
                        
                BasicDBObject onew = Parsers.parseJavaRARQToJsonJARQ(ra);
                        
                conn.getDBCollection().update(new BasicDBObject("processo.pid", ra.getPid()), onew, false, false, WriteConcern.SAFE);
        
    }
            
            
    
    private void criacaoRequisicaoArquivo(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //REQUISICAO ARQUIVO criacao
 
        List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
        Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar requisição ao arquivo", "");
        lstAcoes.add(acao);
                    
        if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
        String dados = rsi.getString("procdata");
                    
                    
        List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
        lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Processamento de pedidos de serviços de suporte", rs.getString("servicoOrigem"));
        if(oi==null){
            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
            new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
            new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
        } 
                    
                    
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    
        Date d1 = rs.getTimestamp("data");
                    
        String qenvia = rs.getString("local1");String qRecebe = rs.getString("local2");String just = rs.getString("c3");String coment = rs.getString("c4");
        String numProc = rs.getString("c5");String dtPeriodo = rs.getString("c6");String emNomeProc = rs.getString("c7");String freg = rs.getString("c8");
        String tipoDesc = rs.getString("c9"); String reqPapel = rs.getString("c10");
        String obs = rs.getString("c11");
        
        RequisicaoArquivo rarq = new RequisicaoArquivo("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <MacroProcesso> (),
                
                                                    "Iniciado", qenvia, qRecebe, d1, just, coment, numProc, dtPeriodo, emNomeProc, freg, tipoDesc, reqPapel, obs);
                    
        BasicDBObject onew = Parsers.parseJavaRARQToJsonJARQ(rarq);
        conn.getDBCollection().insert(onew, WriteConcern.SAFE);
        
        
    }
    
    
    
    
    private void associarRequisicaoViaturas(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{

       
        String linha = "";
        
        if(rs.getString("c1")!=null){
            linha = "Requisição interna de viaturas relacionada com o PG: " + rs.getLong("c1");
        }
                            
        sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                                    + "values("+rs.getLong("pid")+","+rs.getLong("c1")+",'"+linha+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+
                                    rs.getLong("c2")+","+rs.getLong("fid")+",'"+"Req.Interna Viaturas n. "+rs.getLong("pid")+"')");
                            
        
       
    }
    
    
    
    private void tramitarRequisicaoViaturas(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                        
                ReqIntViatura riv = Parsers.jsonToRIVJava(o);
                        
                //Obter a acao
                Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "RIV encaminhada"; 
                String descricao=rs.getString("local2");
                Acao acao = new Acao(data, user, designacao, descricao);
                riv.getLstAcoes().add(acao);
                        
                if(!rs.getString("local2").isEmpty()){
                    riv.setLocalAnterior(rs.getString("local1"));
                    riv.setLocalAtual(rs.getString("local2"));
                }    
                
                riv.setDados(rsi.getString("procdata"));
                riv.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                if(riv.getLstColabs().indexOf(rs.getString("local1"))==-1){ riv.getLstColabs().add(rs.getString("local1")); }
                if(riv.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ riv.getLstColabs().add(rs.getString("local2")); }
                        
                if(riv.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ riv.getLstServicos().add(rs.getString("servicoOrigem")); }
                if(riv.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ riv.getLstServicos().add(rs.getString("servicoDestino")); }

                
                if(rs.getString("local2")!=null){ riv.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                else if(rs.getString("local1")!=null){ riv.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ riv.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ riv.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                riv.setEstado("Em processamento.");
                        
                BasicDBObject onew = Parsers.parseJavaRIVToJsonRIV(riv);
                        
                conn.getDBCollection().update(new BasicDBObject("processo.pid", riv.getPid()), onew, false, false, WriteConcern.SAFE);
    }
    
    
    
    
    private void fimRequisicaoViaturas(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                        
                ReqIntViatura riv = Parsers.jsonToRIVJava(o);
                        
                //Obter a acao
                Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "RIV encaminhada"; 
                String descricao=rs.getString("local2");
                Acao acao = new Acao(data, user, designacao, descricao);
                riv.getLstAcoes().add(acao);
                        
                if(!rs.getString("local2").isEmpty()){
                    riv.setLocalAnterior(rs.getString("local1"));
                    riv.setLocalAtual(rs.getString("local2"));
                }    
                
                riv.setDados(rsi.getString("procdata"));
                riv.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                if(riv.getLstColabs().indexOf(rs.getString("local1"))==-1){ riv.getLstColabs().add(rs.getString("local1")); }
                if(riv.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ riv.getLstColabs().add(rs.getString("local2")); }
                        
                if(riv.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ riv.getLstServicos().add(rs.getString("servicoOrigem")); }
                if(riv.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ riv.getLstServicos().add(rs.getString("servicoDestino")); }

                
                if(rs.getString("local2")!=null){ riv.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                else if(rs.getString("local1")!=null){ riv.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ riv.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ riv.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                riv.setEstado("Concluída");
                riv.setLocalAtual("pre-arquivo");
                        
                BasicDBObject onew = Parsers.parseJavaRIVToJsonRIV(riv);
                        
                conn.getDBCollection().update(new BasicDBObject("processo.pid", riv.getPid()), onew, false, false, WriteConcern.SAFE);
    }
    
    
    
    
    private void criacaoRequisicaoViaturas(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //REQUISICAO INTERNA DE VIATURAS criacao
 
        List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
        Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar requisição interna de viaturas", "");
        lstAcoes.add(acao);
                    
        if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
        String dados = rsi.getString("procdata");
                    
                    
        List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
        lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Processamento de pedidos de serviços de suporte - Transportes", rs.getString("servicoOrigem"));
        if(oi==null){
            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
            new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
            new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
        } 
                    
                    
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    
        Date d1 = rs.getTimestamp("data");
                    
        String servReq = rs.getString("c2");
        String tv1 = rs.getString("c3");String tv2 = rs.getString("c4");String tv3 = rs.getString("c5");String tv4 = rs.getString("c6");String tv5 = rs.getString("c7"); String tv6 = rs.getString("c8");
        
        Date dt1 = null;Date dt2 = null;Date dt3 = null;Date dt4 = null;Date dt5 = null; Date dt6 = null;
        
        try{ dt1 = rs.getDate("c9"); }catch(Exception e){} try{ dt2 = rs.getDate("c10"); }catch(Exception e){} try{ dt3 = rs.getDate("c11"); }catch(Exception e){}  
        try{ dt4 = rs.getDate("c12"); }catch(Exception e){} try{ dt5 = rs.getDate("c13"); }catch(Exception e){} try{ dt6 = rs.getDate("c14"); }catch(Exception e){} 

        String o1 = rs.getString("c15");String o2 = rs.getString("c16");String o3 = rs.getString("c17");String o4 = rs.getString("c18");String o5 = rs.getString("c19"); String o6 = rs.getString("c20");
        String m1 = rs.getString("c21");String m2 = rs.getString("c22");String m3 = rs.getString("c23");String m4 = rs.getString("c24");String m5 = rs.getString("c25"); String m6 = rs.getString("c26");
        String tm1 = rs.getString("c27");String tm2 = rs.getString("c28");String tm3 = rs.getString("c29");String tm4 = rs.getString("c30");String tm5 = rs.getString("c31"); String tm6 = rs.getString("c32");
        String mo1 = rs.getString("c33");String mo2 = rs.getString("c34");String mo3 = rs.getString("c35");String mo4 = rs.getString("c36");String mo5 = rs.getString("c37"); String mo6 = rs.getString("c38");
        
        List <String> lstTV = new ArrayList <> (); lstTV.add(tv1);lstTV.add(tv2);lstTV.add(tv3);lstTV.add(tv4);lstTV.add(tv5);lstTV.add(tv6);
        List <Date> lstDT = new ArrayList <> (); lstDT.add(dt1);lstDT.add(dt2);lstDT.add(dt3);lstDT.add(dt4);lstDT.add(dt5);lstDT.add(dt6);
        List <String> lstO = new ArrayList <> (); lstO.add(o1);lstO.add(o2);lstO.add(o3);lstO.add(o4);lstO.add(o5);lstO.add(o6);
        List <String> lstM = new ArrayList <> (); lstM.add(m1);lstM.add(m2);lstM.add(m3);lstM.add(m4);lstM.add(m5);lstM.add(m6);
        List <String> lstTM = new ArrayList <> (); lstTM.add(tm1);lstTM.add(tm2);lstTM.add(tm3);lstTM.add(tm4);lstTM.add(tm5);lstTM.add(tm6);
        List <String> lstMO = new ArrayList <> (); lstMO.add(mo1);lstMO.add(mo2);lstMO.add(mo3);lstMO.add(mo4);lstMO.add(mo5);lstMO.add(mo6);
        
        ReqIntViatura riv = new ReqIntViatura("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <MacroProcesso> (),
                
                                                    "Req.Interna Viaturas", "Iniciado", servReq, lstTV, lstDT, lstO, lstM, lstTM, lstMO);
                    
        BasicDBObject onew = Parsers.parseJavaRIVToJsonRIV(riv);
        conn.getDBCollection().insert(onew, WriteConcern.SAFE);
        
        
    }
    
    
    private void tcExternaAnexos(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    }
    
    private void urbCriacaoFichaProcesso(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    }
    
    
    private void abusaCriacao(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    }
    
    private void abusaEncaminhar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    }
    
    
    private void juncaoDocEncaminhar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        try{
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));

                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Junção de documentos"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c3") ;
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pg.setDados(rsi.getString("procdata"));
                        
                        //Documento d = new Documento(data, user, rs.getString("c1"));
                        //pg.getLstDocs().add(d);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
 
                    }
                    }catch(Exception e){}
        
    }
    
    
    
    
    private void associadoAProcesso(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        BasicDBList bl = new BasicDBList();
                    long nx = rs.getLong("c1");
                    bl.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    bl.add(new BasicDBObject("processo.pid", nx));
                    BasicDBObject df = new BasicDBObject("$and", bl);
                    BasicDBObject o = (BasicDBObject) conn.getColl().findOne(df);
                            
                    if(o==null){
                        o = (BasicDBObject) conn.getColl().findOne(new BasicDBObject("processo.tipo","Processo Genérico"));
                    }
                            
                    if(o!=null){
                        if(o.getString("refInterna")!=null){
                            o = (BasicDBObject) conn.getColl().findOne(new BasicDBObject("processo.pid", rs.getLong("c1")));
                        }
                    
                        String assunto = "";
                        if(o.getString("assunto")!=null && !o.getString("assunto").isEmpty()){
                            assunto = o.getString("assunto");
                        }
                            
                        String linha = "";
                        if(rs.getString("c4")!=null){
                            linha = rs.getString("c4").replaceAll("'","''");
                        }
                            
                        sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                                    + "values("+rs.getLong("pid")+","+rs.getLong("c1")+",'"+linha+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+
                                    rs.getLong("c2")+","+rs.getLong("fid")+",'"+assunto.replaceAll("'", "''")+"')");
                            
                        
                        //injetar XML no processo
                        
                        /*
                        ResultSet rsx = sqlconn.search("select procdata from process where pid="+rs.getLong("c1")+" and flowid="+rs.getLong("c2"));
                        rsx.next();
                        String procdata = rsx.getString(1);
                            
                            
                        //REGISTAR O PID
                        int pos0 = procdata.indexOf("l n=\"passocFilho\"");
                        //construir um array
                        StringBuilder sb = new StringBuilder(procdata);
                        //obter a posicao do numero de elems do array
                        int pos1 = sb.indexOf("s=\"", pos0); pos1+=3;
                        //verificar quantos carateres tem o numero
                        int nc = 0;
                        
                        for(int i = pos1; i < pos0+24; i++){
                            if(sb.charAt(i)!='\"'){
                                nc++;
                            }else{
                                break;
                            }
                        }
                    
                        String num = sb.substring(pos1, pos1+nc);
                        int numi = Integer.parseInt(num);
                        numi++;
                        sb.replace(pos1, pos1+nc, Integer.toString(numi));
                        numi--;
                        sb.insert(pos0+24, "<i p=\""+numi+"\">"+rs.getLong("pid")+"</i>");
                            
                            
                        //REGISTAR O FID
                        pos0 = sb.indexOf("l n=\"passocFilho_fid\"");
                        //construir um array
                        sb = new StringBuilder(procdata);
                        //obter a posicao do numero de elems do array
                        pos1 = sb.indexOf("s=\"", pos0); pos1+=3;
                        //verificar quantos carateres tem o numero
                        nc = 0;
                            
                        for(int i = pos1; i < pos0+28; i++){
                            if(sb.charAt(i)!='\"'){
                                nc++;
                            }else{
                                break;
                            }
                        }
                            
                        num = sb.substring(pos1, pos1+nc);
                        numi = Integer.parseInt(num);
                        numi++;
                        sb.replace(pos1, pos1+nc, Integer.toString(numi));
                        numi--;
                        sb.insert(pos0+28, "<i p=\""+numi+"\">"+rs.getLong("fid")+"</i>");
                            
                            
                        //REGISTAR O ASSUNTO
                        pos0 = sb.indexOf("l n=\"passocFilho_assuntos\"");
                        //construir um array
                        sb = new StringBuilder(procdata);
                        //obter a posicao do numero de elems do array
                        pos1 = sb.indexOf("s=\"", pos0); pos1+=3;
                        //verificar quantos carateres tem o numero
                        nc = 0;
                            
                        for(int i = pos1; i < pos0+33; i++){
                            if(sb.charAt(i)!='\"'){
                                nc++;
                            }else{
                                break;
                            }
                        }
                    
                        num = sb.substring(pos1, pos1+nc);
                        numi = Integer.parseInt(num);
                        numi++;
                        sb.replace(pos1, pos1+nc, Integer.toString(numi));
                        numi--;
                        sb.insert(pos0+33, "<i p=\""+numi+"\">"+linha+"</i>");
                            

                        //REGISTAR DATAS
                        pos0 = sb.indexOf("l n=\"passocFilho_datas\"");
                        //construir um array
                        sb = new StringBuilder(procdata);
                        //obter a posicao do numero de elems do array
                        pos1 = sb.indexOf("s=\"", pos0); pos1+=3;
                        //verificar quantos carateres tem o numero
                        nc = 0;
                            
                        for(int i = pos1; i < pos0+30; i++){
                            if(sb.charAt(i)!='\"'){
                                nc++;
                            }else{
                                break;
                            }
                        }
                    
                        num = sb.substring(pos1, pos1+nc);
                        numi = Integer.parseInt(num);
                        numi++;
                        sb.replace(pos1, pos1+nc, Integer.toString(numi));
                        numi--;
                        sb.insert(pos0+30, "<i p=\""+numi+"\">"+rs.getDate("data")+"</i>");
                            
                        //sqlconn.update("update iflow.process set procdata = '"+sb.toString().replaceAll("'", "''")+"' where pid="+rs.getLong("c1")+" and flowid="+rs.getLong("c2"));
                        */
                            
                    }
                    
                    
    }
    
    
    
    
    
    
    
    
    private void fim_AssiduidadeIrregular(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
        
        
                    try{
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Concluído"; String descricao="O processo está concluído";
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Assiduidade irregular",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Concluído");
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && rs.getString("local1").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        

                    }
                    }catch(Exception e){}
                
    
    
    }
    
    
    
    private void tramitacao_AssiduidadeIrregular(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
                
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pRH.setEstado("Em processamento");
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Assiduidade irregular",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        
                    }
                
        
        
    }
    
    
    
    
    private void devolucao_AssiduidadeIrregular(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
                
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pRH.setEstado("Em processamento");
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Assiduidade irregular",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        
                    }
                
        
        
    }
    
    
    
    private void criacao_AutTrabSuplementar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                    Acao acao = new Acao(rs.getTimestamp("data"), rs.getString("local1"), "Criar pedido de autorização de trabalho suplementar", "");
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Autorização de trabalho suplementar",rs.getString("servicoOrigem"));
                    if(oi==null){
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                    } 
                    
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    //fim entidade
                    
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    Date d1 = rs.getTimestamp("data");
                    
                    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
                    Date dtIncid = sdf2.parse(rs.getString("c5"));
                    
                    
                    ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    "Autorização de trabalho suplementar", rs.getString("c3"), rs.getString("c2") + " | " + rs.getString("c4"),
                                                    dtIncid, "Iniciado",e); 
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                    
                      
    }
    
    
    
    private void assocMacro_AutTrabSuplementar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
        sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
           
    }
    
    
    private void assocMacroAssidIrregular(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
        sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
           
    }
    
    
    private void assocMacroServExterno(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
        sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
           
    }
    
    
    
    
    
    private void encaminhamento_AutTrabSuplementar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="Associado ao macroprocesso";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pRH.setEstado("Em processamento");
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Autorização de trabalho suplementar",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                    }else{
                        sqlconn.insert("insert into cma.cma_acoes_processo(pid, fid, acao, local1, local2, data, c1,c2,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13, n1, c14, n2, comentario, c15, c16, c17, c18, "
                                + "c19, c20, c21, c22, c23, c24, c25, c26, c27, servicoOrigem, servicoDestino, n3) "
                                    + "values ("+rs.getLong("pid")+","+rs.getLong("fid")+",'"+rs.getString("acao")+"','"+rs.getString("local1")+"','"+rs.getString("local2")+"','"+rs.getString("data")+"','"+rs.getString("c1")+"','"+
                                rs.getString("c2")+"','"+rs.getString("c3")+"','"+rs.getString("c4")+"','"+rs.getString("c5")+"','"+rs.getString("c6")+"','"+rs.getString("c7")+"','"+rs.getString("c8")+"','"+
                                rs.getString("c10")+"','"+rs.getString("c9")+"','"+rs.getString("c11")+"','"+rs.getString("c12")+"',"+rs.getInt("n1")+",'"+rs.getString("c14")+"',"+rs.getInt("n2")+
                                ",'"+rs.getString("comentario")+"','"+rs.getString("c15")+"','"+rs.getString("c16")+"','"+rs.getString("c17")+"','"+rs.getString("c18")+"','"+rs.getString("c19")+"','"+rs.getString("c20")+
                                "','"+rs.getString("c21")+"','"+rs.getString("c22")+"','"+rs.getString("c23")+"','"+rs.getString("c24")+"','"+rs.getString("c25")+"','"+rs.getString("c26")+"','"+rs.getString("c27")+
                                    "','"+rs.getString("servicoOrigem")+"','"+rs.getString("servicoDestino")+"',"+rs.getInt("n3")+")");
                    }
        
    }
    
    
    
    private void fim_AutTrabSuplementar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="Associado ao macroprocesso";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pRH.setEstado("Concluído");
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Autorização de trabalho suplementar",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                    }else{
                        sqlconn.insert("insert into cma.cma_acoes_processo(pid, fid, acao, local1, local2, data, c1,c2,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13, n1, c14, n2, comentario, c15, c16, c17, c18, "
                                + "c19, c20, c21, c22, c23, c24, c25, c26, c27, servicoOrigem, servicoDestino, n3) "
                                    + "values ("+rs.getLong("pid")+","+rs.getLong("fid")+",'"+rs.getString("acao")+"','"+rs.getString("local1")+"','"+rs.getString("local2")+"','"+rs.getString("data")+"','"+rs.getString("c1")+"','"+
                                rs.getString("c2")+"','"+rs.getString("c3")+"','"+rs.getString("c4")+"','"+rs.getString("c5")+"','"+rs.getString("c6")+"','"+rs.getString("c7")+"','"+rs.getString("c8")+"','"+
                                rs.getString("c10")+"','"+rs.getString("c9")+"','"+rs.getString("c11")+"','"+rs.getString("c12")+"',"+rs.getInt("n1")+",'"+rs.getString("c14")+"',"+rs.getInt("n2")+
                                ",'"+rs.getString("comentario")+"','"+rs.getString("c15")+"','"+rs.getString("c16")+"','"+rs.getString("c17")+"','"+rs.getString("c18")+"','"+rs.getString("c19")+"','"+rs.getString("c20")+
                                "','"+rs.getString("c21")+"','"+rs.getString("c22")+"','"+rs.getString("c23")+"','"+rs.getString("c24")+"','"+rs.getString("c25")+"','"+rs.getString("c26")+"','"+rs.getString("c27")+
                                    "','"+rs.getString("servicoOrigem")+"','"+rs.getString("servicoDestino")+"',"+rs.getInt("n3")+")");
                    }
    
    }
    
    
    
    
    private void criacao_ExeTrabSuplementar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                    Acao acao = new Acao(rs.getTimestamp("data"), rs.getString("local1"), "Criar pedido de execução de trabalho suplementar", "");
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Execução de trabalho suplementar",rs.getString("servicoOrigem"));
                    if(oi==null){
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                    } 
                    
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    if(oe==null){
                        //pesquisar por nome
                        BasicDBList lor = new BasicDBList();
                        lor.add(new BasicDBObject("nome", rs.getString("c2")));
                        lor.add(new BasicDBObject("designEnt", rs.getString("c2")));
                        BasicDBObject or = new BasicDBObject("$or", lor);
                        oe = (BasicDBObject) conn.getDBCollection().findOne(or);
                    }
                    
                    Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    //fim entidade
                    
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    Date d1 = rs.getTimestamp("data");
                    
                    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
                    Date dtIncid = null;
                    
                    try{ 
                        dtIncid = sdf2.parse(rs.getString("c6"));
                    }catch(Exception ee){
                        dtIncid = sdf2.parse(rs.getString("c7"));
                    }
                    
                    
                    ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <MacroProcesso> (),
                                                    "Execução de trabalho suplementar", rs.getString("c5"), "Solicita a aprovação da execução do trabalho suplementar",
                                                    dtIncid, "Iniciado",e); 
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
    }
    
    
    
    private void assocMacro_ExeTrabSuplementar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
        sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
           
    }
    
    
    
    private void encaminhamento_ExeTrabSuplementar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1") + rs.getString("c3");}
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pRH.setEstado("Em processamento");
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Execução de trabalho suplementar",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                    }else{
                        sqlconn.insert("insert into cma.cma_acoes_processo(pid, fid, acao, local1, local2, data, c1,c2,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13, n1, c14, n2, comentario, c15, c16, c17, c18, "
                                + "c19, c20, c21, c22, c23, c24, c25, c26, c27, servicoOrigem, servicoDestino, n3) "
                                    + "values ("+rs.getLong("pid")+","+rs.getLong("fid")+",'"+rs.getString("acao")+"','"+rs.getString("local1")+"','"+rs.getString("local2")+"','"+rs.getString("data")+"','"+rs.getString("c1")+"','"+
                                rs.getString("c2")+"','"+rs.getString("c3")+"','"+rs.getString("c4")+"','"+rs.getString("c5")+"','"+rs.getString("c6")+"','"+rs.getString("c7")+"','"+rs.getString("c8")+"','"+
                                rs.getString("c10")+"','"+rs.getString("c9")+"','"+rs.getString("c11")+"','"+rs.getString("c12")+"',"+rs.getInt("n1")+",'"+rs.getString("c14")+"',"+rs.getInt("n2")+
                                ",'"+rs.getString("comentario")+"','"+rs.getString("c15")+"','"+rs.getString("c16")+"','"+rs.getString("c17")+"','"+rs.getString("c18")+"','"+rs.getString("c19")+"','"+rs.getString("c20")+
                                "','"+rs.getString("c21")+"','"+rs.getString("c22")+"','"+rs.getString("c23")+"','"+rs.getString("c24")+"','"+rs.getString("c25")+"','"+rs.getString("c26")+"','"+rs.getString("c27")+
                                    "','"+rs.getString("servicoOrigem")+"','"+rs.getString("servicoDestino")+"',"+rs.getInt("n3")+")");
                    }
        
    }
    
    
    
    
    
    
    
    
    private void assocMacro_EsclarecimentoDuvidas(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        if(o!=null){
            MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
            sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                         + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
        }                    
        
    }
           
    
    
    private void assocMacro_Plataforma(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        if(o!=null){
            MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
            sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
        }                   
        
    }
    
    
    private void assocMacro_Falta(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        if(o!=null){
            MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
            sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
        }                   
        
    }
    
    
    private void assocMacro_AusenciaDiaAniversario(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        if(o!=null){
            MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
            sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
        }                   
        
    }
    
    
    private void assocMacro_Ferias(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        //obter o assunto do macro processo
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n1")));
        if(o!=null){
            MacroProcesso mp = Parsers.parseJsonToMacroProcesso(o);
            sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho, linhaPai) "
                     + "values("+rs.getLong("pid")+","+rs.getLong("n1")+",'"+rs.getString("local1")+" | "+rs.getString("acao")+"','"+rs.getDate("data")+"','"+rs.getString("local1")+"',"+rs.getLong("fid")+","+rs.getLong("n2")+",'"+mp.getTitulo()+"')");
        }                   
        
    }
    
    
    
    private void criacao_AssiduidadeIrregular(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
    
        //ASSIDUIDADE IRREGULAR criacao
 
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                    Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar processo de assiduidade irregular", "");
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Assiduidade irregular", rs.getString("servicoOrigem"));
                    if(oi==null){
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                    } 
                    
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    Entidade e=null;
                    if(oe!=null){
                        e = MyParsers.parseEntidadeJSONtoObj(oe);
                    }
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    //fim entidade
                    
                    SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy");
                    Date dtIncid = sdf2.parse(rs.getString("c6"));
                    
                    
                    ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <MacroProcesso> (),
                                                    "Assiduidade irregular", rs.getString("c5"), "EM:" + rs.getString("c7")+ " SM:" + rs.getString("c8")+ 
                                                    " ET:" + rs.getString("c9") + " ST:" + rs.getString("c10") , dtIncid, rs.getString("c1"), e); 
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                   
                    
                
    
    }
            
            
    private void fim_ExeTrabSuplementar(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{
        
        BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="Associado ao macroprocesso";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pRH.setEstado("Concluído");
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Autorização de trabalho suplementar",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                    }else{
                        sqlconn.insert("insert into cma.cma_acoes_processo(pid, fid, acao, local1, local2, data, c1,c2,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13, n1, c14, n2, comentario, c15, c16, c17, c18, "
                                + "c19, c20, c21, c22, c23, c24, c25, c26, c27, servicoOrigem, servicoDestino, n3) "
                                    + "values ("+rs.getLong("pid")+","+rs.getLong("fid")+",'"+rs.getString("acao")+"','"+rs.getString("local1")+"','"+rs.getString("local2")+"','"+rs.getString("data")+"','"+rs.getString("c1")+"','"+
                                rs.getString("c2")+"','"+rs.getString("c3")+"','"+rs.getString("c4")+"','"+rs.getString("c5")+"','"+rs.getString("c6")+"','"+rs.getString("c7")+"','"+rs.getString("c8")+"','"+
                                rs.getString("c10")+"','"+rs.getString("c9")+"','"+rs.getString("c11")+"','"+rs.getString("c12")+"',"+rs.getInt("n1")+",'"+rs.getString("c14")+"',"+rs.getInt("n2")+
                                ",'"+rs.getString("comentario")+"','"+rs.getString("c15")+"','"+rs.getString("c16")+"','"+rs.getString("c17")+"','"+rs.getString("c18")+"','"+rs.getString("c19")+"','"+rs.getString("c20")+
                                "','"+rs.getString("c21")+"','"+rs.getString("c22")+"','"+rs.getString("c23")+"','"+rs.getString("c24")+"','"+rs.getString("c25")+"','"+rs.getString("c26")+"','"+rs.getString("c27")+
                                    "','"+rs.getString("servicoOrigem")+"','"+rs.getString("servicoDestino")+"',"+rs.getInt("n3")+")");
                    }
        
    }
    
    
    
    private boolean validarOpcoes(MyConnection conn, MySQLConnIFlow sqlconn, ResultSet rs, ResultSet rsi, long mpid) throws SQLException, ParseException{

                boolean processado = false;
                //TERMOS DE INDEXACAO criacao
                if(rs.getString("acao").equals("termos do serviço")){
                    
                    String collOld = conn.getDBCollection().getName();
                    String titulo = rs.getString("c1");
                    
                    conn.setDBCollection("collMapeamento");

                    BasicDBObject q = new BasicDBObject("desig", titulo);
                    BasicDBObject elemMatch = new BasicDBObject("$elemMatch", q);
                    BasicDBObject qf = new BasicDBObject("Titulos", elemMatch);
                    
                    String xx = rs.getString("c3");
                    
                    String[] lstTermos = xx.split(";");
                    if(lstTermos[0].isEmpty()){ lstTermos[0] = rs.getString("c3"); } 
                    ObjIndexacao oi = null;
                    DBCursor cur = conn.getColl().find(qf);
                    for(;cur.hasNext();){
                        BasicDBObject r = (BasicDBObject) cur.next();
                        BasicDBList ltit = (BasicDBList) r.get("Titulos");
                        if(!ltit.isEmpty()){
                            BasicDBObject ot = (BasicDBObject) ltit.get(0);
                            oi = this.obterObjIndexacaoByTituloServico(conn, ot.getString("desig"),rs.getString("servicoOrigem"));
                        }
                        for(String s : lstTermos){
                            if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                                if(oi!=null){ 
                                    oi.getLstIndexacao().add(new Indexacao(s.trim(), rs.getString("servicoOrigem")));
                                    //obter os servicos do neutro
                                    ResultSet rsAdm = sqlconn.search("select perfil from cma.cma_mapear_perfis_neutro where perfilNeutro = '"+rs.getString("servicoOrigem")+"'");
                                    for(;rsAdm.next();){
                                        oi.getLstIndexacao().add(new Indexacao(s.trim(), rsAdm.getString(1)));
                                    }
                                } 
                            }else{
                                if(oi!=null){ 
                                    oi.getLstIndexacao().add(new Indexacao(s.trim(), rs.getString("servicoOrigem")));
                                } 
                            }
                        }    
                    }
                    
                    if(oi!=null){
                        BasicDBObject ooi = Parsers.parseObjIndexacaoJson(oi);
                        conn.getDBCollection().update(qf, ooi, false, false, WriteConcern.SAFE);
                    }
                    
                    conn.setDBCollection(collOld);
                    processado = true;
                }
                
                
                //FALTAS criacao
                else if(rs.getString("acao").equals("Faltas criação")){
                    try{
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                        
                    Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar processo de falta", "");
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Faltas",rs.getString("servicoOrigem"));
                    if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    Date dtIncid = null;
                    try{ dtIncid = sdf.parse(rs.getString("c8")); }
                    catch(Exception exx){
                        try{ dtIncid = sdf.parse(rs.getString("c10")); }catch(Exception exxx){}
                    }
                    
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    
                    ProcessoRH pRH = null;
                    
                    //verificar se e falta POR CONTA DO PERIODO DE FERIAS
                    if(rs.getString("c6").equals("Por conta do período de férias")){
                        
                        try{ dtIncid = sdf.parse(rs.getString("c8")); }catch(Exception exxx){}
                        
                        pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Falta", rs.getString("c5"), rs.getString("c6")+ " - " + rs.getString("c7") + " | " + rs.getString("c10") + " | " + 
                                                    rs.getString("c9") + ")" , dtIncid, "Em processamento",e);
                        
                    }
                    
                    //verificar se e falta PREPARACAO PARA O PARTO
                    else if(rs.getString("c6").equals("Preparação para o parto")){
                        try{ dtIncid = sdf.parse(rs.getString("c8")); }catch(Exception exxx){}
                        
                        pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Falta", rs.getString("c5"), rs.getString("c6")+ " | " + rs.getString("c9") + " | " + 
                                                    rs.getString("c10") + ")" , dtIncid, "Em processamento",e);
                    }
                    
                    //verificar se e falta CONSULTA PRE-NATAL
                    else if(rs.getString("c6").equals("Consulta pré-natal")){
                        try{ dtIncid = sdf.parse(rs.getString("c8")); }catch(Exception exxx){}
                        
                        pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Falta", rs.getString("c5"), rs.getString("c6")+ " | " + rs.getString("c9") + " | " + 
                                                    rs.getString("c10"), dtIncid, "Em processamento",e);
                    }
                    
                    //verificar se e falta DISPENSA PARA AVALIACAO DO PROCESSO DE ADOCAO
                    else if(rs.getString("c6").equals("Dispensa para avaliação do processo de adoção")){
                        try{ dtIncid = sdf.parse(rs.getString("c7")); }catch(Exception exxx){}
                        
                        pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Falta", rs.getString("c5"), rs.getString("c6")+ " | " + rs.getString("c8") + " | " + 
                                                    rs.getString("c9"), dtIncid, "Em processamento",e);
                        
                    }
                    
                    //verificar se e falta DOACAO DE SANGUE
                    else if(rs.getString("c6").equals("Doação de sangue")){
                        try{ dtIncid = sdf.parse(rs.getString("c7")); }catch(Exception exxx){}
                        
                        pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Falta", rs.getString("c5"), rs.getString("c6")+ " | " + rs.getString("c8") + " | " + 
                                                    rs.getString("c9"), dtIncid, "Em processamento",e);    
                        
                        
                    }//verificar se e falta ACOMPANHAMENTO DE FILHO MENOR A ESCOLA
                    else if(rs.getString("c6").equals("Acompanhamento de filho menor à escola")){
                        try{ dtIncid = sdf.parse(rs.getString("c7")); }catch(Exception exxx){}
                        
                        pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Falta", rs.getString("c5"), rs.getString("c6")+ " | " + rs.getString("c8") + " | " + 
                                                    rs.getString("c9") + "(nome do filho: " + rs.getString("c10")+")", dtIncid, "Em processamento",e);    
                        
                        
                    
                    
                    }//verificar se e falta Candidatos a eleições para cargos públicos
                    else if(rs.getString("c6").equals("Candidatos a eleições para cargos públicos")){
                        /*
                        try{ dtIncid = sdf.parse(rs.getString("c7")); }catch(Exception exxx){}
                        
                        pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Falta", rs.getString("c5"), rs.getString("c6")+ " | " + rs.getString("c8") + " | " + 
                                                    rs.getString("c9") + "(nome do filho: " + rs.getString("c10")+")", dtIncid, "Em processamento",e);    
                        
                        */
                    }
                    
                    else{
                    
                        try{ dtIncid = sdf.parse(rs.getString("c8")); }catch(Exception exxx){}
                        pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Falta", rs.getString("c5"), rs.getString("c6")+ " - " + rs.getString("c7") + " | " + rs.getString("c8") + " | " + 
                                                    rs.getString("c10") + " | " + rs.getString("c9"), dtIncid, "Em processamento",e); 
                    
                    }    
                        
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                    processado = true;
                    
                    }catch(Exception e){}
                        
                }
                
                //FALTA -tramitacao 
                else if(rs.getString("acao").equals("Faltas tramitação")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Faltas",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Em processamento");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && !rs.getString("local2").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //FALTA - devolucao 
                else if(rs.getString("acao").equals("Faltas devolução")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Devolução"; String descricao="";
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Faltas",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Em processamento");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && !rs.getString("local2").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //FALTA - fim 
                else if(rs.getString("acao").equals("Faltas fim")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Concluído"; String descricao="O processo está concluído";
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Faltas",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Concluído");
                        
                        pRH.setLocalAnterior(rs.getString("servicoOrigem"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && !rs.getString("local2").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                
                //AUSENCIA DIA DE ANIVERSARIO criacao
                else if(rs.getString("acao").equals("Ausência dia aniversário criação")){
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                        
                    Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar processo de ausência no dia de aniversário", "");
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Ausência no dia de aniversário",rs.getString("servicoOrigem"));
                    if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    
                    SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy");
                    Date dtIncid = sdf2.parse(rs.getString("c6"));
                    
                    ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Ausência no dia de aniversário", rs.getString("c5"), "", dtIncid, "Em processamento", e); 
                    
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                       
                    processado = true;
                }
                
                //AUSENCIA DIA DE ANIVERSARIO -tramitacao 
                else if(rs.getString("acao").equals("Ausência dia aniversário tramitação")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Ausência no dia de aniversário",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Em processamento");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && !rs.getString("local2").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //AUSENCIA DIA DE ANIVERSARIO - fim 
                else if(rs.getString("acao").equals("Ausência dia aniversário fim")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Concluído"; String descricao="O processo está concluído";
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Ausência no dia de aniversário",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Concluído");
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                
                //PLATAFORMA criacao
                else if(rs.getString("acao").equals("Plataforma criação")){

                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                        
                    Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar processo de plataforma", "");
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Plataforma",rs.getString("servicoOrigem"));
                    if(oi==null){
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    
                    Date dataAux = null;
                    
                    try{ dataAux = sdf.parse(rs.getString("c7")); }catch(Exception eee){}
                    
                    
                    ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Plataforma", rs.getString("c5"), rs.getString("c7") + " | " + rs.getString("c8") + " | " + rs.getString("c9"), 
                                                    dataAux, "Em processamento", e); 
                    
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                    
                    processado = true;
                    
                }
                
                
                //PLATAFORMA -tramitacao 
                else if(rs.getString("acao").equals("Plataforma tramitação") || rs.getString("acao").equals("Plataforma devolução")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Plataforma",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Em processamento");
                        
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && !rs.getString("local2").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //PLATAFORMA - fim 
                else if(rs.getString("acao").equals("Plataforma fim") || rs.getString("acao").equals("Plataforma devolução")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Concluído"; String descricao="O processo está concluído";
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Plataforma",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Concluído");
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                
                //SERVICO EXTERNO criacao
                else if(rs.getString("acao").equals("Serviço externo criação")){

                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                        
                    Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar processo de serviço externo", "");
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn,"Serviço Externo",rs.getString("servicoOrigem"));
                    if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    if(oe!=null){
                        Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                    
                        conn.setDB(dbOld);
                        conn.setDBCollection(collOld);
                    
                        SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy");
                        Date dtIncid = sdf2.parse(rs.getString("c6"));
                    
                        ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Serviço externo", rs.getString("c5"), rs.getString("c6") + " | " + rs.getString("c7") + " | " + rs.getString("c8") + 
                                                    " | " + rs.getString("c9") + " | " + rs.getString("c10") + " | " + rs.getString("c11"), dtIncid, 
                                                    "Em processamento", e); 
                    
                        pRH.setEstado("Iniciado");
                    
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                    
                        processado = true;
                    }    
                }
                       
                
                //SERVICO EXTERNO -tramitacao 
                else if(rs.getString("acao").equals("Serviço externo tramitação")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        //if(rs.getString("c6").equals("Concordo") || rs.getString("c6").equals("Autorizo")){ designacao = "Decisão"; descricao = rs.getString("c6"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Serviço Externo",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Em processamento");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && !rs.getString("local2").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                //SERVICO EXTERNO - fim 
                else if(rs.getString("acao").equals("Serviço externo fim")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Concluído"; String descricao="O processo está concluído";
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Serviço Externo",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Concluído");
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                
                
                //FÉRIAS criacao
                else if(rs.getString("acao").equals("Férias criação")){
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                        
                    Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar processo de férias", "");
                    lstAcoes.add(acao);
                    
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    lstColabs.add(rs.getString("local2"));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Férias",rs.getString("servicoOrigem"));
                    if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    Date dtIncid = null;
                    if(rs.getString("c10")!=null && !rs.getString("c10").isEmpty()){
                        dtIncid = sdf.parse(rs.getString("c10"));
                    }else if(rs.getString("c7")!=null && !rs.getString("c7").isEmpty()){
                        dtIncid = sdf.parse(rs.getString("c7"));
                    }else if(rs.getString("c8")!=null && !rs.getString("c8").isEmpty()){
                        dtIncid = sdf.parse(rs.getString("c7"));
                    }
                    
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    if(oe==null){
                        //pesquisar por nome
                        BasicDBList lor = new BasicDBList();
                        lor.add(new BasicDBObject("nome", rs.getString("c2")));
                        lor.add(new BasicDBObject("designEnt", rs.getString("c2")));
                        BasicDBObject or = new BasicDBObject("$or", lor);
                        oe = (BasicDBObject) conn.getDBCollection().findOne(or);
                    }
                    
                    Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    
                    
                    ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Férias", rs.getString("c5"), rs.getString("c7") + " | " + rs.getString("c8") + " | Nº de dias: " + rs.getString("c9") + 
                                                    " | " + rs.getString("c12"), dtIncid, "Em processamento", e); 
                    
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                
                    processado = true;
                }
                       
                
                //FERIAS - tramitacao ou devolucao
                else if(rs.getString("acao").equals("Férias tramitação") || rs.getString("acao").equals("Férias devolução")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao="";
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Férias",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Em processamento");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && !rs.getString("local2").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //FERIAS - fim
                else if(rs.getString("acao").equals("Férias fim")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Concluído"; String descricao="O processo está concluído";
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Férias",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Concluído");
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                     
                        processado = true;
                    }
                }
                
                
                
                //ESCLARECIMENTO DE DUVIDAS - criacao
                else if(rs.getString("acao").equals("Esclarecimento dúvidas RH criação")){

                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                        
                    Acao acao = new Acao(rs.getDate("data"), rs.getString("local1"), "Criar processo de esclarecimento de dúvudas", rs.getString("c7"));
                    lstAcoes.add(acao);
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstServs.add(rs.getString("servicoDestino")); }
                    String dados = rsi.getString("procdata");
                    
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Esclarecimento de dúvidas de recursos humanos",rs.getString("servicoOrigem"));
                    if(oi==null){
                       oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    //obter entidade 
                    String dbOld = conn.getDB().getName();
                    String collOld = conn.getDBCollection().getName();
                    conn.setDB("dbEntidadesGerais");
                    conn.setDBCollection("collEntidades");
                    
                    BasicDBObject q0 = new BasicDBObject("aplicacao", "COLABORADOR").append("ref", rs.getString("c1"));
                    BasicDBObject q1 = new BasicDBObject("$elemMatch", q0);
                    BasicDBObject oe = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("refExt", q1));
                    
                    Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                    
                    conn.setDB(dbOld);
                    conn.setDBCollection(collOld);
                    
                    
                    ProcessoRH pRH = new ProcessoRH("",rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <> (),
                                                    
                                                    "Esclarecimento de dúvidas RH", rs.getString("c5"), rs.getString("c7"), 
                                                    rs.getDate("data"), "Em processamento", e); 
                    pRH.setEstado("Iniciado");
                    
                    BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                    
                    processado = true;
                }
                
                //ESCLARECIMENTO DE DUVIDAS -tramitacao 
                else if(rs.getString("acao").equals("Esclarecimento dúvidas RH tramitação")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; String descricao=rs.getString("c6");
                        if(rs.getString("c1")!=null && (rs.getString("c1").equals("Concordo") || rs.getString("c1").equals("Autorizo"))){ designacao = "Decisão"; descricao = rs.getString("c1"); }
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual(rs.getString("local2"));
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Esclarecimento de dúvidas de recursos humanos",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        }  
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Em processamento");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        if(pRH.getLstColabs().indexOf(rs.getString("local2"))==-1 && !rs.getString("local2").isEmpty()){ pRH.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                //ESCLARECIMENTO DE DUVIDAS -fim 
                else if(rs.getString("acao").equals("Esclarecimento dúvidas RH fim")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        ProcessoRH pRH = Parsers.parseJsonPRHToJavaPRH(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Concluído"; String descricao="O processo está concluído";
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pRH.getLstAcoes().add(acao);
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        pRH.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        pRH.setLocalAnterior(rs.getString("local1"));
                        pRH.setLocalAtual("pre-arquivo");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        
                        if(rs.getString("local2")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pRH.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pRH.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Esclarecimento de dúvidas de recursos humanos",rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        } 
                        
                        pRH.setLstOE(oi.getLstOE()); pRH.setLstOO(oi.getLstOO()); pRH.setLstMA(oi.getLstMA());
                        pRH.setLstAI(oi.getLstAI()); pRH.setLstAF(oi.getLstAF()); pRH.setFuncao(oi.getFuncao()); pRH.setSubFuncao(oi.getSubFuncao());
                        pRH.setLstTitulos(oi.getLstTitulos()); pRH.setLstIndexTit(oi.getLstIndexacao()); pRH.setLstIndexacao(oi.getLstIndexacao());
                        pRH.setMef(oi.getCmef()); pRH.setAmec(oi.getCamec());
                        pRH.setDados(rsi.getString("procdata"));
                        pRH.setEstado("Concluído");
                        
                        if(pRH.getLstColabs().indexOf(rs.getString("local1"))==-1){ pRH.getLstColabs().add(rs.getString("local1")); }
                        
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pRH.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pRH.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        BasicDBObject onew = Parsers.parseJavaPRHToJsonPRH(pRH);
                        long pid = rs.getLong("pid");
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                
                
                //criar macroprocesso
                else if(rs.getString("acao").equals("criação macro processo")){
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    
                    String lu = rs.getString("c6");
                    String[] lstU = null;
                    if(!lu.isEmpty()){
                        lstU = lu.split(";");
                    }
                    List <String> lstColabs = new ArrayList <String> ();
                    List <String> lstServs = new ArrayList <> ();
                    
                    for(String s : lstU){
                        
                        if(!s.trim().isEmpty()){
                            lstColabs.add(s.trim());
                            lstIntColabs.add(new Intervencao(s.trim(),new Date()));
                            ResultSet rss = sqlconn.search("select name as servicoOrigem from profiles p, users u, userprofiles up where u.username = '"+s.trim()+"' and u.userid = up.userid and up.profileid = p.profileid and p.name like 'ADM@%'");
                            
                            if(rss.next()){
                                lstServs.add(rss.getString(1));
                                lstIntServs.add(new Intervencao(rss.getString(1),new Date()));
                            }    
                        }    
                            
                    }
            

                    ObjIndexacao oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
        
                    
                    List <Acao> lstAcoes = new ArrayList <> ();
            
                    Processo p = new Processo("Macroprocesso","",rs.getLong("pid"),0, rs.getTimestamp("data"), rs.getTimestamp("data"), lstColabs, lstServs, lstIntColabs, lstIntServs, "", lstAcoes, "", "", 
                                      oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(), oi.getLstTitulos(), oi.getLstIndexacao(),
                                      oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <MacroProcesso> ());
                    
                    String[] lc = rs.getString("c6").trim().split(";");
                    List <String> lstc = new ArrayList <String> (Arrays.asList(lc));
                    
                    MacroProcesso mp = new MacroProcesso(p,"","", rs.getString("c4"), rs.getString("c3"), rs.getString("local1"), rs.getString("c5"), lstc);
                        
                    BasicDBObject omp = Parsers.parseMacroProcessoToJson(mp);
                    
                    conn.getDBCollection().insert(omp);
                
                    processado = true;
                }
                
                
                //alterar macroprocesso
                else if(rs.getString("acao").equals("alterar macro processo")){
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    
                    String lu = rs.getString("c6");
                    String[] lstU = null;
                    if(!lu.isEmpty()){
                        lstU = lu.split(";");
                    }
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> ();
                    for(String s : lstU){
                        if(lstColabs.indexOf(s.trim())==-1){ lstColabs.add(s.trim()); }
                        lstIntColabs.add(new Intervencao(s.trim(),new Date()));
                        ResultSet rss = sqlconn.search("select name as servicoOrigem from profiles p, users u, userprofiles up where u.username = '"+s.replaceAll("'", "").trim()+"' and u.userid = up.userid and up.profileid = p.profileid and p.name like 'ADM@%'");
                        if(rss.next()){
                            if(lstServs.indexOf(s.trim())==-1){ lstServs.add(rss.getString(1)); }
                            lstIntServs.add(new Intervencao(rss.getString(1),new Date()));
                        }       
                    }
            

                    ObjIndexacao oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
        
                    List <Acao> lstAcoes = new ArrayList <> ();
            
                    Processo p = new Processo("Macroprocesso","",rs.getLong("pid"),0, rs.getDate("data"), rs.getDate("data"), lstColabs, lstServs, lstIntColabs, lstIntServs, "", lstAcoes, "", "", 
                                      oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(), oi.getLstTitulos(), oi.getLstIndexacao(),
                                      oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <MacroProcesso> ());
                    
                    String[] lc = rs.getString("c6").trim().split(";");
                    List <String> lstc = new ArrayList <String> (Arrays.asList(lc));
                    
                    MacroProcesso mp = new MacroProcesso(p,"","", rs.getString("c4"), rs.getString("c3"), rs.getString("local1"), rs.getString("c5"), lstc);
                        
                    BasicDBObject omp = Parsers.parseMacroProcessoToJson(mp);
                    
                    conn.getDBCollection().update(new BasicDBObject("processo.pid",mp.getProcesso().getPid()),omp);
                
                    processado = true;
                }
                
                
                
                
                
                
                
                
                
                
                /***********
                 * TRATAMENTO DO PG
                 **/
                
                //PG a partir da EE - criar
                else if(rs.getString("acao").equals("criação pg EE")){
                    
                    //tratar da EE
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","EE"));
                    BasicDBObject qf= new BasicDBObject("$and", land);
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(qf);
                    if(o!=null){
                        
                        EntradaExpediente ee = Parsers.jsonToEntradaExpediente(o);
                        ee.getProcesso().setLocalAnterior(ee.getProcesso().getLocalAtual());
                        ee.getProcesso().setLocalAtual("pre-arquivo");
                        
                        
                        BasicDBObject onew = Parsers.entradaExpedienteToJson(ee);
                        conn.getDBCollection().update(qf, onew, false, false, WriteConcern.SAFE);
                        
                    }
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                    Acao acao = new Acao(rs.getTimestamp("data"), rs.getString("local1"), "Criar processo genérico", "");
                    lstAcoes.add(acao);
                    
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ 
                        lstServs.add(rs.getString("servicoOrigem")); 
                    }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ 
                        lstServs.add(rs.getString("servicoDestino")); 
                    }
                    String dados = rsi.getString("procdata");
                    
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,rs.getString("c1"),rs.getString("servicoOrigem"));
                    if(oi==null){
                        if(rs.getString("c1")!=null && rs.getString("c2")!=null){ oi = obterObjIndexacaoByAreaSubArea(conn,rs.getString("c1"), rs.getString("c2")); }
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        }
                    } 
                    
                    ResultSet rsAdm = null;
                    //associar o servico ao titulo escolhidoo
                    if(!oi.getLstTitulos().isEmpty()){
                        oi.getLstTitulos().get(0).setServico(rs.getString("servicoOrigem"));
                        
                        if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                            rsAdm = sqlconn.search("select perfil from cma.cma_mapear_perfis_neutro where perfilNeutro = '"+rs.getString("servicoOrigem")+"'");
                            for(;rsAdm.next();){
                                oi.getLstTitulos().add(new Titulo(oi.getLstTitulos().get(0).getDesignacao(),rsAdm.getString(1)));
                            }
                        }
                            
                            
                        
                    }            
                    
                    //carregar os termos que foram escolhidos para o processo
                    if(rs.getString("c3")!=null){
                        String[] termos = rs.getString("c3").split(";");
                        for(String t : termos){
                        
                            if(!t.isEmpty()){
                                oi.getLstIndexacao().add(new Indexacao(t, rs.getString("servicoOrigem")));
         
                                if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                                 
                                    //obter a lista dos servicos do neutro
                                    if(rsAdm!=null){ rsAdm.first(); }
                                    //carregar o neutro
                                    oi.getLstIndexacao().add(new Indexacao(t, rsAdm.getString(1)));
                                    for(;rsAdm.next();){
                                        oi.getLstIndexacao().add(new Indexacao(t, rsAdm.getString(1)));
                                    }
                                }       
                            }
                        }
                    }        
                    
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    List <Entidade> lstEM = new ArrayList <> (); List <Documento> lstDocs = new ArrayList <> (); 
                    List <ProcessoGenerico> lstPAssoc = new ArrayList <> (); List <TarefaColaborativa> lstTC = new ArrayList <> ();
                    List <RequisicaoInterna> lstRI = new ArrayList <> ();
                    
                    ProcessoGenerico pg = new ProcessoGenerico(rsi.getString("num_processo"),rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local1"),
                                                    
                                                    "",rs.getString("c5"), rs.getString("c1"), rs.getString("c2"), rs.getString("c4"), lstEM, lstDocs, lstPAssoc, lstTC, lstRI,
                                                    new ArrayList <> (),    
                            
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec()); 
                        
                    
                    
                    BasicDBObject opg = Parsers.parseJavaPGToJsonPG(pg);
                    
                    conn.getDBCollection().insert(opg, WriteConcern.SAFE);
                            
                    processado = true;
                
                }
                
                
                //PG - criar
                else if(rs.getString("acao").equals("criação pg")){
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                    Acao acao = new Acao(rs.getTimestamp("data"), rs.getString("local1"), "Criar processo genérico", "");
                    lstAcoes.add(acao);
                    
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ 
                        lstServs.add(rs.getString("servicoOrigem")); 
                    }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ 
                        lstServs.add(rs.getString("servicoDestino")); 
                    }
                    String dados = rsi.getString("procdata");
                    
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,rs.getString("c1"),rs.getString("servicoOrigem"));
                    if(oi==null){
                        if(rs.getString("c1")!=null && rs.getString("c2")!=null){ oi = obterObjIndexacaoByAreaSubArea(conn,rs.getString("c1"), rs.getString("c2")); }
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        }
                    } 
                    
                    ResultSet rsAdm = null;
                    //associar o servico ao titulo escolhidoo
                    if(!oi.getLstTitulos().isEmpty()){
                        oi.getLstTitulos().get(0).setServico(rs.getString("servicoOrigem"));
                        
                        if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                            rsAdm = sqlconn.search("select perfil from cma.cma_mapear_perfis_neutro where perfilNeutro = '"+rs.getString("servicoOrigem")+"'");
                            for(;rsAdm.next();){
                                oi.getLstTitulos().add(new Titulo(oi.getLstTitulos().get(0).getDesignacao(),rsAdm.getString(1)));
                            }
                        }
                            
                            
                        
                    }            
                    
                    //carregar os termos que foram escolhidos para o processo
                    if(rs.getString("c3")!=null){
                        String[] termos = rs.getString("c3").split(";");
                        for(String t : termos){
                        
                            if(!t.isEmpty()){
                                oi.getLstIndexacao().add(new Indexacao(t, rs.getString("servicoOrigem")));
         
                                if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                                 
                                    //obter a lista dos servicos do neutro
                                    if(rsAdm!=null){ rsAdm.first(); }
                                    //carregar o neutro
                                    oi.getLstIndexacao().add(new Indexacao(t, rsAdm.getString(1)));
                                    for(;rsAdm.next();){
                                        oi.getLstIndexacao().add(new Indexacao(t, rsAdm.getString(1)));
                                    }
                                }       
                            }
                        }
                    }        
                    
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    List <Entidade> lstEM = new ArrayList <> (); List <Documento> lstDocs = new ArrayList <> (); 
                    List <ProcessoGenerico> lstPAssoc = new ArrayList <> (); List <TarefaColaborativa> lstTC = new ArrayList <> ();
                    List <RequisicaoInterna> lstRI = new ArrayList <> ();
                    
                    ProcessoGenerico pg = new ProcessoGenerico(rsi.getString("num_processo"),rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local1"),
                                                    
                                                    "",rs.getString("c5"), rs.getString("c1"), rs.getString("c2"), rs.getString("c4"), lstEM, lstDocs, lstPAssoc, lstTC, lstRI,
                                                    new ArrayList <> (),    
                            
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec()); 
                        
                    BasicDBObject opg = Parsers.parseJavaPGToJsonPG(pg);
                    
                    conn.getDBCollection().insert(opg, WriteConcern.SAFE);
                            
                    processado = true;
                
                }
                
                
                else if(rs.getString("acao").equals("criação PG de RI")){
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                    Acao acao = new Acao(rs.getTimestamp("data"), rs.getString("local1"), "Criar processo genérico a partir de RI", "");
                    lstAcoes.add(acao);
                    
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ 
                        lstServs.add(rs.getString("servicoOrigem")); 
                    }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ 
                        lstServs.add(rs.getString("servicoDestino")); 
                    }
                    String dados = rsi.getString("procdata");
                    
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,rs.getString("c1"),rs.getString("servicoOrigem"));
                    if(oi==null){
                        if(rs.getString("c1")!=null && rs.getString("c2")!=null){ oi = obterObjIndexacaoByAreaSubArea(conn,rs.getString("c1"), rs.getString("c2")); }
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        }
                    } 
                    
                    ResultSet rsAdm = null;
                    //associar o servico ao titulo escolhidoo
                    if(!oi.getLstTitulos().isEmpty()){
                        oi.getLstTitulos().get(0).setServico(rs.getString("servicoOrigem"));
                        
                        if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                            rsAdm = sqlconn.search("select perfil from cma.cma_mapear_perfis_neutro where perfilNeutro = '"+rs.getString("servicoOrigem")+"'");
                            for(;rsAdm.next();){
                                oi.getLstTitulos().add(new Titulo(oi.getLstTitulos().get(0).getDesignacao(),rsAdm.getString(1)));
                            }
                        }
                            
                            
                        
                    }            
                    
                    //carregar os termos que foram escolhidos para o processo
                    if(rs.getString("c3")!=null){
                        String[] termos = rs.getString("c3").split(";");
                        for(String t : termos){
                        
                            if(!t.isEmpty()){
                                oi.getLstIndexacao().add(new Indexacao(t, rs.getString("servicoOrigem")));
         
                                if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                                 
                                    //obter a lista dos servicos do neutro
                                    if(rsAdm!=null){ rsAdm.first(); }
                                    //carregar o neutro
                                    oi.getLstIndexacao().add(new Indexacao(t, rsAdm.getString(1)));
                                    for(;rsAdm.next();){
                                        oi.getLstIndexacao().add(new Indexacao(t, rsAdm.getString(1)));
                                    }
                                }       
                            }
                        }
                    }        
                    
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    List <Entidade> lstEM = new ArrayList <> (); List <Documento> lstDocs = new ArrayList <> (); 
                    List <ProcessoGenerico> lstPAssoc = new ArrayList <> (); List <TarefaColaborativa> lstTC = new ArrayList <> ();
                    List <RequisicaoInterna> lstRI = new ArrayList <> ();
                    
                    ProcessoGenerico pg = new ProcessoGenerico(rsi.getString("num_processo"),rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                                    lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                                                    
                                                    "",rs.getString("c4") + " | " + rs.getString("c5"), "", "", "", lstEM, lstDocs, lstPAssoc, lstTC, lstRI,
                                                    new ArrayList <> (),    
                            
                                                    oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                                    oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec()); 
                        
                    //obter RI
                    BasicDBObject ori = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("n2")));
                    RequisicaoInterna ri = Parsers.parseJsonRIToJavaRI(ori);
                    pg.getLstRIsAssociadas().add(ri);
                    
                    BasicDBObject opg = Parsers.parseJavaPGToJsonPG(pg);
                    
                    conn.getDBCollection().insert(opg, WriteConcern.SAFE);
                
                    
                    //carregar dados pg na ri
                    ri.setPgAssociado(pg);
                    BasicDBObject orii = Parsers.parseJavaRIToJsonRI(ri);
                    conn.getDBCollection().update(new BasicDBObject("processo.pid", rs.getLong("n2")), orii, false, false, WriteConcern.SAFE);
                    
                    
                    processado = true;
                
                }
                

                
                //PG -encaminhamento
                else if(rs.getString("acao").equals("encaminhar")){
                    try{
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){

                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        BasicDBObject p = (BasicDBObject) o.get("processo");
                        pg.setLocalAnterior(rs.getString("local1"));
                        pg.setLocalAtual(rs.getString("local2"));

                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        //pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; 
                        String descricao=rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                    }catch(Exception e){
                        String xx = e.toString();
                    }
                    
                }
                
                
                else if(rs.getString("acao").equals("encaminhar EE")){

                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo", "EE"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        
                        o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));

                        try{
                    
                        EntradaExpediente ee = Parsers.jsonToEntradaExpediente(o);

                        ee.getProcesso().setLocalAnterior(rs.getString("local1"));
                        ee.getProcesso().setLocalAtual(rs.getString("local2"));

                        if(!rs.getString("local2").isEmpty()){
                            ee.getProcesso().setLocalAnterior(rs.getString("local1"));
                            ee.getProcesso().setLocalAtual(rs.getString("local2"));
                        }    
                        ee.getProcesso().setDados(rsi.getString("procdata"));
                        ee.getProcesso().setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(ee.getProcesso().getLstColabs().indexOf(rs.getString("local1"))==-1){ ee.getProcesso().getLstColabs().add(rs.getString("local1")); }
                        if(ee.getProcesso().getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ ee.getProcesso().getLstColabs().add(rs.getString("local2")); }
                        
                        if(ee.getProcesso().getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ ee.getProcesso().getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(ee.getProcesso().getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ ee.getProcesso().getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        
                        if(rs.getString("local2")!=null){ ee.getProcesso().getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ ee.getProcesso().getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ ee.getProcesso().getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ ee.getProcesso().getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Encaminhamento"; 
                        String descricao=rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        ee.getProcesso().getLstAcoes().add(acao);
                        ee.getProcesso().setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        BasicDBObject onew = Parsers.entradaExpedienteToJson(ee);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", ee.getProcesso().getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        
                        }catch(Exception e){}
                    
                    }
                    
                
                }
                
                
                //PG -pre-arquivo
                else if(rs.getString("acao").equals("pre-arquivo")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        //Obter a acao
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        pg.setLocalAtual("pre-arquivo");
                        pg.setLocalAnterior(rs.getString("local1"));
                        
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Arquivar"; 
                        String descricao=rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf("pre-arquivo")==-1){ pg.getLstColabs().add("pre-arquivo"); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        List <ProcessoGenerico> lstPG = new ArrayList <> ();
                        
                        if(pg.getLstProcessosAssociados().size()>50){
                        
                            for(int i=0; i < 20; i++){
                                lstPG.add(pg.getLstProcessosAssociados().get(i));
                            }        
                        }        
                          
                        pg.setLstProcessosAssociados(lstPG)     ;
                                
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                //PG - arquivo
                else if(rs.getString("acao").equals("arquivo")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        //Obter a acao
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        pg.setLocalAtual("pre-arquivo");
                        pg.setLocalAnterior(rs.getString("local1"));
                        
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Arquivar"; 
                        String descricao=rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf("pre-arquivo")==-1){ pg.getLstColabs().add("pre-arquivo"); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                       
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                //RI - arquivar RI
                else if(rs.getString("acao").equals("arquivar ri")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        //Obter a acao
                        RequisicaoInterna ri = Parsers.parseJsonRIToJavaRI(o);
                        ri.setLocalAtual("pre-arquivo");
                        ri.setLocalAnterior(rs.getString("local1"));
                        
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Arquivar"; 
                        String descricao=rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        ri.getLstAcoes().add(acao);
                        
                        if(ri.getLstColabs().indexOf(rs.getString("local1"))==-1){ ri.getLstColabs().add(rs.getString("local1")); }
                        if(ri.getLstColabs().indexOf("pre-arquivo")==-1){ ri.getLstColabs().add("pre-arquivo"); }
                        
                        if(ri.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ ri.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(ri.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ ri.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        if(!rs.getString("local2").isEmpty()){
                            ri.setLocalAnterior(rs.getString("local1"));
                            ri.setLocalAtual(rs.getString("local2"));
                        }    
                        
                        
                        
                        if(rs.getString("local2")!=null){ ri.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ ri.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ ri.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ ri.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        ri.setDados(rsi.getString("procdata"));
                        ri.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        BasicDBObject onew = Parsers.parseJavaRIToJsonRI(ri);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", ri.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //PG - ordem de execução
                else if(rs.getString("acao").equals("ordem de execução")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Ordem de execução"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c2") + " | " + rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //PG - envio de SMS
                else if(rs.getString("acao").equals("SMS")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Envio de SMS"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c2");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                //PG - dados da operação urbanística
                else if(rs.getString("acao").equals("dados da operação urbanística")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Dados da operação urbanística"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c2") + " | " + rs.getString("c4");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                //PG - ocultar entidade
                else if(rs.getString("acao").equals("ocultar entidade")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Ocultar entidade"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c2") + " | " + rs.getString("c3") + " | " + rs.getString("c4");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        for(Entidade e : pg.getLstEM()){
                            if(e.getReferencia().equals(rs.getString("c1").trim())){
                                pg.getLstEM().remove(e);
                                break;
                            }
                        }

                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        processado = true;
                    }
                }
                
                //PG - ocultar representante legal
                else if(rs.getString("acao").equals("ocultar representante legal")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Ocultar representante legal"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c2") + " | " + rs.getString("c3") + " | " + rs.getString("c4");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //PG - averbamento
                else if(rs.getString("acao").equals("averbamento")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Averbamento"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c2");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //PG -junção de documentos
                else if(rs.getString("acao").equals("junção de documentos")){
                    try{
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Junção de documentos"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        //Documento d = new Documento(data, user, rs.getString("c1"));
                        //pg.getLstDocs().add(d);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        processado = true;
                    }
                    }catch(Exception e){}
                }
                
                
                //PG -ocultar documento
                else if(rs.getString("acao").equals("ocultar documento")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Ocultar documento"; 
                        String descricao=rs.getString("c1") + " | " + rs.getString("c2") + " | " + rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        ArrayList <Documento> al = new ArrayList <> (); 
                        for(Documento d : pg.getLstDocs()){
                            if(!d.getNome().equals(rs.getString("c2"))){
                                al.add(d);
                            }
                        }
                        
                        pg.setLstDocs(al);
                        
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                //DOGU - ficha obra
                else if(rs.getString("acao").equals("ficha obra criação")){
                    return true;
                }
                
                //DOGU - ficha obra alteracao
                else if(rs.getString("acao").equals("ficha obra alteração")){
                    return true;
                }

                //PG - associado a macro processo
                else if(rs.getString("acao").equals("associado a macro processo")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        
                        //obter o macroprocesso
                        BasicDBObject omp = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid", rs.getLong("c1")));
                        MacroProcesso mp = Parsers.parseJsonToMacroProcesso(omp);
                        mp.getProcesso().setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        //atualizar o macroprocesso
                        conn.getDBCollection().update(new BasicDBObject("processo.pid",mp.getProcesso().getPid()), omp);
                        
                        sqlconn.insert("insert into cma.cma_associacao_processos(pidFilho, pidPai, linha, data, utilizador, fidPai, fidFilho) "
                        + "values("+rs.getInt("pid")+","+rs.getInt("c1")+", '"+rs.getString("c3")+"','"+rs.getTimestamp("data")+"','"+rs.getString("local1")+"',"+rs.getInt("c2")+","+rs.getInt("fid")+")");
            
                        
                        processado = true;
                    }
                }
                
                
                //ENTRADA DE EXPEDIENTE - criacao entrada expediente
                else if(rs.getString("acao").equals("criação entrada expediente ")){
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                    Acao acao = new Acao(rs.getTimestamp("data"), rs.getString("local1"), "Criar entrada de expediente", "");
                    lstAcoes.add(acao);
                    
                    
                    if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ lstColabs.add(rs.getString("local1")); }
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ 
                        lstServs.add(rs.getString("servicoOrigem")); 
                    }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ 
                        lstServs.add(rs.getString("servicoDestino")); 
                    }
                    String dados = rsi.getString("procdata");
                    
                    
                    
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
                    
                    
                    ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,"Entrada de expediente",rs.getString("servicoOrigem"));
                    if(oi==null){
                        if(rs.getString("c1")!=null && rs.getString("c2")!=null){ oi = obterObjIndexacaoByAreaSubArea(conn,rs.getString("c1"), rs.getString("c2")); }
                        if(oi==null){
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                        }
                    } 
                    
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    
                    Date d1 = rs.getTimestamp("data");
                    
                    List <Entidade> lstEM = new ArrayList <> (); List <Documento> lstDocs = new ArrayList <> (); 
                    List <ProcessoGenerico> lstPAssoc = new ArrayList <> (); List <TarefaColaborativa> lstTC = new ArrayList <> ();
                    List <RequisicaoInterna> lstRI = new ArrayList <> ();
                    
                    Processo p = new Processo("EE",rsi.getString("num_processo"),rs.getLong("pid"),rs.getInt("fid"), d1, d1, lstColabs, 
                                              lstServs, lstIntColabs, lstIntServs, dados, lstAcoes, rs.getString("local1"), rs.getString("local2"),
                              
                                              oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                              oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(), new ArrayList <MacroProcesso> ());
                    
                    EntradaExpediente ee = new EntradaExpediente(p, "Entrada de expediente", rs.getString("c1"), rs.getTimestamp("data"),rs.getString("c8"), rs.getString("c2"), rs.getString("c4"), rs.getString("c6")); 
                  
                    
                    BasicDBObject oee = Parsers.entradaExpedienteToJson(ee);
                    
                    conn.getDBCollection().insert(oee, WriteConcern.SAFE);
                
                    processado = true;
                    
                    
                }
                
                
                //ENTRADA DE EXPEDIENTE - arquivar
                else if(rs.getString("acao").equals("arquivar")){

                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","EE"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                    
                        EntradaExpediente ee = Parsers.jsonToEntradaExpediente(o);
                        
                        List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); List <Acao> lstAcoes = new ArrayList <> ();
                    
                        Acao acao = new Acao(rs.getTimestamp("data"), rs.getString("local1"), "Arquivar entrada de expediente", "");
                        ee.getProcesso().getLstAcoes().add(acao);
                    
                    
                        if(rs.getString("local1")!=null && !rs.getString("local1").isEmpty()){ 
                            ee.getProcesso().getLstColabs().add(rs.getString("local1")); 
                        }
                            if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").isEmpty() && 
                                    lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null && 
                                    !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ 
                            ee.getProcesso().getLstServicos().add(rs.getString("servicoOrigem")); 
                        }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").isEmpty() && lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ 
                            ee.getProcesso().getLstServicos().add(rs.getString("servicoDestino")); 
                        }
                        ee.getProcesso().setDados(rsi.getString("procdata"));
                    
                    
                    
                        List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                        ee.getProcesso().getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ 
                            ee.getProcesso().getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); 
                        }
                    
                        ee.getProcesso().setLocalAtual("pre-arquivo");
                        ee.getProcesso().setLocalAnterior(rs.getString("local1"));
                        
                        BasicDBObject oee = Parsers.entradaExpedienteToJson(ee);
                    
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", ee.getProcesso().getPid()), oee);
                
                        processado = true;
                    
                    }
                    
                }
                
                
                
                
                //PG -associação documentos entrada de expediente
                else if(rs.getString("acao").equals("associação documentos entrada de expediente")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("c2")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));

                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        
                        //Documento d = new Documento(data, user, rs.getString("c5"));
                        //pg.getLstDocs().add(d);
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", rs.getLong("c2")), onew,false, false, WriteConcern.SAFE);
                        
                        //obter a EE para a atualizar
                        o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                        EntradaExpediente ee = Parsers.jsonToEntradaExpediente(o);
                        
                        ee.getProcesso().setLocalAnterior(rs.getString("local1"));
                        ee.getProcesso().setLocalAtual("pre-arquivo");
                        
                        BasicDBObject ooe = Parsers.entradaExpedienteToJson(ee);
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", rs.getLong("pid")), ooe,false, false, WriteConcern.SAFE);
                        
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ ee.getProcesso().getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ ee.getProcesso().getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        processado = true;
                    }
                }
                
                //PG -permissões
                else if(rs.getString("acao").equals("PG permissões")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));

                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Permissões de acesso ao processo"; 
                        String descricao="";
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
 
                
                //PG - data reuniao camara
                else if(rs.getString("acao").equals("PG  data reunião")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));

                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Renião de Câmara"; 
                        String descricao="";
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        processado = true;
                        
                    }
                }
                
                
                //PG -registar informacao e assinar
                else if(rs.getString("acao").equals("registar informação e assinar")){
                    try{
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));

                    if(o!=null){
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Registar informação (assinada)"; 
                        String descricao=rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        //Documento d = new Documento(data, user, rs.getString("c1"));
                        //pg.getLstDocs().add(d);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                    }catch(Exception e){}
                }
                
                
                //PG -registar informacao (entrada de expediente) - não está aa passar a informação - ver com o octávio
                else if(rs.getString("acao").equals("registar informação")){
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));
                    if(o!=null){
                         ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Registar informação"; 
                        String descricao=rs.getString("c3");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        //Documento d = new Documento(data, user, rs.getString("c1"));
                        //pg.getLstDocs().add(d);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                    }
                }
                
                
                //PG -associar entidade
                else if(rs.getString("acao").equals("associar entidade")){
                    long pid = rs.getLong("pid");
                    
                    BasicDBList land = new BasicDBList();
                    land.add(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    land.add(new BasicDBObject("processo.tipo","Processo Genérico"));
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("$and", land));

                    if(o!=null){
                        try{
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Associar entidade"; 
                        String descricao=rs.getString("c2");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        String db = conn.getDB().getName();
                        String coll = conn.getColl().getName();
                        
                        conn.setDB("dbEntidadesGerais");
                        conn.setDBCollection("collEntidades");
                        BasicDBObject oe = (BasicDBObject) conn.getColl().findOne(new BasicDBObject("ref", rs.getString("c1")));
                        Entidade e = MyParsers.parseEntidadeJSONtoObj(oe);
                        
                        pg.getLstEM().add(e);
                        
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        conn.setDB(db);
                        conn.setDBCollection(coll);
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject newo = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pid), newo,false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                }
                
                
                //PG - decisao
                else if(rs.getString("acao").equals("decisao")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        
                        //Obter a acao
                        
                        String designacao = ""; String descricao = "";
                        if(rs.getString("c1").contains(".pdf")){
                            designacao = rs.getString("c2") + " (assinada)";
                            descricao=rs.getString("c3");
                        }else{
                            designacao = rs.getString("c1");
                            descricao=rs.getString("c3");
                        }
                        
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                    
                }
                
                
                //PG - alterar dados do processo - TITULO
                else if(rs.getString("acao").equals("alterar dados do processo título")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        
                        //Obter a acao
                        String designacao = "Alterar título"; String descricao = "";
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,rs.getString("c1"),rs.getString("servicoOrigem"));
                        if(oi==null){
                            oi = obterObjIndexacaoByAreaSubArea(conn,rs.getString("c1"), rs.getString("c2"));
                            if(oi==null){
                                oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                     new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                    new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                            }
                        } 
                        
                        pg.setLstOE(oi.getLstOE()); pg.setLstOO(oi.getLstOO()); pg.setLstMA(oi.getLstMA()); pg.setLstAI(oi.getLstAI()); pg.setLstAF(oi.getLstAF());
                        pg.setArea(oi.getArea());pg.setSubArea(oi.getSubArea()); pg.setFuncao(oi.getFuncao()); pg.setSubFuncao(oi.getSubFuncao());
                        pg.setAmec(oi.getCamec()); pg.setMef(oi.getCmef());
                        
                        
                        //tratar do titulo e dos termos
                        //se insert
                        if(rs.getString("c7").equals("insert")){
                            pg.getLstTitulos().add(new Titulo(rs.getString("c1"), rs.getString("servicoOrigem")));
                            ResultSet rsAdm = null;
                            if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                                rsAdm = sqlconn.search("select perfil from cma.cma_mapear_perfis_neutro where perfilNeutro = '"+rs.getString("servicoOrigem")+"'");
                                for(;rsAdm.next();){
                                    pg.getLstTitulos().add(new Titulo(rs.getString("c1"), rsAdm.getString(1)));
                                }
                            }
                            
                            //repartir dos termos
                            String[] termos = rs.getString("c2").split(";");
                            for(String t : termos){
                                if(!t.isEmpty()){
                                    if(rs.getString("servicoOrigem").toLowerCase().contains("neutro")){
                                 
                                        //obter a lista dos servicos do neutro
                                        if(rsAdm!=null){ rsAdm.first(); }
                                        //carregar o neutro
                                        pg.getLstIndexacao().add(new Indexacao(t, rs.getString("servicoOrigem")));
                                        for(;rsAdm.next();){
                                            pg.getLstIndexacao().add(new Indexacao(t, rsAdm.getString(1)));
                                        }
                                    
                                    }else{
                                        pg.getLstIndexacao().add(new Indexacao(t, rs.getString("servicoOrigem")));
                                    }
                                }    
                            }    
                        }else{
                            for(int i=0; i < pg.getLstTitulos().size(); i++){
                                Titulo t = pg.getLstTitulos().get(i);
                                if(t.getServico()!=null){
                                    if(t.getServico().equals(rs.getString("servicoOrigem"))){
                                        pg.getLstTitulos().set(i, new Titulo(rs.getString("c1"), rs.getString("servicoOrigem")));
                                    }
                                }else{
                                    pg.getLstTitulos().set(i, new Titulo(rs.getString("c1"), rs.getString("servicoOrigem")));
                                }    
                            }
                            //repartir dos termos   
                            String[] termos = null;
                            if(!rs.getString("c2").isEmpty()){
                                termos = rs.getString("c2").split(";");
                            }
                            
                            //remover os termos do servico
                            ArrayList <Indexacao> li = new ArrayList <> ();
                            for(int i=0; i < pg.getLstIndexacao().size(); i++){
                                Indexacao ind = pg.getLstIndexacao().get(i);
                                if(!ind.getDesignacao().isEmpty() && !ind.getServico().equals(rs.getString("servicoOrigem"))){
                                    li.add(ind);
                                }
                            }
                            
                            pg.setLstIndexacao(li);
                            
                            //add termos
                            if(termos!=null){
                                for(String t : termos){
                                    pg.getLstIndexacao().add(new Indexacao(t, rs.getString("servicoOrigem")));
                                }
                            }    
                        }
                        
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        
                        }catch(Exception e){}  
                        
                    }
                
                }
                
                
                
                
                //PG - alterar dados do processo - TERMOS
                else if(rs.getString("acao").equals("alterar dados do processo termos")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        
                        //Obter a acao
                        String designacao = "Alterar termos"; String descricao = "";
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        
                        ObjIndexacao oi = obterObjIndexacaoByTituloServico(conn,rs.getString("c2"),rs.getString("servicoOrigem"));
                        if(oi==null){
                           
                            oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                            
                        } 
                        
                        pg.setLstOE(oi.getLstOE()); pg.setLstOO(oi.getLstOO()); pg.setLstMA(oi.getLstMA()); pg.setLstAI(oi.getLstAI()); pg.setLstAF(oi.getLstAF());
                        pg.setArea(oi.getArea());pg.setSubArea(oi.getSubArea()); pg.setFuncao(oi.getFuncao()); pg.setSubFuncao(oi.getSubFuncao());
                        pg.setAmec(oi.getCamec()); pg.setMef(oi.getCmef());
                        
                        /*
                            if(rs.getString("c1")!=null){
                                for(int i=0; i < pg.getLstTitulos().size(); i++){
                                    Titulo t = pg.getLstTitulos().get(i);
                                    if(t.getServico().equals(rs.getString("servicoOrigem"))){
                                        pg.getLstTitulos().set(i, new Titulo(rs.getString("c1"), rs.getString("servicoOrigem")));
                                    }    
                                }
                            }   
                        */    
                            //repartir dos termos
                            String[] termos = null;
                            if(!rs.getString("c2").isEmpty()){
                                termos = rs.getString("c2").split(";");
                            }
                            
                            if(termos!=null){
                                ArrayList <Indexacao> la = new ArrayList <> ();
                                //remover os termos do servico
                                for(Indexacao ind : pg.getLstIndexacao()){
                                    if(!ind.getDesignacao().isEmpty() && !ind.getServico().equals(rs.getString("servicoOrigem"))){
                                        la.add(ind);
                                    }
                                }
                                pg.setLstIndexacao(la);
                                //add termos
                                if(termos!=null){
                                    for(String t : termos){
                                        pg.getLstIndexacao().add(new Indexacao(t, rs.getString("servicoOrigem")));
                                    }
                                }    
                            }    
                        
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                
                
                //Alterar dados do processo - OUTROS
                else if(rs.getString("acao").equals("alterar dados do processo outros")){
                    
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        String designacao = "Alterar outros dados do processo"; String descricao = "Alteração do assunto e/ou enquadramento do processo." ;
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        pg.setAssunto(rs.getString("c5"));
                        pg.setEnquadramento(rs.getString("c3"));
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }    
                    
                }
                
                
                
                //PG - expedir
                else if(rs.getString("acao").equals("TC interna expedir")){
                    try{
                    //Obter a acao
                    List <Acao> lstAcoes = new ArrayList <> ();
                    String designacao = "TC"; String descricao = rs.getString("c3"); 
                    Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                    Acao acao = new Acao(data, user, designacao, descricao);
                    lstAcoes.add(acao);
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); 
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    
                    if(lstColabs.indexOf(rs.getString("local1"))==-1){ lstColabs.add(rs.getString("local1")); }
                    if(lstColabs.indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ lstColabs.add(rs.getString("local2")); }
                        
                    if(lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ lstServs.add(rs.getString("servicoDestino")); }
                        
                        
                    if(rs.getString("local2")!=null){ lstIntColabs.add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                    else if(rs.getString("local1")!=null){ lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                    
                    ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Tarefa colaborativa",rs.getString("servicoOrigem"));
                    if(oi==null){
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                    }
                    
 
                    TarefaColaborativa tc = new TarefaColaborativa("", rs.getLong("n1"), rs.getInt("n2"), rs.getTimestamp("data"), rs.getTimestamp("data"), lstColabs, lstServs, 
			       lstIntColabs, lstIntServs, rsi.getString("procdata"), lstAcoes, rs.getString("local1"), rs.getString("local2"),
                               
                               oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                               oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(),
                               new ArrayList <MacroProcesso> (),
                               
                               rs.getString("c4")+" | "+rs.getString("c3"), new ArrayList <Documento> (), "Iniciada", rs.getString("c1"));    
                        
                
                    BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc);
                    
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    
                
                }
                
                
                //PG - EMAIL
                else if(rs.getString("acao").equals("EMAIL")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        String designacao = "Envio de e-mail"; String descricao = "Dest. " + rs.getString("c1") + " | Assunto: " + rs.getString("c2") + " | " + rs.getString("c3");
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                //PG - dar conhecimento
                else if(rs.getString("acao").equals("dar conhecimento")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        String designacao = "Dar conhecimento"; String descricao = rs.getString("local2"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        
                           
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg); 
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                
                //TC EXTERNA - criar enviar
                else if(rs.getString("acao").equals("TC externa enviar")){
                    try{
                    //Obter a acao
                    List <Acao> lstAcoes = new ArrayList <> ();
                    String designacao = "TC"; String descricao = rs.getString("c3"); 
                    Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                    Acao acao = new Acao(data, user, designacao, descricao);
                    lstAcoes.add(acao);
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); 
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    
                    if(lstColabs.indexOf(rs.getString("local1"))==-1){ lstColabs.add(rs.getString("local1")); }
                    if(lstColabs.indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ lstColabs.add(rs.getString("local2")); }
                        
                    if(lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ lstServs.add(rs.getString("servicoDestino")); }
                        
                        
                    if(rs.getString("local2")!=null){ lstIntColabs.add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                    else if(rs.getString("local1")!=null){ lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                    
                    ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Tarefa colaborativa",rs.getString("servicoOrigem"));
                    if(oi==null){
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                    }
                    
 
                    TarefaColaborativa tc = new TarefaColaborativa("", rs.getLong("pid"), rs.getInt("fid"), rs.getTimestamp("data"), rs.getTimestamp("data"), lstColabs, lstServs, 
			       lstIntColabs, lstIntServs, rsi.getString("procdata"), lstAcoes, rs.getString("local1"), rs.getString("local2"),
                               
                               oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                               oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(),
                               new ArrayList <MacroProcesso> (),
                               
                               rs.getString("c4")+" | "+rs.getString("c3"), new ArrayList <Documento> (), "Iniciada", rs.getString("c1"));    
                        
                
                    BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc);
                    
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                    
                    processado = true;
                    }catch(Exception e){}  
                }
                
                
                
                //TC EXTERNA - encaminhar
                else if(rs.getString("acao").equals("TC externa encaminhar")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        TarefaColaborativa tc = Parsers.parseJsonTCToJavaTC(o);
                        
                        //Obter a acao
                        String designacao = "Encaminhar"; String descricao = rs.getString("c3"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        tc.getLstAcoes().add(acao);
                    
                        tc.setLocalAnterior(rs.getString("local1"));
                        tc.setLocalAtual(rs.getString("local2"));

                        if(!rs.getString("local2").isEmpty()){
                            tc.setLocalAnterior(rs.getString("local1"));
                            tc.setLocalAtual(rs.getString("local2"));
                        }    
                        tc.setDados(rsi.getString("procdata"));
                        tc.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(tc.getLstColabs().indexOf(rs.getString("local1"))==-1){ tc.getLstColabs().add(rs.getString("local1")); }
                        if(tc.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ tc.getLstColabs().add(rs.getString("local2")); }
                        
                        if(tc.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(tc.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        
                        if(rs.getString("local2")!=null){ tc.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ tc.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
     
                        BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc); 
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", tc.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                
                //TC EXTERNA - terminar
                else if(rs.getString("acao").equals("TC externa terminar")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        TarefaColaborativa tc = Parsers.parseJsonTCToJavaTC(o);
                        
                        //Obter a acao
                        String designacao = "Terminar"; String descricao = rs.getString("c3"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        tc.getLstAcoes().add(acao);
                    
                        tc.setLocalAnterior(rs.getString("local1"));
                        tc.setLocalAtual("pre-arquivo");

                        tc.setDados(rsi.getString("procdata"));
                        tc.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(tc.getLstColabs().indexOf(rs.getString("local1"))==-1){ tc.getLstColabs().add(rs.getString("local1")); }
                        
                        if(tc.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoOrigem")); }
                        
                        else if(rs.getString("local1")!=null){ tc.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    
     
                        BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc); 
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", tc.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                
                
                //TC EXTERNA + INTERNA - guardar
                else if(rs.getString("acao").equals("TC externa guardar") || rs.getString("acao").equals("TC interna guardar")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        TarefaColaborativa tc = Parsers.parseJsonTCToJavaTC(o);
                        
                        //Obter a acao
                        String designacao = "Guardar"; String descricao = rs.getString("c3"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        tc.getLstAcoes().add(acao);
                    
                        BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc); 
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", tc.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                //TC EXTERNA TC externa junção de documento
                else if(rs.getString("acao").equals("TC externa junção de documento")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        TarefaColaborativa tc = Parsers.parseJsonTCToJavaTC(o);
                        
                        //Obter a acao
                        String designacao = "Junção de documento"; String descricao = rs.getString("c3"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        tc.getLstAcoes().add(acao);
                    
                        BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc); 
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", tc.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                
                //TC INTERNA - terminar
                else if(rs.getString("acao").equals("TC interna terminar")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        TarefaColaborativa tc = Parsers.parseJsonTCToJavaTC(o);
                        
                        //Obter a acao
                        String designacao = "Encaminhar"; String descricao = rs.getString("c3"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        tc.getLstAcoes().add(acao);
                    
                        tc.setLocalAnterior(rs.getString("local1"));
                        tc.setLocalAtual(rs.getString("local2"));

                            tc.setLocalAnterior(rs.getString("local1"));
                            tc.setLocalAtual("pre-arquivo");
                    
                        tc.setDados(rsi.getString("procdata"));
                        tc.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(tc.getLstColabs().indexOf(rs.getString("local1"))==-1){ tc.getLstColabs().add(rs.getString("local1")); }
                        if(tc.getLstColabs().indexOf("pre-arquivo")==-1){ tc.getLstColabs().add("pre-arquivo"); }
                        
                        if(tc.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(tc.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        
                        tc.getLstIntColabs().add(new Intervencao("pre-arquivo", rs.getTimestamp("data"))); 
                        tc.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data")));
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
     
                        BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc); 
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", tc.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                //TC INTERNA - expedir documentos
                else if(rs.getString("acao").equals("TC interna expedir documentos")){
                    
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                   
                    if(o!=null){
                        try{
                        TarefaColaborativa tc = Parsers.parseJsonTCToJavaTC(o);
                        
                        //Obter a acao
                        String designacao = "Expedir documentos"; String descricao = "Processo origem: " + rs.getString("c1"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        tc.getLstAcoes().add(acao);
                    
                        tc.setLocalAnterior(rs.getString("local1"));
                        tc.setLocalAtual(rs.getString("local2"));

                        if(!rs.getString("local2").isEmpty()){
                            tc.setLocalAnterior(rs.getString("local1"));
                            tc.setLocalAtual(rs.getString("local2"));
                        }    
                        tc.setDados(rsi.getString("procdata"));
                        tc.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(tc.getLstColabs().indexOf(rs.getString("local1"))==-1){ tc.getLstColabs().add(rs.getString("local1")); }
                        if(tc.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ tc.getLstColabs().add(rs.getString("local2")); }
                        
                        if(tc.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(tc.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        
                        if(rs.getString("local2")!=null){ tc.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ tc.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
     
                        BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc); 
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", tc.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                     
                    }
                
                }
                
                //TC INTERNA 
                else if(rs.getString("acao").equals("TC interna")){
                    try{
                    //Obter a acao
                    List <Acao> lstAcoes = new ArrayList <> ();
                    String designacao = "TC"; String descricao = rs.getString("c3"); 
                    Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                    Acao acao = new Acao(data, user, designacao, descricao);
                    lstAcoes.add(acao);
                    
                    List <String> lstColabs = new ArrayList <> (); List <String> lstServs = new ArrayList <> (); 
                    List <Intervencao> lstIntColabs = new ArrayList <> (); List <Intervencao> lstIntServs = new ArrayList <> ();
                    
                    if(lstColabs.indexOf(rs.getString("local1"))==-1){ lstColabs.add(rs.getString("local1")); }
                    if(lstColabs.indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ lstColabs.add(rs.getString("local2")); }
                        
                    if(lstServs.indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ lstServs.add(rs.getString("servicoOrigem")); }
                    if(lstServs.indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ lstServs.add(rs.getString("servicoDestino")); }
                        
                        
                    if(rs.getString("local2")!=null){ lstIntColabs.add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                    else if(rs.getString("local1")!=null){ lstIntColabs.add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                    if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                    if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ lstIntServs.add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                    
                    ObjIndexacao oi = this.obterObjIndexacaoByTituloServico(conn, "Tarefa colaborativa",rs.getString("servicoOrigem"));
                    if(oi==null){
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                    }
                    
                    String c4 = rs.getString("c4");
                    if(rs.getString("c4").length()>500){
                       c4 = rs.getString("c4").substring(0, 500);
                    }
                               
                               
                    TarefaColaborativa tc = new TarefaColaborativa("", rs.getLong("pid"), rs.getInt("fid"), rs.getTimestamp("data"), rs.getTimestamp("data"), lstColabs, lstServs, 
			       lstIntColabs, lstIntServs, rsi.getString("procdata"), lstAcoes, rs.getString("local1"), rs.getString("local2"),
                               
                               oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                               oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(),
                               new ArrayList <MacroProcesso> (),
                               
                               c4 +" | "+rs.getString("c3"), new ArrayList <Documento> (), "Iniciada", rs.getString("c1"));    
                        
                
                    BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc);
                    
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                    
                    processado = true;
                    }catch(Exception e){
                        String xx = e.toString();
                    }  
                }
                
                
                //TC INTERNA - encaminhar
                else if(rs.getString("acao").equals("TC interna encaminhar")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        TarefaColaborativa tc = Parsers.parseJsonTCToJavaTC(o);
                        
                        //Obter a acao
                        String designacao = "Encaminhar"; String descricao = rs.getString("c3"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        tc.getLstAcoes().add(acao);
                    
                        tc.setLocalAnterior(rs.getString("local1"));
                        tc.setLocalAtual(rs.getString("local2"));

                        if(!rs.getString("local2").isEmpty()){
                            tc.setLocalAnterior(rs.getString("local1"));
                            tc.setLocalAtual(rs.getString("local2"));
                        }    
                        tc.setDados(rsi.getString("procdata"));
                        tc.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(tc.getLstColabs().indexOf(rs.getString("local1"))==-1){ tc.getLstColabs().add(rs.getString("local1")); }
                        if(tc.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ tc.getLstColabs().add(rs.getString("local2")); }
                        
                        if(tc.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(tc.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ tc.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        
                        if(rs.getString("local2")!=null){ tc.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ tc.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
     
                        BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc); 
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", tc.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                
                //TC INTERNA - anexos
                else if(rs.getString("acao").equals("TC interna anexos")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        TarefaColaborativa tc = Parsers.parseJsonTCToJavaTC(o);
                        
                        //Obter a acao
                        String designacao = "Juntar anexos"; String descricao = rs.getString("c3"); 
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); 
                        Acao acao = new Acao(data, user, designacao, descricao);
                        tc.getLstAcoes().add(acao);
                    
                        
                        tc.setDados(rsi.getString("procdata"));
                        tc.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(rs.getString("local1")!=null){ tc.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ tc.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        
                        BasicDBObject onew = Parsers.parseJavaTCToJsonTC(tc); 
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", tc.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                
                }
                
                
                //PG -registar e carregar informacao
                else if(rs.getString("acao").equals("registar e carregar informação")){
                    
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Registar e carregar informação"; 
                        String descricao=rs.getString("c3") + "(" + rs.getString("c1") + ")";
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pg.setDados(rsi.getString("procdata"));
                        //Documento d = new Documento(data, user, rs.getString("c1"));
                        //pg.getLstDocs().add(d);
                        
                        if(!rs.getString("local2").isEmpty()){
                            pg.setLocalAnterior(rs.getString("local1"));
                            pg.setLocalAtual(rs.getString("local2"));
                        }    
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(pg.getLstColabs().indexOf(rs.getString("local1"))==-1){ pg.getLstColabs().add(rs.getString("local1")); }
                        if(pg.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ pg.getLstColabs().add(rs.getString("local2")); }
                        
                        if(pg.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(pg.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ pg.getLstServicos().add(rs.getString("servicoDestino")); }
                        
                        
                        
                        if(rs.getString("local2")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ pg.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ pg.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        
                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                    
                }
                
                
                //PG -registar e voltar
                else if(rs.getString("acao").equals("registar e voltar")){
                    
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        ProcessoGenerico pg = Parsers.parseJsonPGToJavaPG(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Registar e voltar"; 
                        String descricao=rs.getString("c3") + "(" + rs.getString("c1") + ")";
                        Acao acao = new Acao(data, user, designacao, descricao);
                        pg.getLstAcoes().add(acao);
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        pg.setDados(rsi.getString("procdata"));
                        //Documento d = new Documento(data, user, rs.getString("c1"));
                        //pg.getLstDocs().add(d);
                        
                           
                        pg.setDados(rsi.getString("procdata"));
                        pg.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        

                        
                        BasicDBObject onew = Parsers.parseJavaPGToJsonPG(pg);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", pg.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                    
                }
                
                
                //RI - criacao
                else if(rs.getString("acao").equals("criação ri") || rs.getString("acao").equals("criação ri oficina")){
                    try{
                    String descricao = "Tipo: " + rs.getString("c2") + " | " + rs.getString("c4");
                    String aux = "";
                    
                    if(rs.getString("acao").equals("criação ri")){ aux = "Criação de RI"; }
                    else{ aux = "Criação de RI de oficina"; }
                    
                    List <Acao> lstAcoes = new ArrayList <> (); Acao acao = new Acao(rs.getTimestamp("data"), rs.getString("c1"), aux, descricao);
                    lstAcoes.add(acao);
                    
                    List <String> lstColabs = new ArrayList <> (); lstColabs.add(rs.getString("c1"));
                    List <String> lstServs = new ArrayList <> (); lstServs.add(rs.getString("servicoOrigem"));
                            
                    List <Intervencao> lstIntColabs = new ArrayList <> (); lstIntColabs.add(new Intervencao(rs.getString("c1"), rs.getTimestamp("data")));
                    List <Intervencao> lstIntServs = new ArrayList <> (); lstIntServs.add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data")));
                            
                    List <MacroProcesso> lstMP = new ArrayList <> (); List <Produto> lstProdutos = new ArrayList <> (); List <Documento> lstDocs = new ArrayList <> ();
                    
                    ObjIndexacao oi=null;
                    if(rs.getString("c2").toLowerCase().equals("material")){
                        oi = this.obterObjIndexacaoByTituloServico(conn, "Requisição interna de material", rs.getString("servicoOrigem"));
                    }else if(rs.getString("c2").toLowerCase().equals("serviço/material não stockável")){
                       oi = this.obterObjIndexacaoByTituloServico(conn, "Requisição interna de serviços e de material não stockável", rs.getString("servicoOrigem"));
                    }else if(rs.getString("c2").toLowerCase().equals("imobilizado")){
                       oi = this.obterObjIndexacaoByTituloServico(conn, "Requisição interna de material imobilizado", rs.getString("servicoOrigem"));   
                    }else{
                        oi = new ObjIndexacao(new ArrayList <ObjetivoEstrategico> (),new ArrayList <ObjetivoOperacional> (),new ArrayList <MacroArea> (),
                                 new ArrayList <AreaIntervencao> (),new ArrayList <AreaFuncional> (), "","","","", new ArrayList <Titulo> (), "","","",
                                 new ArrayList <Indexacao> (), new ClassificacaoAMEC("",""), new ClassificacaoMEF("",""));
                    }
                    
                    RequisicaoInterna ri = new RequisicaoInterna(rs.getString("c7"), rs.getLong("pid"), rs.getInt("fid"), rs.getTimestamp("data"), rs.getTimestamp("data"), 
                                lstColabs, lstServs, lstIntColabs, lstIntServs, rsi.getString("procdata"), 
                                lstAcoes, rs.getString("local1"), rs.getString("local1"),
                
                                oi.getLstOE(), oi.getLstOO(), oi.getLstMA(), oi.getLstAI(), oi.getLstAF(), oi.getFuncao(), oi.getSubFuncao(),
                                oi.getLstTitulos(), oi.getLstIndexacao(), oi.getLstIndexacao(), oi.getCmef(), oi.getCamec(),lstMP,
                
                                rs.getString("c6"), rs.getString("c2"), rs.getString("c4"), rs.getString("c8"), "", "Iniciada",
                                lstProdutos, lstDocs, null);
                        
                        
                        
                    BasicDBObject onew = Parsers.parseJavaRIToJsonRI(ri);
                        
                    conn.getDBCollection().insert(onew, WriteConcern.SAFE);
                    
                    processado = true;
                    }catch(Exception e){}  
                }
                
                
                //RI - encaminhamento
                else if(rs.getString("acao").equals("encaminhar ri") || rs.getString("acao").equals("encaminhar ri nao concordo") || rs.getString("acao").equals("encaminhar ri concordo")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        
                        RequisicaoInterna ri = Parsers.parseJsonRIToJavaRI(o);
                        
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "RI encaminhada"; 
                        String descricao=rs.getString("local2");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        ri.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            ri.setLocalAnterior(rs.getString("local1"));
                            ri.setLocalAtual(rs.getString("local2"));
                        }    
                        ri.setDados(rsi.getString("procdata"));
                        ri.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(ri.getLstColabs().indexOf(rs.getString("local1"))==-1){ ri.getLstColabs().add(rs.getString("local1")); }
                        if(ri.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ ri.getLstColabs().add(rs.getString("local2")); }
                        
                        if(ri.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ ri.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(ri.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ ri.getLstServicos().add(rs.getString("servicoDestino")); }

                        
                        
                        if(rs.getString("local2")!=null){ ri.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ ri.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ ri.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ ri.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        ri.setEstado("Em processamento.");
                        
                        BasicDBObject onew = Parsers.parseJavaRIToJsonRI(ri); 
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", ri.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }else{
                        sqlconn.insert("insert into cma.cma_acoes_processo(pid, fid, acao, local1, local2, data, c1,c2,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13, n1, c14, n2, comentario, c15, c16, c17, c18, "
                                + "c19, c20, c21, c22, c23, c24, c25, c26, c27, servicoOrigem, servicoDestino, n3) "
                                    + "values ("+rs.getLong("pid")+","+rs.getLong("fid")+",'"+rs.getString("acao")+"','"+rs.getString("local1")+"','"+rs.getString("local2")+"','"+rs.getString("data")+"','"+rs.getString("c1")+"','"+
                                rs.getString("c2")+"','"+rs.getString("c3")+"','"+rs.getString("c4")+"','"+rs.getString("c5")+"','"+rs.getString("c6")+"','"+rs.getString("c7")+"','"+rs.getString("c8")+"','"+
                                rs.getString("c10")+"','"+rs.getString("c9")+"','"+rs.getString("c11")+"','"+rs.getString("c12")+"',"+rs.getInt("n1")+",'"+rs.getString("c14")+"',"+rs.getInt("n2")+
                                ",'"+rs.getString("comentario")+"','"+rs.getString("c15")+"','"+rs.getString("c16")+"','"+rs.getString("c17")+"','"+rs.getString("c18")+"','"+rs.getString("c19")+"','"+rs.getString("c20")+
                                "','"+rs.getString("c21")+"','"+rs.getString("c22")+"','"+rs.getString("c23")+"','"+rs.getString("c24")+"','"+rs.getString("c25")+"','"+rs.getString("c26")+"','"+rs.getString("c27")+
                                    "','"+rs.getString("servicoOrigem")+"','"+rs.getString("servicoDestino")+"',"+rs.getInt("n3")+")");
                    }
                }
                
                
                //RI - enviar resposta
                else if(rs.getString("acao").equals("enviar resposta ri")){
                    
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                
                    if(o!=null){
                        try{
                        
                        RequisicaoInterna ri = Parsers.parseJsonRIToJavaRI(o);
                        
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "RI encaminhada"; 
                        String descricao=rs.getString("local2");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        ri.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            ri.setLocalAnterior(rs.getString("local1"));
                            ri.setLocalAtual(rs.getString("local2"));
                        }    
                        ri.setDados(rsi.getString("procdata"));
                        ri.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(ri.getLstColabs().indexOf(rs.getString("local1"))==-1){ ri.getLstColabs().add(rs.getString("local1")); }
                        if(ri.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ ri.getLstColabs().add(rs.getString("local2")); }
                        
                        if(ri.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ ri.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(ri.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ ri.getLstServicos().add(rs.getString("servicoDestino")); }

                        
                        
                        if(rs.getString("local2")!=null){ ri.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ ri.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ ri.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ ri.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        ri.setEstado("Em processamento.");
                        
                        BasicDBObject onew = Parsers.parseJavaRIToJsonRI(ri); 
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", ri.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }else{
                        sqlconn.insert("insert into cma.cma_acoes_processo(pid, fid, acao, local1, local2, data, c1,c2,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13, n1, c14, n2, comentario, c15, c16, c17, c18, "
                                + "c19, c20, c21, c22, c23, c24, c25, c26, c27, servicoOrigem, servicoDestino, n3) "
                                    + "values ("+rs.getLong("pid")+","+rs.getLong("fid")+",'"+rs.getString("acao")+"','"+rs.getString("local1")+"','"+rs.getString("local2")+"','"+rs.getString("data")+"','"+rs.getString("c1")+"','"+
                                rs.getString("c2")+"','"+rs.getString("c3")+"','"+rs.getString("c4")+"','"+rs.getString("c5")+"','"+rs.getString("c6")+"','"+rs.getString("c7")+"','"+rs.getString("c8")+"','"+
                                rs.getString("c10")+"','"+rs.getString("c9")+"','"+rs.getString("c11")+"','"+rs.getString("c12")+"',"+rs.getInt("n1")+",'"+rs.getString("c14")+"',"+rs.getInt("n2")+
                                ",'"+rs.getString("comentario")+"','"+rs.getString("c15")+"','"+rs.getString("c16")+"','"+rs.getString("c17")+"','"+rs.getString("c18")+"','"+rs.getString("c19")+"','"+rs.getString("c20")+
                                "','"+rs.getString("c21")+"','"+rs.getString("c22")+"','"+rs.getString("c23")+"','"+rs.getString("c24")+"','"+rs.getString("c25")+"','"+rs.getString("c26")+"','"+rs.getString("c27")+
                                    "','"+rs.getString("servicoOrigem")+"','"+rs.getString("servicoDestino")+"',"+rs.getInt("n3")+")");
                    }
                }
                
                
                //RI - carregar RI no ERP
                else if(rs.getString("acao").equals("carregar ri")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        RequisicaoInterna ri = Parsers.parseJsonRIToJavaRI(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "RI carregada no ERP"; 
                        String descricao=rs.getString("c1");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        ri.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            ri.setLocalAnterior(rs.getString("local1"));
                            ri.setLocalAtual(rs.getString("local2"));
                        }    
                        ri.setDados(rsi.getString("procdata"));
                        ri.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        ri.setEstado("Carregada no ERP");
                        
                        BasicDBObject onew = Parsers.parseJavaRIToJsonRI(ri);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", ri.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                }
                
                
                //RI - observacoes
                else if(rs.getString("acao").equals("enviar obs ri")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        RequisicaoInterna ri = Parsers.parseJsonRIToJavaRI(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "Observações da RI"; 
                        String descricao=rs.getString("c1");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        ri.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            ri.setLocalAnterior(rs.getString("local1"));
                            ri.setLocalAtual(rs.getString("local2"));
                        }    
                        ri.setDados(rsi.getString("procdata"));
                        ri.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        ri.setEstado("Carregada no ERP");
                        
                        BasicDBObject onew = Parsers.parseJavaRIToJsonRI(ri);
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", ri.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                }
                
                //RI - arquivar RI
                else if(rs.getString("acao").equals("arquivar ri")){
                    BasicDBObject o = (BasicDBObject) conn.getDBCollection().findOne(new BasicDBObject("processo.pid",rs.getLong("pid")));
                    if(o!=null){
                        try{
                        RequisicaoInterna ri = Parsers.parseJsonRIToJavaRI(o);
                        //Obter a acao
                        Date data = rs.getTimestamp("data"); String user = rs.getString("local1"); String designacao  = "RI arquivada."; 
                        String descricao=rs.getString("local2");
                        Acao acao = new Acao(data, user, designacao, descricao);
                        ri.getLstAcoes().add(acao);
                        
                        if(!rs.getString("local2").isEmpty()){
                            ri.setLocalAnterior(rs.getString("local1"));
                            ri.setLocalAtual(rs.getString("local2"));
                        }    
                        ri.setDados(rsi.getString("procdata"));
                        ri.setDataUltimaAlteracao(rs.getTimestamp("data"));
                        
                        if(ri.getLstColabs().indexOf(rs.getString("local1"))==-1){ ri.getLstColabs().add(rs.getString("local1")); }
                        if(ri.getLstColabs().indexOf(rs.getString("local2"))==-1  && !rs.getString("local2").isEmpty()){ ri.getLstColabs().add(rs.getString("local2")); }
                        
                        if(ri.getLstServicos().indexOf(rs.getString("servicoOrigem"))==-1 && rs.getString("servicoOrigem")!=null){ ri.getLstServicos().add(rs.getString("servicoOrigem")); }
                        if(ri.getLstServicos().indexOf(rs.getString("servicoDestino"))==-1 && rs.getString("servicoOrigem")!=null){ ri.getLstServicos().add(rs.getString("servicoDestino")); }

                        
                        
                        if(rs.getString("local2")!=null){ ri.getLstIntColabs().add(new Intervencao(rs.getString("local2"), rs.getTimestamp("data"))); }
                        else if(rs.getString("local1")!=null){ ri.getLstIntColabs().add(new Intervencao(rs.getString("local1"), rs.getTimestamp("data"))); }
                    
                        if(rs.getString("servicoOrigem")!=null && !rs.getString("servicoOrigem").toLowerCase().contains("neutro")){ ri.getLstIntServs().add(new Intervencao(rs.getString("servicoOrigem"), rs.getTimestamp("data"))); }
                        if(rs.getString("servicoDestino")!=null && !rs.getString("servicoDestino").toLowerCase().contains("neutro")){ ri.getLstIntServs().add(new Intervencao(rs.getString("servicoDestino"), rs.getTimestamp("data"))); }
                    
                        ri.setEstado("Arquivada");
                        
                        BasicDBObject onew = Parsers.parseJavaRIToJsonRI(ri); 
                        
                        conn.getDBCollection().update(new BasicDBObject("processo.pid", ri.getPid()), onew, false, false, WriteConcern.SAFE);
                        
                        processado = true;
                        }catch(Exception e){}  
                    }
                }
            
            //}
              
                return processado;
        
    
              
    
    }
    
    
    
    
    
    
    public ObjIndexacao obterObjIndexacaoByTituloServico(MyConnection conn, String titulo, String servico){
        
        //titulo = titulo.toLowerCase().replaceAll("a|à|á|ã|â|A|À|Á|Ã|Â", "[aáàãâAÁÀÃÂ]").replaceAll("e|è|é|ê|E|È|É|Ê", "[eéèêEÉÈÊ]").replaceAll("i|ì|í|î|I|Ì|Í|Î", "[iìíîIÌÍÎ]").replaceAll("o|ò|ó|õ|ô|O|Ò|Ó|Õ|Ô", "[oóòôõOÓÒÔÕ]").replaceAll("u|ù|ú|û|U|Ù|Ú|Û", "[uúùûUÚÙÛ]").replaceAll("c|ç|C|Ç", "[cçCÇ]");
        Pattern regexa = Pattern.compile(titulo, Pattern.CASE_INSENSITIVE);
        
        String coll = conn.getColl().getName();
        conn.setDBCollection("collMapeamento");
        
        BasicDBObject q = new BasicDBObject("desig", titulo);
        BasicDBObject elemM = new BasicDBObject("$elemMatch", q);
        BasicDBObject qTit = new BasicDBObject("Titulos", elemM);
        
        try{
            DBObject r =  conn.getDBCollection().findOne(qTit);
        
            conn.setDBCollection(coll);
        
            if(r!=null){
                BasicDBObject ro = (BasicDBObject) r;
                return Parsers.parseJsonToObjIndexacaoIndexByServico(ro, servico);
            }
        }catch(Exception e){ 
            return null; 
        }

        return null;
    }
    
    
    
    public ObjIndexacao obterObjIndexacaoByAreaSubArea(MyConnection conn, String area, String subArea){
        
        area = area.toLowerCase().replaceAll("a|à|á|ã|â|A|À|Á|Ã|Â", "[aáàãâAÁÀÃÂ]").replaceAll("e|è|é|ê|E|È|É|Ê", "[eéèêEÉÈÊ]").replaceAll("i|ì|í|î|I|Ì|Í|Î", "[iìíîIÌÍÎ]").replaceAll("o|ò|ó|õ|ô|O|Ò|Ó|Õ|Ô", "[oóòôõOÓÒÔÕ]").replaceAll("u|ù|ú|û|U|Ù|Ú|Û", "[uúùûUÚÙÛ]").replaceAll("c|ç|C|Ç", "[cçCÇ]");
        Pattern regexa = Pattern.compile(area, Pattern.CASE_INSENSITIVE);
        
        subArea = subArea.toLowerCase().replaceAll("a|à|á|ã|â|A|À|Á|Ã|Â", "[aáàãâAÁÀÃÂ]").replaceAll("e|è|é|ê|E|È|É|Ê", "[eéèêEÉÈÊ]").replaceAll("i|ì|í|î|I|Ì|Í|Î", "[iìíîIÌÍÎ]").replaceAll("o|ò|ó|õ|ô|O|Ò|Ó|Õ|Ô", "[oóòôõOÓÒÔÕ]").replaceAll("u|ù|ú|û|U|Ù|Ú|Û", "[uúùûUÚÙÛ]").replaceAll("c|ç|C|Ç", "[cçCÇ]");
        Pattern regexs = Pattern.compile(subArea, Pattern.CASE_INSENSITIVE);
        
        String coll = conn.getColl().getName();
        conn.setDBCollection("collMapeamento");
        
        BasicDBObject q = new BasicDBObject();
        q.put("area", regexa);
        q.put("subArea", regexs);
        
        BasicDBObject r = (BasicDBObject) conn.getDBCollection().findOne(q);
        
        conn.setDBCollection(coll);
        
        if(r!=null){
            return Parsers.parseJsonToObjIndexacao(r);
        }
        
        return null;
        
    }
    
}
