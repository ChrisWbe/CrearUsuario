/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formulario;

import BD.ConexionBD;
import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPErrorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author leonskb4
 */

public class CapturarHuella extends javax.swing.JFrame {

    /**
     * Creates new form CapturarHuella
     */
    private DPFPCapture lector = DPFPGlobal.getCaptureFactory().createCapture();
    private DPFPEnrollment reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    private DPFPVerification verificador = DPFPGlobal.getVerificationFactory().createVerification();
    private DPFPTemplate template;
    public static String TEMPLATE_PROPERTY = "template";
    
    public DPFPFeatureSet featuresInscripcion;
    public DPFPFeatureSet featuresVerificacion;
    public DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose){
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try {
            return extractor.createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e) {
            return null;
        }
    }
    public Image crearImagenHuella(DPFPSample sample){
        return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }
    public void dibujarHuella(Image image){
        lblImagen.setIcon(new ImageIcon(image.getScaledInstance(lblImagen.getWidth(), lblImagen.getHeight(), image.SCALE_DEFAULT)));
        repaint();
    }
    public void estadoHuellas(){
        EnviarTexto("Muestra de huellas necesarias para guardar plantilla: " +
        reclutador.getFeaturesNeeded());
    }
    public void EnviarTexto(String string){
        txtMensaje.setText(txtMensaje.getText() + "\n" +string);
    }
    public void start(){
        lector.startCapture();
        EnviarTexto("Utilizando lector de huella");
    }
    public void stop(){
        lector.stopCapture();
        EnviarTexto("No se esta usando el lector");
    }
    public DPFPTemplate getTemplate(){
        return template;
    }
    public void setTemplate(DPFPTemplate template){
        DPFPTemplate old = this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }
    
    public void procesarCaptura(DPFPSample sample){
        featuresInscripcion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        featuresVerificacion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        if(featuresInscripcion!=null){
            try {
                System.out.println("Las caracteristicas de la huella han sido creadas");
                reclutador.addFeatures(featuresInscripcion);
                Image image=crearImagenHuella(sample);
                dibujarHuella(image);
                
                //btnVerificar.setEnabled(true);
                //btnIdentificar.setEnabled(true);
                btnLimpiar.setEnabled(true);
            } catch (DPFPImageQualityException e) {
                System.out.println("Error: " + e.getMessage());
            }finally{
                estadoHuellas();
                switch(reclutador.getTemplateStatus()){
                    case TEMPLATE_STATUS_READY:
                        stop();
                        setTemplate(reclutador.getTemplate());
                        EnviarTexto("La plantilla de la huella ha sido creada, ya puede verificarla o identificarla");
                        //btnIdentificar.setEnabled(false);
                        btnLimpiar.setEnabled(false);
                        //btnVerificar.setEnabled(false);
                        btnGuardar.setEnabled(true);
                        btnGuardar.grabFocus();
                        txtCedula.setEnabled(true);
                        break;
                    case TEMPLATE_STATUS_FAILED:
                        reclutador.clear();
                        stop();
                        estadoHuellas();
                        setTemplate(null);
                        JOptionPane.showMessageDialog(CapturarHuella.this, "La huella no pudo ser creada, intente nuevamente");
                        start();
                        break;
                }
            }
        }
    }
    
    protected void iniciar(){
        lector.addDataListener(new DPFPDataAdapter(){
            @Override public void dataAcquired(final DPFPDataEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("La huella ha sido capturada \n");
                        procesarCaptura(e.getSample());
                    }
                });
            }
        });
        lector.addReaderStatusListener(new DPFPReaderStatusAdapter(){
            @Override public void readerConnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("El sensor esta activado o conectado\n");
                    }
                });
            }
            @Override public void readerDisconnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("El sensor esta desactivado o no conectado\n");
                    }
                });
            }
        });
        lector.addSensorListener(new DPFPSensorAdapter(){
            @Override public void fingerTouched(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("El dedo ha sido colocado sobre el lector de huella\n");
                    }
                });
            }
            @Override public void fingerGone(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("El dedo ha sido quitado del lector de huella\n");
                    }
                });
            }
        });
        lector.addErrorListener(new DPFPErrorAdapter(){
            //@Override
            public void errorReader(final DPFPErrorAdapter e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("Error: " + e.toString()+"\n");
                    }
                });
            }
        });
    }
    
    ConexionBD con=new ConexionBD();
    public void guardarHuella(){
        int nombre = 0;
        ByteArrayInputStream datosHuella=new ByteArrayInputStream(template.serialize());
        Integer tamanoHuella=template.serialize().length;
         
        try {
            nombre = Integer.parseInt(txtCedula.getText());     
            Connection c=con.conectar();
            PreparedStatement guardarStmt=c.prepareStatement("insert into somhue (huenombre,huehuella) values (?,?)");
            guardarStmt.setInt(1, nombre);
            guardarStmt.setBinaryStream(2, datosHuella, tamanoHuella);
            guardarStmt.execute();
            guardarStmt.close();
            JOptionPane.showMessageDialog(null, "Huella guardada correctamente");
            con.desconectar();
            btnGuardar.setEnabled(false);
            //btnVerificar.grabFocus();
        } catch (SQLException e) {
            System.err.println("Error al guardar los datos de la huella");
            System.err.println(e.getMessage());
        }finally{
            con.desconectar();
        }
//        
//        
//        try {
//            String cedula=JOptionPane.showInputDialog("Cédula: ");
//            
//             nombre = Integer.parseInt(cedula);
//        } catch (NumberFormatException e) {
//           JOptionPane.showMessageDialog(null, "Ingrese un DOcumento de Identidad Valido");
//        }
    }
    
    /*public void verificarHuella(String nom){
        try {
            Connection c = con.conectar();
            PreparedStatement verificarStmt = c.prepareStatement("select huehuella from somhue where huenombre = ? ");
            verificarStmt.setString(1, nom);
            ResultSet rs = verificarStmt.executeQuery();
            while (rs.next()) {
                byte templateBuffer[] = rs.getBytes("huehuella");
                DPFPTemplate referenceTemplate=DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
                setTemplate(referenceTemplate);
                DPFPVerificationResult result=verificador.verify(featuresVerificacion, getTemplate());
                if (result.isVerified()) {
                    JOptionPane.showMessageDialog(null, "La huella capturara coincide con la de "+nom);
                }else{
                    JOptionPane.showMessageDialog(null, "La huella no coincide con la de "+nom);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar los datos de la huella");
            System.err.println(e.getMessage());
        }finally{
            con.desconectar();
        }
    }*/
    
    public CapturarHuella() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Imposible cambiar el tema" + e.toString(),"Error",JOptionPane.ERROR_MESSAGE);
        }
        initComponents();
        this.setLocationRelativeTo(null);
    }
    
    /*public void identificarHuella() throws IOException{
        boolean flag = false;
        try {
            
            Connection c=con.conectar();
            PreparedStatement identificarStmt = c.prepareStatement("select huenombre, huehuella from somhue");            
            ResultSet rs = identificarStmt.executeQuery();
            while (rs.next()) {                
                byte templateBuffer[]=rs.getBytes("huehuella");
                String nombre = rs.getString("huenombre");
                DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
                
                setTemplate(referenceTemplate);
                DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());
                if (result.isVerified()) {
                    JOptionPane.showMessageDialog(null, "La huella capturada es de " + nombre,"Verificación Huella",JOptionPane.INFORMATION_MESSAGE);
                    flag = true;
                }   
            }
        } catch (SQLException e) {
            System.err.println("Error al identificar huella dactilar. "+e.getMessage());
        }finally{
            if(!flag){
                JOptionPane.showMessageDialog(null, "La huella suministrada no existe en la Base de Datos", "Verificación Huella",JOptionPane.INFORMATION_MESSAGE);
            }
            con.desconectar();
        }
    }*/

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnLimpiar = new javax.swing.JButton();
        lblImagen = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        btnSalir = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtMensaje = new javax.swing.JTextArea();
        txtCedula = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnSalir.setText("Salir");
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        txtMensaje.setEditable(false);
        txtMensaje.setColumns(20);
        txtMensaje.setRows(5);
        txtMensaje.setEnabled(false);
        jScrollPane1.setViewportView(txtMensaje);

        txtCedula.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCedulaActionPerformed(evt);
            }
        });
        txtCedula.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtCedulaKeyTyped(evt);
            }
        });

        jLabel1.setText("Cédula:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(71, 71, 71)
                        .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(60, 60, 60)
                        .addComponent(btnLimpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(67, 67, 67)
                        .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 14, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1)))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lblImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtCedula)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(lblImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtCedula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(78, 78, 78)))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnLimpiar)
                    .addComponent(btnSalir))
                .addGap(20, 20, 20))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_btnSalirActionPerformed

    
    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        System.setProperty( "java.library.path", "System32" );
        System.out.println(System.getProperty("java.library.path"));
        
        btnGuardar.setEnabled(false);
        txtCedula.setEnabled(false);
        //btnIdentificar.setEnabled(false);
        btnLimpiar.setEnabled(false);
        //btnVerificar.setEnabled(false);
        btnSalir.grabFocus();
        
        iniciar();
        start();
        //estadoHuellas();
    }//GEN-LAST:event_formWindowOpened

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        stop();
    }//GEN-LAST:event_formWindowClosing

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if(txtCedula.getText().isEmpty()){
            JOptionPane.showMessageDialog(null, "Debe Ingresar un numero de Cédula valido");
        }else{
            try {
            guardarHuella();
            reclutador.clear();
            lblImagen.setIcon(null);
            start();
            txtCedula.setText(null);
            txtCedula.setEnabled(false);
        } catch (Exception e) {
            Logger.getLogger(CapturarHuella.class.getName()).log(Level.SEVERE, null, e);
        }
        }
        
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        // TODO add your handling code here:
        //btnIdentificar.setEnabled(false);
        btnLimpiar.setEnabled(false);
        lblImagen.setIcon(null);
        txtMensaje.setText("Nueva Huella");
        reclutador.clear();
        txtCedula.setEnabled(true);
        
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void txtCedulaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCedulaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCedulaActionPerformed

    private void txtCedulaKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCedulaKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if(c<'0' || c>'9') evt.consume();
    }//GEN-LAST:event_txtCedulaKeyTyped


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CapturarHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CapturarHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CapturarHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CapturarHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CapturarHuella().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnSalir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblImagen;
    private javax.swing.JTextField txtCedula;
    private javax.swing.JTextArea txtMensaje;
    // End of variables declaration//GEN-END:variables


}
