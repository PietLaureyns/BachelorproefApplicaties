import scala.Serializable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main implements Serializable {

    private static DateFormat dateFormat2 = new SimpleDateFormat("dd-MM-yyyy__HH-mm-ss_");

    public static TwitterData twitterData;
    public static BitcoinData bitcoinData;

    public static TwitterApp app;

    public static void main(String[] args) {
        startApp();
        twitterData = new TwitterData(app);
        bitcoinData = new BitcoinData(app);

        new Thread(() -> {
            twitterData.startTwitterStream();
        }).start();

        boolean start = false;
        while (!start) {
            if (new Date().getSeconds() == 0) {
                start = true;
            }
        }
        start();

        while(true){
            try {
                Thread.sleep(58000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean start2 = false;
            while (!start2) {
                if (new Date().getSeconds() == 0) {
                    start2 = true;
                }
            }
            new Thread(()->{
                bitcoinData.resetOneMinute();
            }).start();
            new Thread(()->{
                twitterData.resetOneMinute();
            }).start();
        }
    }

    public static void stop(){
        twitterData.stop();
        bitcoinData.stop();

        System.exit(0);
    }

    public static void start(){
        twitterData.filename = dateFormat2.format(new Date())+"Twitter.csv";
        twitterData.teller = 0;
        twitterData.totalTeller = 0;
        bitcoinData.filename = dateFormat2.format(new Date())+"Bitcoin.csv";
    }

    public static void startApp(){
        app = new TwitterApp();
        JFrame frame = new JFrame("Time Series App");
        frame.setContentPane(app.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(800, 600);
        frame.setVisible(true);

        app.stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Stop Button Clicked");
                stop();
            }
        });
    }
}