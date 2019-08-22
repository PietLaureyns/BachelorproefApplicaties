#Spike1--------------------------------------------------------------------------------------------------------
Spike1TwitterTs <- ts(`Spike1.Twitter.12_07.tot.14_07`$V2)
Spike1BitcoinTs <- ts(`Spike1.Bitcoin.12_07.tot.14_07`$V2)

plot(Spike1BitcoinTs)

plot(Spike1TwitterTs, col="blue", ylab="Aantal Tweets", main="Twitter(Blauw) en Bitcoin(Rood) data van 12/07 tot 14/07 (72 uur)")
par(new=TRUE)
plot(Spike1BitcoinTs, col="red", axes=FALSE, xlab="", ylab="")

adf.test(Spike1TwitterTs) #0.3713
adf.test(Spike1BitcoinTs) #0.5241

Acf(diff(Spike1TwitterTs))
Acf(diff(Spike1BitcoinTs))

Ccf(Spike1TwitterTs,Spike1BitcoinTs)
Ccf(diff(Spike1TwitterTs),diff(Spike1BitcoinTs))

#Spike2--------------------------------------------------------------------------------------------------------

Spike2TwitterTs <- ts(`Spike2.Twitter.26_07.tot.28_07`$V2)
Spike2BitcoinTs <- ts(`Spike2.Bitcoin.26_07.tot.28_07`$V2)

plot(Spike2BitcoinTs)

plot(Spike2TwitterTs, col="blue", ylab="Aantal Tweets", main="Twitter(Blauw) en Bitcoin(Rood) data van 26/07 tot 28/07 (72 uur)")
par(new=TRUE)
plot(Spike2BitcoinTs, col="red", axes=FALSE, xlab="", ylab="")

adf.test(Spike2TwitterTs) #0.3298
adf.test(Spike2BitcoinTs) #0.4895

Ccf(Spike2TwitterTs,Spike2BitcoinTs)
Ccf(diff(Spike2TwitterTs),diff(Spike2BitcoinTs))

#Spike3--------------------------------------------------------------------------------------------------------

Spike3TwitterTs <- ts(`Spike3.Twitter.10_08.tot.12_08`$V2)
Spike3BitcoinTs <- ts(`Spike3.Bitcoin.10_08.tot.12_08`$V2)

plot(Spike3BitcoinTs)

plot(Spike3TwitterTs, col="blue", ylab="Aantal Tweets", main="Twitter(Blauw) en Bitcoin(Rood) data van 10/08 tot 12/08 (72 uur)")
par(new=TRUE)
plot(Spike3BitcoinTs, col="red", axes=FALSE, xlab="", ylab="")

cor(Spike1TwitterTs, Spike1BitcoinTs)#0.1840796
cor(Spike2TwitterTs, Spike2BitcoinTs)#0.2298817
cor(Spike3TwitterTs, Spike3BitcoinTs)#-0.3205833

adf.test(Spike3TwitterTs) #0.1745
adf.test(Spike3BitcoinTs) #0.5467

Ccf(Spike3TwitterTs,Spike3BitcoinTs)
Ccf(diff(Spike3TwitterTs),diff(Spike3BitcoinTs))