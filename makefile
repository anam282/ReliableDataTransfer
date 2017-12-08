JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class: 
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        Sender.java \
        Receiver.java \
        Packet.java \
        Host.java \
        RDTSender.java \
        GoBackNSender.java \
        SelectiveRepeatSender.java

all: classes

default: classes

classes: $(CLASSES:.java=.class)

clean: 
	$(RM) *.class *.log recvInfo channelInfo *.tmp
