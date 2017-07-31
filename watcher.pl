#!/usr/bin/env perl

use warnings;
use strict;

$|++;

while(1) {
    my $READ;
    my $pid = open($READ, "java -cp burst.jar:lib/*:conf nxt.Nxt 2>&1 |")  or die "Couldn't fork: $!\n";
    $pid++;
    my $kill = 0;
    print "starting PID: $pid\n";
  INNER:
    while (my $line = <$READ>) {
#        print $line;
        if ($line =~ m{SEVERE}xmsi && !$kill) {
            print "severe error, killing PID: $pid";
            kill 15, $pid;
            $kill = 1;
            close($READ);
            print "Waiting for JAVA process $pid to terminate.\n";
            last INNER;
        } 
    }

    print "$pid terminated\n";
}

# this is watcher.pl - a BURST Wallet monitoring and restarting script
#
# Simply copy this script into the directory of your wallet (where
# run.sh resides), give it execute permission (chmod a+x watcher.pl)
# and run it: ./watcher.pl
#
# It will start up your BURST wallet and monitor its output for any
# SEVERE-error messages. If such a message is detected, it will restart
# the wallet. The output on your display may look like this:
#
# starting PID: 24809
# severe error, killing PID: 24809Waiting for JAVA process 24809 to terminate.
# 24809 terminated
# starting PID: 25187
# severe error, killing PID: 25187Waiting for JAVA process 25187 to terminate.
# 25187 terminated
# starting PID: 30360
