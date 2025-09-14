package bussniffer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.time.LocalTime;
import java.util.List;
// import java.util.function.Consumer;

public class BusSnifferGUI extends JFrame {
    private JComboBox<String> portCombo;
    private JTextField baudField;
    private JButton connectBtn;
    private JTable logTable;
    private DefaultTableModel logModel;
    private JTextField heightField, slideField, inclineField;
    private JButton sendBtn;
    private JButton saveLogBtn, clearLogBtn;
    private JComboBox<String> profileSelect;
    private JButton saveProfileBtn, loadProfileBtn;

    private SerialManager serial;
    private ProfilesManager profilesManager = new ProfilesManager();
    private StatusPanel statusPanel;

    // state machine
    private enum State { IDLE, MOVING, ERROR }
    private volatile State curState = State.IDLE;

    public BusSnifferGUI() {
        super("BusSniffer Tool");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout(10, 10));

        // TOP connection
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connPanel.setBorder(BorderFactory.createTitledBorder("Connection"));
        portCombo = new JComboBox<>(PortUtil.getAvailablePortNames().toArray(new String[0]));
        connPanel.add(new JLabel("Port:")); connPanel.add(portCombo);
        baudField = new JTextField("9600",6);
        connPanel.add(new JLabel("Baud:")); connPanel.add(baudField);
        connectBtn = new JButton("Connect"); connPanel.add(connectBtn);
        add(connPanel, BorderLayout.NORTH);

        // CENTER split: left log + right status & profiles
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        // left: log
        String[] cols = {"Time","Direction","Message"};
        logModel = new DefaultTableModel(cols,0);
        logTable = new JTable(logModel);
        JScrollPane scroll = new JScrollPane(logTable);
        split.setLeftComponent(scroll);

