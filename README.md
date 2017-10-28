# Burstcoin CG-Wallet

The world's first HDD-mined cryptocurrency using an energy efficient
and fair Proof-of-Capacity (PoC) consensus algorithm.

This wallet version is developed and maintained by the PoC consortium (PoCC) and supports several database backends:
- MariaDB (recommended, but complex installation)
- Firebird (2nd best choice)
- H2 (for compatibility/migration purposes)


### Software Installation

#### Linux (Debian, Ubuntu)

Please take a look at http://package.cryptoguru.org/ where you can
find the burstcoincg package. This will take care of the MariaDB
installation for you.

#### Windows

If you are not familiar with MariaDB we recommend you to go for
Firebird, in which case the following data needs to be added to
`conf/nxt.properties`

```
nxt.dbUrl=jdbc:firebirdsql:embedded:burst.firebird.db
nxt.dbUsername=sysdba
nxt.dbPassword=
```
After that you can simply run `burst.cmd` and that should start your wallet.

You can get all additional commands available by running `burst.cmd help`

#### Unix-like systems

Please install Java 8 (JRE 1.8) manually and run it by using burst.sh
You can get further information calling `burst.sh help`

Please note: Firebird (embedded) needs some more work on macOS at the moment.
All other supported databases should work as expected.

##### Configure and Initialize MariaDB

The Debian and Ubuntu packages provide an automatic configuration of
your local mariadb server. If you can't use the packages, you have to
initialize your database with these statements:

```
echo "CREATE DATABASE burstwallet; 
      CREATE USER 'burstwallet'@'localhost' IDENTIFIED BY 'yourpassword';
      GRANT ALL PRIVILEGES ON burstwallet.* TO 'burstwallet'@'localhost';" | mysql -uroot
mysql -uroot burstwallet < init-mysql.sql
```

##### Configure your Wallet

Now you need to add the following stuff to your conf/nxt.properties:

```
nxt.dbUrl=jdbc:mariadb://localhost:3306/burstwallet
echo nxt.dbUsername=burstwallet
nxt.dbPassword=yourpassword
```

## Striking Features

- Proof of Capacity - ASIC proof / Energy efficient mining
- Fast sync. with multithread CPU or OpenCL/GPU (optional)
- Even faster "Quicksync" (load/store DB dumps)
- Turing-complete smart contracts, via Automated Transactions (AT) https://ciyam.org/at/at.html
- Asset Exchange and Digital Goods Store
- Advanced transactions: Escrow and Subscription
- Encrypted Messaging
- No ICO
- No Premine

## Specification

- 4 minute block time
- 2,158,812,800 coins total (see https://burstwiki.org/wiki/Block_Reward)
- Block reward starts at 10,000/block
- Block Reward Decreases at 5% each month

## Version History

For a general overview of Burst history see https://burstwiki.org/wiki/History_of_Burst

```
2017/10/28 1.3.6cg multi-DB support: added Firebird, re-added H2; support for quick binary dump and load
2017/09/04 1.3.4cg improved database deployment; bugfix: utf8 encoding
2017/08/11 1.3.2cg 1st official PoCC release: MariaDB backend based on 1.2.9
```
For a detailed version history of wallets up to 1.2.9 see https://github.com/burst-team/burstcoin/releases

## Build

Burstcoin can be build from source using maven or - preferably - via
the provided `burst.sh compile` script within this repository.

## Links

For further information, please visit the following pages.

Info: https://www.burst-coin.org/

Wiki: https://burstwiki.org

Explorer: https://explore.burst.cryptoguru.org

Forum1: http://burst-team.us/

Forum2: https://forums.getburst.net/

### Bitcointalk

https://bitcointalk.org/index.php?topic=1541310 *(New unmoderated)*

https://bitcointalk.org/index.php?topic=1323657 *(Alternative moderated)*

https://bitcointalk.org/index.php?topic=731923 *(Original unmoderated)*

### Related repositories

https://github.com/rico666/cg_obup *CryptoGuru Optimized BURSTcoin Unix Plotter (Linux)*

https://github.com/Blagodarenko  *Blago's XPlotter, Windows Miner, PlotsChecker, etc.*

https://github.com/de-luxe *GPU assisted jMiner, Faucet Software, Observer, AddressGenerator*

https://github.com/Creepsky/creepMiner *C++ Crossplatform Miner*

https://github.com/BurstTools/BurstSoftware *Windows Plot Generator for SEE4/AVX2*

https://github.com/bhamon *gpuPlotGenerator, BurstMine (graphical plotter/miner)*

https://github.com/kartojal *GUI for Dcct Tools, GUI for gpuPlotGenerator (Linux)*

https://github.com/Kurairaito *Burst Plot Generator by Kurairaito*

https://github.com/uraymeiviar *C Miner, Pool, Block Explorer, Plot Composer (Linux)*

https://github.com/mrpsion/burst-mining-system *Web interface for Plotting and Mining*

### Additional Software
https://forums.burst-team.us/category/9/burst-software
