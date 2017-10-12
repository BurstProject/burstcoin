# For Emacs: -*- mode:cperl; eval: (folding-mode 1); coding:utf-8; -*-
package BURST::API::RequestTypes;

# {{{ use block

use warnings;
use strict;
use utf8;
use feature ':5.12';

# }}}

our @ISA = qw(Exporter);
our @EXPORT = qw($reqtypes
            );


# skip: default 0 (if to skip that test)
# type: default GET
# args: default none/undef
our $reqtypes = [ # we want to define a sequence of tests
    # {{{ getAllAssets
    {
        name => 'getAllAssets',
        required => {
            assets => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getAllOpenAskOrders
    {
        name => 'getAllOpenAskOrders',
        required => {
            openOrders => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getAllOpenBidOrders
    {
        name => 'getAllOpenBidOrders',
        required => {
            openOrders => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getAssetIds
    {
        name => 'getAssetIds',
        required => {
            assetIds => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getATIds
    {
        name => 'getATIds',
        required => {
            atIds => {
                type => '//arr',
                contents => '//int'
            },
            requestProcessingTime => '//int',
        },

    },
    # }}}
    # {{{ getBlock
    {
        name => 'getBlock',
        required => {
            previousBlockHash => '//str',
            payloadLength => '//int',
            totalAmountNQT => '//int',
            generationSignature => '//str',
            generator => '//int',
            generatorPublicKey => '//str',
            baseTarget => '//int',
            payloadHash => '//str',
            generatorRS => '//str',
            blockReward => '//int',
            requestProcessingTime => '//int',
            scoopNum => '//int',
            numberOfTransactions => '//int',
            blockSignature => '//str',
            transactions => {
                type => '//arr',
                contents => '//int',
            },
            nonce => '//int',
            version => '//int',
            totalFeeNQT => '//int',
            previousBlock => '//int',
            block => '//int',
            height => '//int',
            timestamp => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getBlockchainStatus
    {
        name => 'getBlockchainStatus',
        required => {
            lastBlock => '//int',
            application => {
                type => '//str',
                value => "NRS",
            },
            time       => '//int',
            version    => '//str',
            isScanning => '//bool',
            cumulativeDifficulty => '//int',
            lastBlockchainFeederHeight => '//int',
            numberOfBlocks => '//int',
            requestProcessingTime => '//int',
            lastBlockchainFeeder => '//str',
        },
    },
    # }}}
    # {{{ getBlocks
    {
        name => 'getBlocks',
        required => {
            blocks => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getConstants
    {
        skip => 1,
        name => 'getConstants',
        required => {
            maxBlockPayloadLength => {
                type => '//int',
                value => 44880,
            },
            maxArbitraryMessageLength => {
                type => '//int',
                value => 1000,
            },
            genesisBlockId => {
                type => '//int',
                value => 3444294670862540038,
            },
            "transactionTypes" => '//arr',
        },
    },
    # }}}
    # {{{ getDGSGoods
    {
        name => 'getDGSGoods',
        required => {
            goods => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getDGSPurchases
    {
        name => 'getDGSPurchases',
        required => {
            purchases => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getECBlock
    {
        name => 'getECBlock',
        required => {
            ecBlockHeight => '//int',
            ecBlockId => '//int',
            timestamp => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getMiningInfo
    {
        name => 'getMiningInfo',
        required => {
            baseTarget   => '//int',
            height => '//int',
            generationSignature => '//str',
                requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getMyInfo
    {
        name => 'getMyInfo',
        required => {
            address => '//str',
            host    => '//str',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getPeers
    {
        name => 'getPeers',
        required => {
            peers => {
                type     => '//arr',
                contents => '//str',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getState
    {
        name => 'getState',
        required => {
            numberOfPeers => '//int',
            numberOfUnlockedAccounts => '//int',
            numberOfTransfers => '//int',
            numberOfOrders => '//int',
            numberOfTransactions => '//int',
            maxMemory => '//int',
            isScanning => '//bool',
            cumulativeDifficulty => '//int',
            numberOfAssets => '//int',
            freeMemory => '//int',
            availableProcessors => '//int',
            totalEffectiveBalanceNXT => '//int',
            numberOfAccounts => '//int',
            numberOfBlocks => '//int',
            requestProcessingTime => '//int',
            version => '//str',
            numberOfBidOrders => '//int',
            lastBlock => '//int',
            totalMemory => '//int',
            application => '//str',
            numberOfAliases => '//int',
            lastBlockchainFeederHeight => '//int',
            numberOfTrades => '//int',
            time => '//int',
            numberOfAskOrders => '//int',
            lastBlockchainFeeder => '//str',
        },
    },
    # }}}
    # {{{ getTime
    {
        name => 'getTime',
        required => {
            time => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getUnconfirmedTransactionIds
    {
        name => 'getUnconfirmedTransactionIds',
        required => {
            unconfirmedTransactionIds => {
                type => '//arr',
                contents => '//int'
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getUnconfirmedTransactions
    {
        name => 'getUnconfirmedTransactions',
        required => {
            unconfirmedTransactions => {
                type => '//arr',
                contents => '//any'
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ longConvert
    {
        name => 'longConvert',
        required => {
            stringId => {
                type => '//int',
                value => 100,
            },
            longId => {
                type => '//int',
                value => 100,
            },
            requestProcessingTime => '//int',
        },
        args => {
            id => 100,
        },
    },
    # }}}
    # {{{ rsConvert
    {
        name => 'rsConvert',
        required => {
            accountRS => {
                type => '//str',
                value => "BURST-EMXC-3PFB-J8HQ-GJ9HZ",
            },
            account => {
                    type => '//int',
                    value => 17274946210831421354,
                },
            requestProcessingTime => '//int',
        },
        args => {
            account => 17274946210831421354,
        },
    },
    # }}}
];


return 1;
