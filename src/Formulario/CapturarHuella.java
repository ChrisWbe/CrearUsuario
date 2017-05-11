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
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        EnviarTexto("Muestra de huellas necesarias para guaradar template" +
        reclutador.getFeaturesNeeded());
    }
    public void EnviarTexto(String string){
        txtMensaje.setText(string);
    }
    public void start(){
        lector.startCapture();
        EnviarTexto("Utilizando lector de huella\n");
    }
    public void stop(){
        lector.stopCapture();
        EnviarTexto("No se esta usando el lector\n");
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
                txtCedula.setEnabled(true);
            } catch (DPFPImageQualityException e) {
                System.out.println("Error: " + e.getMessage());
            }finally{
                reclutador.clear();
//                estadoHuellas();
//                switch(reclutador.getTemplateStatus()){
//                    case TEMPLATE_STATUS_READY:
//                        stop();
//                        setTemplate(reclutador.getTemplate());
//                        EnviarTexto("La plantilla de la huella ha sido creada, ya puede verificarla o identificarla");
//                        btnIdentificar.setEnabled(false);
//                        //btnGuardar.setEnabled(true);
//                        //btnGuardar.grabFocus();
//                        break;
//                    case TEMPLATE_STATUS_FAILED:
//                        reclutador.clear();
//                        stop();
//                        estadoHuellas();
//                        setTemplate(null);
//                        JOptionPane.showMessageDialog(CapturarHuella.this, "La huella no pudo ser creada, intente nuevamente");
//                }
            }
        }
    }
    
    protected void iniciar(){
        lector.addDataListener(new DPFPDataAdapter(){
            @Override public void dataAcquired(final DPFPDataEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("La huella ha sido capturada ");
                        procesarCaptura(e.getSample());
                    }
                });
            }
        });
        lector.addReaderStatusListener(new DPFPReaderStatusAdapter(){
            @Override public void readerConnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("El sensor esta activado o conectado");
                    }
                });
            }
            @Override public void readerDisconnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("El sensor esta desactivado o no conectado");
                    }
                });
            }
        });
        lector.addSensorListener(new DPFPSensorAdapter(){
            @Override public void fingerTouched(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("Intente Nuevamente.El dedo ha sido retirado rapidamente");
                    }
                });
            }
            @Override public void fingerGone(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        //EnviarTexto("El dedo ha sido quitado del lector de huella");
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
                        EnviarTexto("Error: " + e.toString());
                    }
                });
            }
        });
    }
    
    ConexionBD con=new ConexionBD();
//    public void guardarHuella(){
//        ByteArrayInputStream datosHuella=new ByteArrayInputStream(template.serialize());
//        Integer tamanoHuella=template.serialize().length;
//        String nombre=JOptionPane.showInputDialog("Nombre: ");
//        try {
//            Connection c=con.conectar();
//            PreparedStatement guardarStmt=c.prepareStatement("insert into somhue (huenombre,huehuella) values (?,?)");
//            guardarStmt.setString(1, nombre);
//            guardarStmt.setBinaryStream(2, datosHuella, tamanoHuella);
//            guardarStmt.execute();
//            guardarStmt.close();
//            JOptionPane.showMessageDialog(null, "huella guardada correctamente");
//            con.desconectar();
//            //btnGuardar.setEnabled(false);
//            //btnVerificar.grabFocus();
//        } catch (SQLException e) {
//            System.err.println("Error al guardar los datos de la huella");
//            System.err.println(e.getMessage());
//        }finally{
//            con.desconectar();
//        }
//    }
    
