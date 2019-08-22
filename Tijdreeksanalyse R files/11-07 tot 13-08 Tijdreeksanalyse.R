TwitterTs <- ts(`11.07.2019.tot.13.08.2019.Twitter`$V2)
BitcoinTs <- ts(`11.07.2019.tot.13.08.2019.Bitcoin`$V2)

plot.ts(TwitterTs, xlab="Tijd, 1 = 1u", ylab="Aantal Tweets die Bitcoin bevatten",main="11/07/2019 tot 13/08/2019 - Aantal Tweets ieder uur")
plot.ts(BitcoinTs, xlab="Tijd, 1 = 1u", ylab="Waarde van Bitcoin",main="11/07/2019 tot 13/08/2019 - Bitcoin Waarde ieder uur")


#Autocorrelatie Twitter
adf.test(TwitterTs)
Acf(TwitterTs)
Pacf(TwitterTs)

#Forecasting Twitter
TwitterFit <- auto.arima(TwitterTs)
TwitterForecast <- forecast(TwitterFit, h=48)
plot(TwitterForecast, xlab="Tijd, 1 = 1u", ylab="Aantal Tweets die Bitcoin bevatten")



#Autocorrelatie Bitcoin
adf.test(BitcoinTs)
BitcoinTsDiff <- diff(BitcoinTs)
adf.test(BitcoinTsDiff)
plot(BitcoinTsDiff)

Acf(BitcoinTsDiff)
Pacf(BitcoinTsDiff)

#Forecasting Bitcoin
BitcoinFit <- auto.arima(BitcoinTs)
BitcoinForecast <- forecast(BitcoinFit, h=48)
plot(BitcoinForecast, xlab="Tijd, 1 = 1u", ylab="Waarde van Bitcoin")



#Cross-correlattion functie Twitter en Bitcoin
Ccf(TwitterTs,BitcoinTsDiff, lag.max=124)
Ccf(TwitterTs,BitcoinTsDiff, plot = FALSE)

plot(TwitterTs, col="blue", ylab="Aantal Tweets")
par(new=TRUE)
plot(BitcoinTs, col="red", axes=FALSE, xlab="", ylab="")


