import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TwitterApp {

    private static SimpleDateFormat format = new SimpleDateFormat("dd-MM | HH:mm:ss");

    private JList oneMinuteTwitterValuesList;
    private JLabel aantalTweetsLabel;
    public JButton stopButton;
    public JPanel mainPanel;
    private JList oneMinuteBitcoinValuesList;

    public void updateAantalTweets(int aantal){
        this.aantalTweetsLabel.setText("Aantal Tweets: "+aantal);
    }

    public void writeTwitter1MinValues(TreeMap<Date, Integer> twitterValues) {
        List<String> values = new ArrayList<>();
        twitterValues.entrySet().forEach(item -> values.add(format.format(item.getKey()) + "   -   " + item.getValue()));
        Collections.reverse(values);
        this.oneMinuteTwitterValuesList.setListData(values.toArray());
    }

    public void writeBitcoin1MinValues(TreeMap<Date, Double> bitcoinValues) {
        List<String> values = new ArrayList<>();
        bitcoinValues.entrySet().forEach(item -> values.add(format.format(item.getKey()) + "   -   " + item.getValue()));
        Collections.reverse(values);
        this.oneMinuteBitcoinValuesList.setListData(values.toArray());
    }

}
