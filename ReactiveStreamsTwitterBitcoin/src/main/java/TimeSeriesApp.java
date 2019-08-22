import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TimeSeriesApp {
    public JPanel mainPanel;
    private JList twitterList;
    private JList bitcoinList;
    private JLabel timeStartedLabel;
    private JLabel aantalTweetsLabel;


    public JButton showPlotTwitterButton;
    public JButton showPlotBitcoinButton;
    public JButton stopButton;
    public JButton showTwitterAcfPlotButton;
    public JButton showBitcoinAcfPlotButton;
    private JLabel corLabel;
    private JLabel covLabel;
    public JButton updateCorAndCovButton;
    public JButton showTwitterForecastingPlotButton;
    public JButton showBitcoinForecastingPlotButton;

    private static SimpleDateFormat format = new SimpleDateFormat("dd-MM | HH:mm:ss");

    public void updateStartTime(Date startdate) {
        timeStartedLabel.setText("Starttijd: " + format.format(startdate));
    }

    public void updateAantalTweets(int aantal) {
        this.aantalTweetsLabel.setText("Aantal Tweets: " + aantal);
    }

    public void writeTwitter1MinValues(TreeMap<Date, Double> twitterValues) {
        List<String> values = new ArrayList<>();
        twitterValues.entrySet().forEach(item -> values.add(format.format(item.getKey()) + "   -   " + item.getValue()));
        Collections.reverse(values);
        this.twitterList.setListData(values.toArray());
    }

    public void writeBitcoin1MinValues(TreeMap<Date, Double> bitcoinValues) {
        List<String> values = new ArrayList<>();
        bitcoinValues.entrySet().forEach(item -> values.add(format.format(item.getKey()) + "   -   " + item.getValue()));
        Collections.reverse(values);
        this.bitcoinList.setListData(values.toArray());
    }

    public void updateCorrelation(double cor) {
        this.corLabel.setText("Correlatie tussen Twitter en Bitcoin Time Series: " + cor);
    }

    public void updateCovariance(double cov) {
        this.covLabel.setText("Covariante tussen Twitter en Bitcoin Time Series: " + cov);
    }
}
