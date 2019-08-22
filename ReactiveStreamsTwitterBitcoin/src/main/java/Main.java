import com.github.signaflo.data.visualization.Plots;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class Main {

    private static TimeSeriesApp timeSeriesApp;

    private static DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy__HH-mm-ss_");

    private static BitcoinData bitcoinData;
    private static TwitterSubscriber twitterSubscriber;

    public static void main(String[] args) {
        timeSeriesApp = new TimeSeriesApp();
        JFrame frame = new JFrame("Time Series App");
        frame.setContentPane(timeSeriesApp.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(1000, 1000);

        frame.setVisible(true);

        timeSeriesApp.stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Stop Button Clicked");
                stop();
            }
        });

        timeSeriesApp.showPlotTwitterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Show Twitter Plot Button Clicked");
                showTwitterPlot();
            }
        });

        timeSeriesApp.showPlotBitcoinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Show Bitcoin Plot Button Clicked");
                showBitcoinPlot();
            }
        });

        timeSeriesApp.showTwitterAcfPlotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Show Bitcoin Acf Plot Button Clicked");
                showTwitterAcfPlot();
            }
        });

        timeSeriesApp.showBitcoinAcfPlotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Show Bitcoin Acf Plot Button Clicked");
                showBitcoinAcfPlot();
            }
        });

        timeSeriesApp.updateCorAndCovButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeSeriesApp.updateCorrelation(getCorrelation());
                timeSeriesApp.updateCovariance(getCovariance());
            }
        });

        timeSeriesApp.showTwitterForecastingPlotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showTwitterForecastingPlot();
            }
        });

        timeSeriesApp.showBitcoinForecastingPlotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showBitcoinForecastingPlot();
            }
        });

        start();
    }

    public static void showTwitterPlot() {
        TimeSeries ts = twitterSubscriber.getTwitterTimeSeries();
        Plots.plot(ts, "Aantal Tweets Iedere Minuut");
    }

    public static void showTwitterForecastingPlot() {
        TimeSeries ts = twitterSubscriber.getTwitterTimeSeries();

        if (ts.size() > 0) {
            ArimaOrder modelOrder = ArimaOrder.order(1, 1, 0);
            Arima model = Arima.model(ts, modelOrder);

            Forecast forecast = model.forecast((int) Math.floor(ts.size() / 3));

            List<Double> points = new ArrayList<>();
            ts.asList().forEach(d -> points.add(d));

            for (Double d : forecast.pointEstimates().asList()) {
                points.add(d);
            }

            SimpleGraphPanel.createAndShowGui(points, ts.size() - 1, "Twitter Forecasting - ARIMA.Model( 1 , 1 , 0 )");
        }
    }

    public static void showBitcoinForecastingPlot() {
        TimeSeries ts = bitcoinData.getBitcoinTimeSeries();

        if (ts.size() > 0) {
            ArimaOrder modelOrder = ArimaOrder.order(1, 1, 0);
            Arima model = Arima.model(ts, modelOrder);

            Forecast forecast = model.forecast((int) Math.floor(ts.size() / 3));

            List<Double> points = new ArrayList<>();
            ts.asList().forEach(d -> points.add(d));

            for (Double d : forecast.pointEstimates().asList()) {
                points.add(d);
            }

            SimpleGraphPanel.createAndShowGui(points, ts.size() - 1, "Bitcoin Forecasting - ARIMA.Model( 1 , 1 , 0 )");
        }
    }

    public static void showBitcoinPlot() {
        TimeSeries ts = bitcoinData.getBitcoinTimeSeries();
        Plots.plot(ts, "Waarde Van Bitcoin Iedere Minuut");
    }

    public static void showTwitterAcfPlot() {
        TimeSeries ts = twitterSubscriber.getTwitterTimeSeries();
        Plots.plotAcf(ts, ts.size() - 1);
    }

    public static void showBitcoinAcfPlot() {
        TimeSeries ts = bitcoinData.getBitcoinTimeSeries();
        Plots.plotAcf(ts, ts.size() - 1);
    }

    public static double getCorrelation() {
        TimeSeries ts = twitterSubscriber.getTwitterTimeSeries();
        TimeSeries ts2 = bitcoinData.getBitcoinTimeSeries();
        return ts.correlation(ts2);
    }

    public static double getCovariance() {
        TimeSeries ts = twitterSubscriber.getTwitterTimeSeries();
        TimeSeries ts2 = bitcoinData.getBitcoinTimeSeries();
        return ts.covariance(ts2);
    }

    public static void start() {
        bitcoinData = new BitcoinData(timeSeriesApp);

        twitterSubscriber = new TwitterSubscriber(timeSeriesApp);

        TwitterStreamClass twitterStreamClass = new TwitterStreamClass(twitterSubscriber);
        twitterStreamClass.twitterStream(twitterStreamClass.getTwitterStreamInstance());

        System.setProperty("http.proxyHost", "webcache.mydomain.com");
        System.setProperty("http.proxyPort", "8080");

        boolean start2 = false;
        while (!start2) {
            if (new Date().getSeconds() == 0) {
                start2 = true;
            }
        }

        System.out.println("Start: " + dateFormat.format(new Date()));
        timeSeriesApp.updateStartTime(new Date());
        twitterSubscriber.start();
        bitcoinData.start();

        while (true) {
            try {
                Thread.sleep(50000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean start = false;
            while (!start) {
                if (new Date().getSeconds() == 0)
                    start = true;
            }
            new Thread(() -> {
                bitcoinData.resetOneMinute();
            }).start();
            new Thread(() -> {
                twitterSubscriber.resetOneMinute();
            }).start();
        }
    }

    public static void stop() {
        bitcoinData.stop();
        twitterSubscriber.stop();

        try {
            Thread.sleep(5000); //5 seconden buffer om alles tijd te geven om correct af te sluiten.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
