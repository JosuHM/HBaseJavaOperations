/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bigdata.hbase;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.net.ntp.TimeStamp;
import org.apache.commons.pool2.ObjectPool;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
/**
 *
 * @author jafernandez
 */
public class HBase{        
    private ObjectPool<Connection> pool;      
    private String table;
    
    public HBase(ObjectPool<Connection> pool, String table){
        this.pool = pool;
        this.table = table;
    }   
    // Insertar un registro    
    public void writeIntoTable(String colFamily, String colQualifier, String value) throws Exception{
        Connection c = pool.borrowObject();
        TableName tableName = TableName.valueOf(this.table); 
        Table t = c.getTable(tableName);
        TimeStamp ts = new TimeStamp(new Date());        
        Date d = ts.getDate();        
        Put p = new Put(Bytes.toBytes(d.toString()));
        p.addColumn(
            Bytes.toBytes(colFamily),
            Bytes.toBytes(colQualifier),
            Bytes.toBytes(value));
        t.put(p);
        t.close();
        pool.returnObject(c);   
    }     
    ////////////////////////////////////////////////////////////////////////////
    // METODOS EN PRUEBA
    public Map<String, String> getDescription() throws Exception{
        Connection c = pool.borrowObject();
        Admin admin = c.getAdmin();
        TableName tableName = TableName.valueOf(this.table);      
        HTableDescriptor htd = admin.getTableDescriptor(tableName);                
        Map<String, String> descripcion = new HashMap<>();
        descripcion.put("tableAvailable", String.valueOf(admin.isTableEnabled(tableName)));
        descripcion.put("tableName", htd.getTableName().toString());                                                        
        pool.returnObject(c);           
        return descripcion;
    }   
    
    public void getRows(int i) throws Exception{
        Connection c = pool.borrowObject();
        TableName tableName = TableName.valueOf(this.table);                 
        Table t = c.getTable(tableName);
        Scan s = new Scan();
        ResultScanner scanner = t.getScanner(s);
        int j = 0;
        for (Result result = scanner.next(); result != null && j < i; result = scanner.next()){   
            Result getResult = t.get(new Get(result.getRow()));
            String value = Bytes.toString(getResult.getValue(Bytes.toBytes("msg"), Bytes.toBytes("sender")));
            System.out.println(value);            
            j++;
        }        
        scanner.close();
        /*
        byte[] columnMin = Bytes.toBytes(200);
        byte[] columnMax = Bytes.toBytes(300);
        byte[] cf = Bytes.toBytes("familyName");
        byte[] column = Bytes.toBytes("columnNameToBeFiltered");
        Scan scan = new Scan();
        FilterList list = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        SingleColumnValueFilter filter1 = new SingleColumnValueFilter(
                cf, column, CompareOp.GREATER, columnMin);
        list.addFilter(filter1);
        SingleColumnValueFilter filter2 = new SingleColumnValueFilter(
                cf, column, CompareOp.LESS, columnMax);
        list.addFilter(filter2);
        scan.setFilter(list);
        ResultScanner scanner = t.getScanner(scan);
        */
        t.close();
        pool.returnObject(c);   
    }  
    ////////////////////////////////////////////////////////////////////////////
    // Leer los primeros i registros de la tabla
    public void getLimitRows(int i) throws Exception{
        Connection c = pool.borrowObject();              
        Table t = c.getTable(TableName.valueOf(this.table));
        Scan s = new Scan();
        ResultScanner scanner = t.getScanner(s);
        int j = 0;
        for (Result result = scanner.next(); result != null && j < i; result = scanner.next()){            
            Map<byte[],byte[]> qualifiers = result.getFamilyMap(Bytes.toBytes("msg"));
            for(int k = 0; k < qualifiers.size(); k++){
                Object[] values = qualifiers.values().toArray();
                Object[] keys = qualifiers.keySet().toArray();                
                String key = Bytes.toString((byte[]) keys[k]);
                String value = Bytes.toString((byte[]) values[k]);
                System.out.println("[msg:" + key + "] =  " + value);            
            }
            System.out.println("------------------------------------------------------------------------");            
            j++;
        }        
        scanner.close();
        t.close();
        pool.returnObject(c);   
    } 
    // Leer un valor de las filas de la tabla
    public void getValue(String colF, String colQ) throws Exception{
        Connection c = pool.borrowObject();             
        Table t = c.getTable(TableName.valueOf(this.table));
        Scan s = new Scan();
        ResultScanner scanner = t.getScanner(s);         
        for (Result result = scanner.next(); result != null; result = scanner.next()){  
            String value = Bytes.toString(result.getValue(Bytes.toBytes(colF), Bytes.toBytes(colQ)));
            System.out.println("[" + colF + ":" + colQ + "] value= " + value);                                           
        }        
        scanner.close();
        t.close();
        pool.returnObject(c);   
    } 
    // Leer filtrando los registros de la tabla
    public void getFilterRows(String colF, String colQ) throws Exception{
        Connection c = pool.borrowObject();                 
        Table t = c.getTable(TableName.valueOf(this.table));
        Scan scan = new Scan();
        
        FilterList list = new FilterList(FilterList.Operator.MUST_PASS_ONE);        
        SingleColumnValueFilter filter1 = new SingleColumnValueFilter(
                Bytes.toBytes(colF), Bytes.toBytes(colQ), CompareOp.EQUAL, Bytes.toBytes(4));
        list.addFilter(filter1);
        //SingleColumnValueFilter filter2 = new SingleColumnValueFilter(
        //        Bytes.toBytes(colF), Bytes.toBytes(colQ), CompareOp.EQUAL, Bytes.toBytes(5));
        //list.addFilter(filter2);        
        scan.setFilter(list);
        
        ResultScanner scanner = t.getScanner(scan);        
        for (Result result = scanner.next(); result != null; result = scanner.next()){               
            String value = Bytes.toString(result.getValue(Bytes.toBytes(colF), Bytes.toBytes(colQ)));
            System.out.println(value);     
        }        
        scanner.close();
        t.close();
        pool.returnObject(c);   
    }  
    // Actualizar row
    public void updateRow(String row, String colFamily, String colQualifier, String value) throws Exception{
        Connection c = pool.borrowObject();
        Table t = c.getTable(TableName.valueOf(this.table));
        Put p = new Put(Bytes.toBytes(row));
        p.addColumn(
            Bytes.toBytes(colFamily),
            Bytes.toBytes(colQualifier),
            Bytes.toBytes(value));
        t.put(p);
        t.close();
        pool.returnObject(c);   
    }
}