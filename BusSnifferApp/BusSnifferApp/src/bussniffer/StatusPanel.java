package bussniffer;

import javax.swing.*;
import java.awt.*;

public class StatusPanel extends JPanel {
    private JLabel stateLabel;
    private JLabel errLabel;
    private JLabel heightLabel, slideLabel, inclineLabel;

    public StatusPanel() {
        setLayout(new GridLayout(6,1));
        setBorder(BorderFactory.createTitledBorder("Status"));
        stateLabel = new JLabel("State: UNKNOWN");
        errLabel = new JLabel("Error: 0");
        heightLabel = new JLabel("Height: -");
        slideLabel = new JLabel("Slide: -");
        inclineLabel = new JLabel("Incline: -");
        add(stateLabel); add(errLabel); add(heightLabel); add(slideLabel); add(inclineLabel);
    }

    public void updateState(int state, int err) {
        String s = (state==0?"IDLE":state==1?"MOVING":state==2?"ERROR":"UNKNOWN");
        stateLabel.setText("State: " + s);
        errLabel.setText("Error: " + err);
    }

    public void updateMeasured(int h,int s,int i) {
        heightLabel.setText("Height: "+h+" mm");
        slideLabel.setText("Slide: "+s+" mm");
        inclineLabel.setText("Incline: "+i+" deg");
    }
}