//    public void verificarHuella(String nom){
//        try {
//            Connection c = con.conectar();
//            PreparedStatement verificarStmt = c.prepareStatement("select huehuella from somhue where huenombre = ? ");
//            
//            verificarStmt.setString(1, nom);
//            ResultSet rs = verificarStmt.executeQuery();
//            while (rs.next()) {
//                byte templateBuffer[] = rs.getBytes("huehuella");
//                DPFPTemplate referenceTemplate=DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
//                setTemplate(referenceTemplate);
//                DPFPVerificationResult result=verificador.verify(featuresVerificacion, getTemplate());
//                if (result.isVerified()) {
//                    JOptionPane.showMessageDialog(null, "La huella capturara coincide con la de "+nom);
//                }else{
//                    JOptionPane.showMessageDialog(null, "La huella no coincide con la de "+nom);
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("Error al verificar los datos de la huella");
//            System.err.println(e.getMessage());
//        }finally{
//            con.desconectar();
//        }
//    }
    
    public CapturarHuella() {
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Imposible cambiar el tema" + e.toString(),"Error",JOptionPane.ERROR_MESSAGE);
        }
        initComponents();
        this.setLocationRelativeTo(null);
    }
    
    public void identificarHuella() throws IOException{
        boolean flag = false;
        try {
            Connection c=con.conectar();
            PreparedStatement identificarStmt = c.prepareStatement("select huenombre, huehuella from somhue where huenombre =" + txtCedula.getText());            
            ResultSet rs = identificarStmt.executeQuery();
            while (rs.next()) {                
                byte templateBuffer[]=rs.getBytes("huehuella");
                String nombre = rs.getString("huenombre");
                DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
                
                setTemplate(referenceTemplate);
                DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());
                if (result.isVerified()) {
                    
                    JOptionPane.showMessageDialog(null, "La huella capturada es de " + nombre,"Verificacion Huella",JOptionPane.INFORMATION_MESSAGE);
                    txtCedula.setEnabled(false);
                    flag=true;
                    txtCodiBarras.setVisible(true);
                    //txt para giyHUb
                    lblCod.setVisible(true);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al identificar huella dactilar. "+e.getMessage());
        }finally{
            con.desconectar();
        }
        if(!flag){
            JOptionPane.showMessageDialog(null, "Huella no encontrada en la Base de Datos");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblImagen = new javax.swing.JLabel();
        btnIdentificar = new javax.swing.JButton();
        btnSalir = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        txtCedula = new javax.swing.JTextField();
        txtMensaje = new javax.swing.JLabel();
        txtCodiBarras = new javax.swing.JTextField();
        lblCod = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        btnIdentificar.setText("Identificar");
        btnIdentificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIdentificarActionPerformed(evt);
            }
        });

        btnSalir.setText("Salir");
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });

        jLabel1.setText("Cédula:");

        txtCedula.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtCedulaKeyTyped(evt);
            }
        });

        txtCodiBarras.setEditable(false);

        lblCod.setText("Codigo de Barras");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(btnIdentificar)
                .addGap(66, 66, 66)
                .addComponent(btnLimpiar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtCedula, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1)
                            .addComponent(txtCodiBarras, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblCod))))
                .addContainerGap(35, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblImagen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(27, 27, 27))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCedula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 88, Short.MAX_VALUE)
                        .addComponent(lblCod)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtCodiBarras, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(59, 59, 59)))
                .addComponent(txtMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIdentificar)
                    .addComponent(btnSalir)
                    .addComponent(btnLimpiar))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_btnSalirActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        iniciar();
        start();
        //estadoHuellas();
        //btnGuardar.setEnabled(false);
        btnIdentificar.setEnabled(false);
        //btnVerificar.setEnabled(false);
        btnSalir.grabFocus();
        txtCedula.setEnabled(false);
        txtCodiBarras.setVisible(false);
        lblCod.setVisible(false);
        
    }//GEN-LAST:event_formWindowOpened

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        stop();
    }//GEN-LAST:event_formWindowClosing

    private void btnIdentificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIdentificarActionPerformed
        
           try {
            identificarHuella();
            reclutador.clear();
            } catch (IOException e) {
            //Logger.getLogger(CapturarHuella.class.getName()).log(Level.SEVERE, null, e);
            } 
        
    }//GEN-LAST:event_btnIdentificarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        // TODO add your handling code here:
        lblImagen.setIcon(null);
        //btnLimpiar.setEnabled(false);
        btnIdentificar.setEnabled(false);
        reclutador.clear();
        txtCedula.setEnabled(false);
        txtCedula.setText(null);
        txtCodiBarras.setVisible(false);
        lblCod.setVisible(false);
        EnviarTexto("Esperando Huella");
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void txtCedulaKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCedulaKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if ((c < '0') || (c > '9')) { evt.consume();
        }
        if(!txtCedula.getText().equals(null)){
            btnIdentificar.setEnabled(true);
        }
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
    private javax.swing.JButton btnIdentificar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnSalir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblCod;
    private javax.swing.JLabel lblImagen;
    private javax.swing.JTextField txtCedula;
    private javax.swing.JTextField txtCodiBarras;
    private javax.swing.JLabel txtMensaje;
    // End of variables declaration//GEN-END:variables
}
