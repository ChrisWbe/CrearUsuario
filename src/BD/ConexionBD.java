/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BD;
/**
 *
 * @author leonskb4
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;
public class ConexionBD {
    public String puerto="3306";
    public String nomServidor="172.18.0.10";
    public String db="huellas";
    public String user="ene";
    public String pass="";
    Connection conn=null;
    
    public Connection conectar(){
        try {
            String ruta="jdbc:mysql://";
            String servidor=nomServidor+":"+puerto+"/";
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(ruta+servidor+db,user,pass);
            if(conn!=null){
                System.out.println("Conexión a BD... listo!!!");
            }else if(conn==null){
                throw new SQLException();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }catch(ClassNotFoundException e){
            JOptionPane.showMessageDialog(null, "Se produjo el sgte. error: "+e.getMessage());
        }catch(NullPointerException e){
            JOptionPane.showMessageDialog(null, "Se produjo el sgte. error: "+e.getMessage());
        }finally{
            return conn;
        }
    }
    
    public void desconectar(){
        conn = null;
        System.out.println("Desconexion... listo!!!");
    }
    
    
}