        // right: vertical box
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout());
        statusPanel = new StatusPanel();
        right.add(statusPanel, BorderLayout.NORTH);

        // profiles panel
        JPanel profiles = new JPanel(new FlowLayout());
        profileSelect = new JComboBox<>();
        refreshProfilesCombo();
        saveProfileBtn = new JButton("Save Profile");
        loadProfileBtn = new JButton("Load Profile");
        profiles.add(new JLabel("Profile:")); profiles.add(profileSelect);
        profiles.add(saveProfileBtn); profiles.add(loadProfileBtn);
        right.add(profiles, BorderLayout.CENTER);

        split.setRightComponent(right);
        add(split, BorderLayout.CENTER);

        // bottom control + log actions
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        heightField = new JTextField("0",5); slideField = new JTextField("0",5); inclineField = new JTextField("0",5);
        sendBtn = new JButton("Send");
        saveLogBtn = new JButton("Save Log");
        clearLogBtn = new JButton("Clear Log");
        bottom.add(new JLabel("Height(mm):")); bottom.add(heightField);
        bottom.add(new JLabel("Slide(mm):")); bottom.add(slideField);
        bottom.add(new JLabel("Incline(deg):")); bottom.add(inclineField);
        bottom.add(sendBtn); bottom.add(saveLogBtn); bottom.add(clearLogBtn);
        add(bottom, BorderLayout.SOUTH);

        // actions
        connectBtn.addActionListener(e -> toggleConnect());
        sendBtn.addActionListener(e -> onSend());
        saveLogBtn.addActionListener(e -> onSaveLog());
        clearLogBtn.addActionListener(e -> logModel.setRowCount(0));
        saveProfileBtn.addActionListener(e -> onSaveProfile());
        loadProfileBtn.addActionListener(e -> onLoadProfile());

        setLocationRelativeTo(null); setVisible(true);
    }

    private void refreshProfilesCombo(){
        profileSelect.removeAllItems();
        List<ProfilesManager.Profile> list = profilesManager.list();
        for (ProfilesManager.Profile p : list) profileSelect.addItem("P"+p.id);
    }

    private void toggleConnect(){
        if (serial != null && serial.isOpen()) {
            serial.close();
            log("System","Disconnected");
            connectBtn.setText("Connect");
            return;
        }
        String port = (String) portCombo.getSelectedItem();
        int baud = Integer.parseInt(baudField.getText());
        serial = new SerialManager(port, baud, this::handleFrame);
        if (serial.open()) { log("System","Connected to "+port); connectBtn.setText("Disconnect"); }
        else log("System","Failed to connect");
    }

    private void handleFrame(byte[] frame) {
        BusProtocol.ParsedMessage pm = BusProtocol.parseRawFrame(frame);
        log("IN", pm.text);
        // update state if status
        if (pm.state >= 0) {
            if (pm.state == 0) curState = State.IDLE;
            else if (pm.state == 1) curState = State.MOVING;
            else if (pm.state == 2) curState = State.ERROR;
            statusPanel.updateState(pm.state, pm.err);
            swingEnableControls();
        }
        // if it's control req feedback or similar, we could parse measured values and update
        // (assuming STM32 sends ControlReq as report of current measured)
        if (pm.text.startsWith("ControlReq")) {
            // parse numbers from text quick (or better: extend parser to return numbers)
            try {
                String[] parts = pm.text.split(" ");
                int h = Integer.parseInt(parts[1].split("=")[1]);
                int s = Integer.parseInt(parts[2].split("=")[1]);
                int ii= Integer.parseInt(parts[3].split("=")[1]);
                statusPanel.updateMeasured(h,s,ii);
            } catch(Exception ex){}
        }
    }

    private void swingEnableControls() {
        SwingUtilities.invokeLater(() -> {
            boolean allow = (curState == State.IDLE);
            sendBtn.setEnabled(allow);
            saveProfileBtn.setEnabled(allow);
            loadProfileBtn.setEnabled(allow);
            profileSelect.setEnabled(allow);
            if (!allow) {
                log("System","Controls disabled because state="+curState);
            }
        });
    }

    private void onSend(){
        if (curState == State.MOVING) {
            JOptionPane.showMessageDialog(this, "Cannot send control while chair is moving.");
            log("Warn","Attempted send while moving");
            return;
        }
        try {
            int h = Integer.parseInt(heightField.getText());
            int s = Integer.parseInt(slideField.getText());
            int i = Integer.parseInt(inclineField.getText());
            byte[] f = BusProtocol.buildControlRequest(h,s,i);
            serial.send(f);
            log("OUT","Sent ControlReq H="+h+" S="+s+" I="+i);
        } catch(Exception ex) {
            log("Error","Invalid send values");
        }
    }

    private void onSaveLog(){
        try (FileWriter fw = new FileWriter("buslog.csv")) {
            for (int r=0;r<logModel.getRowCount();r++){
                fw.write(logModel.getValueAt(r,0)+","+logModel.getValueAt(r,1)+",\"" + logModel.getValueAt(r,2) + "\"\n");
            }
            fw.flush();
            log("System","Saved log to buslog.csv");
        } catch(Exception ex){ log("Error","Save log failed: "+ex.getMessage()); }
    }

    private void onSaveProfile(){
        if (curState != State.IDLE) { JOptionPane.showMessageDialog(this,"Cannot save profile while moving/error."); return; }
        try {
            int id = profileSelect.getSelectedIndex();
            int h = Integer.parseInt(heightField.getText());
            int s = Integer.parseInt(slideField.getText());
            int i = Integer.parseInt(inclineField.getText());
            profilesManager.saveProfile(id,h,s,i);
            refreshProfilesCombo();
            // send profile save command to STM
            byte[] f = BusProtocol.buildProfileSave(id,h,s,i);
            serial.send(f);
            log("OUT","ProfileSave id="+id+" H="+h+" S="+s+" I="+i);
        } catch(Exception ex) { log("Error","Save profile failed"); }
    }

    private void onLoadProfile(){
        if (curState != State.IDLE) { JOptionPane.showMessageDialog(this,"Cannot load profile while moving/error."); return; }
        int id = profileSelect.getSelectedIndex();
        ProfilesManager.Profile p = profilesManager.getProfile(id);
        // send profile load
        byte[] f = BusProtocol.buildProfileLoad(id);
        serial.send(f);
        log("OUT","ProfileLoad id="+id);
        // update fields with profile (local)
        heightField.setText(String.valueOf(p.height));
        slideField.setText(String.valueOf(p.slide));
        inclineField.setText(String.valueOf(p.incline));
    }

    private void log(String dir, String msg) {
        SwingUtilities.invokeLater(() -> logModel.addRow(new Object[]{LocalTime.now().toString(),dir,msg}));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BusSnifferGUI::new);
    }
}
