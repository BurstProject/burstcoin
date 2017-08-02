Burst ver 1.2.9

Requirements:
Java 8 compatible JVM

Setup:
Before you do anything, sync your clock.  Burst uses your computer clock's time, and having the time off too much could cause you to reject legit blocks, or miss out on blocks you could have mined. On linux "sudo ntpd -gq", on windows go to change date/time, and go to internet time and tell it to sync with a time server.

Please open conf/nxt-default.properties in a text editor, and add your ip:8123 to the line nxt.myAddress=, so it looks like "nxt.myAddress=111.111.111.111:8123", and forward 8123 if behind a NAT. This is not required, but will help the network by reducing the load on other nodes.

Usage:
Run run.sh or run.bat to start the server. The interface is accessed through a web browser on port 8125. ex: http://127.0.0.1:8125 or http://localhost:8125

Pick a long passphrase, as it is the only thing needed to access your account.
